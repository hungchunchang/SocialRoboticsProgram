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

# 02 - HTTP Chat：Android App + Flask

<div class="subtitle">從終端機走向手機 App —— 用 Android 連接聊天伺服器</div>

<div class="info-card">

**本節重點**

- 從 Project 01 延伸：Client 從 Python CLI 換成 Android App
- 認識 Android 專案架構與 MVVM 模式
- 了解 HTTP 在手機 App 中如何運作
- 學會修改伺服器 IP 來連接不同電腦

</div>

---

## 回顧：Project 01 vs Project 02

<div class="columns">

<div class="method-box">

### Project 01

```
Python Client (CLI)
       ↓ HTTP POST
FastAPI Server
       ↓
   OpenAI API
```

客戶端 = 終端機程式

</div>

<div class="method-box">

### Project 02

```
Android App (Java)
       ↓ HTTP POST
Flask Server (Python)
       ↓
   OpenAI API
```

客戶端 = 手機 App

</div>

</div>

> 協定相同（HTTP），只是 Client 從 Python 變成 Android App

---

## 專案架構

```
02_jxw_http_chat/
├── backend/
│   ├── app.py              ← Flask 伺服器
│   ├── intro.txt           ← 展覽背景資料
│   └── requirement.txt     ← Python 依賴
└── frontend/               ← Android 專案
    └── app/src/main/java/com/example/jxw/
        ├── MainActivity.java       ← 主畫面
        ├── viewmodel/RobotViewModel.java
        ├── repository/DataRepository.java
        ├── util/HttpHandler.java   ← HTTP 請求工具
        └── fragment/SettingFragment.java ← 設定頁面
```

---

## 概念：MVVM 架構

Android App 使用 **MVVM**（Model-View-ViewModel）架構：

<div class="columns">

<div class="info-card">

**View**（畫面）
`MainActivity` / `Fragment`
負責顯示 UI、接收使用者操作

**ViewModel**（邏輯層）
`RobotViewModel`
管理 UI 狀態與商業邏輯

</div>

<div class="info-card">

**Repository**（資料層）
`DataRepository`
統一管理資料來源

**HttpHandler**（工具）
負責發送 HTTP 請求到伺服器

</div>

</div>

> 類似 Project 01 的 `requests.post()`，但在 Android 中需用專門的工具類別

---

## 後端：Flask Server

### 與 Project 01 的差異

| 項目 | Project 01 (FastAPI) | Project 02 (Flask) |
|------|---------------------|-------------------|
| 框架 | FastAPI | Flask |
| 角色設計 | 通用助手 | 磯永吉小屋導覽員「阿蓬」 |
| 對話流程 | 自由對話 | 分階段引導（知識問答 → 訪談） |
| 資料儲存 | 記憶體（重啟消失） | JSON 檔案（持久化） |
| 回應欄位 | `reply` | `question` |

---

## 後端：對話流程設計

```python
def jxw_bot(user_name, question):
    turns = user_data.get("turns", 0)

    if turns <= 2:        # 階段一：知識問答（3 題）
        system_prompt = system_prompt_template.format(...)
    elif turns <= 7:      # 階段二：體驗訪談（3 題）
        system_prompt = interview_prompt.format(...)
    elif turns == 8:      # 階段三：國際博物館日小知識
        system_prompt = international_museum_day.format(...)
    elif turns >= 10:     # 結束對話
        return {"question": "感謝參觀！", "is_ended": True}
```

<div class="highlight">

**設計重點**：透過 `turns` 計數器控制對話階段，每階段使用不同的 System Prompt。
這是一種常見的對話流程設計模式。

</div>

---

## 前端：Android HTTP 請求

### HttpHandler.java 核心概念

```java
// 設定伺服器網址
private String serverUrl = "http://172.20.10.2:8000/";

// 發送 POST 請求
public void sendMessage(String message, String userName) {
    JSONObject json = new JSONObject();
    json.put("message", message);
    json.put("user_name", userName);
    // → HTTP POST 到 /api/chat
}
```

<div class="highlight">

**重要**：`serverUrl` 需要改成實際的伺服器 IP！
在 App 的 Settings 頁面可以修改。

</div>

---

## 實作步驟一：啟動後端

### 1. 建立環境

```bash
conda create -n jxw_chat python=3.12
conda activate jxw_chat
cd 02_jxw_http_chat/backend
pip install -r requirement.txt
```

### 2. 設定 API Key

在 `backend/` 建立 `.env` 檔案：

```
OPENAI_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxx
```

### 3. 啟動伺服器

```bash
python app.py
```

伺服器會在 `http://0.0.0.0:8080` 啟動

---

## 實作步驟二：查看自己的 IP

### 找到你的電腦 IP 地址

**macOS / Linux：**
```bash
ifconfig | grep "inet "
```

**Windows：**
```bash
ipconfig
```

<div class="highlight">

記下你的 IP 地址（如 `192.168.1.100`），待會要填入 Android App 的設定頁面。
確保手機和電腦在**同一個 Wi-Fi 網路**。

</div>

---

## 實作步驟三：設定 Android App

### 1. 用 Android Studio 開啟 `frontend/` 資料夾

### 2. 在設定頁面填入伺服器網址

在 App 的 Settings 頁面輸入：
```
http://你的電腦IP:8080
```

### 3. 建置並安裝到手機或模擬器

<div class="info-card">

**Android Studio 快捷鍵**
- 執行 App：`Shift + F10`（Windows）/ `Ctrl + R`（macOS）
- 重新編譯：`Ctrl + F9`（Windows）/ `Cmd + F9`（macOS）

</div>

---

## 實作步驟四：測試對話

1. 確認後端伺服器已啟動
2. 在 App 設定頁面填入正確的伺服器 IP
3. 回到主畫面，開始和機器人對話
4. 觀察對話階段的變化（知識問答 → 訪談 → 結束）

<div class="highlight">

**除錯技巧**：觀察後端終端機的 log 輸出，可以看到每次請求和回覆的內容。
如果連不上，確認：(1) 同一個 Wi-Fi (2) IP 正確 (3) Port 正確

</div>

---

## 練習任務

<div class="method-box">

### 修改後端角色

1. 編輯 `backend/intro.txt`，換成你想要的展覽內容
2. 修改 `backend/app.py` 中的 `system_prompt_template`
3. 調整對話階段數量和流程

</div>

<div class="method-box">

### 連線到同學的伺服器

在 App 設定頁面改成同學的 IP，體驗不同角色的機器人！

</div>

---

## 重點回顧

<div class="tricolumns">

<div class="info-card">

**HTTP 不變**

無論 Client 是 Python 還是 Android，HTTP 協定都一樣

</div>

<div class="info-card">

**MVVM 架構**

Android App 的標準架構：View / ViewModel / Repository

</div>

<div class="info-card">

**IP 連線**

手機和電腦在同一網路，透過 IP 地址溝通

</div>

</div>

> 下一節：從 HTTP 升級為 **WebSocket**，實現即時語音串流！
