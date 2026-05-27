import torch
import numpy as np
import logging

logger = logging.getLogger("vad-handler")

class SileroVADHandler:
    def __init__(self, sample_rate=16000, threshold=0.5, min_silence_duration_ms=500):
        self.model, utils = torch.hub.load(repo_or_dir='snakers4/silero-vad',
                                          model='silero_vad',
                                          force_reload=False,
                                          onnx=Fals)
        (self.get_speech_timestamps, _, self.read_audio, _, _) = utils
        self.sample_rate = sample_rate
        self.threshold = threshold
        self.min_silence_samples = (min_silence_duration_ms * sample_rate) / 1000
        
        # Silero VAD 要求固定的 chunk size (16kHz 時為 512 samples)
        self.required_samples = 512 if sample_rate == 16000 else 256
        self.internal_buffer = np.array([], dtype=np.float32)
        
        self.reset()

    def reset(self):
        self.is_speaking = False
        self.silence_counter = 0
        self.audio_buffer = [] # 儲存所有語音片段 (bytes)
        self.internal_buffer = np.array([], dtype=np.float32)

    def process_chunk(self, audio_chunk_bytes):
        """
        處理音訊片段，回傳 True 表示偵測到語音結束
        """
        # 將 bytes 轉為 float32 numpy array 並加入內部緩衝區
        audio_int16 = np.frombuffer(audio_chunk_bytes, dtype=np.int16)
        audio_float32 = audio_int16.astype(np.float32) / 32768.0
        self.internal_buffer = np.append(self.internal_buffer, audio_float32)
        
        speech_ended = False

        # 持續處理直到緩衝區不足一個 required_samples
        while len(self.internal_buffer) >= self.required_samples:
            current_chunk = self.internal_buffer[:self.required_samples]
            self.internal_buffer = self.internal_buffer[self.required_samples:]
            
            # 轉換為 torch tensor
            new_conf = self.model(torch.from_numpy(current_chunk), self.sample_rate).item()
            
            if new_conf > self.threshold:
                if not self.is_speaking:
                    logger.info("Speech started")
                    self.is_speaking = True
                self.silence_counter = 0
                # 這裡我們將處理過的 chunk 轉回 bytes 存入語音緩衝區
                self.audio_buffer.append((current_chunk * 32768.0).astype(np.int16).tobytes())
            else:
                if self.is_speaking:
                    self.silence_counter += self.required_samples
                    self.audio_buffer.append((current_chunk * 32768.0).astype(np.int16).tobytes())
                    
                    if self.silence_counter >= self.min_silence_samples:
                        logger.info("Speech ended")
                        self.is_speaking = False
                        speech_ended = True
                        break 
        
        return speech_ended

    def get_audio_data(self):
        data = b"".join(self.audio_buffer)
        self.reset()
        return data
