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

# 04 - Video Streaming

<div class="subtitle">即時影像串流 —— 初探 WebRTC</div>

<div class="info-card">

**本節重點**

- 認識 WebRTC 技術與應用場景
- 理解 Signaling（信令）的角色
- 了解 SDP 與 ICE Candidate
- 從手機攝影機串流影像到伺服器

</div>

---

## 為什麼需要 WebRTC？

<div class="columns">

<div class="method-box">

### WebSocket 的限制

- 所有資料都經過伺服器
- 影像資料量大，伺服器負擔重
- 延遲較高（Server 轉送）

</div>

<div class="method-box">

### WebRTC 的優勢

- **點對點**（Peer-to-Peer）傳輸
- 資料直接在裝置間傳送
- 超低延遲
- 內建音視訊編解碼

</div>

</div>

> WebRTC = Web Real-Time Communication
> Google Meet、LINE 通話、Discord 都在用！

---

## WebRTC 的三步驟

```
步驟 1：Signaling（信令交換）
   透過 WebSocket 交換連線資訊
   「我是誰、我在哪、我支援什麼格式」

步驟 2：ICE Candidate（網路探測）
   找到雙方都能用的網路路徑
   「我的 IP 是 xxx，你能連到嗎？」

步驟 3：Media Streaming（媒體串流）
   建立直接連線，開始傳送音視訊
   「開始傳影像了！」
```

<div class="highlight">

**Signaling 用 WebSocket，但影像資料走 WebRTC 直連。**
WebSocket 只是「牽線」的角色，真正的影像不經過它。

</div>

---

## SDP 是什麼？

**SDP**（Session Description Protocol）描述媒體通訊的能力：

```
我支援的影像格式：H.264, VP8
我支援的音訊格式：Opus
我的影像解析度：640x480
我的 IP 位址：192.168.1.100
```

<div class="columns">

<div class="info-card">

**Offer**（提議）
A 告訴 B：「我能做到這些」

</div>

<div class="info-card">

**Answer**（回應）
B 告訴 A：「我也能做到這些，就這麼辦」

</div>

</div>

---

## 專案架構

```
04_video_streaming/
└── app/app/src/main/java/com/example/videostreaming/
    ├── MainActivity.kt         ← 設定頁面（輸入 IP / Port）
    ├── CameraActivity.kt       ← 攝影機畫面 + 串流
    └── managers/
        ├── WebrtcManager.kt    ← WebRTC 連線管理
        └── SignalingClient.kt  ← WebSocket 信令
```

<div class="info-card">

**運作流程**

1. `MainActivity` 輸入伺服器 IP 和 Port
2. 跳轉到 `CameraActivity`，開啟攝影機
3. `SignalingClient` 透過 WebSocket 連到信令伺服器
4. `WebrtcManager` 建立 PeerConnection，開始串流

</div>

---

## 核心流程圖

```
     Android App                    Signaling Server
         │                               │
    1. 開啟攝影機                          │
         │──── WebSocket 連線 ────────→   │
         │                               │
    2. 建立 PeerConnection                │
         │──── SDP Offer ─────────────→  │
         │←─── SDP Answer ────────────── │
         │                               │
    3. ICE 探測                           │
         │⇄─── ICE Candidates ────────⇄  │
         │                               │
    4. 影像串流（P2P）                     │
         │═══════════════════════════════ │
```

---

## 核心元件：WebrtcManager

### 建立 PeerConnection

```kotlin
// 初始化 WebRTC
val factory = PeerConnectionFactory.builder()
    .setVideoEncoderFactory(encoderFactory)
    .setVideoDecoderFactory(decoderFactory)
    .createPeerConnectionFactory()

// 建立 PeerConnection
val peerConnection = factory.createPeerConnection(
    rtcConfig,   // ICE server 設定
    observer     // 監聽連線狀態
)
```

---

## 核心元件：SignalingClient

### WebSocket 信令

```kotlin
// 連線到信令伺服器
val url = "ws://$serverIp:$serverPort"
webSocket = client.newWebSocket(request, listener)

// 傳送 SDP Offer
fun sendOffer(sdp: SessionDescription) {
    val json = JSONObject()
    json.put("type", "offer")
    json.put("sdp", sdp.description)
    webSocket.send(json.toString())
}
```

<div class="highlight">

信令伺服器只負責轉送 SDP 和 ICE 資訊，不處理影像資料。

</div>

---

## 攝影機設定

```kotlin
// 攝影機參數
val videoCapturer = Camera2Capturer(context, cameraId, null)
videoCapturer.startCapture(
    640,    // 寬度
    480,    // 高度
    30      // FPS
)
```

<div class="columns">

<div class="info-card">

**功能按鈕**
- 切換前/後鏡頭
- 開關影像預覽
- 開始/停止串流

</div>

<div class="info-card">

**權限需求**
- `CAMERA`（攝影機）
- App 啟動時會請求授權

</div>

</div>

---

## 實作步驟一：設定 App

### 1. 用 Android Studio 開啟 `app/` 資料夾

### 2. 在主畫面輸入伺服器資訊

- **Server IP**：信令伺服器 IP（如 `140.112.92.133`）
- **Server Port**：信令伺服器埠號（如 `6868`）

### 3. 建置並安裝到手機

```
點擊「Start」進入攝影機畫面
```

---

## 實作步驟二：測試串流

1. 確認信令伺服器已啟動
2. 在 App 填入正確的伺服器 IP 和 Port
3. 點擊「Start」進入攝影機畫面
4. App 會自動：
   - 開啟攝影機
   - 連線到信令伺服器
   - 交換 SDP / ICE
   - 開始串流

<div class="highlight">

**除錯技巧**
- 觀察 Logcat 中的 WebSocket 和 WebRTC 狀態
- 確認攝影機權限已授予
- 確認 IP 和 Port 正確

</div>

---

## 通訊協定進化

| 協定 | Project | 特性 | 延遲 |
|------|---------|------|------|
| **HTTP** | 01, 02 | 一問一答 | 高 |
| **WebSocket** | 03 | 持續雙向 | 中 |
| **WebRTC** | 04 | 點對點直連 | 極低 |

<div class="info-card">

每種協定適合不同場景：
- HTTP → 文字聊天、API 查詢
- WebSocket → 語音串流、即時通知
- WebRTC → 視訊通話、螢幕分享

</div>

---

## 重點回顧

<div class="tricolumns">

<div class="info-card">

**WebRTC**

點對點即時通訊，超低延遲影像串流

</div>

<div class="info-card">

**Signaling**

WebSocket 負責牽線，交換 SDP 和 ICE

</div>

<div class="info-card">

**SDP + ICE**

描述媒體能力，探測可用網路路徑

</div>

</div>

> 下一節：從單向串流升級為 **雙向視訊通話**！
