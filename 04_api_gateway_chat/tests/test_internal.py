import requests
import asyncio
import websockets
import json
import time

# --- 配置 (內網直連) ---
ASR_WS_URL = "ws://192.168.50.208:9000"
TTS_HTTP_URL = "http://192.168.50.208:9001/generate/"
LLM_HTTP_URL = "http://100.97.8.89:8000/v1/chat/completions"

async def test_asr_direct(audio_data):
    print(f"Testing ASR Direct: {ASR_WS_URL}")
    start = time.time()
    async with websockets.connect(ASR_WS_URL) as ws:
        await ws.send(audio_data)
        result = await ws.recv()
    print(f"ASR Result: {result} (Time: {time.time()-start:.2f}s)")
    return result

def test_tts_direct(text):
    print(f"Testing TTS Direct: {TTS_HTTP_URL}")
    start = time.time()
    params = {
        "text": text,
        "speaker": "ellie",
        "language": "ZH",
        "format": "mp3"
    }
    resp = requests.get(TTS_HTTP_URL, params=params)
    if resp.status_code == 200:
        print(f"TTS Success: {len(resp.content)} bytes (Time: {time.time()-start:.2f}s)")
        return resp.content
    else:
        print(f"TTS Failed: {resp.status_code} - {resp.text}")
        return None

def test_llm_direct(prompt):
    print(f"Testing LLM Direct: {LLM_HTTP_URL}")
    start = time.time()
    payload = {
        "model": "Qwen/Qwen2.5-7B-Instruct-AWQ",
        "messages": [{"role": "user", "content": prompt}]
    }
    resp = requests.post(LLM_HTTP_URL, json=payload)
    if resp.status_code == 200:
        result = resp.json()
        content = result['choices'][0]['message']['content']
        print(f"LLM Result: {content[:50]}... (Time: {time.time()-start:.2f}s)")
        return content
    else:
        print(f"LLM Failed: {resp.status_code} - {resp.text}")
        return None

async def main():
    print("=== Internal Endpoints Direct Test ===")
    
    # 1. Test LLM
    reply = test_llm_direct("你好，請用一句話自我介紹。")
    
    # 2. Test TTS (Generate MP3)
    if reply:
        audio_mp3 = test_tts_direct(reply)
        
        # 3. Test ASR with the MP3 from TTS (Crucial part of Step 2)
        if audio_mp3:
            print("\nFeeding TTS-generated MP3 into ASR...")
            asr_text = await test_asr_direct(audio_mp3)
            print(f"Final Verification - ASR transcribed TTS output as: {asr_text}")

if __name__ == "__main__":
    asyncio.run(main())
