# API Gateway 聊天伺服器 (自實作 VAD 版)

本專案實作了一個基於 FastAPI 的 WebSocket 伺服器，直接整合了 **Silero VAD** 進行語音活動偵測，並透過 API Gateway 呼叫 LLM、ASR 與 TTS 服務。

此版本不使用 Pipecat 框架，而是手動處理音訊串流與 Pipeline 邏輯。

## 專案功能

- **WebSocket Server**: 監聽於 `:8080/ws` (或依設定)，接收即時音訊串流。
- **Silero VAD**: 使用 PyTorch 版 Silero VAD 偵測說話開始與結束。
- **API Gateway 整合**: 自動將偵測到的語音傳送至 ASR，取得回覆後呼叫 LLM 與 TTS。
- **協議相容性**: 與 Social Robotics Program 的 Android 客戶端協議相容。

## 準備工作

1. 取得 API Key 與 Endpoints。
2. 複製 `.env.example` 為 `.env` 並填入正確資訊。

## 安裝依據

```bash
pip install -r requirements.txt
```

## 執行方式

啟動伺服器：

```bash
python server.py
```

## 檔案說明

- `server.py`: 主程式，包含 FastAPI 與 WebSocket 邏輯。
- `vad_handler.py`: Silero VAD 處理邏輯。
- `api_client.py`: 呼叫 API Gateway 的封裝。
