package com.example.translator

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var textViewSubtitle: TextView

    companion object {
        private var instance: OverlayService? = null

        fun updateSubtitleText(text: String) {
            instance?.let { service ->
                TranslatorManager.translate(text) { translatedText ->
                    Handler(Looper.getMainLooper()).post {
                        service.textViewSubtitle.text = translatedText
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        textViewSubtitle = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#C8000000")) // Yarı saydam siyah
            setPadding(24, 16, 24, 16)
            text = "Altyazı bekleniyor..."
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 180
        }

        windowManager.addView(textViewSubtitle, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::textViewSubtitle.isInitialized) {
            windowManager.removeView(textViewSubtitle)
        }
        instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
