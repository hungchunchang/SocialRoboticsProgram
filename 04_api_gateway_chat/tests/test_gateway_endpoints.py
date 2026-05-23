import asyncio
import base64
import json
import os
import requests
from dotenv import load_dotenv

# 加載 .env
load_dotenv("../../../api-gateway/.env")

API_KEY = os.getenv("API_KEY")
# 測試 Gateway 的本地埠口
GATEWAY_URL = "http://localhost:8000"

HEADERS = {"Authorization": f"Bearer {API_KEY}"}
DUMMY_AUDIO = b"\x00" * 3200

def test_gateway_asr(audio_data=None):
    print("\n--- [透過 Gateway 測試 ASR] ---")
    # 如果有傳入音訊則使用傳入的，否則使用 DUMMY_AUDIO
    data_to_send = audio_data if audio_data else DUMMY_AUDIO
    try:
        resp = requests.post(f"{GATEWAY_URL}/asr", headers=HEADERS, data=data_to_send, timeout=30)
        if resp.status_code == 200:
            print(f"ASR 成功: {resp.json()}")
        else:
            print(f"ASR 失敗 ({resp.status_code}): {resp.text}")
    except Exception as e:
        print(f"ASR 異常: {e}")

def test_gateway_tts():
    print("\n--- [透過 Gateway 測試 TTS (含排隊機制)] ---")
    params = {
        "text": "今天天氣真好，我們一起去散步吧。",
        "speaker": "ellie",
        "language": "ZH",
        "format": "wav" # ASR 通常對 wav 支援更好
    }
    try:
        resp = requests.get(f"{GATEWAY_URL}/tts", headers=HEADERS, params=params, timeout=60)
        if resp.status_code == 200:
            print(f"TTS 成功: 收到 {len(resp.content)} bytes")
            # 將產出的音訊存入檔案
            filename = "tts_test_output.wav"
            with open(filename, "wb") as f:
                f.write(resp.content)
            print(f"音訊已儲存至: {filename}")
            return resp.content
        else:
            print(f"TTS 失敗 ({resp.status_code}): {resp.text}")
            return None
    except Exception as e:
        print(f"TTS 異常: {e}")
        return None

def test_gateway_llm():
    print("\n--- [透過 Gateway 測試 LLM] ---")
    data = {
        "model": "Qwen/Qwen2.5-7B-Instruct-AWQ",
        "messages": [{"role": "user", "content": "你好，請用五個字內回覆『測試通過』。"}]
    }
    try:
        resp = requests.post(f"{GATEWAY_URL}/llm", headers=HEADERS, json=data, timeout=30)
        if resp.status_code == 200:
            result = resp.json()
            print(f"LLM 成功: {result['choices'][0]['message']['content']}")
        else:
            print(f"LLM 失敗 ({resp.status_code}): {resp.text}")
    except Exception as e:
        print(f"LLM 異常: {e}")

if __name__ == "__main__":
    if not API_KEY:
        print("錯誤: 找不到 API_KEY")
    else:
        # 1. 先測 LLM
        test_gateway_llm()
        
        # 2. 測 TTS 並取得產出的音訊
        audio = test_gateway_tts()
        
        # 3. 將 TTS 的結果餵給 ASR 測試，達成閉環測試
        if audio:
            test_gateway_asr(audio)
        else:
            print("跳過 TTS->ASR 閉環測試，因為 TTS 未能生成音訊。")
            test_gateway_asr()
