package com.example.translator

import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorManager {

    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.TURKISH)
        .build()

    private val englishTurkishTranslator = Translation.getClient(options)

    fun translate(text: String, onResult: (String) -> Unit) {
        englishTurkishTranslator.translate(text)
            .addOnSuccessListener { translatedText ->
                onResult(translatedText)
            }
            .addOnFailureListener {
                onResult("Çeviri Hatası")
            }
    }
}
