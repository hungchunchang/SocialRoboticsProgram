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

# 07 - 拆解一個 Android App

<div class="subtitle">以 Video Call App 為例，理解 Kotlin Android 專案結構</div>

<div class="info-card">

**本節重點**

- 理解 Android 專案的資料夾結構
- 認識 Activity 生命週期
- 拆解 Layout XML 與程式碼的對應關係
- 理解 RecyclerView + Adapter 模式
- 看懂 WebSocket / WebRTC 如何嵌入 App

</div>

---

## 為什麼要學這個？

前幾章我們學了怎麼**使用** App，這章要學怎麼**讀懂** App。

<div class="columns">

<div class="method-box">

### 你已經知道

- HTTP / WebSocket / WebRTC 概念
- 伺服器和客戶端的角色
- 如何修改 IP 連到伺服器

</div>

<div class="method-box">

### 這章要學的

- App 的程式碼怎麼組織的？
- 畫面上的按鈕怎麼連到程式？
- 按下「撥打」到底發生了什麼事？

</div>

</div>

---

## 拆解對象：Project 05 Video Call

```
05_video_call/app/app/
├── src/main/
│   ├── AndroidManifest.xml          ← App 的身分證
│   ├── java/.../videocall/
│   │   ├── MainActivity.kt          ← 主畫面（註冊、撥打）
│   │   ├── VideoCallActivity.kt     ← 通話畫面（WebRTC）
│   │   └── OnlineUsersAdapter.kt    ← 使用者列表元件
│   └── res/
│       └── layout/
│           ├── activity_main.xml         ← 主畫面 UI
│           └── activity_video_call.xml   ← 通話畫面 UI
└── build.gradle.kts                 ← 依賴設定
```

---

## 第一層：AndroidManifest.xml

App 的「身分證」，告訴 Android 系統這個 App 需要什麼。

```xml
<!-- 需要的權限 -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />

<!-- 有哪些畫面（Activity） -->
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter>  <!-- 這是 App 啟動時的第一個畫面 -->
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
<activity android:name=".VideoCallActivity" />
```

<div class="highlight">

**permission** = App 需要什麼能力（相機、麥克風、網路）
**activity** = App 有哪些畫面

</div>

---

## 第二層：build.gradle.kts

App 的「食材清單」，宣告需要哪些外部套件。

```kotlin
dependencies {
    // Android 基本元件
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // WebRTC（視訊通話核心）
    implementation("io.getstream:stream-webrtc-android:1.1.1")

    // CameraX（攝影機操作）
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")

    // OkHttp（WebSocket 連線）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

> 和 Python 的 `requirements.txt` 一樣的概念！

---

## 第三層：Layout XML —— 畫面長什麼樣

### activity_main.xml（主畫面）

```xml
<LinearLayout android:orientation="vertical" android:padding="16dp">

    <!-- Server URL 輸入框 -->
    <EditText android:id="@+id/etServerUrl"
        android:hint="Server URL" />

    <!-- 註冊區域：ID 輸入 + 按鈕 -->
    <EditText android:id="@+id/etMyId" android:hint="輸入你的ID" />
    <Button android:id="@+id/btnRegister" android:text="註冊" />

    <!-- 狀態顯示 -->
    <TextView android:id="@+id/tvStatus" android:text="未連接" />

    <!-- 撥打區域 -->
    <EditText android:id="@+id/etCallId" android:hint="輸入要撥打的ID" />
    <Button android:id="@+id/btnCall" android:text="撥打" />

    <!-- 接聽 / 拒絕 -->
    <Button android:id="@+id/btnAnswer" android:text="接聽" />
    <Button android:id="@+id/btnDecline" android:text="拒絕" />

    <!-- 線上使用者列表 -->
    <RecyclerView android:id="@+id/rvOnlineUsers" />
