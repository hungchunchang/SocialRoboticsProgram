#!/usr/bin/env python3
"""
Pipecat Audio Backend — 可設定的語音 AI Pipeline

兩個服務：
1. FastAPI HTTP (:8080) — 設定 API + 供應商查詢
2. Pipecat WebSocket (:8765) — 雙向音訊串流

協議相容 03_audio_websocket/backend.py，Android 端不需更動。

啟動方式：
    uvicorn backend:app --host 0.0.0.0 --port 8080
    （WebSocket server 會在背景自動啟動於 :8765）
"""

import asyncio
import base64
import json
import logging
import os
from contextlib import asynccontextmanager

import websockets
from dotenv import load_dotenv
from fastapi import FastAPI, WebSocket as FWS
from fastapi.middleware.cors import CORSMiddleware

from pipecat.pipeline.pipeline import Pipeline
from pipecat.pipeline.runner import PipelineRunner
from pipecat.pipeline.task import PipelineTask, PipelineParams
from pipecat.audio.vad.silero import SileroVADAnalyzer
from pipecat.transports.services.helpers.daily_rest import DailyRESTHelper

from pipeline_factory import (
    AVAILABLE_PROVIDERS,
    DEFAULT_CONFIG,
    create_stt,
    create_llm,
    create_tts,
    create_vad,
    merge_config,
)
from protocol_adapter import ProtocolAdapter

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("pipecat-backend")

# ── 全域狀態 ──

# session_id → config dict
session_configs: dict[str, dict] = {}

WS_HOST = "0.0.0.0"
WS_PORT = int(os.getenv("WS_PORT", "8765"))
HTTP_PORT = int(os.getenv("HTTP_PORT", "8080"))
SAMPLE_RATE = 24000


# ══════════════════════════════════════════════════════════════
# FastAPI HTTP Server（設定 API）
# ══════════════════════════════════════════════════════════════

@asynccontextmanager
async def lifespan(application: FastAPI):
    """啟動時同時啟動 WebSocket server。"""
    ws_task = asyncio.create_task(run_websocket_server())
    logger.info(f"WebSocket server starting on ws://{WS_HOST}:{WS_PORT}")
    yield
    ws_task.cancel()


