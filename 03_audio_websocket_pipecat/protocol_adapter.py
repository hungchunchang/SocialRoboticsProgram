"""
協議轉換器：將 Pipecat 內部 frame 轉換為 Ch3 Android 客戶端的 JSON 協議。

輸出格式（與 03_audio_websocket/backend.py 相同）：
- {"type": "response.audio.delta", "delta": "<base64 PCM>"}
- {"type": "response.audio.done"}
- {"type": "response.emotion_transcript", "emotion": "...", "transcript": "..."}
"""

import base64
import json
import re

from pipecat.frames.frames import (
    Frame,
    AudioRawFrame,
    TextFrame,
    TTSStartedFrame,
    TTSStoppedFrame,
)
from pipecat.processors.frame_processor import FrameDirection, FrameProcessor


class ProtocolAdapter(FrameProcessor):
    """
    Pipecat FrameProcessor that converts internal frames into
    the JSON WebSocket protocol expected by the Android client.
    """

    def __init__(self, websocket=None, **kwargs):
        super().__init__(**kwargs)
        self._websocket = websocket
        self._transcript_buffer = ""

    def set_websocket(self, ws):
        self._websocket = ws

    async def process_frame(self, frame: Frame, direction: FrameDirection):
        await super().process_frame(frame, direction)

        if self._websocket is None:
            await self.push_frame(frame, direction)
            return

        if isinstance(frame, AudioRawFrame):
            # 音訊資料 → base64 編碼後送出
            audio_b64 = base64.b64encode(frame.audio).decode("utf-8")
            await self._websocket.send_text(json.dumps({
                "type": "response.audio.delta",
                "delta": audio_b64,
            }))

        elif isinstance(frame, TTSStoppedFrame):
            # TTS 結束
            await self._websocket.send_text(json.dumps({
                "type": "response.audio.done",
            }))
            # 送出累積的文字 + 情緒
            if self._transcript_buffer:
                emotion = detect_emotion(self._transcript_buffer)
                await self._websocket.send_text(json.dumps({
                    "type": "response.emotion_transcript",
                    "emotion": emotion,
                    "transcript": self._transcript_buffer,
                }))
                self._transcript_buffer = ""

        elif isinstance(frame, TextFrame):
            # 累積 LLM 輸出文字（用於最終的 emotion_transcript）
            self._transcript_buffer += frame.text

        # 繼續傳遞 frame 給下游
        await self.push_frame(frame, direction)


# ── 簡易情緒偵測 ──

_EMOTION_KEYWORDS = {
    "angry": ["生氣", "憤怒", "討厭", "煩", "angry", "mad", "furious"],
    "joy": ["開心", "高興", "快樂", "太好了", "棒", "happy", "great", "wonderful", "glad"],
    "sad": ["難過", "傷心", "可惜", "抱歉", "sad", "sorry", "unfortunately"],
    "surprise": ["哇", "驚訝", "意外", "沒想到", "wow", "amazing", "surprised"],
    "scared": ["害怕", "可怕", "擔心", "scared", "afraid", "worried"],
    "disgusted": ["噁心", "受不了", "disgusted"],
}


def detect_emotion(text: str) -> str:
    """根據關鍵詞偵測文字中的情緒，預設為 neutral。"""
    text_lower = text.lower()
    for emotion, keywords in _EMOTION_KEYWORDS.items():
        for kw in keywords:
            if kw in text_lower:
                return emotion
    return "neutral"
