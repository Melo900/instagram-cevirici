package com.example.translator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var translatorManager: TranslatorManager? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRecognizerActive = false

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        translatorManager = TranslatorManager(this)

        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sourceLang = intent?.getStringExtra("SOURCE_LANG") ?: TranslateLanguage.ENGLISH
        val targetLang = intent?.getStringExtra("TARGET_LANG") ?: TranslateLanguage.TURKISH
        val textSize = intent?.getFloatExtra("TEXT_SIZE", 18f) ?: 18f
        val textColorHex = intent?.getStringExtra("TEXT_COLOR") ?: "#FFFFFF"
        val bgColorHex = intent?.getStringExtra("BG_COLOR") ?: "#CC000000"

        translatorManager?.setupTranslator(sourceLang, targetLang)

        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor(bgColorHex))
            setCornerRadius(28f)
            setStroke(2, Color.parseColor("#33FFFFFF"))
        }

        overlayTextView?.apply {
            setTextSize(textSize)
            setTextColor(Color.parseColor(textColorHex))
            background = backgroundDrawable
            text = "Dinleniyor..."
        }

        startFreshSpeechRecognizer()

        return START_STICKY
    }

    private fun setupOverlayView() {
        overlayTextView = TextView(this).apply {
            text = "Başlatılıyor..."
            setPadding(48, 28, 48, 28)
            elevation = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC000000"))
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

        windowManager?.addView(overlayTextView, params)
    }

    // Takılmayı önlemek için SpeechRecognizer'ı her döngüde yenileyen güvenli başlatıcı
    private fun startFreshSpeechRecognizer() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {}

            if (!SpeechRecognizer.isRecognitionAvailable(this)) return@post

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isRecognizerActive = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    scheduleRestart()
                }

                override fun onError(error: Int) {
                    scheduleRestart()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndShow(matches[0])
                    }
                    scheduleRestart()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndShow(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                scheduleRestart()
            }
        }
    }

    private fun scheduleRestart() {
        isRecognizerActive = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            startFreshSpeechRecognizer()
        }, 100)
    }

    private fun translateAndShow(rawSpeechText: String) {
        translatorManager?.translateSmartConversation(
            rawSpeechText = rawSpeechText,
            onSuccess = { smartTranslatedText ->
                overlayTextView?.text = smartTranslatedText
            },
            onFailure = {
                overlayTextView?.text = rawSpeechText
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
            .setContentText("Altyazı akışı aktif...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        if (overlayTextView != null) { windowManager?.removeView(overlayTextView) }
        translatorManager?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
