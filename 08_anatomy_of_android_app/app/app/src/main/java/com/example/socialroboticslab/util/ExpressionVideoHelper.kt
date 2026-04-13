package com.example.socialroboticslab.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import com.example.socialroboticslab.R

/**
 * 管理機器人表情影片的播放。
 * 根據 personality type (e/i) 和 emotion/status 播放對應的表情影片。
 */
class ExpressionVideoHelper(private val context: Context) {

    companion object {
        private const val TAG = "ExpressionVideoHelper"
    }

    private var shouldLoop = false

    /** 表情影片對應表：key = "{personality}_{emotion}", value = raw resource ID */
    private val videoMap: Map<String, Int> = mapOf(
        // Extrovert - Status
        "e_idling" to R.raw.e_neutral_n,
        "e_thinking" to R.raw.e_thinking_n,
        "e_listening" to R.raw.e_listening_n,
        "e_error" to R.raw.e_sad_n,
        "e_reset" to R.raw.e_neutral_n,
        // Extrovert - Emotion (speaking)
        "e_neutral" to R.raw.e_neutral_s,
        "e_angry" to R.raw.e_angry_s,
        "e_joy" to R.raw.e_joy_s,
        "e_sad" to R.raw.e_sad_n,
        "e_surprise" to R.raw.e_surprise_s,
        "e_scared" to R.raw.e_scared_s,
        "e_disgusted" to R.raw.e_disgusted_n,
        // Introvert - Status
        "i_idling" to R.raw.i_neutral_n,
        "i_thinking" to R.raw.i_thinking_n,
        "i_listening" to R.raw.i_listening_n,
        "i_error" to R.raw.i_sad_n,
        "i_reset" to R.raw.i_neutral_n,
        // Introvert - Emotion (speaking)
        "i_neutral" to R.raw.i_neutral_s,
        "i_angry" to R.raw.i_angry_s,
        "i_joy" to R.raw.i_joy_s,
        "i_sad" to R.raw.i_sad_n,
        "i_surprise" to R.raw.i_surprise_s,
        "i_scared" to R.raw.i_scared_s,
        "i_disgusted" to R.raw.i_disgusted_n,
    )

    /**
     * 在 VideoView 上播放表情影片。
     * @param videoView 要播放影片的 VideoView
     * @param personalityType "e" (extrovert) 或 "i" (introvert)
     * @param baseEmotion 基礎情緒或狀態 (e.g. "listening", "joy", "neutral")
     * @param loop 是否循環播放（speaking 狀態時通常為 true）
     */
    fun playExpression(
        videoView: VideoView,
        personalityType: String = "e",
        baseEmotion: String,
        loop: Boolean = false
    ) {
        val key = "${personalityType}_${baseEmotion}"
        shouldLoop = loop

        val resId = videoMap[key]
        if (resId == null) {
            Log.w(TAG, "No video for key: $key, falling back to neutral")
            val fallbackId = videoMap["${personalityType}_idling"] ?: return
            playVideo(videoView, fallbackId)
            return
        }

        playVideo(videoView, resId)
    }

    private fun playVideo(videoView: VideoView, resId: Int) {
        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = shouldLoop
        }
        videoView.setOnCompletionListener {
            if (shouldLoop) videoView.start()
        }
        videoView.start()
    }
}
