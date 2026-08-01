package com.example.translator

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorManager {
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.TURKISH)
        .build()

    private val translator = Translation.getClient(options)

    fun prepareModel(onSuccess: () -> Unit) {
        translator.downloadModelIfNeeded()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { }
    }

    fun translate(text: String, onResult: (String) -> Unit) {
        if (text.isBlank()) return
        translator.translate(text)
            .addOnSuccessListener { onResult(it) }
    }
}
