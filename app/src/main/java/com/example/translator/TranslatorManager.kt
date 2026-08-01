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

    // Desteklenen Popüler Dil Kodları Listesi
    val supportedLanguages = mapOf(
        "Türkçe" to TranslateLanguage.TURKISH,
        "İngilizce" to TranslateLanguage.ENGLISH,
        "İspanyolca" to TranslateLanguage.SPANISH,
        "Almanca" to TranslateLanguage.GERMAN,
        "Fransızca" to TranslateLanguage.FRENCH,
        "İtalyanca" to TranslateLanguage.ITALIAN,
        "Rusça" to TranslateLanguage.RUSSIAN,
        "Japonca" to TranslateLanguage.JAPANESE,
        "Çince" to TranslateLanguage.CHINESE,
        "Arapça" to TranslateLanguage.ARABIC,
        "Korece" to TranslateLanguage.KOREAN
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
        val conditions = DownloadConditions.Builder().build()

        modelManager.download(model, conditions)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun isModelDownloaded(langCode: String, onResult: (Boolean) -> Unit) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { isDownloaded -> onResult(isDownloaded) }
            .addOnFailureListener { onResult(false) }
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
