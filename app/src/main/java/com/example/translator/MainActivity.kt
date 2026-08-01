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
    private var selectedRadius = 24f

    private lateinit var previewText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#121212")) // Dark Mode Teması
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        // Başlık
        val titleText = TextView(this).apply {
            text = "Instagram Altyazı Stüdyosu"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 32)
        }

        // CANLI ÖNİZLEME KARTI
        val previewCard = CardView(this).apply {
            radius = 32f
            setCardBackgroundColor(Color.parseColor("#1E1E1E"))
            setContentPadding(40, 40, 40, 40)
        }

        val previewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val previewLabel = TextView(this).apply {
            text = "CANLI ALTYAZI ÖNİZLEMESİ"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, 20)
        }

        previewText = TextView(this).apply {
            text = "Örnek altyazı metni bu şekilde görünecek."
            setPadding(36, 20, 36, 20)
        }

        previewContainer.addView(previewLabel)
        previewContainer.addView(previewText)
        previewCard.addView(previewContainer)

        // AYARLAR PANELİ
        val settingsCard = CardView(this).apply {
            radius = 32f
            setCardBackgroundColor(Color.parseColor("#1E1E1E"))
            setContentPadding(40, 40, 40, 40)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 32, 0, 32)
            layoutParams = params
        }

        val settingsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. Yazı Boyutu Slider
        val sizeLabel = TextView(this).apply {
            text = "Yazı Boyutu"
            setTextColor(Color.WHITE)
        }
        val sizeSeekBar = SeekBar(this).apply {
            max = 20
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

        // 2. Renk Seçenekleri
        val colorLabel = TextView(this).apply {
            text = "Yazı Rengi"
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 12)
        }
        val colorLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val colors = listOf("#FFFFFF" to "Beyaz", "#FFD700" to "Sarı", "#00FFFF" to "Turkuaz")
        colors.forEach { (hex, name) ->
            val btn = Button(this).apply {
                text = name
                setOnClickListener {
                    selectedTextColor = hex
                    updatePreview()
                }
            }
            colorLayout.addView(btn)
        }

        // 3. Arka Plan Şeffaflığı
        val bgLabel = TextView(this).apply {
            text = "Arka Plan Şeffaflığı"
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 12)
        }
        val bgLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val bgOptions = listOf("#CC000000" to "Koyu", "#80000000" to "Yarı Şeffaf", "#FF000000" to "Siyah")
        bgOptions.forEach { (hex, name) ->
            val btn = Button(this).apply {
                text = name
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

        // BAŞLAT BUTONU
        val btnStart = Button(this).apply {
            text = "ÇEVİRİ SERVİSİNİ BAŞLAT"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#6200EE"))
            setPadding(0, 32, 0, 32)
            setOnClickListener { checkPermissionsAndStart() }
        }

        mainLayout.addView(titleText)
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
        Toast.makeText(this, "Çevirici Başlatıldı!", Toast.LENGTH_SHORT).show()
    }
}
