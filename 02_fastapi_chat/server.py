import os
import json
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from google import genai
from google.genai import types  # 引入 types 以便處理系統提示詞

# 載入環境變數
load_dotenv()
# 注意：這裡請確保你的 .env 檔案中 key 名稱與程式碼一致
api_key = os.getenv("GEMINI_API_KEY")

# 初始化 Gemini Client
client = genai.Client(api_key=api_key)

app = FastAPI(title="Social Robot Chat Server")

# 啟用 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 儲存對話歷史（Gemini 的格式為 {"role": "user/model", "parts": [{"text": "..."}]}）
conversations: dict[str, list] = {}

# ===== 系統提示詞（System Instruction） =====
# 在新的 SDK 中，System Prompt 是在生成時獨立帶入的，不放在 contents 列表裡
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
        conversations[req.user_name] = []

    history = conversations[req.user_name]
    
    # 1. 轉換為 Gemini 的內容格式 (role 使用 'user' 與 'model')
    history.append(types.Content(role="user", parts=[types.Part.from_text(text=req.message)]))

    try:
        # 2. 呼叫 Gemini API
        # config 中設定 system_instruction 與 response_mime_type
        response = client.models.generate_content(
            model="gemini-2.0-flash", # 建議使用目前的穩定版本
            contents=history,
            config=types.GenerateContentConfig(
                system_instruction=SYSTEM_PROMPT,
                response_mime_type="application/json", # 強制輸出的格式
            ),
        )
        
        # 3. 解析 JSON 回覆
        raw_text = response.text
        try:
            res_json = json.loads(raw_text)
            reply_text = res_json.get("reply", raw_text)
            is_ended = res_json.get("is_ended", False)
        except json.JSONDecodeError:
            # 如果 AI 沒有乖乖回 JSON，就直接回傳原始文字
            reply_text = raw_text
            is_ended = False

        # 4. 儲存模型回覆到歷史紀錄 (role 必須是 'model')
        history.append(types.Content(role="model", parts=[types.Part.from_text(text=raw_text)]))

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