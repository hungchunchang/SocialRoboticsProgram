import asyncio
import base64
import json
import time
import websockets
import aiohttp
import os
import random
from dotenv import load_dotenv

# 直接讀取 api-gateway 下的 .env
load_dotenv("../../../api-gateway/.env")

# --- 配置 ---
API_KEY = os.getenv("API_KEY")
GATEWAY_URL = os.getenv("GATEWAY_URL", "https://sociallab.duckdns.org")
# 壓力測試連線到本地正在運行的 VAD Server
WS_URL = "ws://localhost:8081/ws"

class UserTester:
    def __init__(self, api_key):
        self.api_key = api_key

    async def stress_test_session(self, session_idx):
        """單一使用者連線的壓力測試流程"""
        if not self.api_key:
            print(f"[Client {session_idx}] 錯誤: 沒有 API Key")
            return

        start_time = time.time()
        try:
            async with websockets.connect(WS_URL) as ws:
                # 1. Login
                await ws.send(json.dumps({
                    "type": "user.login",
                    "user_id": f"stress_test_{session_idx}",
                    "api_key": self.api_key
                }))
                
                # 2. 發送模擬音訊 (1s, 10 chunks)
                for _ in range(10):
                    noise = os.urandom(3200) 
                    await ws.send(json.dumps({
                        "type": "audio",
                        "payload": base64.b64encode(noise).decode("utf-8")
                    }))
                    await asyncio.sleep(0.1)

                # 發送靜音以觸發 VAD End
                silence = b"\x00" * 16000 
                await ws.send(json.dumps({
                    "type": "audio",
                    "payload": base64.b64encode(silence).decode("utf-8")
                }))

                # 3. 等待回應
                while True:
                    try:
                        msg = await asyncio.wait_for(ws.recv(), timeout=30)
                        data = json.loads(msg)
                        if data.get("type") == "server.text":
                            print(f"[Client {session_idx}] 機器人回覆文字: {data.get('text')}")
                        elif data.get("type") == "server.audio":
                            print(f"[Client {session_idx}] 收到語音回覆 (長度: {len(data.get('payload'))})")
                            break
                    except asyncio.TimeoutError:
                        print(f"[Client {session_idx}] 等待回應逾時")
                        break

        except Exception as e:
            print(f"[Client {session_idx}] 連線出錯: {e}")
        
        return time.time() - start_time

async def main():
    if not API_KEY:
        print("錯誤: 找不到 API_KEY，請先執行 get_api_key.sh")
        return

    print("=== API Gateway 壓力測試 (使用已有的 API Key) ===")
    print(f"使用的 API Key: {API_KEY[:10]}...")
    
    tester = UserTester(API_KEY)
    num_clients = 10
    print(f"\n啟動 {num_clients} 個併發 WebSocket 連線...")
    
    tasks = [tester.stress_test_session(i) for i in range(num_clients)]
    
    start_total = time.time()
    durations = await asyncio.gather(*tasks)
    end_total = time.time()
    
    valid_durations = [d for d in durations if d is not None]
    
    print("\n" + "="*40)
    print(f"壓力測試結果：")
    print(f"併發數量: {num_clients}")
    print(f"總執行時間: {end_total - start_total:.2f}s")
    if valid_durations:
        print(f"平均響應時間: {sum(valid_durations)/len(valid_durations):.2f}s")
    print("="*40)

if __name__ == "__main__":
    asyncio.run(main())
