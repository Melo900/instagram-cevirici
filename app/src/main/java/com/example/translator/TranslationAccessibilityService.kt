package com.example.translator

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TranslationAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Sadece Instagram uygulamasındaki ekran değişikliklerini dinle
        if (event.packageName == "com.instagram.android") {
            val rootNode = rootInActiveWindow ?: return
            findAndTranslateSubtitles(rootNode)
        }
    }

    private fun findAndTranslateSubtitles(node: AccessibilityNodeInfo) {
        // Ekrandaki tüm metin öğelerini tara
        if (node.text != null && node.text.isNotEmpty()) {
            val text = node.text.toString()
            
            // Eğer yakalanan metin altyazı niteliğindeyse OverlayService'e gönder
            if (text.length > 3) {
                OverlayService.instance?.translateAndShow(text)
            }
        }

        // Alt düğümleri (child nodes) özyinelemeli (recursive) olarak tara
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findAndTranslateSubtitles(child)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        // Servis kesintiye uğradığında çalışır
    }
}