</LinearLayout>
```

---

## XML 與 Kotlin 的對應

<div class="info-card">

每個 XML 元件透過 **`android:id`** 和 Kotlin 程式碼連結：

| XML（畫面） | Kotlin（程式） | 作用 |
|------------|---------------|------|
| `@+id/etServerUrl` | `findViewById(R.id.etServerUrl)` | 取得輸入框 |
| `@+id/btnRegister` | `btnRegister.setOnClickListener { }` | 按鈕點擊事件 |
| `@+id/tvStatus` | `tvStatus.text = "已連接"` | 更新文字 |
| `@+id/rvOnlineUsers` | `rvOnlineUsers.adapter = usersAdapter` | 綁定資料列表 |

</div>

> `R.id.xxx` 是 Android 自動產生的，把 XML 的 id 對應成 Kotlin 可以用的常數。

---

## 第四層：Activity —— 程式的進入點

### Activity 生命週期

```
onCreate()  → App 開啟，初始化一切
    ↓
onResume()  → 畫面可見，可以互動
    ↓
（使用者操作中...）
    ↓
onDestroy() → App 關閉，清理資源
```

---

## MainActivity.kt 拆解（1/4）

### onCreate —— 一切的起點

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)  // 載入 XML 畫面

        // 1. 請求權限（相機、麥克風）
        if (checkSelfPermission(CAMERA) != GRANTED) {
            requestPermissions(arrayOf(CAMERA, RECORD_AUDIO), 1)
        }

        // 2. 從 SharedPreferences 載入設定
        websocketUrl = prefs.getString("server_url", DEFAULT_URL)

        // 3. 初始化各元件
        initViews()          // 連結 XML 元件
        initWebSocket()      // 建立 WebSocket 連線
        setupClickListeners() // 綁定按鈕事件
        setupRecyclerView()  // 設定使用者列表
    }
}
```

---

## MainActivity.kt 拆解（2/4）

### initViews —— 把 XML 和 Kotlin 連起來

```kotlin
private fun initViews() {
    etMyId = findViewById(R.id.etMyId)
    btnRegister = findViewById(R.id.btnRegister)
    etCallId = findViewById(R.id.etCallId)
    btnCall = findViewById(R.id.btnCall)
    btnAnswer = findViewById(R.id.btnAnswer)
    btnDecline = findViewById(R.id.btnDecline)
    tvStatus = findViewById(R.id.tvStatus)
    rvOnlineUsers = findViewById(R.id.rvOnlineUsers)
    etServerUrl = findViewById(R.id.etServerUrl)

    etServerUrl.setText(websocketUrl)        // 填入已存的 URL
    etMyId.setText("User${(1000..9999).random()}")  // 隨機生成 ID

    updateUIState()  // 根據目前狀態更新按鈕可否點擊
}
```

<div class="highlight">

`findViewById` 是最基本的連結方式。
每個 `R.id.xxx` 對應到 XML 裡的 `android:id="@+id/xxx"`。

</div>

---

## MainActivity.kt 拆解（3/4）

### setupClickListeners —— 按下按鈕會發生什麼？

```kotlin
private fun setupClickListeners() {
    btnRegister.setOnClickListener {
        if (!isRegistered) {
            registerUser()   // 透過 WebSocket 發送註冊訊息
        } else {
            unregisterUser() // 取消註冊
        }
    }

    btnCall.setOnClickListener { makeCall() }
    btnAnswer.setOnClickListener { answerCall() }
    btnDecline.setOnClickListener { declineCall() }
}
```

<div class="info-card">

**`setOnClickListener { }`** 就是在說：「當這個按鈕被按下時，執行大括號裡的程式。」
這就是 UI 和邏輯之間的橋樑。

</div>

---

## MainActivity.kt 拆解（4/4）

### updateUIState —— 根據狀態控制畫面

```kotlin
private fun updateUIState() {
    btnRegister.text = if (isRegistered) "取消註冊" else "註冊"
    btnCall.isEnabled = isRegistered && incomingCall.isEmpty()
    btnAnswer.isEnabled = incomingCall.isNotEmpty()
    btnDecline.isEnabled = incomingCall.isNotEmpty()
    etMyId.isEnabled = !isRegistered
    etCallId.isEnabled = isRegistered && incomingCall.isEmpty()
}
```

<div class="highlight">

**核心概念**：用少數幾個狀態變數（`isRegistered`、`incomingCall`）控制整個畫面的行為。
每次狀態改變，就呼叫 `updateUIState()` 統一更新所有元件。

</div>

---

## WebSocket 在 App 裡怎麼用？

### 建立連線

