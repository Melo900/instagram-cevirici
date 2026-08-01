package com.example.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY = 101
    private val REQUEST_CODE_AUDIO = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()

        val button = Button(this).apply {
            text = "Çeviri Servisini Başlat"
            setOnClickListener { checkPermissionsAndStart() }
        }
        setContentView(button)
    }

    private fun checkPermissionsAndStart() {
        // Overlay (Ekran Üzerinde Gösterim) İzni
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            return
        }

        // Mikrofon İzni
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
        val intent = Intent(this, OverlayService::class.java)
        startService(intent)
        Toast.makeText(this, "Çevirici Başlatıldı!", Toast.LENGTH_SHORT).show()
    }
}
