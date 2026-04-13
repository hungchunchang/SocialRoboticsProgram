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

# 06 - WoZ Controller

<div class="subtitle">Wizard-of-Oz 遙控機器人 —— 整合所有技術</div>

<div class="info-card">

**本節重點**

- 認識 Wizard-of-Oz（WoZ）實驗方法
- 結合 WebSocket + WebRTC 遙控機器人
- 了解三層架構：Web 控制台 / 信令伺服器 / 機器人 App
- 學會用腳本自動化機器人表演

</div>

---

## 什麼是 Wizard-of-Oz？

<div class="columns">

<div class="method-box">

### 概念

參與者以為在和「自主的機器人」互動，
但其實背後有**人類操控員**（Wizard）在遙控。

</div>

<div class="method-box">

### 為什麼需要？

- 在 AI 技術尚未完善時模擬理想互動
- 研究人對機器人的自然反應
- 快速原型驗證設計想法
- HRI（人機互動）研究的常用方法

</div>

</div>

> 就像綠野仙蹤裡的「偉大的奧茲」，幕後其實有人在操控！

---

## 系統架構

```
┌─────────────────┐     WebSocket      ┌──────────────┐
│   Web Controller │ ←──────────────→  │  Signaling   │
│  （操控員電腦）    │                    │   Server     │
└─────────────────┘                    │  (Node.js)   │
                                       └──────┬───────┘
                                              │
                                    WebSocket + WebRTC
                                              │
                                       ┌──────┴───────┐
                                       │  Android App │
                                       │  （機器人上）  │
                                       │  + Nuwa API  │
                                       └──────────────┘
```

---

## 專案架構

```
06_woz_controller/
├── server/                      ← 信令伺服器（同 Project 05）
│   └── src/server.ts
├── web-controller/              ← 操控員網頁介面
│   ├── index.html               ← 主介面
│   ├── js/
│   │   ├── robot-controller.js  ← 機器人控制邏輯
│   │   └── script-engine.js     ← 腳本執行引擎
│   ├── css/styles.css
│   └── scripts/                 ← 表演腳本
│       └── lab-opening.txt
└── app/                         ← 機器人端 Android App
    └── app/src/main/java/com/example/ntldemo/
        ├── MainActivity.kt          ← 主程式 + 機器人 API
        ├── MainFragment.kt          ← 主控制介面
        ├── VideoCallFragment.kt     ← 視訊通話
        └── ControlModeFragment.kt   ← 手動控制模式
```

---

## Web Controller 功能

<div class="columns">

<div class="method-box">

### 手動控制

- **動作**：揮手、點頭、敬禮、鞠躬...
- **表情**：開心、難過、生氣、思考...
- **說話**：輸入文字讓機器人說出來
- **視訊**：即時觀看機器人攝影機畫面

</div>

<div class="method-box">

### 腳本模式

上傳 `.txt` 腳本，自動執行表演：

```
[動作]:[表情]:[台詞]:[持續秒數]
揮手:開心:大家好！:2
點頭:中性:歡迎來參觀:3
鞠躬:開心:謝謝大家:2
```

</div>

</div>

---

## 機器人動作對照表

### Nuwa Robot API 動作映射

| 中文指令 | API 動作 ID | 說明 |
|---------|------------|------|
| 揮手 | `666_SA_Discover` | 發現/招手 |
| 點頭 | `666_PE_PushGlasses` | 推眼鏡（點頭動作） |
| 敬禮 | `666_RE_Ask` | 舉手/敬禮 |
| 鞠躬 | `666_RE_Bye` | 鞠躬道別 |
| 舉手 | `666_TA_Raise_Hand_S` | 舉起手 |
| 思考 | `666_DA_Think` | 思考動作 |

---

## 機器人表情系統

### 表情透過影片播放呈現

| 表情 | 影片檔案 | 狀態 |
|------|---------|------|
| 中性 | `e_neutral_n.mp4` | 待機 |
| 開心 | `e_joy_s.mp4` | 特殊 |
| 難過 | `e_sad_s.mp4` | 特殊 |
| 生氣 | `e_angry_s.mp4` | 特殊 |
| 害怕 | `e_scared_s.mp4` | 特殊 |
| 思考 | `e_thinking_n.mp4` | 一般 |
| 聆聽 | `e_listening_n.mp4` | 一般 |
| 興奮 | `e_excited_s.mp4` | 特殊 |

