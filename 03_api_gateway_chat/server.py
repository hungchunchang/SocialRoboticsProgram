import asyncio
import base64
import json
import logging
import os
import io
import wave
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI, WebSocket
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv

from vad_handler import SileroVADHandler
from api_client import call_asr, call_llm, call_tts

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("api-gateway-server")

# ── 全域狀態 ──
session_configs = {}
DEFAULT_CONFIG = {
    "llm_provider": "openai",
    "stt_provider": "openai",
    "tts_provider": "openai",
}

# ══════════════════════════════════════════════════════════════
# FastAPI HTTP Server
# ══════════════════════════════════════════════════════════════

app = FastAPI(title="API Gateway Custom VAD Server")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/api/providers")
async def get_providers():
    return {"stt": ["api-gateway"], "llm": ["api-gateway"], "tts": ["api-gateway"]}

@app.post("/api/config")
async def set_config(config: dict):
    session_id = config.get("session_id", "default")
    session_configs[session_id] = config
    return {"status": "ok", "session_id": session_id}

# ══════════════════════════════════════════════════════════════
# WebSocket Server
# ══════════════════════════════════════════════════════════════

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    logger.info(f"Client connected: {websocket.client}")
    
    vad = SileroVADHandler(sample_rate=16000)
    session_id = "default"

    try:
        while True:
            message = await websocket.receive_text()
            data = json.loads(message)
            msg_type = data.get("type")

            if msg_type == "user.login":
                session_id = data.get("user_id", "default")
                logger.info(f"User logged in: {session_id}")
            
            elif msg_type == "audio":
                payload = data.get("payload")
                if payload:
                    audio_bytes = base64.b64decode(payload)
                    # 偵測語音
                    if vad.process_chunk(audio_bytes):
                        # 語音結束，觸發 Pipeline
                        full_audio = vad.get_audio_data()
                        
                        # 包裝成 WAV 格式 (ASR endpoint 可能需要)
                        wav_buffer = io.BytesIO()
                        with wave.open(wav_buffer, "wb") as wf:
                            wf.setnchannels(1)
                            wf.setsampwidth(2)
                            wf.setframerate(16000)
                            wf.writeframes(full_audio)
                        wav_data = wav_buffer.getvalue()

                        # 執行 ASR -> LLM -> TTS
                        logger.info("Processing Pipeline...")
                        text = await asyncio.to_thread(call_asr, wav_data)
                        if text:
                            logger.info(f"ASR: {text}")
                            reply_text = await asyncio.to_thread(call_llm, text)
                            logger.info(f"LLM: {reply_text}")
                            
                            # 發送文字回應
                            await websocket.send_text(json.dumps({
                                "type": "server.text",
                                "text": reply_text
                            }))

                            # 生成語音
                            tts_audio = await asyncio.to_thread(call_tts, reply_text)
                            if tts_audio:
                                logger.info("Sending TTS audio...")
                                encoded_audio = base64.b64encode(tts_audio).decode("utf-8")
                                await websocket.send_text(json.dumps({
                                    "type": "server.audio",
                                    "payload": encoded_audio
                                }))
                        else:
                            logger.info("ASR result is empty, skipping.")

    except Exception as e:
        logger.error(f"WebSocket error: {e}")
    finally:
        logger.info("Client disconnected")

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8080)
