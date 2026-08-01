package com.example.translator

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var textViewSubtitle: TextView

    companion object {
        private var instance: OverlayService? = null

        fun updateSubtitleText(text: String) {
            instance?.let { service ->
                // Yakalanan metni Türkçe'ye çevirip ekrana basma mantığı
                TranslatorManager.translate(text) { translatedText ->
                    service.textViewSubtitle.post {
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
        
        // Basit bir TextView oluşturup ekrana balon şeklinde çiziyoruz
        textViewSubtitle = TextView(this).apply {
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
            setPadding(16, 16, 16, 16)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100
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
