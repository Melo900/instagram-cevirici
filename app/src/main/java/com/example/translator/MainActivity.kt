package com.example.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY = 101
    private val REQUEST_CODE_AUDIO = 102
    private var selectedTextSize = 18f
    private var selectedTextColor = "#FFFFFF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        val title = TextView(this).apply {
            text = "Altyazı Özelleştirme"
            textSize = 22f
            setPadding(0, 0, 0, 30)
        }

        val btnSize = Button(this).apply {
            text = "Yazı Boyutu: Orta (18sp)"
            setOnClickListener {
                if (selectedTextSize == 18f) {
                    selectedTextSize = 24f
                    text = "Yazı Boyutu: Büyük (24sp)"
                } else {
                    selectedTextSize = 18f
                    text = "Yazı Boyutu: Orta (18sp)"
                }
            }
        }

        val btnColor = Button(this).apply {
            text = "Yazı Rengi: Beyaz"
            setOnClickListener {
                if (selectedTextColor == "#FFFFFF") {
                    selectedTextColor = "#FFD700"
                    text = "Yazı Rengi: Sarı"
                } else {
                    selectedTextColor = "#FFFFFF"
                    text = "Yazı Rengi: Beyaz"
                }
            }
        }

        val btnStart = Button(this).apply {
            text = "Çeviri Servisini Başlat"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener { checkPermissionsAndStart() }
        }

        layout.addView(title)
        layout.addView(btnSize)
        layout.addView(btnColor)
        layout.addView(btnStart)

        setContentView(layout)
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
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Çevirici Başlatıldı!", Toast.LENGTH_SHORT).show()
    }
}
