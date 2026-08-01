package com.example.translator

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslatorManager {

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.TURKISH)
        .build()

    private val englishTurkishTranslator = Translation.getClient(options)

    fun translate(text: String, onResult: (String) -> Unit) {
        englishTurkishTranslator.downloadModelIfNeeded()
            .addOnSuccessListener {
                englishTurkishTranslator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        onResult(translatedText)
                    }
                    .addOnFailureListener {
                        onResult(text) // Hata durumunda orijinal metni bas
                    }
            }
            .addOnFailureListener {
                onResult(text)
            }
    }
}
