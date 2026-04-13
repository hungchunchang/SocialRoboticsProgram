package com.example.socialroboticslab.videocall

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.socialroboticslab.R
import okhttp3.*
import org.json.JSONObject
import org.webrtc.*

/**
 * Ch5 對應功能：進行中的視訊通話畫面
 * 使用 WebRTC 建立 P2P 視訊連線。
 */
class VideoCallActivity : AppCompatActivity() {

    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var isCameraOn = true
    private var isMuted = false

    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private val rootEglBase: EglBase = EglBase.create()

    private lateinit var localView: SurfaceViewRenderer
    private lateinit var remoteView: SurfaceViewRenderer
    private lateinit var btnToggleCamera: Button
    private lateinit var btnToggleMute: Button
    private lateinit var btnHangUp: Button
    private lateinit var tvCallStatus: TextView

    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient()

    private lateinit var peerId: String
    private lateinit var myId: String
    private var isCaller: Boolean = false
    private lateinit var websocketUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        peerId = intent.getStringExtra("PEER_ID") ?: ""
        myId = intent.getStringExtra("MY_ID") ?: ""
        isCaller = intent.getBooleanExtra("IS_CALLER", false)
        websocketUrl = intent.getStringExtra("WEBSOCKET_URL") ?: ""

        initViews()
        initWebRTC()
        initWebSocket()
        startLocalVideo()

