"""
Pipeline 工廠：根據設定建立 Pipecat pipeline 的各個元件。

支援的供應商：
- STT: deepgram, whisper (OpenAI)
- LLM: openai, anthropic
- TTS: elevenlabs, openai
- VAD: silero (固定)
"""

import os
import logging

logger = logging.getLogger(__name__)

# ── 預設設定 ──

DEFAULT_CONFIG = {
    "stt_provider": "deepgram",
    "llm_provider": "openai",
    "llm_model": "gpt-4o",
    "tts_provider": "openai",
    "tts_voice": "alloy",
    "system_prompt": (
        "你是一個友善的社交機器人助手。"
        "你會用繁體中文回答問題，保持禮貌並使用簡單的語言。"
        "每次回覆請簡短，不超過 100 字。"
    ),
    "temperature": 0.7,
    "language": "zh-TW",
}

# ── 可用的供應商清單（給 GET /api/providers 用） ──

AVAILABLE_PROVIDERS = {
    "llm": [
        {"id": "openai", "name": "OpenAI", "models": ["gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo"]},
        {"id": "anthropic", "name": "Anthropic", "models": ["claude-sonnet-4-20250514", "claude-haiku-4-5-20251001"]},
    ],
    "stt": [
        {"id": "deepgram", "name": "Deepgram"},
        {"id": "whisper", "name": "OpenAI Whisper"},
    ],
    "tts": [
        {"id": "elevenlabs", "name": "ElevenLabs", "voices": ["rachel", "adam", "bella", "josh"]},
        {"id": "openai", "name": "OpenAI TTS", "voices": ["alloy", "echo", "fable", "nova", "onyx", "shimmer"]},
    ],
}


def create_stt(provider: str):
    """建立 STT 服務。"""
    if provider == "deepgram":
        from pipecat.services.deepgram import DeepgramSTTService
        return DeepgramSTTService(
            api_key=os.getenv("DEEPGRAM_API_KEY", ""),
        )
    elif provider == "whisper":
        from pipecat.services.openai import OpenAISTTService
        return OpenAISTTService(
            api_key=os.getenv("OPENAI_API_KEY", ""),
            model="whisper-1",
        )
    else:
        raise ValueError(f"Unknown STT provider: {provider}")


def create_llm(provider: str, model: str, system_prompt: str, temperature: float = 0.7):
    """建立 LLM 服務。"""
    if provider == "openai":
        from pipecat.services.openai import OpenAILLMService
        return OpenAILLMService(
            api_key=os.getenv("OPENAI_API_KEY", ""),
            model=model,
            system_instruction=system_prompt,
            params=OpenAILLMService.InputParams(temperature=temperature),
        )
    elif provider == "anthropic":
        from pipecat.services.anthropic import AnthropicLLMService
        return AnthropicLLMService(
            api_key=os.getenv("ANTHROPIC_API_KEY", ""),
            model=model,
            system_instruction=system_prompt,
            params=AnthropicLLMService.InputParams(temperature=temperature),
        )
    else:
        raise ValueError(f"Unknown LLM provider: {provider}")


def create_tts(provider: str, voice: str):
    """建立 TTS 服務。"""
    if provider == "elevenlabs":
        from pipecat.services.elevenlabs import ElevenLabsTTSService
        return ElevenLabsTTSService(
            api_key=os.getenv("ELEVENLABS_API_KEY", ""),
            voice_id=voice,
        )
    else:
        from pipecat.services.openai import OpenAITTSService
        return OpenAITTSService(
            api_key=os.getenv("OPENAI_API_KEY", ""),
            voice=voice,
        )


def create_vad():
    """建立 VAD（固定使用 Silero）。"""
    from pipecat.audio.vad.silero import SileroVADAnalyzer
    return SileroVADAnalyzer()


def merge_config(user_config: dict) -> dict:
    """合併使用者設定與預設值。"""
    merged = dict(DEFAULT_CONFIG)
    for key, value in user_config.items():
        if value is not None and value != "":
            merged[key] = value
    return merged
