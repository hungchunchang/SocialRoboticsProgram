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

# 03 - 話輪流程控制

<div class="subtitle">從「收到一句回一句」進化到「多階段對話流程」</div>

<div class="info-card">

**本節重點**

- 理解話輪（Turns）流程控制
- 學習將單檔程式拆成模組化結構
- 用 Docker 容器化部署服務

</div>

---

## 回顧：02 做了什麼？

在 `02_fastapi_chat` 中，我們建了一個最小可行的 HTTP chatbot：

```python
# 02 的核心：一個 system prompt，無限對話
messages = [{"role": "system", "content": SYSTEM_PROMPT}] + history
completion = client.chat.completions.create(model="gpt-4o", messages=messages)
```

<div class="highlight">

**02 的限制**

- 只有一個固定的 system prompt
- 對話沒有階段，不會自動結束
- 所有程式碼塞在一個 `server.py` 裡
- 對話紀錄存在記憶體，重啟就消失

</div>

---

## 03 要解決什麼問題？

如果你的機器人需要「先問知識題 → 再做訪談 → 最後結尾」，02 的架構不夠用。

<div class="columns">

<div class="method-box">

### 02：自由對話

- 一個 prompt 到底
- 靠使用者決定何時結束
- 適合通用聊天

</div>

<div class="method-box">

### 03：流程型對話

- 依照話輪切換 prompt
- Server 控制何時結束
- 適合導覽、教學、問卷

</div>

</div>

> 03 的 HTTP 協定、REST API 端點設計跟 02 完全一樣，差別只在 Server 內部邏輯。

---

## 概念一：話輪（Turns）流程控制

---

## 什麼是話輪？

每一次「使用者發話 → 機器人回應」就是一個話輪（Turn）。

在 03 裡，Server 會記錄目前是第幾輪，並根據輪數切換不同的 system prompt：

| 話輪 | 階段 | 行為 |
|------|------|------|
| 0–2 | 知識問答 | 根據展覽內容出 3 題 |
| 3–7 | 參觀訪談 | 問印象、推薦意願、建議 |
| 8 | 博物館日提問 | 問「5/18 是什麼日子？」 |
| 9 | 博物館日收尾 | 揭曉答案、結束對話 |
| ≥10 | 已完成 | 直接回傳結束訊息 |

---

## 流程控制的核心程式碼

### 判斷目前階段

```python
def get_stage_name(turns: int) -> str:
    if turns <= 2:
        return "knowledge_quiz"
    if turns <= 7:
        return "experience_interview"
    if turns == 8:
        return "museum_day_question"
    if turns == 9:
        return "museum_day_answer"
    return "completed"
```

<div class="highlight">

**關鍵概念**：用一個簡單的 `if-else` 就實現了狀態機。`turns` 是唯一的狀態變數。

</div>

---

## 依階段選擇 Prompt

```python
def build_system_prompt(user_data: UserData) -> str | None:
    history = "".join(
        f"{msg.timestamp} | {msg.speaker}: {msg.message}\n"
        for msg in user_data.conversation
    )
    intro_text = get_intro_text()
    turns = user_data.turns

    if turns <= 2:
        template = read_prompt('system_prompt_template.txt')
        return template.format(intro=intro_text, context=history)
    if turns <= 7:
        template = read_prompt('interview_prompt.txt')
        return template.format(context=history)
    # ...
```

- 每個階段有獨立的 prompt 模板，放在 `data/prompt/` 資料夾
- `{context}` 會被替換成目前的對話紀錄
- `{intro}` 會被替換成展覽介紹文字

---

## 完整的一次請求流程

```text
Client                          Server
  │                               │
  ├── POST /api/chat ───────────► │
  │   {user_name, message}        │
  │                               ├─ 1. 寫入使用者訊息到 JSON
  │                               ├─ 2. 讀取目前 turns
  │                               ├─ 3. 根據 turns 選擇 prompt
  │                               ├─ 4. 呼叫 OpenAI API
  │                               ├─ 5. 解析回應、turns + 1
  │                               ├─ 6. 寫入機器人訊息到 JSON
  │                               │
  │ ◄── {reply, turn_index, ──────┤
  │      current_stage, is_ended} │
```

跟 02 相比，多了步驟 2、3 和 5 的狀態管理。

---

## 對話持久化

02 把對話存在記憶體（Python dict），重啟就消失：

```python
# 02 的做法
conversations = defaultdict(list)  # 記憶體中
```

03 改用 JSON 檔案，每位使用者一份：

```python
# 03 的做法：data/conversation/{user_name}.json
{
  "turns": 3,
  "is_ended": false,
  "conversation": [
    {"timestamp": "2025-04-21 14:30:00", "speaker": "user", "message": "你好"},
    {"timestamp": "2025-04-21 14:30:01", "speaker": "bot", "message": "歡迎！"}
  ]
}
```

