package com.example.translator

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var translatorManager: TranslatorManager? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        translatorManager = TranslatorManager(this)

        overlayTextView = TextView(this).apply {
            text = "Dil Modeli Yükleniyor..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(32, 16, 32, 16)
        }

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 150
        }

        windowManager?.addView(overlayTextView, params)

        // Dil modelini indir ve hazır olunca test çevirisi yap
        translatorManager?.downloadModelIfNeeded(
            onSuccess = {
                overlayTextView?.text = "Model Hazır! Test Ediliyor..."
                testTranslation()
            },
            onFailure = { e ->
                overlayTextView?.text = "Model İndirilemedi!"
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun testTranslation() {
        translatorManager?.translate(
            text = "Hello, this is a live caption test.",
            onSuccess = { translatedText ->
                overlayTextView?.text = translatedText
            },
            onFailure = {
                overlayTextView?.text = "Çeviri hatası!"
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayTextView != null) {
            windowManager?.removeView(overlayTextView)
        }
        translatorManager?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
