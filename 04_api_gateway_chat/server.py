import asyncio
import base64
import json
import logging
import os
import io
import wave
import torch
import numpy as np
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

from vad_handler import SileroVADHandler
from api_client import call_asr, call_llm, call_tts

# 讀取 api-gateway 下的 .env
load_dotenv("../../api-gateway/.env")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("api-gateway-server")

pipeline_semaphore = asyncio.Semaphore(2)

app = FastAPI(title="API Gateway Full-Duplex VAD Server")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class SessionManager:
    def __init__(self, websocket: WebSocket):
        self.websocket = websocket
        self.vad = SileroVADHandler(sample_rate=16000)
        self.is_processing = False
        self.current_task = None
        self.interrupted = False

    async def send_json(self, data: dict):
        try:
            await self.websocket.send_text(json.dumps(data))
        except:
            pass

    async def process_pipeline(self, audio_data: bytes):
        """處理 ASR -> LLM -> TTS 流程"""
        async with pipeline_semaphore:
            self.is_processing = True
            self.interrupted = False
            
            try:
                # 1. ASR
            logger.info("Pipeline: Starting ASR...")
            # 包裝成 WAV
            wav_buffer = io.BytesIO()
            with wave.open(wav_buffer, "wb") as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(16000)
                wf.writeframes(audio_data)
            wav_data = wav_buffer.getvalue()
            
            text = await asyncio.to_thread(call_asr, wav_data)
            if not text or self.interrupted:
                return

            logger.info(f"ASR Result: {text}")
            await self.send_json({"type": "server.text", "text": text, "role": "user"})

            # 2. LLM
            logger.info("Pipeline: Starting LLM...")
            reply_text = await asyncio.to_thread(call_llm, text)
            if not reply_text or self.interrupted:
                return

            logger.info(f"LLM Reply: {reply_text}")
            await self.send_json({"type": "server.text", "text": reply_text, "role": "assistant"})

            # 3. TTS
            logger.info("Pipeline: Starting TTS...")
            tts_audio = await asyncio.to_thread(call_tts, reply_text)
            if not tts_audio or self.interrupted:
                return

            # 發送語音
            encoded_audio = base64.b64encode(tts_audio).decode("utf-8")
            await self.send_json({
                "type": "server.audio",
                "payload": encoded_audio
            })
            logger.info("Pipeline: Completed.")

        except Exception as e:
            logger.error(f"Pipeline error: {e}")
        finally:
            self.is_processing = False

    def interrupt(self):
        """打斷目前的處理流程"""
        if self.is_processing:
            logger.info("Pipeline: Interrupted by user speech.")
            self.interrupted = True
            # 在這裡可以加入更強制的打斷邏輯，例如取消目前正在執行的 Task
            if self.current_task:
                self.current_task.cancel()

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    session = SessionManager(websocket)
    logger.info(f"Client connected: {websocket.client}")

    try:
        while True:
            message = await websocket.receive_text()
            data = json.loads(message)
            msg_type = data.get("type")

            if msg_type == "audio":
                payload = data.get("payload")
                if payload:
                    audio_bytes = base64.b64decode(payload)
                    
                    # 偵測是否有人在說話
                    speech_ended = session.vad.process_chunk(audio_bytes)
                    
                    # 如果偵測到開始說話，且目前正在處理之前的回應，則打斷
                    if session.vad.is_speaking and session.is_processing:
                        session.interrupt()
                        await session.send_json({"type": "server.interrupt"})

                    if speech_ended:
                        # 語音結束，獲取完整音訊並啟動 Pipeline
                        full_audio = session.vad.get_audio_data()
                        if len(full_audio) > 3200: # 至少要有 100ms 的有效音訊
                            session.current_task = asyncio.create_task(session.process_pipeline(full_audio))

            elif msg_type == "user.login":
                logger.info(f"User logged in: {data.get('user_id')}")
                await session.send_json({"type": "server.ready"})

    except WebSocketDisconnect:
        logger.info("Client disconnected")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
    finally:
        if session.current_task:
            session.current_task.cancel()

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
