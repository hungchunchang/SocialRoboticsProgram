package com.example.socialroboticslab.woz

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.socialroboticslab.R
import com.example.socialroboticslab.util.ExpressionVideoHelper
import com.example.socialroboticslab.util.PrefsManager
import com.nuwarobotics.service.IClientId
import com.nuwarobotics.service.agent.NuwaRobotAPI
import com.nuwarobotics.service.agent.RobotEventListener
import com.nuwarobotics.service.agent.VoiceEventListener
import okhttp3.*
import org.json.JSONObject
import org.webrtc.*

/**
 * Ch6 對應功能：WoZ（Wizard of Oz）多機器人控制客戶端
 *
 * 流程：
 * 1. 連線 WebSocket 伺服器並註冊為機器人角色
 * 2. 等待控制端分配角色
 * 3. 進入控制模式後顯示全螢幕表情影片
 * 4. 接收並執行指令（speak / gesture / expression / stop）
 * 5. 執行腳本台詞（play_line）
 * 6. 支援視訊通話（start_video_call / end_video_call）
 * 7. 回報機器人狀態給伺服器
 *
 * 長按感測器（sensor value 3）→ 返回主畫面
 */
class WozRobotActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "WozRobotActivity"
    }

    // ── UI Views ──
    private lateinit var setupView: View
    private lateinit var controlModeView: View
    private lateinit var videoCallView: View

    // Setup view widgets
    private lateinit var tvConnectionStatus: TextView
    private lateinit var etRobotId: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvAssignedRole: TextView
    private lateinit var tvCurrentCommand: TextView

    // Control mode widgets
    private lateinit var videoView: VideoView

    // Video call widgets
    private lateinit var remoteView: SurfaceViewRenderer
    private lateinit var localView: SurfaceViewRenderer
    private lateinit var ivRobotFaceMain: ImageView
    private lateinit var ivRobotFaceLocal: ImageView
    private lateinit var btnToggleCamera: Button
    private lateinit var btnToggleMute: Button
    private lateinit var btnHangUp: Button
    private lateinit var tvCallStatus: TextView

    // ── Robot ──
    private lateinit var mRobotAPI: NuwaRobotAPI
    private lateinit var expressionHelper: ExpressionVideoHelper
    private val mainHandler = Handler(Looper.getMainLooper())
    private val personalityType = "e"

    // ── WebSocket ──
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private var isWebSocketConnected = false

    // ── State ──
    private var myId: String = ""
    private var assignedRole: String = ""
    private var isRegistered = false
    private var isControlMode = false
    private var isInVideoCall = false
    private var shouldLoopVideo = false

    // ── WebRTC ──
    private var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var rootEglBase: EglBase? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var isCameraOn = true
    private var isMuted = false
    private var peerId: String = ""

    // ── 動作對應表 ──
    private val motionMap = mapOf(
        "揮手" to "666_SA_Discover",
        "點頭" to "666_PE_PushGlasses",
        "敬禮" to "666_RE_Ask",
        "雙手擺動" to "666_SA_Think",
        "鞠躬" to "666_RE_Bye",
        "wave" to "666_SA_Discover",
        "nod" to "666_PE_PushGlasses",
        "salute" to "666_RE_Ask",
        "bow" to "666_RE_Bye",
        "raise_hand" to "666_SA_Discover",
        "lower_hand" to "666_RE_Bye"
    )

    private enum class ViewMode { SETUP, CONTROL, VIDEO_CALL }

    // ══════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_woz_robot)

        initViews()
        expressionHelper = ExpressionVideoHelper(this)
        initRobot()
        initWebSocket()
    }

    private fun initViews() {
        // View containers
        setupView = findViewById(R.id.setupView)
        controlModeView = findViewById(R.id.controlModeView)
        videoCallView = findViewById(R.id.videoCallView)

        // Setup
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        etRobotId = findViewById(R.id.etRobotId)
        btnRegister = findViewById(R.id.btnRegister)
        tvStatus = findViewById(R.id.tvStatus)
        tvAssignedRole = findViewById(R.id.tvAssignedRole)
        tvCurrentCommand = findViewById(R.id.tvCurrentCommand)

        // Control mode
        videoView = findViewById(R.id.videoView)
        videoView.setOnCompletionListener {
            if (shouldLoopVideo) videoView.start()
        }

        // Video call
        remoteView = findViewById(R.id.remoteView)
        localView = findViewById(R.id.localView)
        ivRobotFaceMain = findViewById(R.id.ivRobotFaceMain)
        ivRobotFaceLocal = findViewById(R.id.ivRobotFaceLocal)
        btnToggleCamera = findViewById(R.id.btnToggleCamera)
        btnToggleMute = findViewById(R.id.btnToggleMute)
        btnHangUp = findViewById(R.id.btnHangUp)
        tvCallStatus = findViewById(R.id.tvCallStatus)

        // Generate random robot ID
        myId = "Robot_${(1000..9999).random()}"
        etRobotId.setText(myId)

        // Buttons
        btnRegister.setOnClickListener { toggleRegistration() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        btnToggleCamera.setOnClickListener { toggleCamera() }
        btnToggleMute.setOnClickListener { toggleMute() }
        btnHangUp.setOnClickListener { hangUp() }
    }

    // ══════════════════════════════════════
    // Robot 初始化
    // ══════════════════════════════════════

    private fun initRobot() {
        try {
            val clientId = IClientId(packageName)
            mRobotAPI = NuwaRobotAPI(this, clientId)
            mRobotAPI.registerRobotEventListener(robotEventListener)
            mRobotAPI.registerVoiceEventListener(voiceEventListener)
            Log.d(TAG, "NuwaRobotAPI initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init NuwaRobotAPI", e)
        }
    }

    private val robotEventListener = object : RobotEventListener {
        override fun onWikiServiceStart() {
            Log.d(TAG, "Robot service ready")
            mRobotAPI.requestSensor(
                NuwaRobotAPI.SENSOR_TOUCH or NuwaRobotAPI.SENSOR_PIR or NuwaRobotAPI.SENSOR_DROP
            )
        }

        override fun onLongPress(value: Int) {
            if (value == 3) mainHandler.post { finish() }
        }

        override fun onTouchEvent(type: Int, value: Int) {
            mainHandler.post { finish() }
        }

        override fun onStartOfMotionPlay(motion: String?) {
            Log.d(TAG, "Motion started: $motion")
            sendStatusToServer("motion_start", motion ?: "unknown", "busy")
            mainHandler.post { tvCurrentCommand.text = "動作開始: $motion" }
        }

        override fun onCompleteOfMotionPlay(motion: String?) {
            Log.d(TAG, "Motion completed: $motion")
            sendStatusToServer("motion_complete", motion ?: "unknown", "ready")
            mainHandler.post { tvCurrentCommand.text = "動作完成: $motion" }
        }

        override fun onErrorOfMotionPlay(errorCode: Int) {
            sendStatusToServer("motion_error", errorCode.toString(), "error")
        }

        override fun onTap(i: Int) {}
        override fun onPIREvent(i: Int) {}
        override fun onWindowSurfaceReady() {}
        override fun onWindowSurfaceDestroy() {}
        override fun onTouchEyes(i: Int, i1: Int) {}
        override fun onRawTouch(i: Int, i1: Int, i2: Int) {}
        override fun onFaceSpeaker(v: Float) {}
        override fun onActionEvent(i: Int, i1: Int) {}
        override fun onDropSensorEvent(i: Int) {}
        override fun onMotorErrorEvent(i: Int, i1: Int) {}
        override fun onWikiServiceStop() {}
        override fun onWikiServiceCrash() {}
        override fun onWikiServiceRecovery() {}
        override fun onPrepareMotion(b: Boolean, s: String?, v: Float) {}
        override fun onCameraOfMotionPlay(s: String?) {}
        override fun onGetCameraPose(
            v: Float, v1: Float, v2: Float, v3: Float, v4: Float, v5: Float,
            v6: Float, v7: Float, v8: Float, v9: Float, v10: Float, v11: Float
        ) {}
        override fun onPlayBackOfMotionPlay(s: String?) {}
        override fun onStopOfMotionPlay(s: String?) {}
        override fun onPauseOfMotionPlay(s: String?) {}
    }

    private val voiceEventListener = object : VoiceEventListener {
        override fun onTTSComplete(isError: Boolean) {
            Log.d(TAG, "TTS complete, error=$isError")
            sendStatusToServer("tts_complete", if (isError) "error" else "success",
                if (isError) "error" else "ready")
            // 停止 TTS 表情，回到 idling
            mainHandler.post {
                stopTTSEmotion()
                tvCurrentCommand.text = if (isError) "TTS錯誤" else "TTS完成"
            }
        }

        override fun onWakeup(isError: Boolean, score: String?, direction: Float) {}
        override fun onSpeechRecognizeComplete(isError: Boolean, resultType: VoiceEventListener.ResultType?, json: String?) {}
        override fun onSpeech2TextComplete(isError: Boolean, json: String?) {}
        override fun onMixUnderstandComplete(isError: Boolean, resultType: VoiceEventListener.ResultType?, s: String?) {}
        override fun onSpeechState(listenType: VoiceEventListener.ListenType?, speechState: VoiceEventListener.SpeechState?) {}
        override fun onSpeakState(speakType: VoiceEventListener.SpeakType?, speakState: VoiceEventListener.SpeakState?) {}
        override fun onGrammarState(isError: Boolean, s: String?) {}
        override fun onListenVolumeChanged(listenType: VoiceEventListener.ListenType?, i: Int) {}
        override fun onHotwordChange(hotwordState: VoiceEventListener.HotwordState?, hotwordType: VoiceEventListener.HotwordType?, s: String?) {}
    }

    // ══════════════════════════════════════
    // WebSocket 連線
    // ══════════════════════════════════════

    private fun initWebSocket() {
        val url = PrefsManager.getWozUrl(this)
        tvStatus.text = "連線中: $url"

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isWebSocketConnected = true
                runOnUiThread {
                    tvConnectionStatus.text = "已連接"
                    tvConnectionStatus.setTextColor(0xFF27ae60.toInt())
                    tvStatus.text = "已連線，請註冊機器人"
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                runOnUiThread { handleServerMessage(text) }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isWebSocketConnected = false
                runOnUiThread {
                    tvConnectionStatus.text = "連線失敗"
                    tvConnectionStatus.setTextColor(0xFFe74c3c.toInt())
                    tvStatus.text = "錯誤: ${t.message}"
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isWebSocketConnected = false
                runOnUiThread {
                    tvConnectionStatus.text = "已斷線"
                    tvConnectionStatus.setTextColor(0xFFe74c3c.toInt())
                    tvStatus.text = "連接關閉"
                }
            }
        })
    }

    // ══════════════════════════════════════
    // 註冊
    // ══════════════════════════════════════

    private fun toggleRegistration() {
        if (!isWebSocketConnected) {
            tvStatus.text = "尚未連線，無法註冊"
            return
        }

        if (isRegistered) {
            // 目前 Ch6 server 不支援取消註冊，重新連線即可
            webSocket?.close(1000, "Unregister")
            isRegistered = false
            assignedRole = ""
            btnRegister.text = "Register Robot"
            etRobotId.isEnabled = true
            tvAssignedRole.text = "未分配"
            tvStatus.text = "已取消註冊"
            // 重新連線
            initWebSocket()
            return
        }

        myId = etRobotId.text.toString().trim()
        if (myId.isEmpty()) {
            myId = "Robot_${(1000..9999).random()}"
            etRobotId.setText(myId)
        }

        val msg = JSONObject().apply {
            put("type", "register")
            put("id", myId)
            put("role", "robot")
        }
        webSocket?.send(msg.toString())
        tvStatus.text = "註冊中..."
    }

    // ══════════════════════════════════════
    // 處理伺服器訊息
    // ══════════════════════════════════════

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.getString("type")
            Log.d(TAG, "Server message: $type")

            when (type) {
                "register_success" -> {
                    isRegistered = true
                    myId = json.getString("id")
                    tvStatus.text = "已註冊: $myId"
                    btnRegister.text = "Unregister"
                    etRobotId.isEnabled = false
                }

                "register_error" -> {
                    tvStatus.text = "註冊失敗: ${json.getString("message")}"
                }

                "roles_assigned" -> {
                    val assignments = json.getJSONObject("assignments")
                    if (assignments.has(myId)) {
                        assignedRole = assignments.getString(myId)
                        tvAssignedRole.text = assignedRole
                        tvStatus.text = "角色已分配，等待控制指令"
                        sendStatusToServer("role_assigned", assignedRole, "ready")
                    }
                }

                "role_cleared" -> {
                    val robotId = json.getString("robotId")
                    if (robotId == myId) {
                        assignedRole = ""
                        tvAssignedRole.text = "未分配"
                        tvStatus.text = "等待角色分配"
                    }
                }

                "all_roles_reset" -> {
                    assignedRole = ""
                    tvAssignedRole.text = "未分配"
                    tvStatus.text = "等待角色分配"
                }

                "control_mode_started" -> {
                    isControlMode = true
                    showView(ViewMode.CONTROL)
                    playExpression("idling")
                    sendStatusToServer("control_mode_active", "face_shown", "ready")
                }

                "control_mode_stopped" -> {
                    isControlMode = false
                    showView(ViewMode.SETUP)
                    sendStatusToServer("control_mode_inactive", "face_hidden", "ready")
                }

                "robot_command" -> {
                    executeRobotCommand(json)
                }

                "play_line" -> {
                    executeScriptLine(json)
                }

                "start_video_call" -> {
                    val from = json.optString("from", "")
                    peerId = from
                    isInVideoCall = true
                    showView(ViewMode.VIDEO_CALL)
                    initWebRTC()
                    // AI讀 啟動攝影機並建立 offer
                    if (assignedRole == "AI讀") {
                        startLocalVideo()
                        mainHandler.postDelayed({ createOffer() }, 2000)
                    }
                }

                "end_video_call" -> {
                    if (isInVideoCall) {
                        isInVideoCall = false
                        cleanupWebRTC()
                        val targetView = if (isControlMode) ViewMode.CONTROL else ViewMode.SETUP
                        showView(targetView)
                    }
                }

                "offer", "answer", "ice_candidate" -> {
                    if (isInVideoCall) handleSignalingMessage(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Message parse error", e)
            tvStatus.text = "訊息處理錯誤: ${e.message}"
        }
    }

    // ══════════════════════════════════════
    // 指令執行
    // ══════════════════════════════════════

    private fun executeRobotCommand(json: JSONObject) {
        try {
            val targetRobot = json.optString("targetRobot", "")
            val action = json.getString("action")
            val content = json.getString("content")
            val parameters = json.optJSONObject("parameters")

            if (targetRobot != assignedRole && targetRobot.isNotEmpty()) {
                Log.d(TAG, "Command not for me ($assignedRole): $targetRobot")
                return
            }

            tvCurrentCommand.text = "執行: $action - $content"

            when (action) {
                "speak" -> {
                    startTTSEmotion()
                    mRobotAPI.startTTS(content)
                    sendStatusToServer("command_tts", "$action:$content", "speaking")
                }

                "gesture" -> {
                    val gestureType = parameters?.optString("gesture") ?: content
                    performMotion(gestureType)
                    sendStatusToServer("command_gesture", "$action:$gestureType", "busy")
                }

                "move" -> {
                    Log.d(TAG, "Move command: $content")
                    sendStatusToServer("command_move", "$action:$content", "moving")
                }

                "expression" -> {
                    playExpression(content)
                    sendStatusToServer("command_expression", "$action:$content", "busy")
                }

                "stop" -> {
                    mRobotAPI.motionStop(true)
                    mRobotAPI.stopTTS()
                    tvCurrentCommand.text = "所有動作已停止"
                    sendStatusToServer("command_stop", "all_actions_stopped", "ready")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Command error", e)
            sendStatusToServer("command_error", e.message ?: "unknown_error", "error")
        }
    }

    private fun executeScriptLine(json: JSONObject) {
        try {
            val scriptId = json.optString("scriptId", "")
            val lineId = json.optInt("lineId", 0)
            val lineData = json.optJSONObject("line")
            val targetRobots = json.optJSONArray("targetRobots")

            if (lineData == null || targetRobots == null) return

            val content = lineData.optString("content", "")
            val actions = lineData.optJSONArray("actions")

            // 檢查這個機器人是否應該執行
            var shouldExecute = false
            for (i in 0 until targetRobots.length()) {
                if (targetRobots.getString(i) == assignedRole) {
                    shouldExecute = true
                    break
                }
            }

            if (!shouldExecute) {
                Log.d(TAG, "Line $lineId not for this robot ($assignedRole)")
                return
            }

            tvCurrentCommand.text = "執行台詞: $content"

            if (content.isNotEmpty()) {
                startTTSEmotion()
                mRobotAPI.startTTS(content)
                sendStatusToServer("script_tts_start", "script_$scriptId: line_$lineId: $content", "speaking")
            }

            // 處理動作
            actions?.let { actionsArray ->
                for (i in 0 until actionsArray.length()) {
                    val action = actionsArray.getJSONObject(i)
                    val actionType = action.optString("type", "")
                    val actionValue = action.optString("value", "")

                    when (actionType) {
                        "gesture" -> {
                            performMotion(actionValue)
                            sendStatusToServer("script_gesture", "line_$lineId: $actionValue", "busy")
                        }
                        "emotion" -> {
                            playExpression(actionValue)
                            sendStatusToServer("script_emotion", "line_$lineId: $actionValue", "busy")
                        }
                        "video_call" -> {
                            // Video call will be triggered by server's start_video_call message
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Script execution error", e)
            sendStatusToServer("script_error", e.message ?: "unknown_error", "error")
        }
    }

    // ══════════════════════════════════════
    // 表情影片 & 動作
    // ══════════════════════════════════════

    private fun playExpression(emotion: String) {
        expressionHelper.playExpression(videoView, personalityType, emotion, shouldLoopVideo)
    }

    private fun startTTSEmotion() {
        shouldLoopVideo = true
        playExpression("neutral")
    }

    private fun stopTTSEmotion() {
        shouldLoopVideo = false
        playExpression("idling")
    }

    private fun performMotion(motionId: String) {
        if (::mRobotAPI.isInitialized) {
            // 嘗試從 motionMap 查找，如果沒有則直接使用
            val resolved = motionMap[motionId] ?: motionId
            mRobotAPI.motionPlay(resolved, true)
            Log.d(TAG, "Motion: $resolved")
        }
    }

    // ══════════════════════════════════════
    // View 切換
    // ══════════════════════════════════════

    private fun showView(mode: ViewMode) {
        setupView.visibility = if (mode == ViewMode.SETUP) View.VISIBLE else View.GONE
        controlModeView.visibility = if (mode == ViewMode.CONTROL) View.VISIBLE else View.GONE
        videoCallView.visibility = if (mode == ViewMode.VIDEO_CALL) View.VISIBLE else View.GONE
    }

    // ══════════════════════════════════════
    // 狀態回報
    // ══════════════════════════════════════

    private fun sendStatusToServer(eventType: String, data: String, status: String = "ready") {
        if (!isWebSocketConnected || webSocket == null) return
        val msg = JSONObject().apply {
            put("type", "robot_status")
            put("robotId", myId)
            put("status", status)
            put("eventType", eventType)
            put("data", data)
            put("assignedRole", assignedRole)
            put("battery", 85)
            put("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(msg.toString())
    }

    // ══════════════════════════════════════
    // WebRTC 視訊通話
    // ══════════════════════════════════════

    private fun initWebRTC() {
        rootEglBase = EglBase.create()

        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase!!.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase!!.eglBaseContext, true, true))
            .createPeerConnectionFactory()

        remoteView.init(rootEglBase!!.eglBaseContext, null)
        localView.init(rootEglBase!!.eglBaseContext, null)

        // 根據角色設定 UI
        if (assignedRole == "AI讀") {
            remoteView.visibility = View.GONE
            ivRobotFaceMain.visibility = View.VISIBLE
            localView.visibility = View.VISIBLE
            ivRobotFaceLocal.visibility = View.GONE
        } else {
            remoteView.visibility = View.VISIBLE
            ivRobotFaceMain.visibility = View.GONE
            localView.visibility = View.GONE
            ivRobotFaceLocal.visibility = View.VISIBLE
        }

        tvCallStatus.text = "視訊通話中..."
        createPeerConnection()
    }

    private fun createPeerConnection() {
        val stunUrl = PrefsManager.getStunUrl(this)
        val iceServers = listOf(
            PeerConnection.IceServer.builder(stunUrl).createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { sendIceCandidate(it) }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                runOnUiThread {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED -> tvCallStatus.text = "視訊連接成功"
                        PeerConnection.IceConnectionState.DISCONNECTED -> tvCallStatus.text = "連接中斷"
                        else -> {}
                    }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    remoteVideoTrack = track
                    runOnUiThread { remoteVideoTrack?.addSink(remoteView) }
                }
            }

            override fun onDataChannel(p0: DataChannel?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddStream(p0: MediaStream?) {}
        })
    }

    private fun startLocalVideo() {
        val enumerator = Camera2Enumerator(this)
        videoCapturer = enumerator.deviceNames
            .firstOrNull { enumerator.isFrontFacing(it) }
            ?.let { enumerator.createCapturer(it, null) }
            ?: return

        val videoSource = peerConnectionFactory!!.createVideoSource(false)
        val surfaceHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name, rootEglBase!!.eglBaseContext
        )
        videoCapturer?.initialize(surfaceHelper, this, videoSource.capturerObserver)
        videoCapturer?.startCapture(640, 480, 30)

        localVideoTrack = peerConnectionFactory!!.createVideoTrack("video", videoSource)
        if (assignedRole == "AI讀") {
            localVideoTrack?.addSink(localView)
        }

        val audioSource = peerConnectionFactory!!.createAudioSource(MediaConstraints())
        val audioTrack = peerConnectionFactory!!.createAudioTrack("audio", audioSource)

        peerConnection?.addTrack(localVideoTrack, listOf("local_stream"))
        peerConnection?.addTrack(audioTrack, listOf("local_stream"))
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { sendOffer(sdp!!) }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { sendAnswer(sdp!!) }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, constraints)
    }

    private fun handleSignalingMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.getString("type")
            val fromId = json.optString("from", "")

            when (type) {
                "offer" -> {
                    peerId = fromId
                    val sdp = json.getString("sdp")
                    val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() { createAnswer() }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, offer)
                }
                "answer" -> {
                    val sdp = json.getString("sdp")
                    val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() {}
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, answer)
                }
                "ice_candidate" -> {
                    val candidate = IceCandidate(
                        json.getString("sdpMid"),
                        json.getInt("sdpMLineIndex"),
                        json.getString("candidate")
                    )
                    peerConnection?.addIceCandidate(candidate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signaling error", e)
        }
    }

    private fun sendOffer(offer: SessionDescription) {
        val msg = JSONObject().apply {
            put("type", "offer")
            put("from", myId)
            put("to", peerId)
            put("sdp", offer.description)
        }
        webSocket?.send(msg.toString())
    }

    private fun sendAnswer(answer: SessionDescription) {
        val msg = JSONObject().apply {
            put("type", "answer")
            put("from", myId)
            put("to", peerId)
            put("sdp", answer.description)
        }
        webSocket?.send(msg.toString())
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val msg = JSONObject().apply {
            put("type", "ice_candidate")
            put("from", myId)
            put("to", peerId)
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }
        webSocket?.send(msg.toString())
    }

    private fun toggleCamera() {
        if (isCameraOn) {
            videoCapturer?.stopCapture()
        } else {
            videoCapturer?.startCapture(640, 480, 30)
        }
        isCameraOn = !isCameraOn
        btnToggleCamera.text = if (isCameraOn) "Cam" else "No Cam"
    }

    private fun toggleMute() {
        isMuted = !isMuted
        btnToggleMute.text = if (isMuted) "Unmute" else "Mute"
    }

    private fun hangUp() {
        val msg = JSONObject().apply {
            put("type", "call_ended")
            put("from", myId)
            put("to", peerId)
        }
        webSocket?.send(msg.toString())
        isInVideoCall = false
        cleanupWebRTC()
        val targetView = if (isControlMode) ViewMode.CONTROL else ViewMode.SETUP
        showView(targetView)
    }

    private fun cleanupWebRTC() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null
            localVideoTrack?.dispose()
            localVideoTrack = null
            remoteVideoTrack?.dispose()
            remoteVideoTrack = null
            peerConnection?.close()
            peerConnection = null
            peerConnectionFactory?.dispose()
            peerConnectionFactory = null
            rootEglBase?.release()
            rootEglBase = null
        } catch (e: Exception) {
            Log.e(TAG, "WebRTC cleanup error", e)
        }
    }

    // ══════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════

    override fun onPause() {
        super.onPause()
        sendStatusToServer("robot_offline", "app_paused", "offline")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::mRobotAPI.isInitialized) {
                mRobotAPI.motionStop(true)
                mRobotAPI.stopTTS()
                mRobotAPI.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Robot release error", e)
        }
        cleanupWebRTC()
        webSocket?.close(1000, "Activity destroyed")
        videoView.stopPlayback()
    }
}
