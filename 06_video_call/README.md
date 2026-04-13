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

# 05 - Video Call

<div class="subtitle">雙向視訊通話 —— 完整的 WebRTC 應用</div>

<div class="info-card">

**本節重點**

- 從單向串流升級為雙向視訊通話
- 認識完整的通話流程（撥打、接聽、掛斷）
- 了解信令伺服器如何管理使用者和通話
- 自己架設 Node.js 信令伺服器

</div>

---

## Project 04 vs Project 05

<div class="columns">

<div class="method-box">

### Project 04（單向串流）

```
App ──→ 影像 ──→ Server
```

- 手機送出影像
- 伺服器接收
- 沒有回傳畫面

</div>

<div class="method-box">

### Project 05（雙向通話）

```
App A ←──→ 影像/音訊 ←──→ App B
```

- 兩支手機互相傳送
- 同時看到對方畫面
- 完整的通話體驗

</div>

</div>

> 這就是 Google Meet、FaceTime 的基本原理！

---

## 專案架構

```
05_video_call/
├── server/                  ← Node.js 信令伺服器
│   ├── src/server.ts        ← 伺服器程式碼
│   ├── package.json         ← 依賴
│   └── tsconfig.json
└── app/                     ← Android 專案
    └── app/src/main/java/com/example/videocall/
        ├── MainActivity.kt         ← 註冊 + 使用者列表
        ├── VideoCallActivity.kt    ← 通話畫面
        └── OnlineUsersAdapter.kt   ← 使用者列表元件
```

---

## 通話流程

```
  使用者 A                   信令伺服器                 使用者 B
     │                          │                          │
  1. register ──────────────→  │  ←────────────── register │
     │                          │                          │
     │  ←─ online_users_update ─│─ online_users_update ──→ │
     │                          │                          │
  2. call(target: B) ────────→ │ ──── incoming_call ────→  │
     │                          │                          │
     │                          │  ←──── call_answer ──── │
     │  ←── call_accepted ───── │                          │
     │                          │                          │
  3. offer ──────────────────→  │ ──────── offer ────────→ │
     │  ←────── answer ──────── │ ←─────── answer ──────── │
     │  ⇄──── ICE candidates ──│──── ICE candidates ────⇄ │
     │                          │                          │
  4. ═══════ 視訊通話中 ════════│═══════════════════════════│
```

---

## 信令伺服器：訊息類型

### server.ts 處理的訊息

| 訊息類型 | 方向 | 說明 |
|---------|------|------|
| `register` | Client → Server | 使用者上線，註冊 ID |
| `unregister` | Client → Server | 使用者離線 |
| `call` | A → Server → B | 發起通話 |
| `call_answer` | B → Server → A | 接受/拒絕通話 |
| `offer` / `answer` | A ⇄ Server ⇄ B | 交換 SDP |
| `ice_candidate` | A ⇄ Server ⇄ B | 交換 ICE |
| `call_ended` | Any → Server → Other | 結束通話 |

---

## 信令伺服器：核心程式碼

```typescript
// 使用者管理
const users = new Map<string, WebSocket>();

// 處理訊息
ws.on("message", (data) => {
    const message = JSON.parse(data.toString());

    switch (message.type) {
        case "register":
            users.set(message.userId, ws);
            broadcastOnlineUsers();  // 通知所有人
            break;
        case "call":
            const target = users.get(message.targetUserId);
            target?.send(JSON.stringify({
                type: "incoming_call",
                from: message.userId
            }));
            break;
        // ... offer, answer, ice_candidate
    }
});
```

---

## 實作步驟一：架設信令伺服器

### 1. 安裝 Node.js

```bash
# 確認 Node.js 已安裝
node --version   # 需要 v18+
npm --version
```

### 2. 安裝依賴並啟動

```bash
cd 05_video_call/server
npm install
npm run dev    # 開發模式（自動重載）
```

<div class="highlight">

伺服器啟動後會監聽 `ws://0.0.0.0:8666`。
終端機會顯示 `Server started on port 8666`。

</div>

---

## 實作步驟二：設定 Android App

### 1. 用 Android Studio 開啟 `app/` 資料夾

### 2. 在主畫面輸入

- **Server URL**：`ws://你的電腦IP:8666`
- **User ID**：你的暱稱（需唯一）

### 3. 點擊「Register」註冊

<div class="info-card">

註冊後會看到目前上線的使用者列表。
點擊其他使用者即可發起通話。

</div>

---

## 實作步驟三：雙人測試

### 需要兩支手機或模擬器

1. 兩台裝置都安裝 App
2. 兩台都填入相同的 Server URL
3. 各自用不同的 User ID 註冊
4. 在使用者列表中點擊對方
5. 對方會收到來電通知
6. 接聽後開始視訊通話

<div class="highlight">

**注意**：WebRTC 連線建立後，如果要再次通話，需要重新註冊。
這是目前的已知限制。

</div>

---

## 實作步驟四：觀察信令

在伺服器終端機觀察 log：

```
[2024-03-15 10:00:01] User registered: alice
[2024-03-15 10:00:05] User registered: bob
[2024-03-15 10:00:10] Call: alice → bob
[2024-03-15 10:00:12] Call accepted: bob → alice
[2024-03-15 10:00:12] Relaying offer: alice → bob
[2024-03-15 10:00:13] Relaying answer: bob → alice
[2024-03-15 10:00:13] Relaying ICE candidate
```

<div class="info-card">

觀察信令交換的順序，理解 WebRTC 連線建立的完整過程。

</div>

---

## 進階概念：NAT 穿越

<div class="columns">

<div class="info-card">

### 問題

大多數裝置在 NAT（網路位址轉換）後面，
沒有公開 IP，無法直接連線。

</div>

<div class="info-card">

### 解決方案

- **STUN Server**：幫你找到你的公開 IP
- **TURN Server**：如果直連失敗，透過中繼伺服器轉送

</div>

</div>

<div class="highlight">

在區域網路（同一個 Wi-Fi）內測試通常不需要 STUN/TURN。
跨網路時才需要設定。

</div>

---

## 重點回顧

<div class="tricolumns">

<div class="info-card">

**完整通話流程**

註冊 → 撥打 → 接聽 → SDP/ICE 交換 → 通話

</div>

<div class="info-card">

**信令伺服器**

Node.js + WebSocket，管理使用者和轉送信令

</div>

<div class="info-card">

**NAT 穿越**

STUN/TURN 幫助跨網路的裝置建立直連

</div>

</div>

> 下一節：結合所有技術，打造 **Wizard-of-Oz 遙控機器人系統**！
