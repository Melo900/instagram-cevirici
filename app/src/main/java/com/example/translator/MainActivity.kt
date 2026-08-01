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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY = 101
    private val REQUEST_CODE_AUDIO = 102

    private var selectedTextSize = 18f
    private var selectedTextColor = "#FFFFFF"
    private var selectedBgColor = "#CC000000"
    private var selectedRadius = 28f

    private lateinit var previewText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#0F0F12")) // Premium Ultra Dark Background
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(52, 60, 52, 60)
        }

        // --- BÖLÜM 1: ÜST BAŞLIK & ROLES ---
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 40)
        }

        val titleText = TextView(this).apply {
            text = "Instagram Altyazı Stüdyosu"
            textSize = 26f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        }

        val subTitleText = TextView(this).apply {
            text = "Görünümü özelleştirin ve canlı medya çevirisini başlatın"
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
        }

        headerLayout.addView(titleText)
        headerLayout.addView(subTitleText)

        // --- BÖLÜM 2: CANLI ÖNİZLEME KARTI (Glassmorphism Effect) ---
        val previewCard = CardView(this).apply {
            radius = 36f
            setCardBackgroundColor(Color.parseColor("#1C1C1E"))
            cardElevation = 12f
            setContentPadding(48, 48, 48, 48)
        }

        val previewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val previewLabel = TextView(this).apply {
            text = "✨ CANLI ÖNİZLEME"
            textSize = 11f
            setTextColor(Color.parseColor("#0A84FF"))
            setPadding(0, 0, 0, 24)
        }

        previewText = TextView(this).apply {
            text = "Tebrikler! Altyazınız canlı olarak burada görünecek."
            setPadding(42, 24, 42, 24)
            gravity = Gravity.CENTER
        }

        previewContainer.addView(previewLabel)
        previewContainer.addView(previewText)
        previewCard.addView(previewContainer)

        // --- BÖLÜM 3: ÖZELLEŞTİRME PANELİ ---
        val settingsCard = CardView(this).apply {
            radius = 36f
            setCardBackgroundColor(Color.parseColor("#1C1C1E"))
            cardElevation = 8f
            setContentPadding(48, 48, 48, 48)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 40, 0, 40)
            layoutParams = params
        }

        val settingsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. Yazı Boyutu Slider
        val sizeLabel = TextView(this).apply {
            text = "Yazı Boyutu (Font Size)"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }

        val sizeSeekBar = SeekBar(this).apply {
            max = 16
            progress = 4 // Default 18sp
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    selectedTextSize = 14f + progress
                    updatePreview()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        // 2. Metin Rengi Paleti
        val colorLabel = TextView(this).apply {
            text = "Metin Rengi"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 16)
        }

        val colorLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val colors = listOf(
            "#FFFFFF" to "Beyaz",
            "#FFD60A" to "Altın Sarı",
            "#30D158" to "Neon Yeşil",
            "#64D2FF" to "Buz Mavi"
        )

        colors.forEach { (hex, name) ->
            val btn = Button(this).apply {
                text = name
                textSize = 11f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#2C2C2E"))
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                params.setMargins(6, 0, 6, 0)
                layoutParams = params
                setOnClickListener {
                    selectedTextColor = hex
                    updatePreview()
                }
            }
            colorLayout.addView(btn)
        }

        // 3. Arka Plan Şeffaflığı
        val bgLabel = TextView(this).apply {
            text = "Arka Plan Şeffaflığı & Teması"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 32, 0, 16)
        }

        val bgLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val bgOptions = listOf(
            "#E6000000" to "%90 Koyu",
            "#99000000" to "%60 Şeffaf",
            "#4D000000" to "%30 Cam",
            "#FF000000" to "Opak Siyah"
        )

        bgOptions.forEach { (hex, name) ->
            val btn = Button(this).apply {
                text = name
                textSize = 11f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#2C2C2E"))
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                params.setMargins(6, 0, 6, 0)
                layoutParams = params
                setOnClickListener {
                    selectedBgColor = hex
                    updatePreview()
                }
            }
            bgLayout.addView(btn)
        }

        settingsLayout.addView(sizeLabel)
        settingsLayout.addView(sizeSeekBar)
        settingsLayout.addView(colorLabel)
        settingsLayout.addView(colorLayout)
        settingsLayout.addView(bgLabel)
        settingsLayout.addView(bgLayout)
        settingsCard.addView(settingsLayout)

        // --- BÖLÜM 4: BAŞLATMA BUTONU ---
        val btnStart = Button(this).apply {
            text = "🚀  ÇEVİRİ SERVİSİNİ BAŞLAT"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0A84FF")) // iOS Accent Blue
            setPadding(0, 36, 0, 36)
            setOnClickListener { checkPermissionsAndStart() }
        }

        mainLayout.addView(headerLayout)
        mainLayout.addView(previewCard)
        mainLayout.addView(settingsCard)
        mainLayout.addView(btnStart)

        scrollView.addView(mainLayout)
        setContentView(scrollView)

        updatePreview()
    }

    private fun updatePreview() {
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor(selectedBgColor))
            setCornerRadius(selectedRadius)
        }
        previewText.apply {
            textSize = selectedTextSize
            setTextColor(Color.parseColor(selectedTextColor))
            background = backgroundDrawable
        }
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
            putExtra("TEXT_SIZE", selectedTextSize)
            putExtra("TEXT_COLOR", selectedTextColor)
            putExtra("BG_COLOR", selectedBgColor)
            putExtra("CORNER_RADIUS", selectedRadius)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Çeviri Servisi Aktif!", Toast.LENGTH_SHORT).show()
    }
}
