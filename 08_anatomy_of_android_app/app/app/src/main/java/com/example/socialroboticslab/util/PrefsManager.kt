package com.example.socialroboticslab.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 統一管理所有伺服器設定的 SharedPreferences 工具。
 * 各 Activity 透過此物件讀寫伺服器 URL，避免各自維護 key 字串。
 */
object PrefsManager {

    private const val PREF_NAME = "social_robotics_lab_prefs"

    // ── Keys ──
    private const val KEY_HTTP_URL = "http_url"
    private const val KEY_WS_AUDIO_URL = "ws_audio_url"
    private const val KEY_WEBRTC_STREAM_URL = "webrtc_stream_url"
    private const val KEY_VIDEO_CALL_URL = "video_call_url"
    private const val KEY_STUN_URL = "stun_url"
    private const val KEY_TURN_URL = "turn_url"
    private const val KEY_TURN_USER = "turn_user"
    private const val KEY_TURN_PASS = "turn_pass"
    private const val KEY_WOZ_URL = "woz_url"

    // ── Pipeline 設定 Keys（Pipecat 對話設計） ──
    private const val KEY_LLM_PROVIDER = "llm_provider"
    private const val KEY_LLM_MODEL = "llm_model"
    private const val KEY_STT_PROVIDER = "stt_provider"
    private const val KEY_TTS_PROVIDER = "tts_provider"
    private const val KEY_TTS_VOICE = "tts_voice"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
    private const val KEY_TEMPERATURE = "temperature"
    private const val KEY_LANGUAGE = "language"

    // ── Defaults ──
    const val DEFAULT_HTTP_URL = "http://192.168.0.100:8000"
    const val DEFAULT_WS_AUDIO_URL = "ws://192.168.0.100:8765"
    const val DEFAULT_WEBRTC_STREAM_URL = "ws://192.168.0.100:6868"
    const val DEFAULT_VIDEO_CALL_URL = "wss://sociallab.duckdns.org/videoCall/"
    const val DEFAULT_STUN_URL = "stun:stun.l.google.com:19302"
    const val DEFAULT_TURN_URL = ""
    const val DEFAULT_TURN_USER = ""
    const val DEFAULT_TURN_PASS = ""
    const val DEFAULT_WOZ_URL = "wss://sociallab.duckdns.org/ntl_demo/"

    // ── Pipeline Defaults ──
    const val DEFAULT_LLM_PROVIDER = "openai"
    const val DEFAULT_LLM_MODEL = "gpt-4o"
    const val DEFAULT_STT_PROVIDER = "deepgram"
    const val DEFAULT_TTS_PROVIDER = "openai"
    const val DEFAULT_TTS_VOICE = "alloy"
    const val DEFAULT_SYSTEM_PROMPT = "你是一個友善的社交機器人助手。你會用繁體中文回答問題，保持禮貌並使用簡單的語言。每次回覆請簡短，不超過 100 字。"
    const val DEFAULT_TEMPERATURE = 0.7f
    const val DEFAULT_LANGUAGE = "zh-TW"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ── HTTP Chat (Ch2) ──
    fun getHttpUrl(ctx: Context): String =
        prefs(ctx).getString(KEY_HTTP_URL, DEFAULT_HTTP_URL) ?: DEFAULT_HTTP_URL

    fun setHttpUrl(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_HTTP_URL, url).apply()

    // ── WebSocket Audio (Ch3) ──
    fun getWsAudioUrl(ctx: Context): String =
        prefs(ctx).getString(KEY_WS_AUDIO_URL, DEFAULT_WS_AUDIO_URL) ?: DEFAULT_WS_AUDIO_URL

    fun setWsAudioUrl(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_WS_AUDIO_URL, url).apply()

    // ── WebRTC Streaming (Ch4) ──
    fun getWebrtcStreamUrl(ctx: Context): String =
        prefs(ctx).getString(KEY_WEBRTC_STREAM_URL, DEFAULT_WEBRTC_STREAM_URL) ?: DEFAULT_WEBRTC_STREAM_URL

    fun setWebrtcStreamUrl(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_WEBRTC_STREAM_URL, url).apply()

