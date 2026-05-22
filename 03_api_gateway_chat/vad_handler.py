import torch
import numpy as np
import logging

logger = logging.getLogger("vad-handler")

class SileroVADHandler:
    def __init__(self, sample_rate=16000, threshold=0.5, min_silence_duration_ms=500):
        self.model, utils = torch.hub.load(repo_or_dir='snakers4/silero-vad',
                                          model='silero_vad',
                                          force_reload=False,
                                          onnx=False)
        (self.get_speech_timestamps, _, self.read_audio, _, _) = utils
        self.sample_rate = sample_rate
        self.threshold = threshold
        self.min_silence_samples = (min_silence_duration_ms * sample_rate) / 1000
        
        self.reset()

    def reset(self):
        self.is_speaking = False
        self.silence_counter = 0
        self.audio_buffer = [] # 儲存所有語音片段

    def process_chunk(self, audio_chunk_bytes):
        """
        處理音訊片段，回傳 True 表示偵測到語音結束
        """
        # 將 bytes 轉為 float32 numpy array
        audio_int16 = np.frombuffer(audio_chunk_bytes, dtype=np.int16)
        audio_float32 = audio_int16.astype(np.float32) / 32768.0
        
        # 轉換為 torch tensor
        new_conf = self.model(torch.from_numpy(audio_float32), self.sample_rate).item()
        
        if new_conf > self.threshold:
            if not self.is_speaking:
                logger.info("Speech started")
                self.is_speaking = True
            self.silence_counter = 0
            self.audio_buffer.append(audio_chunk_bytes)
        else:
            if self.is_speaking:
                self.silence_counter += len(audio_float32)
                self.audio_buffer.append(audio_chunk_bytes)
                
                if self.silence_counter >= self.min_silence_samples:
                    logger.info("Speech ended")
                    self.is_speaking = False
                    return True # 語音結束
            else:
                # 尚未開始說話，清空緩衝避免累積雜音
                pass
                
        return False

    def get_audio_data(self):
        data = b"".join(self.audio_buffer)
        self.reset()
        return data
