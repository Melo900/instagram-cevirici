package com.example.translator

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.mlkit.nl.translate.TranslateLanguage

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var translatorManager: TranslatorManager? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    companion object {
        var instance: OverlayService? = null
            private set

        // Ekrandaki yazıyı dışarıdan değiştirmek için çağıracağımız fonksiyon
        fun updateSubtitleText(text: String) {
            instance?.mainHandler?.post {
                instance?.overlayTextView?.text = text
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        startForegroundServiceWithNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        translatorManager = TranslatorManager(this)

        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sourceLang = intent?.getStringExtra("SOURCE_LANG") ?: TranslateLanguage.ENGLISH
        val targetLang = intent?.getStringExtra("TARGET_LANG") ?: TranslateLanguage.TURKISH

        translatorManager?.setupTranslator(sourceLang, targetLang)
        overlayTextView?.text = "Altyazı bekleniyor..."

        return START_STICKY
    }

    private fun setupOverlayView() {
        overlayTextView = TextView(this).apply {
            text = "Başlatılıyor..."
            setPadding(48, 28, 48, 28)
            elevation = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            
            val backgroundDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#CC000000")) // Yarı saydam siyah
                setCornerRadius(28f)
                setStroke(2, Color.parseColor("#33FFFFFF"))
            }
            background = backgroundDrawable
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
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Baloncuğu sürükleyip taşıma mantığı
        overlayTextView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(overlayTextView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Instagram'dan gelen ham metni çevirip baloncukta gösterme fonksiyonu
    fun translateAndShow(rawText: String) {
        if (rawText.isBlank()) return
        
        translatorManager?.translateSmartConversation(
            rawSpeechText = rawText,
            onSuccess = { smartTranslatedText ->
                overlayTextView?.text = smartTranslatedText
            },
            onFailure = {
                overlayTextView?.text = rawText
            }
        )
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "translator_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Canlı Çeviri Servisi", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Instagram Canlı Çevirici")
            .setContentText("Çeviri balonu aktif...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        mainHandler.removeCallbacksAndMessages(null)
        if (overlayTextView != null) {
            try {
                windowManager?.removeView(overlayTextView)
            } catch (e: Exception) {}
        }
        translatorManager?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
