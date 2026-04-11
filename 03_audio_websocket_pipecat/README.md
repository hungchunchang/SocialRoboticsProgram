# Pipecat Audio Backend

本專案是一個基於 **Pipecat** 框架的可設定語音 AI Pipeline，專為 Social Robotics Program 設計。它提供了一個可彈性配置的後端，支援多種語音轉文字 (STT)、大型語言模型 (LLM) 以及文字轉語音 (TTS) 服務。

## 專案功能

- **雙服務架構**：
  - **FastAPI HTTP (:8080)**：負責 Pipeline 設定管理與供應商查詢。
  - **Pipecat WebSocket (:8765)**：處理即時雙向音訊串流。
- **動態設定**：Android 客戶端可以在連線前透過 API 設定所需的 AI 供應商。
- **協議轉換**：內建 `protocol_adapter`，將 Pipecat 的內部 Frame 轉換為與 Android 端相容的 JSON 協議。
- **VAD 支援**：使用 Silero 進行即時語音活動偵測。

## 支援的供應商

| 類型 | 支援服務 |
| :--- | :--- |
| **STT** | Deepgram, Whisper (OpenAI) |
| **LLM** | OpenAI, Anthropic |
| **TTS** | ElevenLabs, OpenAI |
| **VAD** | Silero (固定) |

## 安裝與設定

### 1. 安裝環境
確保您的系統已安裝 Python 3.10+。

```bash
pip install -r requirements.txt
```

### 2. 設定環境變數
複製 `.env.example` 並填入您的 API 金鑰：

```bash
cp .env.example .env
```

在 `.env` 中設定：
- `OPENAI_API_KEY`
- `ANTHROPIC_API_KEY`
- `DEEPGRAM_API_KEY`
- `ELEVENLABS_API_KEY`

## 執行方式

啟動 FastAPI 與 WebSocket 服務：

```bash
uvicorn backend:app --host 0.0.0.0 --port 8080
```

- **HTTP API 端口**: `8080`
- **WebSocket 端口**: `8765`

## API 接口說明

### `GET /api/providers`
回傳目前系統支援的所有 STT, LLM, TTS 供應商清單。

### `POST /api/config`
設定特定 Session 的 Pipeline 配置。
**範例 Payload:**
```json
{
  "session_id": "user_123",
  "stt_provider": "deepgram",
  "llm_provider": "openai",
  "tts_provider": "elevenlabs"
}
```

### `GET /api/config/{session_id}`
查詢特定 Session 的目前 Pipeline 設定。

## 檔案結構

- `backend.py`: 主程式，包含 FastAPI 與 WebSocket 伺服器邏輯。
- `pipeline_factory.py`: 負責根據設定建立 Pipecat pipeline 組件的工廠類別。
- `protocol_adapter.py`: 處理與 Android 客戶端通訊的協議封裝與轉換。
- `requirements.txt`: 專案相依套件清單。
