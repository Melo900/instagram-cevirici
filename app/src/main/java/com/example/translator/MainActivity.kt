package com.example.translator

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
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
    private val REQUEST_CODE_MEDIA_PROJECTION = 103

    private var selectedTextSize = 18f
    private var selectedTextColor = "#FFFFFF"
    private var selectedBgColor = "#CC000000"

    private lateinit var previewText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#0F0F12"))
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(52, 60, 52, 60)
        }

        val titleText = TextView(this).apply {
            text = "Instagram Canlı İç Ses Çevirici"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        }

        val subTitleText = TextView(this).apply {
            text = "Mikrofonsuz, doğrudan sistem sesinden anlık çeviri yapın."
            textSize = 13f
            setTextColor(Color.parseColor("#8E8E93"))
            setPadding(0, 0, 0, 32)
        }

        // Önizleme Kartı
        val previewCard = CardView(this).apply {
            radius = 36f
            setCardBackgroundColor(Color.parseColor("#1C1C1E"))
            setContentPadding(40, 40, 40, 40)
        }

        val previewContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        previewText = TextView(this).apply {
            text = "Anlık çeviriler burada görünecektir."
            setPadding(36, 20, 36, 20)
            gravity = Gravity.CENTER
        }

        previewContainer.addView(previewText)
        previewCard.addView(previewContainer)

        // Başlat Butonu
        val btnStart = Button(this).apply {
            text = "🚀  İÇ SES ÇEVİRİSİNİ BAŞLAT"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0A84FF"))
            setPadding(0, 36, 0, 36)
            setOnClickListener { checkOverlayAndRequestProjection() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 40, 0, 0)
            layoutParams = params
        }

        mainLayout.addView(titleText)
        mainLayout.addView(subTitleText)
        mainLayout.addView(previewCard)
        mainLayout.addView(btnStart)

        scrollView.addView(mainLayout)
        setContentView(scrollView)

        updatePreview()
    }

    private fun updatePreview() {
        val backgroundDrawable = GradientDrawable().apply {
            setColor(Color.parseColor(selectedBgColor))
            setCornerRadius(28f)
        }
        previewText.apply {
            textSize = selectedTextSize
            setTextColor(Color.parseColor(selectedTextColor))
            background = backgroundDrawable
        }
    }

    private fun checkOverlayAndRequestProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            return
        }

        // Medya sesini yakalamak için sistem izni açıyoruz
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION && resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, OverlayService::class.java).apply {
                putExtra("PROJECTION_DATA", data)
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
