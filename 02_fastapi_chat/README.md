---
marp: true
theme: default
paginate: true
backgroundColor: #ffffff
color: #2c3e50
style: |
  section {
    font-family: 'Segoe UI', 'PingFang TC', 'Microsoft YaHei', sans-serif;
    font-size: 28px;
    line-height: 1.6;
    padding: 60px;
  }
  h1 {
    color: #2c3e50;
    text-align: center;
    font-size: 48px;
    font-weight: 700;
    margin-bottom: 40px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    text-shadow: 0 2px 4px rgba(0,0,0,0.1);
  }
  h2 {
    color: #3498db;
    font-size: 36px;
    font-weight: 600;
    border-left: 6px solid #3498db;
    padding-left: 20px;
    margin: 30px 0 20px 0;
    background: linear-gradient(90deg, rgba(52,152,219,0.1) 0%, rgba(255,255,255,0) 100%);
    padding: 15px 0 15px 20px;
    border-radius: 0 8px 8px 0;
  }
  h3 {
    color: #27ae60;
    font-size: 30px;
    font-weight: 500;
    margin: 20px 0 15px 0;
  }
  .highlight {
    background: linear-gradient(135deg, #fff3cd 0%, #ffeaa7 100%);
    padding: 20px;
    border-radius: 12px;
    border-left: 5px solid #fdcb6e;
    margin: 20px 0;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  }
  .method-box {
    background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
    padding: 25px;
    border-radius: 12px;
    margin: 20px 0;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    border: 1px solid #90caf9;
  }
  .info-card {
    background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
    padding: 25px;
    border-radius: 15px;
    margin: 20px 0;
    box-shadow: 0 6px 20px rgba(0,0,0,0.1);
    border: 2px solid #dee2e6;
  }
  .columns {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 2rem;
    margin: 20px 0;
  }
  .tricolumns {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1.5rem;
    margin: 20px 0;
  }
  .accent-text {
    color: #e74c3c;
    font-weight: 600;
  }
  .subtitle {
    color: #7f8c8d;
    font-size: 22px;
    text-align: center;
    margin-top: -20px;
    margin-bottom: 40px;
  }
  ul, ol {
    margin: 15px 0;
    padding-left: 30px;
  }
  li {
    margin: 10px 0;
    line-height: 1.5;
  }
  strong {
    color: #2c3e50;
    font-weight: 600;
  }
  code {
    background: #f0f0f0;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 0.9em;
  }
  pre code {
    background: none;
    padding: 0;
  }
---

# 01 - FastAPI Chat

<div class="subtitle">用 Python 建立你的第一個聊天機器人 API</div>

<div class="info-card">

**本節重點**

- 理解 Client-Server 架構
- 認識 HTTP 協定與 REST API
- 用 FastAPI 建立後端伺服器
- 用 Python 撰寫客戶端

</div>

---

## 什麼是 Client-Server？

<div class="columns">

<div class="method-box">

### Client（客戶端）

發出請求的一方

- 瀏覽器
- 手機 App
- Python 程式

</div>

<div class="method-box">

### Server（伺服器）

回應請求的一方

- 接收請求
- 處理邏輯
- 回傳結果

</div>

</div>

> 就像在餐廳：客人（Client）點餐 → 廚房（Server）做菜 → 送回給客人

---

## 什麼是 HTTP？

HTTP（HyperText Transfer Protocol）是網路上最常見的通訊協定。

<div class="info-card">

**常見的 HTTP 方法**

| 方法 | 用途 | 例子 |
|------|------|------|
| **GET** | 取得資料 | 瀏覽網頁 |
| **POST** | 送出資料 | 送出表單、傳送訊息 |
| **PUT** | 更新資料 | 修改個人資料 |
| **DELETE** | 刪除資料 | 刪除帳號 |

</div>

---

## 什麼是 REST API？

REST API 是一種設計風格，讓 Client 和 Server 用 **URL + HTTP 方法** 來溝通。

<div class="highlight">

**本專案的 API 端點**

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/chat` | POST | 傳送訊息，取得機器人回覆 |
| `/api/reset` | POST | 重置對話歷史 |
| `/api/health` | GET | 檢查伺服器狀態 |

</div>

---

## 專案架構

```
01_fastapi_chat/
├── server.py          ← 伺服器（FastAPI）
├── client.py          ← 客戶端（Python CLI）
├── requirements.txt   ← 依賴套件
└── .env               ← API 金鑰（需自行建立）
```

<div class="info-card">

**運作流程**

1. `server.py` 啟動，監聽 `http://0.0.0.0:8000`
2. `client.py` 透過 HTTP POST 發送使用者訊息
3. Server 呼叫 OpenAI API 產生回覆
4. Server 將回覆以 JSON 格式回傳給 Client

</div>

---

## 實作步驟一：環境準備

### 1. 啟動 Conda 環境

在終端機中：

```bash
conda activate socialrobot
```

### 2. 安裝依賴套件

```bash
cd 01_fastapi_chat
pip install -r requirements.txt
```

會使用 `pip` 的依賴管理套件，逐個安裝 `requirements.txt` 文件中的每個資源庫

---

## 實作步驟二：設定 API Key

### 建立 `.env` 檔案

回到 VSCode，在 `01_fastapi_chat/` 資料夾中建立 `.env` 檔案：

```
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
```

其中，你要把`sk-xxxxxxxxxxxxxxxxxxxxxxxx`替換成你自己的 key

<div class="highlight">

**注意事項**

- `.env` 檔案不應上傳到 GitHub（已加入 `.gitignore`）
- API Key 可從 [platform.openai.com](https://platform.openai.com) 取得
- 請妥善保管，不要分享給他人

</div>

---

## 程式碼解說：server.py（1/3）

### 初始化與設定

```python
from fastapi import FastAPI
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()  # 從 .env 讀取 API Key
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

app = FastAPI(title="Social Robot Chat Server")
```

- `load_dotenv()` 會自動讀取 `.env` 檔案中的環境變數
- `OpenAI()` 建立 OpenAI API 客戶端

---

## 程式碼解說：server.py（2/3）

### System Prompt —— 定義機器人的角色

```python
SYSTEM_PROMPT = """你是一個友善的社交機器人助手。
你會用繁體中文回答問題，保持禮貌並使用簡單的語言。
每次回覆請簡短，不超過 100 字。

回答格式為 JSON：
1. "reply"(string) - 你的回覆
2. "is_ended"(bool) - 對話是否結束（預設 false）
"""
```

<div class="highlight">

**自訂重點**：修改 `SYSTEM_PROMPT` 就能改變機器人的個性和行為！

</div>

---

## 程式碼解說：server.py（3/3）

### 聊天 API 端點

```python
@app.post("/api/chat")
def chat(req: ChatRequest):
    history = conversations[req.user_name]
    history.append({"role": "user", "content": req.message})

    messages = [{"role": "system", "content": SYSTEM_PROMPT}] + history

    completion = client.chat.completions.create(
        model="gpt-4o",
        messages=messages,
    )
    # 解析 JSON 回覆 → 回傳 {reply, is_ended}
```

- 每個使用者有獨立的對話歷史
- 每次請求都會把完整歷史送給 OpenAI

---

## 程式碼解說：client.py

### 客戶端核心邏輯

```python
SERVER_URL = "http://localhost:8000"

def chat(message: str) -> dict:
    response = requests.post(
        f"{SERVER_URL}/api/chat",
        json={"message": message, "user_name": USER_NAME},
    )
    return response.json()
```

- 使用 `requests.post()` 發送 HTTP POST 請求
- 傳送 JSON 格式的訊息，接收 JSON 格式的回覆

---

## 實作步驟三：啟動與測試

### 終端機 1 —— 啟動伺服器

```bash
conda activate socialrobot
uvicorn server:app --host 0.0.0.0 --port 8000
```

### 終端機 2 —— 啟動客戶端

```bash
conda activate socialrobot
python client.py
```

<div class="highlight">

啟動後可以在終端機直接輸入訊息和機器人對話！
輸入 `quit` 結束，輸入 `reset` 重置對話。

</div>

---

## 實作步驟四：互動 API 文件

FastAPI 自動產生互動式 API 文件：

- **Swagger UI**：`http://localhost:8000/docs`
- **ReDoc**：`http://localhost:8000/redoc`

<div class="info-card">

打開瀏覽器前往 `/docs`，可以直接在網頁上測試 API，不需要寫程式！

</div>

---

## 練習任務

<div class="method-box">

### 修改 SYSTEM_PROMPT

試著把機器人改成不同的角色，例如：
- 導覽員：介紹校園景點
- 心理師：簡單的情緒支持
- 故事大王：根據使用者的話編故事

</div>

<div class="method-box">

### 連線到同學的伺服器

修改 `client.py` 中的 `SERVER_URL`，改成同學電腦的 IP：

```python
SERVER_URL = "http://192.168.x.x:8000"
```

</div>

---

## 重點回顧

<div class="tricolumns">

<div class="info-card">

**Client-Server**

Client 發送請求
Server 處理並回應

</div>

<div class="info-card">

**HTTP + REST API**

用 URL + 方法
定義溝通方式

</div>

<div class="info-card">

**FastAPI**

Python 框架
自動文件、型別驗證

</div>

</div>

> 下一節：將 Client 從 Python 終端機換成 **Android App**！