```kotlin
private fun initWebSocket() {
    val request = Request.Builder().url(websocketUrl).build()

    webSocket = client.newWebSocket(request, object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            runOnUiThread { tvStatus.text = "已連接到服務器" }
            registerUser()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            runOnUiThread { handleSignalingMessage(text) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, ...) {
            runOnUiThread { tvStatus.text = "連接失敗: ${t.message}" }
        }
    })
}
```

---

## WebSocket 訊息處理

### 收到訊息後的分流邏輯

```kotlin
private fun handleSignalingMessage(message: String) {
    val json = JSONObject(message)

    when (json.getString("type")) {
        "register_success" → {
            isRegistered = true
            myId = json.getString("id")
            updateUIState()
        }
        "user_list" → {
            // 解析使用者列表，更新 RecyclerView
            usersAdapter.updateUsers(userList)
        }
        "incoming_call" → {
            incomingCall = json.getString("from")
            tvStatus.text = "來電: $incomingCall"
            updateUIState()
        }
        "call_accepted" → {
            startVideoCall(peerId, isCaller = true)
        }
    }
}
```

---

## runOnUiThread 是什麼？

<div class="columns">

<div class="method-box">

### 問題

WebSocket 的回調在**背景執行緒**執行，
但 Android 規定：只有**主執行緒**才能更新畫面。

</div>

<div class="method-box">

### 解法

```kotlin
// 錯誤：直接更新 UI
tvStatus.text = "已連接" // 會崩潰！

// 正確：切回主執行緒
runOnUiThread {
    tvStatus.text = "已連接" // 安全！
}
```

</div>

</div>

<div class="highlight">

**記住這個規則**：所有 UI 操作（改文字、顯示/隱藏、更新列表）都必須在 `runOnUiThread { }` 裡面。

</div>

---

## RecyclerView + Adapter

### 顯示線上使用者列表

<div class="columns">

<div class="info-card">

### RecyclerView

- Android 的「高效能列表」元件
- 只渲染畫面上可見的項目
- 滾動時回收並重用已離開畫面的元件

</div>

<div class="info-card">

### Adapter

- 告訴 RecyclerView「每一筆資料長什麼樣」
- 負責建立 / 綁定 / 回收每一項的 UI

</div>

</div>

```kotlin
// 設定 RecyclerView
usersAdapter = OnlineUsersAdapter { userId ->
    etCallId.setText(userId)  // 點擊使用者 → 填入 ID
}
rvOnlineUsers.layoutManager = LinearLayoutManager(this)
rvOnlineUsers.adapter = usersAdapter
```

---

## OnlineUsersAdapter.kt 拆解

```kotlin
class OnlineUsersAdapter(
    private val onUserClick: (String) -> Unit  // 點擊回調
) : RecyclerView.Adapter<...>() {

    private var users: List<String> = emptyList()

    // 更新資料
    fun updateUsers(newUsers: List<String>) {
        users = newUsers
        notifyDataSetChanged()  // 通知 RecyclerView 重新渲染
    }

    // 建立每一項的畫面
    override fun onCreateViewHolder(...): UserViewHolder { ... }

    // 綁定資料到畫面
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    // 總共幾筆
    override fun getItemCount(): Int = users.size
}
```

---

## 畫面跳轉：Intent

### 從 MainActivity 跳到 VideoCallActivity

```kotlin
private fun startVideoCall(peerId: String, isCaller: Boolean) {
    val intent = Intent(this, VideoCallActivity::class.java).apply {
        putExtra("PEER_ID", peerId)
        putExtra("MY_ID", myId)
        putExtra("IS_CALLER", isCaller)
        putExtra("WEBSOCKET_URL", websocketUrl)
    }
    startActivity(intent)  // 啟動新畫面
}
```

```kotlin
// VideoCallActivity 接收參數
override fun onCreate(savedInstanceState: Bundle?) {
    peerId = intent.getStringExtra("PEER_ID") ?: ""
    myId = intent.getStringExtra("MY_ID") ?: ""
    isCaller = intent.getBooleanExtra("IS_CALLER", false)
}
```

<div class="highlight">

**Intent** = Android 的「信件」，用來啟動新畫面並傳遞資料。
`putExtra` 裝信、`getStringExtra` 拆信。

</div>

---

## VideoCallActivity 的核心流程

