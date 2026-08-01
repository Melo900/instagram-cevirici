package com.example.translator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.mlkit.nl.translate.TranslateLanguage

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY = 101
    private val REQUEST_CODE_MEDIA_PROJECTION = 103

    private lateinit var translatorManager: TranslatorManager
    private var selectedSourceLang = TranslateLanguage.ENGLISH
    private var selectedTargetLang = TranslateLanguage.TURKISH

    private var selectedTextSize = 18f
    private var selectedTextColor = "#FFFFFF"
    private var selectedBgColor = "#CC000000"

    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        translatorManager = TranslatorManager(this)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#0F0F12"))
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(52, 60, 52, 60)
        }

        val titleText = TextView(this).apply {
            text = "Akıllı İç Ses Çevirici"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        }

        val subTitleText = TextView(this).apply {
            text = "Medya sesini kısmadan, konuşmanın bağlamına göre anlık altyazı üretin."
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, 0, 0, 32)
        }

        val langCard = CardView(this).apply {
            radius = 36f
            setCardBackgroundColor(Color.parseColor("#1C1C1E"))
            setContentPadding(40, 40, 40, 40)
        }

        val langLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val langList = translatorManager.supportedLanguages.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langList)

        val sourceSpinner = Spinner(this).apply {
            this.adapter = adapter
            setSelection(1) // İngilizce
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) {
                    selectedSourceLang = translatorManager.supportedLanguages[langList[pos]] ?: TranslateLanguage.ENGLISH
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }

        val targetSpinner = Spinner(this).apply {
            this.adapter = adapter
            setSelection(0) // Türkçe
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) {
                    selectedTargetLang = translatorManager.supportedLanguages[langList[pos]] ?: TranslateLanguage.TURKISH
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }
        }

        val btnDownload = Button(this).apply {
            text = "📥 SEÇİLİ DİL PAKETLERİNİ İNDİR"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#32D74B"))
            setOnClickListener { downloadLanguages() }
        }

        statusTextView = TextView(this).apply {
            text = "Durum: Hazır"
            textSize = 12f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, 16, 0, 0)
        }

        langLayout.addView(TextView(this).apply { text = "Kaynak Konuşma Dili:"; setTextColor(Color.WHITE) })
        langLayout.addView(sourceSpinner)
        langLayout.addView(TextView(this).apply { text = "Hedef Altyazı Dili:"; setTextColor(Color.WHITE); setPadding(0, 16, 0, 0) })
        langLayout.addView(targetSpinner)
        langLayout.addView(btnDownload)
        langLayout.addView(statusTextView)
        langCard.addView(langLayout)

        val btnStart = Button(this).apply {
            text = "🚀 İÇ SES ÇEVİRİSİNİ BAŞLAT"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0A84FF"))
            setPadding(0, 36, 0, 36)
            setOnClickListener { checkOverlayAndStartProjection() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 40, 0, 0)
            layoutParams = params
        }

        mainLayout.addView(titleText)
        mainLayout.addView(subTitleText)
        mainLayout.addView(langCard)
        mainLayout.addView(btnStart)

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    private fun downloadLanguages() {
        statusTextView.text = "Paketler İndiriliyor..."
        statusTextView.setTextColor(Color.parseColor("#FF9500"))

        translatorManager.downloadSpecificLanguage(selectedSourceLang, onSuccess = {
            translatorManager.downloadSpecificLanguage(selectedTargetLang, onSuccess = {
                statusTextView.text = "✅ Dil Paketleri Hazır!"
                statusTextView.setTextColor(Color.parseColor("#30D158"))
            }, onFailure = { statusTextView.text = "❌ Hedef İndirilemedi" })
        }, onFailure = { statusTextView.text = "❌ Kaynak İndirilemedi" })
    }

    private fun checkOverlayAndStartProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            return
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            val serviceIntent = Intent(this, OverlayService::class.java).apply {
                putExtra("PROJECTION_DATA", data)
                putExtra("SOURCE_LANG", selectedSourceLang)
                putExtra("TARGET_LANG", selectedTargetLang)
                putExtra("TEXT_SIZE", selectedTextSize)
                putExtra("TEXT_COLOR", selectedTextColor)
                putExtra("BG_COLOR", selectedBgColor)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "İç Ses Çevirici Aktif!", Toast.LENGTH_SHORT).show()
        }
    }
}
