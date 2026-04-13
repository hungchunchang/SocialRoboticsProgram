#!/usr/bin/env python3
import asyncio
import websockets
import json
import cv2
import numpy as np
from aiortc import RTCPeerConnection, RTCSessionDescription, RTCConfiguration, RTCIceServer
from aiortc.contrib.media import MediaRecorder
import logging
from datetime import datetime
from aiortc.sdp import candidate_from_sdp

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

class VideoReceiver:
    def __init__(self, client_id, websocket):
        self.client_id = client_id
        self.websocket = websocket
        self.recorder = None
        self.display_task = None
        logging.info(f"[{self.client_id}] 建立 VideoReceiver")
        
        self.ice_servers = [
            RTCIceServer(
                urls=["stun:sociallab.duckdns.org:3478"]
            ),
            RTCIceServer(
                urls=["turn:sociallab.duckdns.org:3478"],
                username="hcc",
                credential="j0207"
            )
        ]
        self.config = RTCConfiguration(iceServers=self.ice_servers)
        self.pc = RTCPeerConnection(configuration=self.config)

        # # 建立一個 Event 來等待 ICE 收集完成
        # self.gathering_complete = asyncio.Event()

        # @self.pc.on("icegatheringstatechange")
        # def on_icegatheringstatechange():
        #     logging.info(f"[{self.client_id}] ICE Gathering State: {self.pc.iceGatheringState}")
        #     if self.pc.iceGatheringState == "complete":
        #         self.gathering_complete.set() # 收集完成時，觸發 Event

        @self.pc.on("connectionstatechange")
        async def on_connectionstatechange():
            logging.info(f"[{self.client_id}] 連接狀態: {self.pc.connectionState}")

        @self.pc.on("iceconnectionstatechange")
        async def on_iceconnectionstatechange():
            logging.info(f"[{self.client_id}] ICE 連接狀態: {self.pc.iceConnectionState}")
            if self.pc.iceConnectionState == "connected" or self.pc.iceConnectionState == "completed":
                logging.info(f"[{self.client_id}] 🎉 ICE 連接成功！")
            elif self.pc.iceConnectionState == "failed":
                logging.error(f"[{self.client_id}] ❌ ICE 連接失敗！")

        @self.pc.on("track")
        async def on_track(track):
            logging.info(f"[{self.client_id}] 收到軌道: {track.kind} - {track}")
            if track.kind == "video":
                filename = f"received_video_{self.client_id}_{datetime.now().strftime('%H%M%S')}.mp4"
                logging.info(f"[{self.client_id}] 開始錄製: {filename}")
                self.recorder = MediaRecorder(filename)
                self.recorder.addTrack(track)
                await self.recorder.start()
                logging.info(f"[{self.client_id}] 錄製已開始")
                self.display_task = asyncio.create_task(self.display_frames(track))

    async def handle_offer(self, offer_data):
        logging.info(f"[{self.client_id}] 開始處理 Offer")
        
        await self.pc.setRemoteDescription(
            RTCSessionDescription(
                sdp=offer_data["sdp"],
                type=offer_data["type"]
            )
        )
        logging.info(f"[{self.client_id}] 遠端 SDP 已設定")

        answer = await self.pc.createAnswer()
        await self.pc.setLocalDescription(answer)
        
        # Trickle ICE 模式:立即回傳 Answer (不等 ICE 收集完成)
        logging.info(f"[{self.client_id}] 立即發送 Answer SDP")

        return {
            "type": self.pc.localDescription.type,
            "sdp": self.pc.localDescription.sdp
        }


    async def display_frames(self, track):
        logging.info(f"[{self.client_id}] 開始顯示視訊畫面")
        try:
            while True:
                frame = await track.recv()
                img = frame.to_ndarray(format="bgr24")
                cv2.putText(img, f"Client: {self.client_id}", (10, 30), 
                           cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                cv2.putText(img, f"Size: {img.shape[1]}x{img.shape[0]}", (10, 60), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
                cv2.imshow(f"WebRTC Stream - {self.client_id}", img)
                if cv2.waitKey(1) & 0xFF == ord('q'):
                    logging.info(f"[{self.client_id}] 用戶按下 'q'，停止顯示")
                    break
        except asyncio.CancelledError:
            logging.info(f"[{self.client_id}] 顯示任務被取消")
        except Exception as e:
            logging.error(f"[{self.client_id}] 顯示錯誤: {e}")
        finally:
            cv2.destroyWindow(f"WebRTC Stream - {self.client_id}")
            logging.info(f"[{self.client_id}] 視訊顯示已停止")

    async def add_ice_candidate(self, candidate_data):
        try:
            candidate = candidate_from_sdp(candidate_data["candidate"])
            candidate.sdpMid = candidate_data["sdpMid"]
            candidate.sdpMLineIndex = candidate_data["sdpMLineIndex"]
            await self.pc.addIceCandidate(candidate)
            logging.info(f"[{self.client_id}] 已添加遠端 ICE Candidate")
        except Exception as e:
            logging.error(f"[{self.client_id}] 添加 ICE Candidate 失敗: {e}")

    async def close(self):
        logging.info(f"[{self.client_id}] 關閉 VideoReceiver")
        if self.display_task:
            self.display_task.cancel()
            try:
                await self.display_task
            except asyncio.CancelledError:
                pass
        if self.recorder:
            await self.recorder.stop()
            logging.info(f"[{self.client_id}] 錄製已停止")
        if self.pc:
            await self.pc.close()
            logging.info(f"[{self.client_id}] PeerConnection 已關閉")

client_counter = 0

async def handle_client(websocket):
    global client_counter
    client_counter += 1
    client_id = f"Client_{client_counter}"
    
    logging.info(f"[{client_id}] 新客戶端連接，地址: {websocket.remote_address}")
    
    video_receiver = VideoReceiver(client_id, websocket)

    try:
        async for message in websocket:
            logging.info(f"[{client_id}] 收到訊息,長度: {len(message)}")
            
            try:
                data = json.loads(message)
                logging.info(f"[{client_id}] JSON 解析成功,類型: {list(data.keys())}")
                
                if "sdp" in data:
                    offer_data = data["sdp"]
                    if offer_data["type"] == "offer":
                        logging.info(f"[{client_id}] 處理 SDP Offer")
                        answer_data = await video_receiver.handle_offer(offer_data)
                        response = {"sdp": answer_data}
                        await websocket.send(json.dumps(response))
                        logging.info(f"[{client_id}] SDP Answer 已立即發送")
                
                elif "iceCandidate" in data:
                    # Trickle ICE 模式:處理收到的 candidate
                    candidate_data = data["iceCandidate"]
                    logging.info(f"[{client_id}] 處理 ICE Candidate")
                    await video_receiver.add_ice_candidate(candidate_data)
                    
            except json.JSONDecodeError as e:
                logging.error(f"[{client_id}] JSON 解析錯誤: {e}")
                
    except websockets.exceptions.ConnectionClosed:
        logging.info(f"[{client_id}] WebSocket 連接已關閉")
    except Exception as e:
        logging.error(f"[{client_id}] 處理錯誤: {e}")
    finally:
        await video_receiver.close()
        logging.info(f"[{client_id}] 客戶端清理完成")

async def main():
    logging.info("="*50)
    logging.info("啟動 WebRTC 伺服器 (Non-Trickle Mode)")
    logging.info("監聽地址: 0.0.0.0:6868")
    logging.info("按 'q' 關閉視訊窗口")
    logging.info("="*50)
    
    async with websockets.serve(handle_client, "0.0.0.0", 6868):
        await asyncio.Future()

if __name__ == "__main__":
    try:
        import cv2
        logging.info("OpenCV 已載入，版本: " + cv2.__version__)
    except ImportError:
        logging.error("請安裝 OpenCV: pip install opencv-python")
        exit(1)
        
    asyncio.run(main())