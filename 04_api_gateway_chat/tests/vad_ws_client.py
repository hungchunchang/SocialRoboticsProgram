import asyncio
import websockets
import json
import base64
import pyaudio
import os
from dotenv import load_dotenv
from pydub import AudioSegment
from pydub.playback import play
import io

# 載入 .env
load_dotenv(os.path.join(os.path.dirname(__file__), '../.env'))

GATEWAY_WS_URL = os.getenv("GATEWAY_WS_URL", "wss://sociallab.duckdns.org/v1/conversation")
API_KEY = os.getenv("API_KEY")

# 音訊設定
FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 16000
CHUNK = 1024

async def send_audio(websocket):
    """從麥克風讀取音訊並發送到 WebSocket"""
    p = pyaudio.PyAudio()
    stream = p.open(format=FORMAT, channels=CHANNELS, rate=RATE, input=True, frames_per_buffer=CHUNK)
    
    print("Microphone active. Start speaking...")
    try:
        while True:
            data = stream.read(CHUNK, exception_on_overflow=False)
            b64_data = base64.b64encode(data).decode('utf-8')
            message = json.dumps({
                "type": "audio",
                "payload": b64_data
            })
            await websocket.send(message)
            await asyncio.sleep(0.01)
    except Exception as e:
        print(f"Error sending audio: {e}")
    finally:
        stream.stop_stream()
        stream.close()
        p.terminate()

async def receive_messages(websocket):
    """接收伺服器的文字與語音回應"""
    try:
        async for message in websocket:
            data = json.loads(message)
            msg_type = data.get("type")
            
            if msg_type == "server.text":
                role = data.get("role", "assistant")
                text = data.get("text", "")
                print(f"\n[{role.upper()}]: {text}")
                
            elif msg_type == "server.audio":
                print("Received audio response, playing...")
                audio_payload = base64.b64decode(data.get("payload"))
                # 伺服器傳回的是 MP3 格式
                audio_segment = AudioSegment.from_file(io.BytesIO(audio_payload), format="mp3")
                
                # 🌟 強制標準化格式，解決 Mac AUHAL -50 錯誤
                audio_segment = audio_segment.set_frame_rate(44100).set_channels(2)
                
                play(audio_segment)
                
    except Exception as e:
        print(f"Error receiving messages: {e}")

async def main():
    if not API_KEY:
        print("API_KEY not found")
        return

    # 注意：閘道目前 main.py 的 /v1/conversation 暫不強制檢查 token
    # 但實務上建議在連線後第一個消息傳送 Auth
    print(f"Connecting to {GATEWAY_WS_URL}...")
    try:
        async with websockets.connect(GATEWAY_WS_URL) as ws:
            print("Connected!")
            # 同時運行發送與接收
            await asyncio.gather(send_audio(ws), receive_messages(ws))
    except Exception as e:
        print(f"Connection failed: {e}")

if __name__ == "__main__":
    asyncio.run(main())
