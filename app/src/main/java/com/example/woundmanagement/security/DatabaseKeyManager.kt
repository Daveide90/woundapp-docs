package com.example.woundmanagement.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Gestisce la chiave di cifratura del database.
 * La chiave viene generata casualmente al primo avvio (256 bit) e salvata,
 * codificata in Base64, in EncryptedSharedPreferences (protetto dall'Android Keystore).
 *
 * La passphrase passata a SQLCipher sono i byte ASCII della stringa Base64 (NON i 32
 * byte grezzi decodificati): essendo Base64 composto da soli caratteri ASCII, la
 * passphrase è priva di perdite quando attraversa le API char[]/byte[] di SQLCipher
 * (getBytes/getChars usano UTF-8). Questo è ciò che rende possibile il rekey sicuro
 * dei database legacy in [com.example.woundmanagement.data.db.AppDatabase].
 */
object DatabaseKeyManager {

    private const val PREFS_NAME = "wound_db_key"
    private const val KEY_PASSPHRASE = "db_passphrase"
    private const val KEY_LENGTH_BYTES = 32

    /**
     * Restituisce la passphrase SQLCipher come byte ASCII della stringa Base64 memorizzata.
     * Alla prima esecuzione genera 256 bit casuali e li persiste.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray =
        getOrCreatePassphraseString(context).toByteArray(Charsets.US_ASCII)

    private fun getOrCreatePassphraseString(context: Context): String {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val stored = prefs.getString(KEY_PASSPHRASE, null)
        if (stored != null) return stored

        // Prima esecuzione: genera una chiave casuale a 256 bit e la salva in Base64.
        val raw = ByteArray(KEY_LENGTH_BYTES)
        SecureRandom().nextBytes(raw)
        val encoded = Base64.encodeToString(raw, Base64.DEFAULT)
        raw.fill(0)
        // commit() e NON apply(): apply scrive su disco in modo asincrono. Se il processo
        // morisse tra la generazione della chiave e la scrittura effettiva, al riavvio si
        // genererebbe una chiave DIVERSA e il database appena cifrato diventerebbe
        // illeggibile per sempre. Succede una volta sola nella vita dell'app: il costo
        // della scrittura sincrona è irrilevante, la perdita dei dati no.
        prefs.edit().putString(KEY_PASSPHRASE, encoded).commit()
        return encoded
    }
}
