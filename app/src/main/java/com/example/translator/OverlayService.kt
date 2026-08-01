package com.example.translator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
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

    override fun onCreate() {
        super.onCreate()

        startForegroundServiceWithNotification()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        translatorManager = TranslatorManager(this)

        setupOverlayView()
        initSpeechRecognizer()

        overlayTextView?.text = "Google Çeviri Modeli İndiriliyor..."

        translatorManager?.downloadModelIfNeeded(
            onSuccess = {
                overlayTextView?.text = "Model Yüklendi! Dinleniyor..."
                startListening()
            },
            onFailure = { e ->
                overlayTextView?.text = "İndirme Hatası: ${e.localizedMessage}"
            }
        )
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "translator_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Canlı Altyazı Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Instagram Canlı Çeviri")
            .setContentText("Google Dil Paketi Kontrol Ediliyor...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    private fun setupOverlayView() {
        overlayTextView = TextView(this).apply {
            text = "Başlatılıyor..."
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#E6000000"))
            setPadding(32, 20, 32, 20)
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
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    startListening()
                }

                override fun onError(error: Int) {
                    startListening()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        translateAndShow(matches[0])
                    }
                    startListening()
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
        speechRecognizer?.startListening(recognizerIntent)
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

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        if (overlayTextView != null) {
            windowManager?.removeView(overlayTextView)
        }
        translatorManager?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