app = FastAPI(title="Pipecat Audio Backend", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/providers")
async def get_providers():
    """回傳可用的 STT / LLM / TTS 供應商清單。"""
    return AVAILABLE_PROVIDERS


@app.post("/api/config")
async def set_config(config: dict):
    """
    接收 Android 端的 pipeline 設定。
    Android 在開啟 WebSocket 前先呼叫此 API。
    """
    session_id = config.get("session_id", "default")
    merged = merge_config(config)
    session_configs[session_id] = merged

    logger.info(f"Config saved for session '{session_id}': "
                f"LLM={merged['llm_provider']}/{merged['llm_model']}, "
                f"STT={merged['stt_provider']}, "
                f"TTS={merged['tts_provider']}/{merged['tts_voice']}")

    return {
        "status": "ok",
        "session_id": session_id,
        "ws_url": f"ws://{WS_HOST}:{WS_PORT}",
    }


@app.get("/api/config/{session_id}")
async def get_config(session_id: str):
    """查詢某個 session 的目前設定。"""
    config = session_configs.get(session_id, DEFAULT_CONFIG)
    return config


# ══════════════════════════════════════════════════════════════
# WebSocket Server（音訊串流）
# ══════════════════════════════════════════════════════════════

async def run_websocket_server():
    """啟動 WebSocket server，處理音訊串流。"""
    async with websockets.serve(handle_client, WS_HOST, WS_PORT):
        logger.info(f"WebSocket server listening on ws://{WS_HOST}:{WS_PORT}")
        await asyncio.Future()  # run forever


async def handle_client(websocket):
    """處理單一客戶端連線。"""
    addr = websocket.remote_address
    logger.info(f"[WS] Client connected: {addr}")

    session_id = "default"
    config = dict(DEFAULT_CONFIG)
    adapter = ProtocolAdapter(websocket=None)

    try:
        async for raw in websocket:
            try:
                msg = json.loads(raw)
            except json.JSONDecodeError:
                continue

            msg_type = msg.get("type", "")

            if msg_type == "user.login":
                # 登入：查找對應的 config
                user_id = msg.get("user_id", "default")
                session_id = user_id
                config = session_configs.get(session_id, dict(DEFAULT_CONFIG))
                logger.info(f"[WS] User login: {user_id}, config: {config.get('llm_provider')}/{config.get('llm_model')}")
                await websocket.send(json.dumps({"type": "user.login.success"}))

            elif msg_type == "input_audio_buffer.start":
                logger.info("[WS] Audio buffer start")

            elif msg_type == "input_audio_buffer.append":
                # 接收音訊 chunk — 在完整 Pipecat 整合中，
                # 這些會直接餵入 pipeline 的 transport input
                pass

            elif msg_type == "input_audio_buffer.stop":
                logger.info("[WS] Audio buffer stop — processing with pipeline")
                # 使用 Pipecat pipeline 處理
                await process_with_pipeline(websocket, config)

            elif msg_type == "input.message":
                # 文字訊息（備用）
                text = msg.get("message", "")
                logger.info(f"[WS] Text message: {text}")
                await process_text_with_pipeline(websocket, config, text)

            elif msg_type == "client.disconnect":
                logger.info("[WS] Client disconnect requested")
                break

    except websockets.exceptions.ConnectionClosed as e:
        logger.info(f"[WS] Client disconnected: {addr} ({e.code})")


async def process_with_pipeline(websocket, config: dict):
    """
    用 Pipecat pipeline 處理音訊。

    注意：這是簡化版本。完整整合時，應該在連線時就建立持久的 pipeline，
    並持續將音訊 frame 餵入。這裡為了教學清晰，每次 stop 時建立新 pipeline。
    """
    try:
        # 建立 pipeline 元件
        stt = create_stt(config["stt_provider"])
        llm = create_llm(
            config["llm_provider"],
            config["llm_model"],
            config["system_prompt"],
            config.get("temperature", 0.7),
        )
        tts = create_tts(config["tts_provider"], config["tts_voice"])
        adapter = ProtocolAdapter()
        adapter.set_websocket(websocket)

        # 建立 pipeline
        pipeline = Pipeline([stt, llm, tts, adapter])
        runner = PipelineRunner()
        task = PipelineTask(pipeline, PipelineParams(
            audio_out_sample_rate=SAMPLE_RATE,
            audio_out_enabled=True,
        ))

        await runner.run(task)

    except Exception as e:
        logger.error(f"Pipeline error: {e}")
        # 回退：送出錯誤訊息
        await websocket.send(json.dumps({
            "type": "response.emotion_transcript",
            "emotion": "neutral",
            "transcript": f"Pipeline 錯誤：{str(e)}",
        }))


async def process_text_with_pipeline(websocket, config: dict, text: str):
    """處理文字訊息（跳過 STT，直接送入 LLM）。"""
    try:
        llm = create_llm(
            config["llm_provider"],
            config["llm_model"],
            config["system_prompt"],
            config.get("temperature", 0.7),
        )
        tts = create_tts(config["tts_provider"], config["tts_voice"])
        adapter = ProtocolAdapter()
        adapter.set_websocket(websocket)

        pipeline = Pipeline([llm, tts, adapter])
        runner = PipelineRunner()
        task = PipelineTask(pipeline, PipelineParams(
            audio_out_sample_rate=SAMPLE_RATE,
            audio_out_enabled=True,
        ))

        # 將文字注入 pipeline
        from pipecat.frames.frames import TextFrame
        await task.queue_frame(TextFrame(text=text))

        await runner.run(task)

    except Exception as e:
        logger.error(f"Text pipeline error: {e}")
        await websocket.send(json.dumps({
            "type": "response.emotion_transcript",
            "emotion": "neutral",
            "transcript": f"處理錯誤：{str(e)}",
        }))


# ══════════════════════════════════════════════════════════════
# Entry Point
# ══════════════════════════════════════════════════════════════

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=HTTP_PORT)
