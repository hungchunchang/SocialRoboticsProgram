package com.example.socialroboticslab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.socialroboticslab.http.HttpChatActivity
import com.example.socialroboticslab.streaming.VideoStreamActivity
import com.example.socialroboticslab.videocall.VideoCallLobbyActivity
import com.example.socialroboticslab.websocket.AudioStreamActivity
import com.example.socialroboticslab.woz.WozRobotActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        findViewById<Button>(R.id.btnHttpChat).setOnClickListener {
            startActivity(Intent(this, HttpChatActivity::class.java))
        }

        findViewById<Button>(R.id.btnWsAudio).setOnClickListener {
            startActivity(Intent(this, AudioStreamActivity::class.java))
        }

        findViewById<Button>(R.id.btnWebrtcStream).setOnClickListener {
            startActivity(Intent(this, VideoStreamActivity::class.java))
        }

        findViewById<Button>(R.id.btnVideoCall).setOnClickListener {
            startActivity(Intent(this, VideoCallLobbyActivity::class.java))
        }

        findViewById<Button>(R.id.btnWozController).setOnClickListener {
            startActivity(Intent(this, WozRobotActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun requestPermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1)
        }
    }
}
