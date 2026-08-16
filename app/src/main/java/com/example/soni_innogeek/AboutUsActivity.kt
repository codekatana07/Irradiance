package com.example.soni_innogeek

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import android.widget.MediaController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_us)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBarLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // Action bar setup
        supportActionBar?.title = "About Us"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 🔥 Model Video Setup
        val videoView = findViewById<VideoView>(R.id.modelVideoView)
        val uri = Uri.parse("android.resource://${packageName}/${R.raw.model}")
        videoView.setVideoURI(uri)

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoView.start()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

