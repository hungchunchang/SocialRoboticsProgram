"""
簡易 FastAPI 聊天伺服器
用途：在本機運行，讓學員學習 Client-Server 架構與 HTTP 通訊

使用方式：
1. pip install fastapi uvicorn openai python-dotenv
2. 在同一個目錄建立 .env 檔案，填入 OPENAI_API_KEY=sk-...
3. uvicorn server:app --host 0.0.0.0 --port 8000
4. 開啟另一個終端機，執行 python client.py
"""

import os
import json
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import google.generativeai as genai

# 載入環境變數
load_dotenv()
api_key = os.getenv("GEMINI_API_KEY")
# 2. 設定 API 金鑰
genai.configure(api_key=api_key)

# 3. 初始化模型 (推薦使用 gemini-2.5-flash，速度快且便宜/免費)
model = genai.GenerativeModel("gemini-2.5-flash")
# client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

app = FastAPI(title="Social Robot Chat Server")

# 啟用 CORS，允許 App 或瀏覽器連線
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 儲存每個使用者的對話歷史（記憶體中，重啟即清除）
conversations: dict[str, list[dict]] = {}

# ===== 可以修改這裡來設計不同的角色 =====
SYSTEM_PROMPT = """你是一個友善的社交機器人助手。
你會用繁體中文回答問題，保持禮貌並使用簡單的語言。
每次回覆請簡短，不超過 100 字。

回答格式為 JSON：
1. "reply"(string) - 你的回覆
2. "is_ended"(bool) - 對話是否結束（預設 false）
"""
# =========================================


class ChatRequest(BaseModel):
    message: str
    user_name: str = "default_user"


class ChatResponse(BaseModel):
    reply: str
    is_ended: bool = False


@app.post("/api/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    """處理聊天請求"""
    # 取得或初始化對話歷史
    if req.user_name not in conversations:
        conversations[req.user_name] = []

    history = conversations[req.user_name]
    history.append({"role": "user", "content": req.message})

    # 組合訊息
    messages = [{"role": "system", "content": SYSTEM_PROMPT}] + history

    # 呼叫 OpenAI API
    try:
        response = model.generate_content(messages)
        reply = response.text

        # 儲存助手回覆
        history.append({"role": "assistant", "content": reply})

        # is_ended= False
        return ChatResponse(reply=reply, is_ended=False)

    except Exception as e:
        return ChatResponse(reply=f"發生錯誤：{str(e)}", is_ended=False)


@app.post("/api/reset")
def reset(req: ChatRequest):
    """重置對話歷史"""
    conversations.pop(req.user_name, None)
    return {"status": "success"}


@app.get("/api/health")
def health():
    """健康檢查"""
    return {"status": "ok"}