        if (isCaller) createOffer()
    }

    private fun initViews() {
        localView = findViewById(R.id.localView)
        remoteView = findViewById(R.id.remoteView)
        btnToggleCamera = findViewById(R.id.btnToggleCamera)
        btnToggleMute = findViewById(R.id.btnToggleMute)
        btnHangUp = findViewById(R.id.btnHangUp)
        tvCallStatus = findViewById(R.id.tvCallStatus)

        tvCallStatus.text = if (isCaller) "Calling: $peerId" else "In call: $peerId"

        btnToggleCamera.setOnClickListener { toggleCamera() }
        btnToggleMute.setOnClickListener { toggleMute() }
        btnHangUp.setOnClickListener { hangUp() }
    }

    private fun initWebRTC() {
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()

        localView.init(rootEglBase.eglBaseContext, null)
        remoteView.init(rootEglBase.eglBaseContext, null)

        createPeerConnection()
    }

    private fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        }

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { sendIceCandidate(it) }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                runOnUiThread {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED ->
                            tvCallStatus.text = "Connected: $peerId"
                        PeerConnection.IceConnectionState.DISCONNECTED ->
                            tvCallStatus.text = "Disconnected"
                        PeerConnection.IceConnectionState.FAILED ->
                            tvCallStatus.text = "Connection failed"
                        else -> {}
                    }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    remoteVideoTrack = track
                    runOnUiThread { remoteVideoTrack?.addSink(remoteView) }
                }
            }

            override fun onAddStream(stream: MediaStream?) {
                stream?.videoTracks?.firstOrNull()?.let {
                    remoteVideoTrack = it
                    runOnUiThread { remoteVideoTrack?.addSink(remoteView) }
                }
            }

            override fun onDataChannel(dc: DataChannel?) {}
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    private fun startLocalVideo() {
        if (!isCameraOn) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) return

        videoCapturer = createCameraCapturer() ?: return

        val videoSource = peerConnectionFactory.createVideoSource(false)
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name, rootEglBase.eglBaseContext
        )
        videoCapturer?.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)
        videoCapturer?.startCapture(640, 480, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("video", videoSource)
        localView.setMirror(true)
        localVideoTrack?.addSink(localView)

        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        val audioTrack = peerConnectionFactory.createAudioTrack("audio", audioSource)

        peerConnection?.addTrack(localVideoTrack, listOf("local_stream"))
        peerConnection?.addTrack(audioTrack, listOf("local_stream"))
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(this)
        for (name in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(name))
                return enumerator.createCapturer(name, null)
        }
        return null
    }

    private fun initWebSocket() {
        val request = Request.Builder().url(websocketUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val msg = JSONObject().apply {
                    put("type", "register")
                    put("id", myId)
                }
                webSocket.send(msg.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleSignaling(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("VideoCall", "WebSocket failed: ${t.message}")
            }
        })
    }

    private fun handleSignaling(message: String) {
        try {
            val json = JSONObject(message)
            when (json.getString("type")) {
                "offer" -> {
                    val offer = SessionDescription(SessionDescription.Type.OFFER, json.getString("sdp"))
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() { createAnswer() }
                        override fun onCreateSuccess(p: SessionDescription?) {}
                        override fun onCreateFailure(s: String?) {}
                        override fun onSetFailure(s: String?) {}
                    }, offer)
                }
                "answer" -> {
                    val answer = SessionDescription(SessionDescription.Type.ANSWER, json.getString("sdp"))
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() {}
                        override fun onCreateSuccess(p: SessionDescription?) {}
                        override fun onCreateFailure(s: String?) {}
                        override fun onSetFailure(s: String?) {}
                    }, answer)
                }
                "ice_candidate" -> {
                    val candidate = IceCandidate(
                        json.getString("sdpMid"),
                        json.getInt("sdpMLineIndex"),
                        json.getString("candidate")
                    )
                    peerConnection?.addIceCandidate(candidate)
                }
                "call_ended" -> {
                    runOnUiThread { finish() }
                }
            }
        } catch (e: Exception) {
            Log.e("VideoCall", "Signaling error", e)
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { sendOffer(sdp!!) }
                    override fun onCreateSuccess(p: SessionDescription?) {}
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) {}
            override fun onSetFailure(s: String?) {}
        }, constraints)
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() { sendAnswer(sdp!!) }
                    override fun onCreateSuccess(p: SessionDescription?) {}
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) {}
            override fun onSetFailure(s: String?) {}
        }, constraints)
    }

    private fun sendOffer(offer: SessionDescription) {
        val msg = JSONObject().apply {
            put("type", "offer")
            put("from", myId)
            put("to", peerId)
            put("sdp", offer.description)
        }
        webSocket.send(msg.toString())
    }

    private fun sendAnswer(answer: SessionDescription) {
        val msg = JSONObject().apply {
            put("type", "answer")
            put("from", myId)
            put("to", peerId)
            put("sdp", answer.description)
        }
        webSocket.send(msg.toString())
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val msg = JSONObject().apply {
            put("type", "ice_candidate")
            put("from", myId)
            put("to", peerId)
            put("candidate", candidate.sdp)
            put("sdpMid", candidate.sdpMid)
            put("sdpMLineIndex", candidate.sdpMLineIndex)
        }
        webSocket.send(msg.toString())
    }

    private fun toggleCamera() {
        if (isCameraOn) {
            videoCapturer?.stopCapture()
            btnToggleCamera.text = "Camera On"
            localView.visibility = View.GONE
        } else {
            videoCapturer?.startCapture(640, 480, 30)
            btnToggleCamera.text = "Camera Off"
            localView.visibility = View.VISIBLE
        }
        isCameraOn = !isCameraOn
    }

    private fun toggleMute() {
        isMuted = !isMuted
        btnToggleMute.text = if (isMuted) "Unmute" else "Mute"
    }

    private fun hangUp() {
        val msg = JSONObject().apply {
            put("type", "call_ended")
            put("from", myId)
            put("to", peerId)
        }
        webSocket.send(msg.toString())
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendBroadcast(Intent("VIDEO_CALL_ENDED"))
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        localVideoTrack?.dispose()
        remoteVideoTrack?.dispose()
        peerConnection?.close()
        peerConnectionFactory.dispose()
        rootEglBase.release()
        webSocket.close(1000, "Call ended")
    }
}
