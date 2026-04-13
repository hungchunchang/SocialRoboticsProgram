package com.example.socialroboticslab.videocall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialroboticslab.R
import com.example.socialroboticslab.util.PrefsManager
import okhttp3.*
import org.json.JSONObject

/**
 * Ch5 對應功能：Video Call 大廳
 * 註冊、查看線上用戶、撥打 / 接聽 / 拒絕通話。
 */
class VideoCallLobbyActivity : AppCompatActivity() {

    private lateinit var etMyId: EditText
    private lateinit var btnRegister: Button
    private lateinit var etCallId: EditText
    private lateinit var btnCall: Button
    private lateinit var btnAnswer: Button
    private lateinit var btnDecline: Button
    private lateinit var tvStatus: TextView
    private lateinit var rvOnlineUsers: RecyclerView
    private lateinit var usersAdapter: OnlineUsersAdapter

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    private var myId: String = ""
    private var isRegistered = false
    private var incomingCall: String = ""

    private val callEndedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "VIDEO_CALL_ENDED") {
                Log.d("VideoCallLobby", "Call ended, reconnecting WebSocket")
                initWebSocket()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call_lobby)

        supportActionBar?.title = "Video Call (Ch5)"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        initWebSocket()
        setupClickListeners()
        setupRecyclerView()

        val filter = IntentFilter("VIDEO_CALL_ENDED")
        registerReceiver(callEndedReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private fun initViews() {
        etMyId = findViewById(R.id.etMyId)
        btnRegister = findViewById(R.id.btnRegister)
        etCallId = findViewById(R.id.etCallId)
        btnCall = findViewById(R.id.btnCall)
        btnAnswer = findViewById(R.id.btnAnswer)
        btnDecline = findViewById(R.id.btnDecline)
        tvStatus = findViewById(R.id.tvStatus)
        rvOnlineUsers = findViewById(R.id.rvOnlineUsers)

        etMyId.setText("User${(1000..9999).random()}")
        updateUIState()
    }

    private fun setupRecyclerView() {
        usersAdapter = OnlineUsersAdapter { userId ->
            etCallId.setText(userId)
        }
        rvOnlineUsers.layoutManager = LinearLayoutManager(this)
        rvOnlineUsers.adapter = usersAdapter
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            if (!isRegistered) registerUser() else unregisterUser()
        }
        btnCall.setOnClickListener { makeCall() }
        btnAnswer.setOnClickListener { answerCall() }
        btnDecline.setOnClickListener { declineCall() }
    }

    private fun initWebSocket() {
        webSocket?.close(1000, "Reconnecting")

        val url = PrefsManager.getVideoCallUrl(this)
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    tvStatus.text = "Connected to server"
                    if (myId.isNotEmpty()) registerUser()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread { handleMessage(text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread { tvStatus.text = "Connection failed: ${t.message}" }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                runOnUiThread { tvStatus.text = "Disconnected" }
            }
        })
    }

    private fun handleMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.getString("type")) {
                "register_success" -> {
                    isRegistered = true
                    myId = json.getString("id")
                    tvStatus.text = "Registered: $myId"
                    updateUIState()
                }
                "register_error" -> {
                    tvStatus.text = "Register failed: ${json.getString("message")}"
                }
                "user_list" -> {
                    val users = json.getJSONArray("users")
                    val list = mutableListOf<String>()
                    for (i in 0 until users.length()) {
                        val uid = users.getString(i)
                        if (uid != myId) list.add(uid)
                    }
                    usersAdapter.updateUsers(list)
                }
                "incoming_call" -> {
                    incomingCall = json.getString("from")
                    tvStatus.text = "Incoming call: $incomingCall"
                    updateUIState()
                }
                "call_accepted" -> {
                    startVideoCall(json.getString("peer"), isCaller = true)
                }
                "call_declined" -> {
                    tvStatus.text = "Call declined"
                    updateUIState()
                }
                "user_offline" -> {
                    tvStatus.text = "${json.getString("user")} went offline"
                }
            }
        } catch (e: Exception) {
            tvStatus.text = "Error: ${e.message}"
        }
    }

    private fun registerUser() {
        val userId = myId.ifEmpty { etMyId.text.toString().trim() }
        if (userId.isEmpty()) {
            Toast.makeText(this, "Enter a user ID", Toast.LENGTH_SHORT).show()
            return
        }
        val msg = JSONObject().apply {
            put("type", "register")
            put("id", userId)
        }
        webSocket?.send(msg.toString())
    }

    private fun unregisterUser() {
        val msg = JSONObject().apply {
            put("type", "unregister")
            put("id", myId)
        }
        webSocket?.send(msg.toString())
        isRegistered = false
        myId = ""
        usersAdapter.updateUsers(emptyList())
        updateUIState()
    }

    private fun makeCall() {
        val callId = etCallId.text.toString().trim()
        if (callId.isEmpty()) {
            Toast.makeText(this, "Enter a call ID", Toast.LENGTH_SHORT).show()
            return
        }
        val msg = JSONObject().apply {
            put("type", "call")
            put("from", myId)
            put("to", callId)
        }
        webSocket?.send(msg.toString())
        tvStatus.text = "Calling $callId..."
    }

    private fun answerCall() {
        val msg = JSONObject().apply {
            put("type", "call_answer")
            put("from", myId)
            put("to", incomingCall)
        }
        webSocket?.send(msg.toString())
        startVideoCall(incomingCall, isCaller = false)
    }

    private fun declineCall() {
        val msg = JSONObject().apply {
            put("type", "call_decline")
            put("from", myId)
            put("to", incomingCall)
        }
        webSocket?.send(msg.toString())
        incomingCall = ""
        tvStatus.text = "Call declined"
        updateUIState()
    }

    private fun startVideoCall(peerId: String, isCaller: Boolean) {
        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra("PEER_ID", peerId)
            putExtra("MY_ID", myId)
            putExtra("IS_CALLER", isCaller)
            putExtra("WEBSOCKET_URL", PrefsManager.getVideoCallUrl(this@VideoCallLobbyActivity))
        }
        startActivity(intent)
        incomingCall = ""
        updateUIState()
    }

    private fun updateUIState() {
        btnRegister.text = if (isRegistered) "Unregister" else "Register"
        btnCall.isEnabled = isRegistered && incomingCall.isEmpty()
        btnAnswer.isEnabled = incomingCall.isNotEmpty()
        btnDecline.isEnabled = incomingCall.isNotEmpty()
        etMyId.isEnabled = !isRegistered
        etCallId.isEnabled = isRegistered && incomingCall.isEmpty()
    }

    override fun onResume() {
        super.onResume()
        if (isRegistered) initWebSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(callEndedReceiver) } catch (_: Exception) {}
        webSocket?.close(1000, "Activity destroyed")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
