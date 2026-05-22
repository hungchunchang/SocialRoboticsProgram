import os
import requests
import json
from dotenv import load_dotenv

load_dotenv()

API_KEY = os.getenv("API_KEY")
ASR_ENDPOINT = os.getenv("ASR_ENDPOINT")
TTS_ENDPOINT = os.getenv("TTS_ENDPOINT")
LLM_ENDPOINT = os.getenv("LLM_ENDPOINT")

HEADERS = {
    "Authorization": f"Bearer {API_KEY}"
}

def call_llm(user_input, system_prompt="你是一個友善的社交機器人助手。"):
    data = {
        "model": "gpt-3.5-turbo",
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_input}
        ]
    }
    try:
        response = requests.post(LLM_ENDPOINT, headers=HEADERS, json=data)
        if response.status_code == 200:
            result = response.json()
            return result['choices'][0]['message']['content']
        else:
            return f"LLM 錯誤 ({response.status_code}): {response.text}"
    except Exception as e:
        return f"LLM 異常: {e}"

def call_tts(text):
    data = {
        "text": text,
        "voice": "zh-TW-Female"
    }
    try:
        response = requests.post(TTS_ENDPOINT, headers=HEADERS, json=data)
        if response.status_code == 200:
            return response.content
        else:
            print(f"TTS 錯誤 ({response.status_code}): {response.text}")
            return None
    except Exception as e:
        print(f"TTS 異常: {e}")
        return None

def call_asr(audio_data):
    """
    接收 binary audio data 並傳送至 ASR endpoint
    """
    try:
        files = {"file": ("audio.wav", audio_data, "audio/wav")}
        response = requests.post(ASR_ENDPOINT, headers=HEADERS, files=files)
        
        if response.status_code == 200:
            return response.json().get("text", "")
        else:
            print(f"ASR 錯誤 ({response.status_code}): {response.text}")
            return ""
    except Exception as e:
        print(f"ASR 異常: {e}")
        return ""
