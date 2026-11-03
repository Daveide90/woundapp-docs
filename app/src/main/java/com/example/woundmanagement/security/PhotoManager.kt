package com.example.woundmanagement.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.util.UUID

object PhotoManager {

    private const val ENCRYPTED_DIR = "wound_photos_enc"

    /**
     * Tetto sul lato maggiore (px) per la decodifica delle bitmap: evita OutOfMemory
     * su foto da molti megapixel. Applicato sia al load per la UI sia alla copia/salvataggio.
     */
    const val MAX_DECODE_SIZE = 2048

    // Cache thumbnail: 1/8 della memoria disponibile
    private val thumbnailCache: LruCache<String, Bitmap> = run {
        val maxMem = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMem / 8
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int =
                bitmap.byteCount / 1024
        }
    }

    /** Ritorna la directory cifrata, creandola se necessario. */
    fun getEncryptedDir(context: Context): File =
        File(context.filesDir, ENCRYPTED_DIR).also { it.mkdirs() }

    /**
     * Salva la bitmap cifrata e ritorna solo il nome del file (es. "abc123.jpg.enc").
     */
    fun saveEncrypted(context: Context, bitmap: Bitmap, quality: Int = 90): String {
        val dir = getEncryptedDir(context)
        val filename = "${UUID.randomUUID()}.jpg.enc"
        val file = File(dir, filename)
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedFile = EncryptedFile.Builder(
            context, file, masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        encryptedFile.openFileOutput().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return filename
    }

    /**
     * Carica la bitmap da un nome file o percorso assoluto.
     * Decodifica a due passate (prima le sole dimensioni, poi con inSampleSize) con
     * tetto [maxSize] sul lato maggiore per evitare OutOfMemory su foto da molti MP.
     */
    fun loadBitmap(context: Context, nameOrPath: String, maxSize: Int = MAX_DECODE_SIZE): Bitmap? {
        val bytes = decryptToBytes(context, nameOrPath) ?: return null
        return decodeSampled(bytes, maxSize)
    }

    /**
     * Carica una thumbnail ridimensionata e cachata.
     * [maxSize] e' il lato massimo in pixel (es. 420 per 140dp @3x).
     */
    fun loadThumbnail(context: Context, nameOrPath: String, maxSize: Int = 420): Bitmap? {
        val cacheKey = "$nameOrPath@$maxSize"
        thumbnailCache.get(cacheKey)?.let { return it }

        val bytes = decryptToBytes(context, nameOrPath) ?: return null
        val bitmap = decodeSampled(bytes, maxSize) ?: return null
        thumbnailCache.put(cacheKey, bitmap)
        return bitmap
    }

    /** Svuota la cache thumbnail (es. dopo restore backup). */
    fun clearCache() {
        thumbnailCache.evictAll()
    }

    // ── Internals ──

    private fun decryptToBytes(context: Context, nameOrPath: String): ByteArray? {
        return try {
            val file = resolveFile(context, nameOrPath) ?: return null
            if (!file.exists() || !file.name.endsWith(".enc")) return null
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val encryptedFile = EncryptedFile.Builder(
                context, file, masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileInput().use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFile(context: Context, nameOrPath: String): File? {
        return if (nameOrPath.startsWith("/")) {
            val f = File(nameOrPath)
            val encDir = getEncryptedDir(context).canonicalPath
            if (!f.canonicalPath.startsWith(encDir)) null else f
        } else {
            if (nameOrPath.contains("..") || nameOrPath.contains("/")) null
            else File(getEncryptedDir(context), nameOrPath)
        }
    }

    /**
     * Decodifica a due passate: prima le sole dimensioni (inJustDecodeBounds), poi
     * con inSampleSize calcolato perché il lato maggiore resti entro [maxSize].
     */
    internal fun decodeSampled(bytes: ByteArray, maxSize: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, maxSize)
        opts.inJustDecodeBounds = false
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    /** inSampleSize (potenza di 2) tale che il lato maggiore risulti <= maxSize. */
    internal fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var inSampleSize = 1
        while (width / inSampleSize > maxSize || height / inSampleSize > maxSize) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
