package com.example.socialroboticslab.streaming

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.socialroboticslab.R
import com.example.socialroboticslab.util.PrefsManager
import okhttp3.*
import org.json.JSONObject
import org.webrtc.*

/**
 * Ch4 對應功能：WebRTC Video Streaming（單向推流）
 * 透過 WebSocket 信令，將本地攝影機畫面推流到伺服器。
 */
class VideoStreamActivity : AppCompatActivity() {

    private lateinit var localView: SurfaceViewRenderer
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnSwitchCamera: Button

    private var peerConnection: PeerConnection? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private val rootEglBase: EglBase = EglBase.create()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    private var isStreaming = false
    private var frontCamera = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_stream)

        supportActionBar?.title = "Video Streaming (Ch4)"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        localView = findViewById(R.id.localView)
        tvStatus = findViewById(R.id.tvStatus)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)

        btnStop.isEnabled = false

        localView.init(rootEglBase.eglBaseContext, null)
        localView.setMirror(true)

        btnStart.setOnClickListener { startStreaming() }
        btnStop.setOnClickListener { stopStreaming() }
        btnSwitchCamera.setOnClickListener { switchCamera() }
    }

    private fun startStreaming() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            tvStatus.text = "Camera permission required"
            return
        }

        tvStatus.text = "Initializing..."
        btnStart.isEnabled = false

        initWebRTC()
        startLocalVideo()
        connectSignaling()
    }

    private fun initWebRTC() {
        val options = PeerConnectionFactory.InitializationOptions.builder(this)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    private fun startLocalVideo() {
        videoCapturer = createCameraCapturer() ?: return

        val videoSource = peerConnectionFactory!!.createVideoSource(false)
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name, rootEglBase.eglBaseContext
        )
        videoCapturer?.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)
        videoCapturer?.startCapture(640, 480, 30)

        localVideoTrack = peerConnectionFactory!!.createVideoTrack("video0", videoSource)
        localVideoTrack?.addSink(localView)
        localView.visibility = View.VISIBLE
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(this)
        for (name in enumerator.deviceNames) {
            if (frontCamera && enumerator.isFrontFacing(name))
                return enumerator.createCapturer(name, null)
            if (!frontCamera && enumerator.isBackFacing(name))
                return enumerator.createCapturer(name, null)
        }
        return null
    }

    private fun connectSignaling() {
        val url = PrefsManager.getWebrtcStreamUrl(this)
        tvStatus.text = "Connecting to $url..."

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    tvStatus.text = "Signaling connected, creating offer..."
                    createPeerConnectionAndOffer()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread { handleSignaling(text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    tvStatus.text = "Signaling failed: ${t.message}"
                    btnStart.isEnabled = true
                }
            }
        })
    }

    private fun createPeerConnectionAndOffer() {
        val stunUrl = PrefsManager.getStunUrl(this)
        val iceServers = mutableListOf(
            PeerConnection.IceServer.builder(stunUrl).createIceServer()
        )
        val turnUrl = PrefsManager.getTurnUrl(this)
        if (turnUrl.isNotEmpty()) {
            iceServers.add(
                PeerConnection.IceServer.builder(turnUrl)
                    .setUsername(PrefsManager.getTurnUser(this))
                    .setPassword(PrefsManager.getTurnPass(this))
                    .createIceServer()
            )
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        peerConnection = peerConnectionFactory!!.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    val msg = JSONObject().apply {
                        put("type", "ice_candidate")
                        put("candidate", it.sdp)
                        put("sdpMid", it.sdpMid)
                        put("sdpMLineIndex", it.sdpMLineIndex)
                    }
                    webSocket?.send(msg.toString())
                }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                runOnUiThread {
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED -> {
                            tvStatus.text = "Streaming"
                            isStreaming = true
                            btnStop.isEnabled = true
                        }
                        PeerConnection.IceConnectionState.DISCONNECTED -> tvStatus.text = "Disconnected"
                        PeerConnection.IceConnectionState.FAILED -> tvStatus.text = "Connection failed"
                        else -> {}
                    }
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
        })

        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("local_stream")) }

        // Audio track
        val audioSource = peerConnectionFactory!!.createAudioSource(MediaConstraints())
        val audioTrack = peerConnectionFactory!!.createAudioTrack("audio0", audioSource)
        peerConnection?.addTrack(audioTrack, listOf("local_stream"))

        // Create offer
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        val msg = JSONObject().apply {
                            put("type", "offer")
                            put("sdp", sdp!!.description)
                        }
                        webSocket?.send(msg.toString())
                    }
                    override fun onCreateSuccess(p: SessionDescription?) {}
                    override fun onCreateFailure(s: String?) {}
                    override fun onSetFailure(s: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(s: String?) {
                Log.e("VideoStream", "Offer creation failed: $s")
            }
            override fun onSetFailure(s: String?) {}
        }, constraints)
    }

    private fun handleSignaling(text: String) {
        try {
            val json = JSONObject(text)
            when (json.getString("type")) {
                "answer" -> {
                    val answer = SessionDescription(
                        SessionDescription.Type.ANSWER, json.getString("sdp")
                    )
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            Log.d("VideoStream", "Remote answer set")
                        }
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
            }
        } catch (e: Exception) {
            Log.e("VideoStream", "Signaling error: ${e.message}")
        }
    }

    private fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
        frontCamera = !frontCamera
        localView.setMirror(frontCamera)
    }

    private fun stopStreaming() {
        isStreaming = false
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        localVideoTrack?.dispose()
        localVideoTrack = null
        peerConnection?.close()
        peerConnection = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        webSocket?.close(1000, "Stopped")
        webSocket = null

        tvStatus.text = "Stopped"
        btnStart.isEnabled = true
        btnStop.isEnabled = false
        localView.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isStreaming) stopStreaming()
        rootEglBase.release()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
