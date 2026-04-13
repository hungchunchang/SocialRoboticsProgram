#!/usr/bin/env python3
import asyncio
import websockets
import json
import cv2
import numpy as np
from aiortc import RTCPeerConnection, RTCSessionDescription, RTCConfiguration, RTCIceServer, MediaStreamTrack
from aiortc.sdp import candidate_from_sdp
import logging
import av       # <--- 移到這裡
import time     # <--- 移到這裡
from datetime import datetime

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - [PY_CLIENT] - %(message)s'
)

# --- 這是修正後的假視訊軌道 ---
class TimestampVideoTrack(MediaStreamTrack):
    kind = "video"
    def __init__(self):
        super().__init__()
        self.counter = 0
        self.width = 640
        self.height = 480
        self._start = time.time() # 記錄開始時間
        logging.info("建立 TimestampVideoTrack (假視訊來源)")

    async def recv(self):
        # 模擬 30 fps
        await asyncio.sleep(1 / 30)
        
        # 手動計算時間戳
        now = time.time()
        pts = int((now - self._start) * 90000) # 使用 90kHz 時鐘頻率
        
        # 建立一個新的黑畫面
        frame = av.VideoFrame(width=self.width, height=self.height, format="bgr24")
        frame.pts = pts
        
        # --- 這是修正點 ---
        # 把 av.Rational(1, 90000) 改成字串 '1/90000'
        frame.time_base = '1/90000'
        # --- 修正結束 ---
        
        # 取得 numpy 視圖以在其上繪圖
        img = frame.to_ndarray()

        # 加上計數器文字
        text = f"Frame: {self.counter}"
        cv2.putText(img, text, (50, 240), 
                    cv2.FONT_HERSHEY_SIMPLEX, 2, (0, 255, 0), 3)
        self.counter += 1

        return frame # 回傳修改後的 frame
# --- 修正結束 ---


async def run_client():
    WEBSOCKET_URL = "ws://140.112.92.133:6868"
    
    ice_servers = [
        RTCIceServer(
            urls=["stun:sociallab.duckdns.org:3478"]
        ),
        RTCIceServer(
            urls=["turn:sociallab.duckdns.org:3478"],
            username="hcc",
            credential="j0207"
        )
    ]
    config = RTCConfiguration(iceServers=ice_servers)
    pc = RTCPeerConnection(configuration=config)

    gathering_complete = asyncio.Event()

    @pc.on("icegatheringstatechange")
    def on_icegatheringstatechange():
        logging.info(f"ICE Gathering State: {pc.iceGatheringState}")
        if pc.iceGatheringState == "complete":
            gathering_complete.set()

    @pc.on("iceconnectionstatechange")
    async def on_iceconnectionstatechange():
        logging.info(f"ICE 連接狀態: {pc.iceConnectionState}")
        if pc.iceConnectionState == "connected" or pc.iceConnectionState == "completed":
            logging.info("🎉 ICE 連接成功！")
        elif pc.iceConnectionState == "failed":
            logging.error("❌ ICE 連接失敗！")

    @pc.on("connectionstatechange")
    async def on_connectionstatechange():
        logging.info(f"連接狀態: {pc.connectionState}")

    try:
        async with websockets.connect(WEBSOCKET_URL) as websocket:
            logging.info(f"WebSocket 已連接到 {WEBSOCKET_URL}")

            pc.addTrack(TimestampVideoTrack())
            offer = await pc.createOffer()
            await pc.setLocalDescription(offer)
            
            logging.info("等待 ICE 候選人收集完成...")
            await gathering_complete.wait()
            logging.info("ICE 收集完成。")

            offer_message = {
                "sdp": {
                    "type": pc.localDescription.type,
                    "sdp": pc.localDescription.sdp
                }
            }
            await websocket.send(json.dumps(offer_message))
            logging.info("SDP Offer (with all candidates) 已發送")

            async for message in websocket:
                data = json.loads(message)
                
                if "sdp" in data and data["sdp"]["type"] == "answer":
                    logging.info("收到伺服器 SDP Answer")
                    answer = RTCSessionDescription(
                        sdp=data["sdp"]["sdp"],
                        type=data["sdp"]["type"]
                    )
                    await pc.setRemoteDescription(answer)
                    logging.info("遠端 SDP (Answer) 已設定")
                
                elif "iceCandidate" in data:
                    logging.warning("收到一個預期外的 iceCandidate 訊息 (已忽略)")

    except Exception as e:
        logging.error(f"連線時發生錯誤: {e}")
    finally:
        logging.info("關閉 PeerConnection")
        await pc.close()
        logging.info("Client 執行完畢")


if __name__ == "__main__":
    logging.info("啟動 Python WebRTC 測試客戶端 (Non-Trickle Mode)...")
    try:
        asyncio.run(run_client())
    except KeyboardInterrupt:
        logging.info("客戶端被手動停止")