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
    private val turkishModel = TranslateRemoteModel.Builder(TranslateLanguage.TURKISH).build()

    init {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.TURKISH)
            .build()
        translator = Translation.getClient(options)
    }

    fun downloadModelExplicitly(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Wi-Fi veya hücresel veri fark etmeksizin indirmesini sağlıyoruz
        val conditions = DownloadConditions.Builder().build()
        
        modelManager.download(turkishModel, conditions)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                // Alternatif fallback indirme yöntemi
                translator?.downloadModelIfNeeded(conditions)
                    ?.addOnSuccessListener { onSuccess() }
                    ?.addOnFailureListener { e -> onFailure(e) }
            }
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