<div class="highlight">

重啟 Server 後，對話可以從上次中斷的地方繼續。

</div>

---

## 概念二：檔案模組化

---

## 為什麼要拆檔案？

02 的 `server.py` 只有 100 行，一個檔案就夠了。

但 03 加入了：turns 狀態管理、多個 prompt 模板、JSON 檔案讀寫、Pydantic 資料模型……全部塞在一個檔案會很難維護。

<div class="info-card">

**拆檔的原則：每個檔案只負責一件事**

- 「HTTP 進出」和「prompt 邏輯」不放在一起
- 「資料結構定義」和「檔案 IO」不放在一起
- 「設定值」獨立出來，方便切換環境

</div>

---

## 專案結構

```text
backend_modular/
├── main.py              ← uvicorn 啟動入口
├── app.py               ← FastAPI app 建立、CORS、掛載 router
├── config.py            ← 路徑設定、環境變數、OpenAI client
├── api/
│   └── chat_api.py      ← API 端點（HTTP 進出）
├── logic/
│   └── chat_logic.py    ← 話輪判斷、prompt 組裝、OpenAI 呼叫
├── models/
│   └── data_structures.py  ← Pydantic 請求/回應模型
├── utils/
│   └── helpers.py       ← JSON 讀寫、prompt 載入
├── data/
│   ├── conversation/    ← 使用者對話紀錄（JSON）
│   └── prompt/          ← 各階段 prompt 模板
├── intro.txt            ← 展覽介紹文字
├── Dockerfile
├── docker-compose.yml
└── requirement.txt
```

---

## 各層的職責

<div class="columns">

<div class="method-box">

### api/ — HTTP 進出

```python
@router.post("/chat")
def chat(req: ChatRequest):
    save_message_service(...)
    response = generate_bot_reply(...)
    save_message_service(...)
    return response
```

只做：收請求、呼叫 logic、回傳結果

</div>

<div class="method-box">

### logic/ — 商業邏輯

```python
def generate_bot_reply(...):
    user_data = load_user_data(...)
    stage = get_stage_name(...)
    prompt = build_system_prompt(...)
    completion = client.chat.completions.create(...)
    # ...
```

負責：turns 判斷、prompt 組裝、模型呼叫

</div>

</div>

---

## 各層的職責（續）

<div class="columns">

<div class="method-box">

### models/ — 資料結構

```python
class UserData(BaseModel):
    turns: int = 0
    conversation: list[MessageRecord] = []
    is_ended: bool = False

class ChatResponse(BaseModel):
    reply: str
    turn_index: int
    current_stage: str
    is_ended: bool = False
```

用 Pydantic 定義，自動做型別驗證

</div>

<div class="method-box">

### utils/ — 共用工具

```python
def load_user_data(user_name):
    file_path = CONVERSATION_DIR / f"{user_name}.json"
    with file_path.open('r') as f:
        data = json.load(f)
        return UserData.model_validate(data)
```

負責：檔案系統 IO、目錄建立

</div>

</div>

---

## 跟 02 的對照

| | 02 `server.py` | 03 `backend_modular/` |
|---|---|---|
| 檔案數 | 1 個 | 7 個 + prompt 模板 |
| API 端點 | 寫在 server.py | `api/chat_api.py` |
| Prompt | 寫死在程式碼中 | `data/prompt/*.txt` 模板 |
| 對話儲存 | `defaultdict(list)` | `data/conversation/*.json` |
| 設定 | 散落各處 | `config.py` 統一管理 |
| 資料模型 | 無 | `models/data_structures.py` |

<div class="highlight">

02 是「能跑就好」，03 是「能維護、能部署」。先學 02 理解核心概念，再學 03 理解工程實踐。

</div>

---

## 概念三：Docker 容器運行

---

## 為什麼需要 Docker？

到目前為止，我們都是在自己電腦上跑 `uvicorn`。但如果要部署到伺服器或雲端：

- 別人的機器可能沒裝 Python 3.13
- `pip install` 可能因為系統差異而失敗
- 環境變數、資料夾結構都要手動設定

<div class="info-card">

**Docker 的概念**：把你的程式 + 所有依賴 + 執行環境打包成一個「容器」，在任何機器上都能跑出一樣的結果。

</div>

---

## Dockerfile 解讀

```dockerfile
FROM python:3.13-slim          # 基底映像：精簡版 Python

WORKDIR /app                   # 工作目錄

COPY requirement.txt .         # 先複製依賴清單
RUN pip install --no-cache-dir -r requirement.txt  # 安裝依賴

COPY . .                       # 複製所有程式碼

RUN mkdir -p data/conversation data/prompt  # 建立資料目錄
RUN chmod -R 755 data

RUN useradd -m appuser         # 建立非 root 使用者（安全性）
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080                    # 宣告使用的 port

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]
```

