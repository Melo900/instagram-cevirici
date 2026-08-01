package com.example.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.TranslateLanguage

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY = 101
    private val REQUEST_CODE_AUDIO = 102

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

        // BAŞLIK
        val titleText = TextView(this).apply {
            text = "Çeviri & Dil Yönetim Merkezi"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        }

        val subTitleText = TextView(this).apply {
            text = "Dilediğiniz dil paketini indirin ve canlı altyazıyı başlatın."
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, 0, 0, 32)
        }

        // DİL SEÇİM & İNDİRME KARTI
        val langCard = CardView(this).apply {
            radius = 36f
            setCardBackgroundColor(Color.parseColor("#1C1C1E"))
            setContentPadding(40, 40, 40, 40)
        }

        val langLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sourceLabel = TextView(this).apply {
            text = "Video Konuşma Dili (Kaynak):"
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 12)
        }

        val langList = translatorManager.supportedLanguages.keys.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, langList)

        val sourceSpinner = Spinner(this).apply {
            this.adapter = adapter
            setSelection(1) // Varsayılan İngilizce
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val langName = langList[position]
                    selectedSourceLang = translatorManager.supportedLanguages[langName] ?: TranslateLanguage.ENGLISH
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        val targetLabel = TextView(this).apply {
            text = "Altyazı Çeviri Dili (Hedef):"
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 12)
        }

        val targetSpinner = Spinner(this).apply {
            this.adapter = adapter
            setSelection(0) // Varsayılan Türkçe
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    val langName = langList[position]
                    selectedTargetLang = translatorManager.supportedLanguages[langName] ?: TranslateLanguage.TURKISH
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        val btnDownloadLangs = Button(this).apply {
            text = "📥  SEÇİLİ DİL PAKETLERİNİ UYGULAMAYA İNDİR"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#32D74B"))
            setOnClickListener { downloadSelectedLanguages() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 32, 0, 0)
            layoutParams = params
        }

        statusTextView = TextView(this).apply {
            text = "Dil Paketi Durumu: Hazır"
            textSize = 12f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, 16, 0, 0)
        }

        langLayout.addView(sourceLabel)
        langLayout.addView(sourceSpinner)
        langLayout.addView(targetLabel)
        langLayout.addView(targetSpinner)
        langLayout.addView(btnDownloadLangs)
        langLayout.addView(statusTextView)
        langCard.addView(langLayout)

        // SERVİSİ BAŞLAT BUTONU
        val btnStart = Button(this).apply {
            text = "🚀  ÇEVİRİ SERVİSİNİ BAŞLAT"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0A84FF"))
            setPadding(0, 36, 0, 36)
            setOnClickListener { checkPermissionsAndStart() }
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

    private fun downloadSelectedLanguages() {
        statusTextView.text = "Dil Paketleri İndiriliyor, Lütfen Bekleyin..."
        statusTextView.setTextColor(Color.parseColor("#FF9500"))

        translatorManager.downloadSpecificLanguage(selectedSourceLang, onSuccess = {
            translatorManager.downloadSpecificLanguage(selectedTargetLang, onSuccess = {
                statusTextView.text = "✅ Seçili Dil Paketleri Başarıyla İndirildi!"
                statusTextView.setTextColor(Color.parseColor("#30D158"))
                Toast.makeText(this, "Dil paketleri hazır!", Toast.LENGTH_SHORT).show()
            }, onFailure = {
                statusTextView.text = "❌ Hedef Dil Paketi İndirilemedi!"
                statusTextView.setTextColor(Color.RED)
            })
        }, onFailure = {
            statusTextView.text = "❌ Kaynak Dil Paketi İndirilemedi!"
            statusTextView.setTextColor(Color.RED)
        })
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_CODE_AUDIO
            )
            return
        }

        startTranslatorService()
    }

    private fun startTranslatorService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra("SOURCE_LANG", selectedSourceLang)
            putExtra("TARGET_LANG", selectedTargetLang)
            putExtra("TEXT_SIZE", selectedTextSize)
            putExtra("TEXT_COLOR", selectedTextColor)
            putExtra("BG_COLOR", selectedBgColor)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Çeviri Servisi Başlatıldı!", Toast.LENGTH_SHORT).show()
    }
}
