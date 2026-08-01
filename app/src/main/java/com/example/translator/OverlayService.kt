package com.example.translator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
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
import java.util.Locale

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayTextView: TextView? = null
    private var translatorManager: TranslatorManager? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false

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
        initNonIntrusiveSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val textSize = intent?.getFloatExtra("TEXT_SIZE", 18f) ?: 18f
        val textColorHex = intent?.getStringExtra("TEXT_COLOR") ?: "#FFFFFF"
        val bgColorHex = intent?.getStringExtra("BG_COLOR") ?: "#CC000000"
        val cornerRadius = intent?.getFloatExtra("CORNER_RADIUS", 24f) ?: 24f

        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor(bgColorHex))
            setCornerRadius(cornerRadius)
        }

        overlayTextView?.apply {
            setTextSize(textSize)
            setTextColor(Color.parseColor(textColorHex))
            background = backgroundDrawable
        }

        translatorManager?.downloadModelExplicitly(
            onSuccess = {
                overlayTextView?.text = "Dinleniyor..."
                startListeningLoop()
            },
            onFailure = {
                overlayTextView?.text = "Dil Modeli Hatası!"
            }
        )

        return START_STICKY
    }

    private fun setupOverlayView() {
        overlayTextView = TextView(this).apply {
            text = "Çevirici Başlatılıyor..."
            setPadding(40, 24, 40, 24)
            elevation = 10f
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
            y = 180
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

    private fun initNonIntrusiveSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Ses odağını istemeden arka planda dinleme sağlama
                putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
                putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 500L)
                putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 500L)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    restartListeningWithDelay()
                }

                override fun onError(error: Int) {
                    restartListeningWithDelay()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndShow(matches[0])
                    }
                    restartListeningWithDelay()
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

    private fun startListeningLoop() {
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            restartListeningWithDelay()
        }
    }

    private fun restartListeningWithDelay() {
        isListening = false
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!isListening) {
                startListeningLoop()
            }
        }, 300) // 300ms içinde ses odağını bozmadan tekrar dinlemeye geçer
    }

    private fun translateAndShow(englishText: String) {
        translatorManager?.translate(
            text = englishText,
            onSuccess = { translatedText ->
                overlayTextView?.text = translatedText
            },
            onFailure = {
                overlayTextView?.text = englishText
            }
        )
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "translator_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Canlı Altyazı Servisi", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Instagram Kesintisiz Çevirici")
            .setContentText("Video sesi kısılmadan anlık çeviri yapılıyor...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        if (overlayTextView != null) {
            windowManager?.removeView(overlayTextView)
        }
        translatorManager?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