<div class="highlight">

表情影片存放在 `app/src/main/res/raw/` 目錄中。
`_n` = normal（一般狀態），`_s` = special（特殊表現）

</div>

---

## 控制流程

```
1. 操控員在 Web Controller 點擊「揮手」按鈕
       │
       ↓ WebSocket
2. 信令伺服器轉送指令
       │
       ↓ WebSocket
3. Android App 收到指令
       │
       ↓ Nuwa Robot API
4. 機器人執行揮手動作
```

<div class="info-card">

操控員也能同時透過 WebRTC 看到機器人的攝影機畫面，
實現「遠端遙控 + 即時監看」。

</div>

---

## 實作步驟一：啟動信令伺服器

```bash
cd 06_woz_controller/server
npm install
npm run dev
```

伺服器會在 `ws://0.0.0.0:8666` 啟動。

---

## 實作步驟二：開啟 Web Controller

### 方法一：直接開啟

```
用瀏覽器開啟 web-controller/index.html
```

### 方法二：用 HTTP Server

```bash
cd 06_woz_controller/web-controller
python -m http.server 8000
# 瀏覽器前往 http://localhost:8000
```

<div class="highlight">

在 Web Controller 的連線設定中填入信令伺服器的 URL，
然後註冊一個操控員 ID。

</div>

---

## 實作步驟三：設定 Android App

### 1. 用 Android Studio 開啟 `app/` 資料夾

### 2. 設定連線資訊

- **WebSocket URL**：信令伺服器地址
- **User ID**：機器人的識別 ID

### 3. 建置並安裝到機器人裝置

<div class="info-card">

**注意**：此 App 使用 Nuwa Robot SDK，需要在 Nuwa 機器人裝置上執行才能控制動作和表情。在一般手機上可以測試 WebSocket 和 WebRTC 連線，但動作控制不會生效。

</div>

---

## 實作步驟四：腳本模式

### 1. 撰寫腳本

建立一個 `.txt` 檔案：

```
揮手:開心:大家好，歡迎來到我們的實驗室！:3
點頭:中性:今天我來為大家介紹這裡的研究:4
舉手:興奮:我們在研究人和機器人的互動:3
鞠躬:開心:希望大家會喜歡！:2
```

### 2. 在 Web Controller 上傳並執行

點擊「Load Script」上傳腳本，然後點擊「Play」自動執行。

---

## 所有技術的整合

<div class="info-card">

| 技術 | 在本專案的角色 |
|------|--------------|
| **HTTP** | Web Controller 載入靜態檔案 |
| **WebSocket** | 信令交換 + 控制指令傳送 |
| **WebRTC** | 即時視訊（操控員看機器人畫面） |
| **Robot API** | Nuwa SDK 控制機器人動作與表情 |

</div>

> 回顧 6 個專案的學習路線：
> HTTP → Android HTTP → WebSocket 語音 → WebRTC 影像 → 視訊通話 → WoZ 整合

---

## 課程總回顧

<div class="columns">

<div class="info-card">

### 通訊協定

| 協定 | 特性 | 適用場景 |
|------|------|---------|
| HTTP | 一問一答 | 文字聊天 |
| WebSocket | 持續雙向 | 語音串流 |
| WebRTC | P2P 低延遲 | 視訊通話 |

</div>

<div class="info-card">

### 架構模式

| 模式 | 說明 |
|------|------|
| Client-Server | 基本的請求-回應 |
| MVVM | Android 標準架構 |
| Signaling | WebRTC 信令交換 |
| WoZ | 人類遙控機器人 |

</div>

</div>

---

## 重點回顧

<div class="tricolumns">

<div class="info-card">

**WoZ 方法**

人類操控員遙控機器人，模擬自主互動

</div>

<div class="info-card">

**三層架構**

Web 控制台 + 信令伺服器 + 機器人 App

</div>

<div class="info-card">

**技術整合**

HTTP + WebSocket + WebRTC + Robot API

</div>

</div>

> 恭喜完成所有專案！你已經掌握了社交機器人應用開發的核心技術。
