package com.example.socialroboticslab.design

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.socialroboticslab.R
import com.example.socialroboticslab.util.PrefsManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * 對話設計介面：設定 Pipecat pipeline 的 LLM / STT / TTS / 系統提示詞。
 *
 * 設定流程：
 * 1. 使用者選擇供應商、模型、語音、撰寫提示詞
 * 2. 點擊 "Save & Apply" → 存入 SharedPreferences + POST 到 Pipecat 後端
 * 3. 進入 AudioStreamActivity 時，會自動帶入這些設定
 */
class ConversationDesignActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ConversationDesign"
    }

    // ── UI 元件 ──
    private lateinit var spinnerLlmProvider: Spinner
    private lateinit var spinnerLlmModel: Spinner
    private lateinit var spinnerSttProvider: Spinner
    private lateinit var spinnerTtsProvider: Spinner
    private lateinit var spinnerTtsVoice: Spinner
    private lateinit var seekTemperature: SeekBar
    private lateinit var tvTemperatureValue: TextView
    private lateinit var spinnerLanguage: Spinner
    private lateinit var etSystemPrompt: EditText

    private val httpClient = OkHttpClient()

    // ── 供應商選項 ──

    private val llmProviders = listOf("openai", "anthropic")
    private val llmModels = mapOf(
        "openai" to listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo"),
        "anthropic" to listOf("claude-sonnet-4-20250514", "claude-haiku-4-5-20251001"),
    )

    private val sttProviders = listOf("deepgram", "whisper")

    private val ttsProviders = listOf("elevenlabs", "openai")
    private val ttsVoices = mapOf(
        "elevenlabs" to listOf("rachel", "adam", "bella", "josh"),
        "openai" to listOf("alloy", "echo", "fable", "nova", "onyx", "shimmer"),
    )

    private val languages = listOf("zh-TW", "en-US", "ja-JP")

    // ── 預設提示詞模板 ──

    private val presetMuseum = """你是一名在台大磯永吉小屋工作的機器人阿蓬，負責與參觀完畢的參與者互動。
你要用繁體中文回答問題，保持禮貌並使用簡單的語言，讓參與者感到輕鬆愉快。
依序詢問：
1. 今天參觀中印象最深刻的事情
2. 會想要再來參觀或推薦家人朋友來嗎？
3. 是否有其他問題或對展覽的建議？
每次回覆請簡短，不超過 100 字。"""

    private val presetInterview = """你是一個訪談機器人，負責與使用者進行半結構式訪談。
請依照以下流程：
1. 先自我介紹並說明訪談目的
2. 依序提出準備好的問題
3. 根據回答進行追問
4. 最後感謝受訪者
保持友善、專業的語氣，用繁體中文對話。"""

    private val presetFreeChat = """你是一個友善的社交機器人助手。
你會用繁體中文回答問題，保持禮貌並使用簡單的語言。
每次回覆請簡短，不超過 100 字。"""

    // ── Lifecycle ──

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation_design)

        supportActionBar?.title = "Conversation Design"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupSpinnerListeners()
        setupButtons()
        loadSettings()
    }

    private fun initViews() {
        spinnerLlmProvider = findViewById(R.id.spinnerLlmProvider)
        spinnerLlmModel = findViewById(R.id.spinnerLlmModel)
        spinnerSttProvider = findViewById(R.id.spinnerSttProvider)
        spinnerTtsProvider = findViewById(R.id.spinnerTtsProvider)
        spinnerTtsVoice = findViewById(R.id.spinnerTtsVoice)
        seekTemperature = findViewById(R.id.seekTemperature)
        tvTemperatureValue = findViewById(R.id.tvTemperatureValue)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        etSystemPrompt = findViewById(R.id.etSystemPrompt)

        // 設定 Spinner adapter
        spinnerLlmProvider.adapter = createAdapter(llmProviders)
        spinnerSttProvider.adapter = createAdapter(sttProviders)
        spinnerTtsProvider.adapter = createAdapter(ttsProviders)
        spinnerLanguage.adapter = createAdapter(languages)
    }

    private fun setupSpinnerListeners() {
        // LLM provider 變更 → 更新 model 選項
        spinnerLlmProvider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val provider = llmProviders[pos]
                val models = llmModels[provider] ?: emptyList()
                spinnerLlmModel.adapter = createAdapter(models)

                // 恢復已存的 model 選擇
                val savedModel = PrefsManager.getLlmModel(this@ConversationDesignActivity)
                val modelIdx = models.indexOf(savedModel)
                if (modelIdx >= 0) spinnerLlmModel.setSelection(modelIdx)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // TTS provider 變更 → 更新 voice 選項
        spinnerTtsProvider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val provider = ttsProviders[pos]
                val voices = ttsVoices[provider] ?: emptyList()
                spinnerTtsVoice.adapter = createAdapter(voices)

                // 恢復已存的 voice 選擇
                val savedVoice = PrefsManager.getTtsVoice(this@ConversationDesignActivity)
                val voiceIdx = voices.indexOf(savedVoice)
                if (voiceIdx >= 0) spinnerTtsVoice.setSelection(voiceIdx)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Temperature SeekBar
        seekTemperature.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val temp = progress / 100f
                tvTemperatureValue.text = String.format("%.2f", temp)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        // 預設模板按鈕
        findViewById<Button>(R.id.btnPresetMuseum).setOnClickListener {
            etSystemPrompt.setText(presetMuseum)
        }
        findViewById<Button>(R.id.btnPresetInterview).setOnClickListener {
            etSystemPrompt.setText(presetInterview)
        }
        findViewById<Button>(R.id.btnPresetFreeChat).setOnClickListener {
            etSystemPrompt.setText(presetFreeChat)
        }

        // Save & Apply
        findViewById<Button>(R.id.btnSaveApply).setOnClickListener {
            saveSettings()
            postConfigToServer()
        }

        // Reset Defaults
        findViewById<Button>(R.id.btnResetDefaults).setOnClickListener {
            resetDefaults()
        }
    }

    // ── 載入 / 儲存 ──

    private fun loadSettings() {
        val llmProvider = PrefsManager.getLlmProvider(this)
        val sttProvider = PrefsManager.getSttProvider(this)
        val ttsProvider = PrefsManager.getTtsProvider(this)
        val temperature = PrefsManager.getTemperature(this)
        val language = PrefsManager.getLanguage(this)
        val systemPrompt = PrefsManager.getSystemPrompt(this)

        spinnerLlmProvider.setSelection(llmProviders.indexOf(llmProvider).coerceAtLeast(0))
        spinnerSttProvider.setSelection(sttProviders.indexOf(sttProvider).coerceAtLeast(0))
        spinnerTtsProvider.setSelection(ttsProviders.indexOf(ttsProvider).coerceAtLeast(0))
        spinnerLanguage.setSelection(languages.indexOf(language).coerceAtLeast(0))

        seekTemperature.progress = (temperature * 100).toInt()
        tvTemperatureValue.text = String.format("%.2f", temperature)

        etSystemPrompt.setText(systemPrompt)
    }

    private fun saveSettings() {
        PrefsManager.setLlmProvider(this, spinnerLlmProvider.selectedItem as String)
        PrefsManager.setLlmModel(this, spinnerLlmModel.selectedItem as String)
        PrefsManager.setSttProvider(this, spinnerSttProvider.selectedItem as String)
        PrefsManager.setTtsProvider(this, spinnerTtsProvider.selectedItem as String)
        PrefsManager.setTtsVoice(this, spinnerTtsVoice.selectedItem as String)
        PrefsManager.setTemperature(this, seekTemperature.progress / 100f)
        PrefsManager.setLanguage(this, spinnerLanguage.selectedItem as String)
        PrefsManager.setSystemPrompt(this, etSystemPrompt.text.toString().trim())

        Toast.makeText(this, "設定已儲存", Toast.LENGTH_SHORT).show()
    }

    private fun resetDefaults() {
        spinnerLlmProvider.setSelection(llmProviders.indexOf(PrefsManager.DEFAULT_LLM_PROVIDER))
        spinnerSttProvider.setSelection(sttProviders.indexOf(PrefsManager.DEFAULT_STT_PROVIDER))
        spinnerTtsProvider.setSelection(ttsProviders.indexOf(PrefsManager.DEFAULT_TTS_PROVIDER))
        spinnerLanguage.setSelection(languages.indexOf(PrefsManager.DEFAULT_LANGUAGE))
        seekTemperature.progress = (PrefsManager.DEFAULT_TEMPERATURE * 100).toInt()
        etSystemPrompt.setText(PrefsManager.DEFAULT_SYSTEM_PROMPT)
        saveSettings()
    }

    // ── POST config 到 Pipecat 後端 ──

    private fun postConfigToServer() {
        val wsUrl = PrefsManager.getWsAudioUrl(this)
        // 從 ws://host:port 取得 host，改為 http://host:8080
        val configUrl = wsUrl
            .replace("ws://", "http://")
            .replace("wss://", "https://")
            .replace(Regex(":\\d+$"), ":8080") + "/api/config"

        val json = JSONObject().apply {
            put("session_id", "android_user")
            put("llm_provider", spinnerLlmProvider.selectedItem as String)
            put("llm_model", spinnerLlmModel.selectedItem as String)
            put("stt_provider", spinnerSttProvider.selectedItem as String)
            put("tts_provider", spinnerTtsProvider.selectedItem as String)
            put("tts_voice", spinnerTtsVoice.selectedItem as String)
            put("system_prompt", etSystemPrompt.text.toString().trim())
            put("temperature", seekTemperature.progress / 100.0)
            put("language", spinnerLanguage.selectedItem as String)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(configUrl).post(body).build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "Config POST failed (server may not be running): ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ConversationDesignActivity,
                        "設定已存本地（伺服器未回應）", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                Log.d(TAG, "Config POST success")
                runOnUiThread {
                    Toast.makeText(this@ConversationDesignActivity,
                        "設定已套用到伺服器", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // ── 工具 ──

    private fun createAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, items).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
