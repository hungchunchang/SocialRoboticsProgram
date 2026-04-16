import os
import json
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from openai import OpenAI

# 載入環境變數
load_dotenv()
# 注意：這裡請確保你的 .env 檔案中 key 名稱與程式碼一致
api_key = os.getenv("OPENAI_API_KEY")

# 初始化 OpenAI Client
client = OpenAI(api_key=api_key)

app = FastAPI(title="Social Robot Chat Server")

# 啟用 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 儲存對話歷史（OpenAI 的格式為 {"role": "system/user/assistant", "content": "..."}）
conversations: dict[str, list] = {}

# ===== 系統提示詞（System Instruction） =====
SYSTEM_PROMPT = """你是一個友善的社交機器人助手。
你會用繁體中文回答問題，保持禮貌並使用簡單的語言。
每次回覆請簡短，不超過 100 字。

請嚴格遵守以下 JSON 回答格式：
{
  "reply": "你的回覆文字",
  "is_ended": false
}
"""

class ChatRequest(BaseModel):
    message: str
    user_name: str = "default_user"

class ChatResponse(BaseModel):
    reply: str
    is_ended: bool = False

@app.post("/api/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    """處理聊天請求"""
    if req.user_name not in conversations:
        # 初始化時加入系統提示詞
        conversations[req.user_name] = [{"role": "system", "content": SYSTEM_PROMPT}]

    history = conversations[req.user_name]
    
    # 1. 加入使用者訊息
    history.append({"role": "user", "content": req.message})

    try:
        # 2. 呼叫 OpenAI API
        response = client.chat.completions.create(
            model="gpt-4o", # 也可以使用 gpt-4o-mini
            messages=history,
            response_format={"type": "json_object"} # 強制輸出的格式為 JSON
        )
        
        # 3. 解析 JSON 回覆
        raw_text = response.choices[0].message.content
        try:
            res_json = json.loads(raw_text)
            reply_text = res_json.get("reply", raw_text)
            is_ended = res_json.get("is_ended", False)
        except json.JSONDecodeError:
            # 如果 AI 沒有乖乖回 JSON，就直接回傳原始文字
            reply_text = raw_text
            is_ended = False

        # 4. 儲存模型回覆到歷史紀錄
        history.append({"role": "assistant", "content": raw_text})

        return ChatResponse(reply=reply_text, is_ended=is_ended)

    except Exception as e:
        return ChatResponse(reply=f"發生錯誤：{str(e)}", is_ended=False)

@app.post("/api/reset")
def reset(req: ChatRequest):
    """重置對話歷史"""
    conversations.pop(req.user_name, None)
    return {"status": "success"}

@app.get("/api/health")
def health():
    return {"status": "ok"}