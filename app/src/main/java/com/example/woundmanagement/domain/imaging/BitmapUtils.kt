package com.example.woundmanagement.domain.imaging

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.woundmanagement.BuildConfig
import com.example.woundmanagement.security.PhotoManager
import java.io.File

object BitmapUtils {

    fun loadFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            // I file salvati internamente sono cifrati (.enc): usare PhotoManager per decifrarli.
            // Gli URI content:// (galleria) vengono letti direttamente con ContentResolver.
            val path = uri.path
            if (uri.scheme == "file" && path != null) {
                // path può essere assoluto (/data/user/...) oppure solo il filename
                PhotoManager.loadBitmap(context, path)
            } else {
                decodeContentUriSampled(context, uri, PhotoManager.MAX_DECODE_SIZE)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("BitmapUtils", "loadFromUri failed", e)
            null
        }
    }

    /**
     * Decodifica da un URI content:// leggendo i byte UNA SOLA VOLTA, poi campionando
     * con inSampleSize (anti-OOM). I provider della galleria spesso forniscono stream
     * NON riapribili: la precedente doppia apertura (bounds + decode) falliva su molti
     * dispositivi. I byte compressi in memoria non sono un problema; l'OOM riguarda la
     * bitmap decodificata, gestita dal campionamento.
     */
    private fun decodeContentUriSampled(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        return PhotoManager.decodeSampled(bytes, maxSize)
    }

    /**
     * Copia un'immagine da un URI content:// nello storage interno cifrato.
     * Restituisce un Uri file:// con percorso assoluto per l'uso in sessione;
     * nel DB viene salvato solo il filename (lastPathSegment) tramite [DressingFlowScreen].
     */
    fun copyToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val bitmap = loadFromUri(context, sourceUri) ?: return null
            val filename = PhotoManager.saveEncrypted(context, bitmap)
            bitmap.recycle()
            val fullPath = File(PhotoManager.getEncryptedDir(context), filename).absolutePath
            Uri.parse("file://$fullPath")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.e("BitmapUtils", "copyToInternalStorage failed", e)
            null
        }
    }
}
