package com.example.translator

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
    private var audioManager: AudioManager? = null

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
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        translatorManager = TranslatorManager(this)

        setupOverlayView()
        initCleanSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val textSize = intent?.getFloatExtra("TEXT_SIZE", 18f) ?: 18f
        val textColorHex = intent?.getStringExtra("TEXT_COLOR") ?: "#FFFFFF"
        val bgColorHex = intent?.getStringExtra("BG_COLOR") ?: "#CC000000"
        val cornerRadius = intent?.getFloatExtra("CORNER_RADIUS", 28f) ?: 28f

        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor(bgColorHex))
            setCornerRadius(cornerRadius)
            setStroke(2, Color.parseColor("#33FFFFFF")) // İnce şık beyaz çerçeve
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
            setPadding(48, 28, 48, 28)
            elevation = 16f
            gravity = Gravity.CENTER
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

    private fun initCleanSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    restartListening()
                }

                override fun onError(error: Int) {
                    restartListening()
                }

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

    private fun startListeningLoop() {
        if (isListening) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(false)
                    .build()
                audioManager?.abandonAudioFocusRequest(focusRequest)
            }

            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            restartListening()
        }
    }

    private fun restartListening() {
        isListening = false
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            startListeningLoop()
        }, 150)
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
            .setContentTitle("Instagram Canlı Çeviri")
            .setContentText("Kesintisiz çeviri aktif...")
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