```
onCreate()
  ├── initViews()        → 連結通話畫面的 UI 元件
  ├── initWebRTC()       → 初始化 WebRTC 引擎
  │     ├── PeerConnectionFactory → 建立工廠
  │     ├── localView.init()      → 初始化本地畫面
  │     ├── remoteView.init()     → 初始化遠端畫面
  │     └── createPeerConnection() → 建立連線物件
  ├── initWebSocket()    → 連到信令伺服器
  ├── startLocalVideo()  → 開啟攝影機、加入音視訊軌道
  └── if (isCaller)
        createOffer()    → 主呼方發送 SDP Offer
```

---

## SharedPreferences —— 本地儲存

### 記住使用者的設定

```kotlin
// 儲存
getSharedPreferences("video_call_prefs", MODE_PRIVATE)
    .edit()
    .putString("server_url", websocketUrl)
    .apply()

// 讀取
val url = getSharedPreferences("video_call_prefs", MODE_PRIVATE)
    .getString("server_url", DEFAULT_URL)
```

<div class="info-card">

**SharedPreferences** 就像 App 的「記事本」：
- 存小量的設定值（URL、使用者名稱）
- App 關閉後再打開，設定還在
- 不適合存大量資料（那要用資料庫）

</div>

---

## BroadcastReceiver —— 畫面之間的通知

### 通話結束時通知主畫面

```kotlin
// VideoCallActivity：通話結束時廣播
override fun onDestroy() {
    sendBroadcast(Intent("VIDEO_CALL_ENDED"))
    // 清理 WebRTC 資源...
}

// MainActivity：接收廣播
private val callEndedReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "VIDEO_CALL_ENDED") {
            initWebSocket()  // 重新建立 WebSocket
        }
    }
}
```

<div class="highlight">

**BroadcastReceiver** 像是 App 內部的「對講機」。
一個畫面廣播訊息，另一個畫面接收並做出反應。

</div>

---

## 完整資料流圖

```
使用者點擊「撥打」
    │
    ↓ setOnClickListener
makeCall()
    │
    ↓ webSocket.send(JSON)
WebSocket → 信令伺服器 → 對方 App
    │
    ↓ onMessage callback
handleSignalingMessage()
    │
    ↓ "call_accepted"
startVideoCall()
    │
    ↓ Intent + putExtra
VideoCallActivity.onCreate()
    │
    ├─→ initWebRTC()     → PeerConnection
    ├─→ initWebSocket()  → 信令通道
    ├─→ startLocalVideo() → 攝影機
    └─→ createOffer()    → SDP 交換 → 通話開始
```

---

## 總結：Android App 的組成元素

<div class="tricolumns">

<div class="info-card">

### 設定檔

- **Manifest** — 權限、畫面
- **build.gradle** — 依賴
- **SharedPreferences** — 本地儲存

</div>

<div class="info-card">

### 畫面

- **XML Layout** — UI 結構
- **Activity** — 畫面邏輯
- **Adapter** — 列表渲染

</div>

<div class="info-card">

### 通訊

- **WebSocket** — 信令
- **WebRTC** — 音視訊
- **Intent** — 畫面跳轉
- **Broadcast** — 內部通知

</div>

</div>

---

## 動手練習

<div class="method-box">

### 閱讀程式碼

1. 打開 `MainActivity.kt`，找到 `handleSignalingMessage`
2. 追蹤 `"incoming_call"` 的處理流程
3. 找到哪裡呼叫了 `updateUIState()`，畫出所有呼叫的位置

</div>

<div class="method-box">

### 修改嘗試

1. 把 `btnRegister` 的預設文字改成「連線」
2. 在 `tvStatus` 顯示目前的 WebSocket URL
3. 在 `handleSignalingMessage` 的 `"user_list"` 中加一行 Log

</div>

---

## 重點回顧

<div class="tricolumns">

<div class="info-card">

**XML ↔ Kotlin**

`android:id` 對應 `findViewById`，按鈕用 `setOnClickListener`

</div>

<div class="info-card">

**Activity 生命週期**

`onCreate` 初始化、`onResume` 恢復、`onDestroy` 清理

</div>

<div class="info-card">

**狀態驅動 UI**

少數變數控制畫面，狀態改變就呼叫 `updateUIState()`

</div>

</div>

> 學會讀懂一個 App，就能自己修改和擴充功能！
