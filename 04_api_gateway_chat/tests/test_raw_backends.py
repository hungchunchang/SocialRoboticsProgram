import asyncio
import base64
import json
import os
import requests
import websockets
from dotenv import load_dotenv

# 加載 .env
load_dotenv("../../../api-gateway/.env")

# --- 設定 (繞過 Gateway，直接測後端) ---
ASR_WS_URL = "ws://localhost:9000"
TTS_URL = "http://localhost:9001/generate/"
LLM_URL = "http://100.97.8.89:8000/v1/chat/completions"

# 生成 100ms 的靜音做為測試 ASR 用的音訊
DUMMY_AUDIO = b"\x00" * 3200

async def test_raw_asr():
    print("\n--- [直接測試 ASR 後端 (WS: 9000)] ---")
    try:
        async with websockets.connect(ASR_WS_URL) as ws:
            await ws.send(DUMMY_AUDIO)
            text = await ws.recv()
            print(f"ASR 結果: '{text}' (預期為空或雜訊辨識)")
    except Exception as e:
        print(f"ASR 失敗: {e}")

def test_raw_tts():
    print("\n--- [直接測試 TTS 後端 (HTTP: 9001)] ---")
    params = {
        "text": "這是直接連線後端的 TTS 測試。",
        "speaker": "ellie",
        "language": "ZH",
        "format": "mp3"
    }
    try:
        resp = requests.get(TTS_URL, params=params, timeout=10)
        if resp.status_code == 200:
            print(f"TTS 成功: 收到 {len(resp.content)} bytes")
        else:
            print(f"TTS 失敗 ({resp.status_code}): {resp.text}")
    except Exception as e:
        print(f"TTS 異常: {e}")

def test_raw_llm():
    print("\n--- [直接測試 LLM 後端 (HTTP: 8000)] ---")
    data = {
        "model": "Qwen/Qwen2.5-7B-Instruct-AWQ",
        "messages": [{"role": "user", "content": "你好，請用五個字內回覆，並且只能用繁體中文。"}]
    }
    try:
        resp = requests.post(LLM_URL, json=data, timeout=10)
        if resp.status_code == 200:
            result = resp.json()
            print(f"LLM 回應: {result['choices'][0]['message']['content']}")
        else:
            print(f"LLM 失敗 ({resp.status_code}): {resp.text}")
    except Exception as e:
        print(f"LLM 異常: {e}")

if __name__ == "__main__":
    asyncio.run(test_raw_asr())
    test_raw_tts()
    test_raw_llm()
