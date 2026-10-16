package com.example.practicafinal

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val continuar = Runnable {
        if (!isFinishing && !isDestroyed) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.img_logo)
        val progress = findViewById<ProgressBar>(R.id.progress)

        logo.scaleX = 0.3f
        logo.scaleY = 0.3f
        logo.alpha = 0f
        progress.max = 100

        
        logo.animate()
            .scaleX(1f).scaleY(1f)
            .alpha(1f)
            .setDuration(1500)
            .start()

        
        val anim = ObjectAnimator.ofInt(progress, "progress", 100)
        anim.duration = 1500
        anim.start()

        
        handler.postDelayed(continuar, 1600)
    }

    override fun onDestroy() {
        handler.removeCallbacks(continuar)
        findViewById<ImageView>(R.id.img_logo).animate().cancel()
        super.onDestroy()
    }
}
