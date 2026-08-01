package com.example.translator

import android.content.Context
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateLanguage

class TranslatorManager(private val context: Context) {

    private var translator: Translator? = null

    init {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.TURKISH)
            .build()
        translator = Translation.getClient(options)
    }

    fun downloadModelIfNeeded(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        translator?.downloadModelIfNeeded()
            ?.addOnSuccessListener { onSuccess() }
            ?.addOnFailureListener { e -> onFailure(e) }
    }

    fun translate(text: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        translator?.translate(text)
            ?.addOnSuccessListener { translatedText -> onSuccess(translatedText) }
            ?.addOnFailureListener { e -> onFailure(e) }
    }

    fun close() {
        translator?.close()
    }
}
