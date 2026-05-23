from locust import HttpUser, task, between, events
import os
from dotenv import load_dotenv
import random

# 載入 .env
load_dotenv(os.path.join(os.path.dirname(__file__), '../.env'))

API_KEY = os.getenv("API_KEY")

class GatewayUser(HttpUser):
    wait_time = between(1, 3)  # 每個用戶任務間隔 1~3 秒
    
    def on_start(self):
        """初始化：設定 Headers 並獲取一個測試音訊片段"""
        self.headers = {"Authorization": f"Bearer {API_KEY}"}
        # 預先拿一個 TTS 音訊，用來壓測 ASR
        resp = self.client.get("/tts?text=壓力測試音訊&speaker=ellie", headers=self.headers)
        if resp.status_code == 200:
            self.test_audio = resp.content
        else:
            self.test_audio = b"fake audio data"

    @task(3)
    def test_llm(self):
        payload = {
            "model": "Qwen/Qwen2.5-7B-Instruct-AWQ",
            "messages": [{"role": "user", "content": f"隨機數: {random.randint(1,1000)}，請回覆 OK"}]
        }
        self.client.post("/llm", json=payload, headers=self.headers, name="/llm")

    @task(2)
    def test_tts(self):
        self.client.get("/tts?text=正在進行併發測試&speaker=ellie", headers=self.headers, name="/tts")

    @task(1)
    def test_asr(self):
        if hasattr(self, 'test_audio'):
            self.client.post("/asr", data=self.test_audio, headers=self.headers, name="/asr")

# 執行指令: locust -f tests/locustfile.py --host https://sociallab.duckdns.org
