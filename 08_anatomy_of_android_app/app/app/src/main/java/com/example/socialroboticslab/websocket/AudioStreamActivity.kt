package com.example.socialroboticslab.websocket

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.socialroboticslab.R
import com.example.socialroboticslab.util.ExpressionVideoHelper
import com.example.socialroboticslab.util.PrefsManager
import com.nuwarobotics.service.IClientId
import com.nuwarobotics.service.agent.NuwaRobotAPI
import com.nuwarobotics.service.agent.RobotEventListener
import com.nuwarobotics.service.agent.SimpleGrammarData
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Ch3 對應功能：WebSocket Audio Streaming + Robot Expression
 *
 * 流程：
 * 1. 連線 WebSocket 伺服器
 * 2. 錄音 → Base64 編碼 → 透過 WebSocket 傳送
 * 3. 接收伺服器回傳的音訊 → AudioTrack 播放
 * 4. 根據伺服器回傳的 emotion 切換表情影片
 * 5. 機器人同步播放對應的肢體動作
 *
 * 長按感測器（sensor value 3）→ 返回主畫面
 */
class AudioStreamActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AudioStreamActivity"
        private const val SAMPLE_RATE = 24000
    }

    // UI
    private lateinit var videoView: VideoView
    private lateinit var tvSubtitle: TextView

    // Robot
    private lateinit var mRobotAPI: NuwaRobotAPI
    private lateinit var expressionHelper: ExpressionVideoHelper
    private val mainHandler = Handler(Looper.getMainLooper())

    // Audio
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isPlayingAudio = false

    // WebSocket
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private var isConnected = false

    // State
    private val personalityType = "e"
    private var currentStatus = "idling"

    // 動作對應表
    private val motionMap = mapOf(
        "idling" to "666_SA_Discover",
        "thinking" to "666_PE_PushGlasses",
        "listening" to "666_SA_Think",
        "speaking" to "666_RE_Ask",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_stream)

        videoView = findViewById(R.id.videoView)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        expressionHelper = ExpressionVideoHelper(this)

        initRobot()
        playExpression("idling")
        tvSubtitle.text = "初始化中..."
    }

    // ── Robot 初始化 ──

    private fun initRobot() {
        val clientId = IClientId(packageName)
        mRobotAPI = NuwaRobotAPI(this, clientId)
        mRobotAPI.registerRobotEventListener(robotEventListener)
    }

    // ── RobotEventListener ──

    private val robotEventListener = object : RobotEventListener {

        override fun onWikiServiceStart() {
            Log.d(TAG, "Robot service ready")
            mRobotAPI.requestSensor(
                NuwaRobotAPI.SENSOR_TOUCH or NuwaRobotAPI.SENSOR_PIR or NuwaRobotAPI.SENSOR_DROP
            )
            val grammar = SimpleGrammarData("example")
            grammar.addSlot("your command")
            grammar.updateBody()
            mRobotAPI.createGrammar(grammar.grammar, grammar.body)

            mainHandler.post { connectAndStartStreaming() }
        }

        override fun onLongPress(value: Int) {
            Log.d(TAG, "onLongPress: $value")
            if (value == 3) {
                mainHandler.post { finish() }
            }
        }

        override fun onTouchEvent(type: Int, value: Int) {
            Log.d(TAG, "onTouchEvent: type=$type value=$value")
            mainHandler.post { finish() }
        }

        override fun onCompleteOfMotionPlay(s: String?) {}
        override fun onStartOfMotionPlay(s: String?) {}
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
        override fun onErrorOfMotionPlay(i: Int) {}
        override fun onPlayBackOfMotionPlay(s: String?) {}
        override fun onStopOfMotionPlay(s: String?) {}
        override fun onPauseOfMotionPlay(s: String?) {}
    }

    // ── WebSocket 連線 ──

    private fun connectAndStartStreaming() {
        // 先將 pipeline 設定 POST 到 Pipecat 後端
        postPipelineConfig()

        val url = PrefsManager.getWsAudioUrl(this)
        tvSubtitle.text = "連線中: $url"

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected = true
                mainHandler.post {
                    tvSubtitle.text = "已連線，開始聆聽..."
                    setStatus("listening")
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failed: ${t.message}")
                isConnected = false
                mainHandler.post {
                    tvSubtitle.text = "連線失敗: ${t.message}"
                    playExpression("error")
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                isConnected = false
                mainHandler.post {
                    tvSubtitle.text = "已斷線"
                    playExpression("idling")
                }
            }
        })
    }

    // ── 處理伺服器訊息 ──

    private fun handleServerMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            when (type) {
                "response.audio.delta" -> {
                    val audioB64 = json.optString("delta", "")
                    if (audioB64.isNotEmpty()) {
                        // 切換到 speaking 狀態
                        if (!isPlayingAudio) {
                            isPlayingAudio = true
                            mainHandler.post { setStatus("speaking") }
                        }
                        playAudioData(audioB64)
                    }
                }

                "response.audio.done" -> {
                    Log.d(TAG, "Audio response complete")
                    isPlayingAudio = false
                    mainHandler.post { setStatus("listening") }
                }

                "response.emotion_transcript" -> {
                    val emotion = json.optString("emotion", "neutral")
                    val transcript = json.optString("transcript", "")
                    mainHandler.post {
                        if (transcript.isNotEmpty()) tvSubtitle.text = transcript
                        // 更新表情（speaking 狀態使用 emotion）
                        if (currentStatus == "speaking") {
                            playExpression(emotion, loop = true)
                        }
                    }
                }

                "user.login.success" -> {
                    Log.d(TAG, "Login success")
                }

                else -> {
                    Log.d(TAG, "Unknown message type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Message parse error: ${e.message}")
        }
    }

    // ── 狀態管理 ──

    private fun setStatus(status: String) {
        currentStatus = status
        Log.d(TAG, "setStatus: $status")

        // 播放對應動作
        val motion = motionMap[status]
        if (!motion.isNullOrEmpty()) {
            if (status != "thinking" || Math.random() > 0.5) {
                mRobotAPI.motionPlay(motion, true)
            }
        }

        when (status) {
            "listening" -> {
                playExpression("listening")
                tvSubtitle.text = "聆聽中..."
                startRecording()
                // 通知伺服器開始接收音訊
                sendWsMessage("input_audio_buffer.start")
            }
            "speaking" -> {
                playExpression("neutral", loop = true)
                stopRecording()
            }
            "idling" -> {
                playExpression("idling")
                stopRecording()
            }
        }
    }

    // ── 音訊錄製 ──

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            tvSubtitle.text = "需要麥克風權限"
            return
        }

        if (isRecording) return

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )

        isRecording = true
        audioRecord?.startRecording()

        Thread {
            val buffer = ByteArray(bufferSize)
            while (isRecording && isConnected) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val encoded = Base64.encodeToString(buffer.copyOf(read), Base64.NO_WRAP)
                    val msg = JSONObject().apply {
                        put("type", "input_audio_buffer.append")
                        put("audio", encoded)
                    }
                    webSocket?.send(msg.toString())
                }
            }

            // 錄音停止時通知伺服器
            if (isConnected) {
                sendWsMessage("input_audio_buffer.stop")
            }
        }.start()
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    // ── 音訊播放 ──

    private fun playAudioData(base64Audio: String) {
        Thread {
            try {
                val audioBytes = Base64.decode(base64Audio, Base64.NO_WRAP)
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(bufferSize, audioBytes.size))
                    .build()
                track.play()
                track.write(audioBytes, 0, audioBytes.size)
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback error: ${e.message}")
            }
        }.start()
    }

    // ── 表情 ──

    private fun playExpression(baseEmotion: String, loop: Boolean = false) {
        expressionHelper.playExpression(videoView, personalityType, baseEmotion, loop)
    }

    // ── Pipeline Config ──

    private fun postPipelineConfig() {
        val wsUrl = PrefsManager.getWsAudioUrl(this)
        val configUrl = wsUrl
            .replace("ws://", "http://")
            .replace("wss://", "https://")
            .replace(Regex(":\\d+$"), ":8080") + "/api/config"

        val json = JSONObject().apply {
            put("session_id", "android_user")
            put("llm_provider", PrefsManager.getLlmProvider(this@AudioStreamActivity))
            put("llm_model", PrefsManager.getLlmModel(this@AudioStreamActivity))
            put("stt_provider", PrefsManager.getSttProvider(this@AudioStreamActivity))
            put("tts_provider", PrefsManager.getTtsProvider(this@AudioStreamActivity))
            put("tts_voice", PrefsManager.getTtsVoice(this@AudioStreamActivity))
            put("system_prompt", PrefsManager.getSystemPrompt(this@AudioStreamActivity))
            put("temperature", PrefsManager.getTemperature(this@AudioStreamActivity).toDouble())
            put("language", PrefsManager.getLanguage(this@AudioStreamActivity))
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(configUrl).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Config POST failed (using defaults): ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                response.close()
                Log.d(TAG, "Pipeline config sent successfully")
            }
        })
    }

    // ── WebSocket 工具 ──

    private fun sendWsMessage(type: String) {
        val msg = JSONObject().apply { put("type", type) }
        webSocket?.send(msg.toString())
    }

    // ── Lifecycle ──

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        webSocket?.close(1000, "Activity destroyed")
        mRobotAPI.motionStop(true)
        mRobotAPI.release()
        videoView.stopPlayback()
    }
}
