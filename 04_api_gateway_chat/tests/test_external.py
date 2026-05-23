import requests
import json
import time
import os
from dotenv import load_dotenv

# 載入 .env
load_dotenv(os.path.join(os.path.dirname(__file__), '../.env'))

GATEWAY_URL = os.getenv("GATEWAY_URL", "https://sociallab.duckdns.org")
API_KEY = os.getenv("API_KEY")

HEADERS = {
    "Authorization": f"Bearer {API_KEY}"
}

def test_llm_gateway(prompt):
    # Nginx 轉發 /llm/ -> http://100.97.8.89:8000/
    # 所以完整路徑應為 /llm/v1/chat/completions
    url = f"{GATEWAY_URL}/llm/v1/chat/completions"
    print(f"Testing LLM via Gateway: {url}")
    payload = {
        "model": "Qwen/Qwen2.5-7B-Instruct-AWQ",
        "messages": [{"role": "user", "content": prompt}]
    }
    start = time.time()
    resp = requests.post(url, json=payload, headers=HEADERS)
    if resp.status_code == 200:
        content = resp.json()['choices'][0]['message']['content']
        print(f"LLM Success (Time: {time.time()-start:.2f}s)")
        return content
    else:
        # 如果 /llm/v1 失敗，嘗試 /llm (FastAPI 內部的 proxy_llm 端點)
        url_alt = f"{GATEWAY_URL}/llm"
        print(f"LLM /v1 failed, trying gateway internal proxy: {url_alt}")
        resp = requests.post(url_alt, json=payload, headers=HEADERS)
        if resp.status_code == 200:
            content = resp.json()['choices'][0]['message']['content']
            print(f"LLM Success via internal proxy (Time: {time.time()-start:.2f}s)")
            return content
        
        print(f"LLM Failed: {resp.status_code} - {resp.text}")
        return None

def test_tts_gateway(text):
    print(f"Testing TTS via Gateway: {GATEWAY_URL}/tts")
    url = f"{GATEWAY_URL}/tts"
    params = {"text": text, "speaker": "ellie", "language": "ZH", "format": "mp3"}
    start = time.time()
    resp = requests.get(url, params=params, headers=HEADERS)
    if resp.status_code == 200:
        print(f"TTS Success: {len(resp.content)} bytes (Time: {time.time()-start:.2f}s)")
        return resp.content
    else:
        print(f"TTS Failed: {resp.status_code} - {resp.text}")
        return None

def test_asr_gateway(audio_data):
    print(f"Testing ASR via Gateway: {GATEWAY_URL}/asr")
    url = f"{GATEWAY_URL}/asr"
    start = time.time()
    # ASR endpoint in gateway handles bytes as body
    resp = requests.post(url, data=audio_data, headers=HEADERS)
    if resp.status_code == 200:
        text = resp.json().get("text", "")
        print(f"ASR Success: {text} (Time: {time.time()-start:.2f}s)")
        return text
    else:
        print(f"ASR Failed: {resp.status_code} - {resp.text}")
        return None

def main():
    if not API_KEY:
        print("Error: API_KEY not found in .env")
        return

    print("=== API Gateway External Test ===")
    
    # 流程: LLM -> TTS -> ASR
    text_to_say = "你好，這是一段從外網閘道測試的語音。"
    
    # 1. LLM
    reply = test_llm_gateway(text_to_say)
    
    # 2. TTS
    if reply:
        audio = test_tts_gateway(reply)
        
        # 3. ASR (驗證 MP3 轉錄能力)
        if audio:
            transcription = test_asr_gateway(audio)
            print(f"\nFinal Result: {transcription}")

if __name__ == "__main__":
    main()