    // ── Video Call (Ch5) ──
    fun getVideoCallUrl(ctx: Context): String =
        prefs(ctx).getString(KEY_VIDEO_CALL_URL, DEFAULT_VIDEO_CALL_URL) ?: DEFAULT_VIDEO_CALL_URL

    fun setVideoCallUrl(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_VIDEO_CALL_URL, url).apply()

    // ── STUN / TURN ──
    fun getStunUrl(ctx: Context): String =
        prefs(ctx).getString(KEY_STUN_URL, DEFAULT_STUN_URL) ?: DEFAULT_STUN_URL

    fun setStunUrl(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_STUN_URL, url).apply()

    fun getTurnUrl(ctx: Context): String =
        prefs(ctx).getString(KEY_TURN_URL, DEFAULT_TURN_URL) ?: DEFAULT_TURN_URL

    fun setTurnUrl(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_TURN_URL, url).apply()

    fun getTurnUser(ctx: Context): String =
        prefs(ctx).getString(KEY_TURN_USER, DEFAULT_TURN_USER) ?: DEFAULT_TURN_USER

    fun setTurnUser(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_TURN_USER, url).apply()

    fun getTurnPass(ctx: Context): String =
        prefs(ctx).getString(KEY_TURN_PASS, DEFAULT_TURN_PASS) ?: DEFAULT_TURN_PASS

    fun setTurnPass(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_TURN_PASS, url).apply()

    // ── WoZ Controller (Ch6) ──
    fun getWozUrl(ctx: Context): String =
        prefs(ctx).getString(KEY_WOZ_URL, DEFAULT_WOZ_URL) ?: DEFAULT_WOZ_URL

    fun setWozUrl(ctx: Context, url: String) =
        prefs(ctx).edit().putString(KEY_WOZ_URL, url).apply()

    // ── Pipeline 設定（Pipecat 對話設計） ──

    fun getLlmProvider(ctx: Context): String =
        prefs(ctx).getString(KEY_LLM_PROVIDER, DEFAULT_LLM_PROVIDER) ?: DEFAULT_LLM_PROVIDER

    fun setLlmProvider(ctx: Context, value: String) =
        prefs(ctx).edit().putString(KEY_LLM_PROVIDER, value).apply()

    fun getLlmModel(ctx: Context): String =
        prefs(ctx).getString(KEY_LLM_MODEL, DEFAULT_LLM_MODEL) ?: DEFAULT_LLM_MODEL

    fun setLlmModel(ctx: Context, value: String) =
        prefs(ctx).edit().putString(KEY_LLM_MODEL, value).apply()

    fun getSttProvider(ctx: Context): String =
        prefs(ctx).getString(KEY_STT_PROVIDER, DEFAULT_STT_PROVIDER) ?: DEFAULT_STT_PROVIDER

    fun setSttProvider(ctx: Context, value: String) =
        prefs(ctx).edit().putString(KEY_STT_PROVIDER, value).apply()

    fun getTtsProvider(ctx: Context): String =
        prefs(ctx).getString(KEY_TTS_PROVIDER, DEFAULT_TTS_PROVIDER) ?: DEFAULT_TTS_PROVIDER

    fun setTtsProvider(ctx: Context, value: String) =
        prefs(ctx).edit().putString(KEY_TTS_PROVIDER, value).apply()

    fun getTtsVoice(ctx: Context): String =
        prefs(ctx).getString(KEY_TTS_VOICE, DEFAULT_TTS_VOICE) ?: DEFAULT_TTS_VOICE

    fun setTtsVoice(ctx: Context, value: String) =
        prefs(ctx).edit().putString(KEY_TTS_VOICE, value).apply()

    fun getSystemPrompt(ctx: Context): String =
        prefs(ctx).getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT

    fun setSystemPrompt(ctx: Context, value: String) =
        prefs(ctx).edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    fun getTemperature(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)

    fun setTemperature(ctx: Context, value: Float) =
        prefs(ctx).edit().putFloat(KEY_TEMPERATURE, value).apply()

    fun getLanguage(ctx: Context): String =
        prefs(ctx).getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun setLanguage(ctx: Context, value: String) =
        prefs(ctx).edit().putString(KEY_LANGUAGE, value).apply()
}
