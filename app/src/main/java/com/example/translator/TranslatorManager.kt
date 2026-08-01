package com.example.translator

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorManager(private val context: Context) {

    private var translator: Translator? = null
    private val modelManager = RemoteModelManager.getInstance()
    private var lastTranslatedText = ""

    val supportedLanguages = mapOf(
        "Türkçe" to TranslateLanguage.TURKISH,
        "İngilizce" to TranslateLanguage.ENGLISH,
        "İspanyolca" to TranslateLanguage.SPANISH,
        "Almanca" to TranslateLanguage.GERMAN,
        "Fransızca" to TranslateLanguage.FRENCH,
        "İtalyanca" to TranslateLanguage.ITALIAN,
        "Rusça" to TranslateLanguage.RUSSIAN,
        "Japonca" to TranslateLanguage.JAPANESE
    )

    fun setupTranslator(sourceLangCode: String, targetLangCode: String) {
        translator?.close()
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(targetLangCode)
            .build()
        translator = Translation.getClient(options)
    }

    fun downloadSpecificLanguage(langCode: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        
        // DİKKAT: Wi-Fi zorunluluğunu kaldırıp Mobil Veri ile de indirmeyi açtık!
        val conditions = DownloadConditions.Builder().build()

        // 1. Doğrudan Model İndiriciyi Tetikle
        modelManager.download(model, conditions)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                // 2. İlk deneme başarısız olursa Translator istemcisi üzerinden zorla indir
                val tempOptions = TranslatorOptions.Builder()
                    .setSourceLanguage(langCode)
                    .setTargetLanguage(TranslateLanguage.TURKISH)
                    .build()
                val tempTranslator = Translation.getClient(tempOptions)
                
                tempTranslator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        tempTranslator.close()
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        tempTranslator.close()
                        onFailure(e)
                    }
            }
    }

    fun translateSmartConversation(rawSpeechText: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val cleanedText = cleanSpeechArtifacts(rawSpeechText)
        if (cleanedText.isEmpty() || cleanedText == lastTranslatedText) return

        lastTranslatedText = cleanedText

        translator?.translate(cleanedText)
            ?.addOnSuccessListener { translatedText ->
                onSuccess(translatedText.trim())
            }
            ?.addOnFailureListener { e -> onFailure(e) }
    }

    private fun cleanSpeechArtifacts(text: String): String {
        return text.replace(Regex("(?i)\\b(uh|um|hmm|err|like|you know)\\b"), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun close() {
        translator?.close()
    }
}