---

## docker-compose.yml 解讀

```yaml
services:
  jxw-app:
    build: .                        # 用當前目錄的 Dockerfile 建置
    ports:
      - "8080:8080"                 # 主機 8080 → 容器 8080
    volumes:
      - ./data:/app/data            # 掛載資料目錄（對話不會因容器重啟消失）
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - OPENAI_MODEL=${OPENAI_MODEL:-gpt-4o-mini}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 1m
```

<div class="highlight">

`volumes` 很重要：沒有掛載的話，容器內的對話紀錄會在容器刪除時一起消失。

</div>

---

## 實作步驟

---

## 步驟一：本地執行（不用 Docker）

```bash
cd 03_jxw_http_chat/backend_modular

# 建立虛擬環境
python -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate

# 安裝依賴
pip install -r requirement.txt

# 設定環境變數
export OPENAI_API_KEY=sk-xxxxxxxx

# 啟動
uvicorn main:app --host 0.0.0.0 --port 8080 --reload
```

啟動後前往 `http://localhost:8080/docs` 查看互動式 API 文件。

---

## 步驟二：測試 API

### 建立使用者

```bash
curl -X POST http://localhost:8080/api/create_user
# → {"user_name": "a1b2c3d4-..."}
```

### 開始對話

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"user_name":"a1b2c3d4-...","message":"你好"}'
```

### 觀察回應

```json
{
  "reply": "你好！我是阿蓬...",
  "turn_index": 1,
  "current_stage": "knowledge_quiz",
  "is_ended": false
}
```

注意 `turn_index` 和 `current_stage` 會隨對話推進而改變。

---

## 步驟三：用 Docker 執行

### 1. 建立 `.env` 檔案

在 `backend_modular/` 目錄下建立 `.env`：

```
OPENAI_API_KEY=sk-xxxxxxxx
OPENAI_MODEL=gpt-4o-mini
```

### 2. 啟動容器

```bash
cd 03_jxw_http_chat/backend_modular
docker compose up --build
```

### 3. 測試

```bash
curl http://localhost:8080/api/health
# → {"status": "healthy"}
```

跟本地執行完全一樣的 API，但現在跑在容器裡了。

---

## 步驟四：整合測試

專案附帶 `test.py`，會自動建立使用者並送出 12 輪對話：

```bash
cd 03_jxw_http_chat
python test.py
```

觀察輸出中的 `turn_index` 和 `current_stage`，確認流程是否正確切換。

---

## 補充：部署到雲端

---

## GCP Cloud Run

Cloud Run 可以直接跑 Docker container，最適合這種 stateless HTTP 服務。

```bash
# 1. 建置並推送映像
gcloud builds submit --tag gcr.io/PROJECT_ID/jxw-app

# 2. 部署
gcloud run deploy jxw-app \
  --image gcr.io/PROJECT_ID/jxw-app \
  --port 8080 \
  --set-secrets OPENAI_API_KEY=openai-key:latest
```

<div class="highlight">

**注意**：Cloud Run 是 stateless，容器可能隨時被回收。`data/conversation/` 的 JSON 檔案會消失。正式上線建議改用 Cloud Storage 或 Firestore 儲存對話。

</div>

---

## AWS App Runner / ECS Fargate

```bash
# App Runner：最簡單，直接連 GitHub repo 或 ECR image
# ECS Fargate：較靈活，適合需要 VPC、負載均衡的場景
```

<div class="info-card">

**雲端部署共通事項**

- API Key 不要寫在程式碼或 Dockerfile 裡，用 **Secret Manager** 管理
- 對話持久化改用雲端儲存（S3 / Cloud Storage / DynamoDB / Firestore）
- 設定 health check endpoint（本專案已有 `/api/health`）

</div>

---

## 練習任務

<div class="method-box">

### 修改對話流程

1. 打開 `logic/chat_logic.py`，修改 `get_stage_name()` 的輪數分配
2. 在 `data/prompt/` 新增或修改 prompt 模板
3. 把展覽主題換成你自己的內容（編輯 `intro.txt`）

</div>

<div class="method-box">

### 加入新的階段

試著新增一個「滿意度評分」階段：
1. 在 `get_stage_name()` 加入新的 stage
2. 建立對應的 prompt 模板
3. 在 `build_system_prompt()` 加入對應的 `if` 分支

</div>

---

## 重點回顧

<div class="tricolumns">

<div class="info-card">

**流程控制**

用 `turns` 狀態變數
控制對話走向

</div>

<div class="info-card">

**模組化**

拆成 api / logic / models / utils，各司其職

</div>

<div class="info-card">

**Docker**

打包成容器
在任何環境都能跑

</div>

</div>

> 下一節：從 HTTP 升級為 **WebSocket**，實現即時語音串流！
