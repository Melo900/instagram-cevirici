package com.example.translator

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TranslationAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Sadece Instagram açıkken çalışır
        if (event.packageName == "com.instagram.android") {
            val rootNode = rootInActiveWindow ?: return
            findSubtitleAndTranslate(rootNode)
        }
    }

    private fun findSubtitleAndTranslate(node: AccessibilityNodeInfo) {
        // Ekrandaki metin düğümlerini tara
        if (node.text != null && node.text.isNotEmpty()) {
            val text = node.text.toString()
            // Eğer metin altyazı niteliği taşıyorsa OverlayService'e gönder
            if (text.length > 5) { 
                OverlayService.updateSubtitleText(text)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findSubtitleAndTranslate(child)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {}
}
