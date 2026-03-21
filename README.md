# Programming for Social Robots

NTU Social Lab 內部培訓工作坊教材。
適合沒有或僅有少量程式經驗的學習者，從零開始學習社交機器人應用開發。

---

## 00 - 開始之前

在進入任何專案之前，請先完成以下環境建置。

### 取得教材

**方法一：下載 ZIP（不需要 Git）**

1. 前往 [GitHub 頁面](https://github.com/hungchunchang/SocialRoboticsProgram)
2. 點擊綠色的 **Code** 按鈕 → **Download ZIP**
3. 解壓縮到你想要的位置

**方法二：使用 Git Clone（推薦）**

1. 安裝 Git：前往 [git-scm.com](https://git-scm.com) 下載並安裝
2. 打開終端機，進入想存放的資料夾，執行：

```bash
git clone https://github.com/hungchunchang/SocialRoboticsProgram.git
```

### 環境建置

詳細步驟請參考 [`00_simple_chatbot/README.md`](00_simple_chatbot/README.md)，包含：

- 安裝 **VSCode** 編輯器
- 安裝 **Python** + **Conda** 環境管理
- 設定 **GitHub PAT**（個人存取權杖）
- 認識 Git、依賴管理等基本概念
- 透過 API 存取 LLM 服務，並利用 Jupyter Notebook 製作簡易 Chatbot（[`connect_to_api.ipynb`](00_simple_chatbot/connect_to_api.ipynb)）

---

## 課程章節

### 01 - FastAPI Chat
> 用 Python 建立你的第一個聊天機器人 API

- 認識 **Client-Server** 架構與 **HTTP / REST API**
- 用 FastAPI 建立後端，用 Python CLI 當客戶端
- 學會修改 System Prompt 自訂機器人角色

### 02 - HTTP Chat：Android App + Flask
> 從終端機走向手機 App

- Client 從 Python 換成 **Android App**（Java）
- 認識 **MVVM** 架構模式
- 後端改用 Flask，設計分階段對話流程

### 03 - Audio WebSocket
> 即時語音串流 —— 從 HTTP 升級為 WebSocket

- 理解 HTTP 與 **WebSocket** 的差異
- 即時雙向音訊傳輸（Full-Duplex）
- 語音活動偵測（VAD）與音訊編碼

### 04 - Video Streaming
> 即時影像串流 —— 初探 WebRTC

- 認識 **WebRTC** 點對點通訊技術
- 理解 **Signaling**（信令）、**SDP**、**ICE Candidate**
- 從手機攝影機串流影像到伺服器

### 05 - Video Call
> 雙向視訊通話 —— 完整的 WebRTC 應用

- 完整通話流程：撥打、接聽、掛斷
- 自己架設 **Node.js** 信令伺服器
- 了解 NAT 穿越（STUN / TURN）

### 06 - WoZ Controller
> Wizard-of-Oz 遙控機器人 —— 整合所有技術

- 認識 **Wizard-of-Oz** 實驗方法
- 三層架構：Web 控制台 + 信令伺服器 + 機器人 App
- 結合 HTTP、WebSocket、WebRTC 與 Robot API
- 用腳本自動化機器人表演

### 07 - 拆解一個 Android App
> 以 Video Call App 為例，理解 Kotlin Android 專案結構

- 理解 Android 專案的資料夾結構與設定檔
- 拆解 **Layout XML** 與 **Kotlin** 的對應關係
- 認識 Activity 生命週期、Intent 跳轉、RecyclerView + Adapter
- 追蹤一次「撥打通話」的完整程式碼執行流程

---

## 技術進程

```
HTTP（一問一答）→ WebSocket（持續雙向）→ WebRTC（點對點低延遲）
   01, 02              03                    04, 05, 06
```
