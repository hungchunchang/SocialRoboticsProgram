package com.example.socialroboticslab.http

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.socialroboticslab.R
import com.example.socialroboticslab.util.ExpressionVideoHelper
import com.example.socialroboticslab.util.PrefsManager
import com.nuwarobotics.service.IClientId
import com.nuwarobotics.service.agent.NuwaRobotAPI
import com.nuwarobotics.service.agent.RobotEventListener
import com.nuwarobotics.service.agent.SimpleGrammarData
import com.nuwarobotics.service.agent.VoiceEventListener
import com.nuwarobotics.service.agent.VoiceResultJsonParser
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Ch2 對應功能：HTTP Chat + Robot TTS/STT
 *
 * 流程：
 * 1. 機器人 STT（語音辨識）→ 取得使用者語音文字
 * 2. 透過 HTTP POST 傳送至後端
 * 3. 後端回傳回覆 + 情緒
 * 4. 機器人 TTS 朗讀回覆 + 播放對應表情影片
 * 5. TTS 完成後自動回到 listening 狀態
 *
 * 長按感測器（sensor value 3）→ 返回主畫面
 */
class HttpChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HttpChatActivity"
    }

    // UI
    private lateinit var videoView: VideoView
    private lateinit var tvSubtitle: TextView

    // Robot
    private lateinit var mRobotAPI: NuwaRobotAPI
    private lateinit var expressionHelper: ExpressionVideoHelper
    private val mainHandler = Handler(Looper.getMainLooper())

    // HTTP
    private val client = OkHttpClient()

    // State
    private val personalityType = "e"
    private val userName = "android_user"
    private var isEnding = false

    // 動作對應表
    private val motionMap = mapOf(
        "idling" to "666_SA_Discover",
        "thinking" to "666_PE_PushGlasses",
        "listening" to "666_SA_Think",
        "speaking" to "666_RE_Ask",
        "bye" to "666_RE_Bye",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_http_chat)

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

    // ── RobotEventListener：感測器、動作完成 ──

    private val robotEventListener = object : RobotEventListener {

        override fun onWikiServiceStart() {
            Log.d(TAG, "Robot service ready")
            mRobotAPI.requestSensor(
                NuwaRobotAPI.SENSOR_TOUCH or NuwaRobotAPI.SENSOR_PIR or NuwaRobotAPI.SENSOR_DROP
            )
            mRobotAPI.registerVoiceEventListener(voiceEventListener)

            // 準備語法
            val grammar = SimpleGrammarData("example")
            grammar.addSlot("your command")
            grammar.updateBody()
            mRobotAPI.createGrammar(grammar.grammar, grammar.body)

            // 開始聆聽
            mainHandler.post { setStatus("listening") }
        }

        override fun onLongPress(value: Int) {
            Log.d(TAG, "onLongPress: $value")
            if (value == 3) {
                mainHandler.post { finish() }
            }
        }

        override fun onTouchEvent(type: Int, value: Int) {
            Log.d(TAG, "onTouchEvent: type=$type value=$value")
            // 觸碰感測器也可返回主畫面
            mainHandler.post { finish() }
        }

        override fun onCompleteOfMotionPlay(name: String?) {
            Log.d(TAG, "Motion complete: $name")
            if (name == "666_RE_Bye") {
                mainHandler.post { finish() }
            }
        }

        // 其他必要但不使用的回調
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

    // ── VoiceEventListener：TTS 完成、STT 結果 ──

    private val voiceEventListener = object : VoiceEventListener {

        override fun onTTSComplete(isError: Boolean) {
            Log.d(TAG, "TTS complete, isError=$isError")
            mainHandler.post {
                if (isEnding) {
                    // 結束對話，播放再見動作
                    playMotion("bye")
                    playExpression("idling")
                } else {
                    setStatus("listening")
                }
            }
        }

        override fun onMixUnderstandComplete(
            isError: Boolean,
            resultType: VoiceEventListener.ResultType?,
            json: String?
        ) {
            Log.d(TAG, "MixUnderstand complete: isError=$isError, json=$json")

            val resultText = parseSTTResult(json)

            if (isError || resultText.isNullOrBlank()) {
                Log.w(TAG, "STT empty or error, restart listening")
                mainHandler.post { setStatus("listening") }
                return
            }

            Log.d(TAG, "User said: $resultText")
            mainHandler.post {
                tvSubtitle.text = resultText
                setStatus("thinking", resultText)
            }
        }

        // 不使用的回調
        override fun onWakeup(isError: Boolean, score: String?, direction: Float) {}
        override fun onSpeechRecognizeComplete(
            isError: Boolean,
            resultType: VoiceEventListener.ResultType?,
            json: String?
        ) {}
        override fun onSpeech2TextComplete(isError: Boolean, json: String?) {}
        override fun onSpeechState(
            listenType: VoiceEventListener.ListenType?,
            speechState: VoiceEventListener.SpeechState?
        ) {}
        override fun onSpeakState(
            speakType: VoiceEventListener.SpeakType?,
            speakState: VoiceEventListener.SpeakState?
        ) {}
        override fun onGrammarState(isError: Boolean, s: String?) {}
        override fun onListenVolumeChanged(listenType: VoiceEventListener.ListenType?, i: Int) {}
        override fun onHotwordChange(
            hotwordState: VoiceEventListener.HotwordState?,
            hotwordType: VoiceEventListener.HotwordType?,
            s: String?
        ) {}
    }

    // ── 狀態管理 ──

    private fun setStatus(status: String, resultText: String? = null, emotion: String? = null) {
        Log.d(TAG, "setStatus: $status")

        // 播放對應動作
        playMotion(status)

        when (status) {
            "idling" -> {
                playExpression("idling")
            }
            "listening" -> {
                playExpression("listening")
                tvSubtitle.text = "聆聽中..."
                mRobotAPI.startMixUnderstand()
            }
            "thinking" -> {
                playExpression("thinking")
                tvSubtitle.text = "思考中..."
                mRobotAPI.stopListen()
                if (!resultText.isNullOrBlank()) {
                    sendToBackend(resultText)
                }
            }
            "speaking" -> {
                val expressionEmotion = if (!emotion.isNullOrBlank()) emotion else "neutral"
                playExpression(expressionEmotion, loop = true)
                if (!resultText.isNullOrBlank()) {
                    tvSubtitle.text = resultText
                    mRobotAPI.startTTS(resultText)
                }
            }
            "ending" -> {
                isEnding = true
                setStatus("speaking", resultText, emotion)
            }
        }
    }

    // ── 表情 & 動作 ──

    private fun playExpression(baseEmotion: String, loop: Boolean = false) {
        expressionHelper.playExpression(videoView, personalityType, baseEmotion, loop)
    }

    private fun playMotion(status: String) {
        val motion = motionMap[status] ?: return
        if (motion.isNotEmpty()) {
            if (status != "thinking" || Math.random() > 0.5) {
                mRobotAPI.motionPlay(motion, true)
            }
        }
    }

    // ── HTTP 通訊 ──

    private fun sendToBackend(message: String) {
        val baseUrl = PrefsManager.getHttpUrl(this).trimEnd('/')
        val url = "$baseUrl/api/chat"

        val json = JSONObject().apply {
            put("message", message)
            put("user_name", userName)
            put("user_id", userName)
            put("robot_mbti", personalityType.uppercase())
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "HTTP error: ${e.message}")
                mainHandler.post {
                    tvSubtitle.text = "連線錯誤: ${e.message}"
                    setStatus("listening")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                mainHandler.post {
                    try {
                        val result = JSONObject(responseBody)
                        val question = result.optString("question", responseBody)
                        val emotion = result.optString("emotion", "neutral")
                        val isEnded = result.optBoolean("is_ended", false)

                        if (isEnded) {
                            setStatus("ending", question, emotion)
                        } else {
                            setStatus("speaking", question, emotion)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Parse error: ${e.message}")
                        setStatus("speaking", responseBody)
                    }
                }
            }
        })
    }

    // ── 工具方法 ──

    private fun parseSTTResult(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            obj.optString("result", null) ?: VoiceResultJsonParser.parseVoiceResult(json)
        } catch (e: Exception) {
            json // 若非 JSON，直接返回原始字串
        }
    }

    // ── Lifecycle ──

    override fun onDestroy() {
        super.onDestroy()
        mRobotAPI.stopTTS()
        mRobotAPI.stopListen()
        mRobotAPI.motionStop(true)
        mRobotAPI.release()
        videoView.stopPlayback()
    }
}
