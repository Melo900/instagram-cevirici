package com.example.translator

import android.annotation.SuppressLint
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
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
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
    private var mediaProjection: MediaProjection? = null

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
        val projectionData = intent?.getParcelableExtra<Intent>("PROJECTION_DATA")
        val sourceLang = intent?.getStringExtra("SOURCE_LANG") ?: TranslateLanguage.ENGLISH
        val targetLang = intent?.getStringExtra("TARGET_LANG") ?: TranslateLanguage.TURKISH
        val textSize = intent?.getFloatExtra("TEXT_SIZE", 18f) ?: 18f
        val textColorHex = intent?.getStringExtra("TEXT_COLOR") ?: "#FFFFFF"
        val bgColorHex = intent?.getStringExtra("BG_COLOR") ?: "#CC000000"

        translatorManager?.setupTranslator(sourceLang, targetLang)

        if (projectionData != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, projectionData)
        }

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

        initCleanSpeechRecognizer()
        startListening()

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

        // Balon Taşıma Mantığı (Drag & Drop)
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

    private fun initCleanSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Ses odağının bozulmasını önleyen ekstra parametre
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { restartListening() }
                override fun onError(error: Int) { restartListening() }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndShow(matches[0])
                    }
                    restartListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndShow(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startListening() {
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            restartListening()
        }
    }

    private fun restartListening() {
        Handler(Looper.getMainLooper()).postDelayed({
            startListening()
        }, 150)
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
            val channel = NotificationChannel(channelId, "İç Ses Çeviri Servisi", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Instagram Akıllı Çevirici")
            .setContentText("Canlı altyazı aktif...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        speechRecognizer?.destroy()
        if (overlayTextView != null) { windowManager?.removeView(overlayTextView) }
        translatorManager?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
