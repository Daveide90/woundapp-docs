package com.example.woundmanagement.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.woundmanagement.ui.strings.Jurisdiction

object AppPreferences {

    private const val PREFS_NAME = "wound_app_prefs"
    private const val KEY_CONSENT = "consent_given"
    private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"
    private const val KEY_CONSENT_VERSION = "consent_version"
    private const val KEY_CONSENT_JURISDICTION = "consent_jurisdiction"
    private const val KEY_LAST_ACTIVITY = "last_activity_ms"
    private const val KEY_LANGUAGE = "app_language"
    /** Giurisdizione scelta dall'utente: "EU" / "US" / "ROW" (vedi Jurisdiction). */
    private const val KEY_REGION = "jurisdiction_region"
    private const val CURRENT_CONSENT_VERSION = 2

    // Istanza riusata: creare EncryptedSharedPreferences costa (Keystore + Tink) e qui si
    // scrive a ogni tocco e a ogni tasto digitato nelle note (rinnovo del timer di
    // inattività) — ricrearla ogni volta rendeva la digitazione a scatti.
    @Volatile
    private var cachedPrefs: android.content.SharedPreferences? = null

    private fun prefs(context: Context): android.content.SharedPreferences =
        cachedPrefs ?: synchronized(this) {
            cachedPrefs ?: createPrefs(context.applicationContext).also { cachedPrefs = it }
        }

    private fun createPrefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Mai fallback a storage non cifrato — meglio crash che leak dati
        throw SecurityException("Keystore non disponibile: impossibile cifrare le preferenze", e)
    }

    fun isConsentGiven(context: Context): Boolean {
        val p = prefs(context)
        val consented = p.getBoolean(KEY_CONSENT, false)
        val version = p.getInt(KEY_CONSENT_VERSION, 0)
        // La giurisdizione al momento del consenso deve combaciare con quella attuale:
        // se il Paese cambia in modo da cambiare la giurisdizione, il consenso decade.
        val consentJurisdiction = p.getString(KEY_CONSENT_JURISDICTION, null)
        val jurisdictionMatches = consentJurisdiction == getJurisdiction(context).name
        return consented && version >= CURRENT_CONSENT_VERSION && jurisdictionMatches
    }

    fun setConsentGiven(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_CONSENT, true)
            .putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
            .putString(KEY_CONSENT_JURISDICTION, getJurisdiction(context).name)
            .apply()
    }

    fun getLastActivityTime(context: Context): Long =
        prefs(context).getLong(KEY_LAST_ACTIVITY, 0L)

    fun updateLastActivityTime(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }

    fun getLanguage(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE, "auto") ?: "auto"

    fun setLanguage(context: Context, langCode: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, langCode).apply()
    }

    /** Salva la giurisdizione scelta dall'utente (EU/US/ROW). */
    fun setJurisdiction(context: Context, j: Jurisdiction) {
        prefs(context).edit().putString(KEY_REGION, j.name).apply()
    }

    /** Giurisdizione scelta dall'utente; EU di default se non ancora impostata. */
    fun getJurisdiction(context: Context): Jurisdiction {
        val stored = prefs(context).getString(KEY_REGION, null)
        return stored?.let { runCatching { Jurisdiction.valueOf(it) }.getOrNull() } ?: Jurisdiction.EU
    }

    /** true se l'utente ha già scelto un'area geografica/giurisdizione. */
    fun isRegionSet(context: Context): Boolean =
        prefs(context).getString(KEY_REGION, null) != null
}
