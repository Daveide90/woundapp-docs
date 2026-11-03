package com.example.woundmanagement.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bozza persistente del flusso medicazione: se il timeout di inattività (o il sistema)
 * chiude l'app a metà compilazione, al rientro si può riprendere da dove si era.
 * Cifrata su EncryptedSharedPreferences (contiene dati clinici), una bozza per lesione.
 */
object DressingDraftManager {

    private const val PREFS_NAME = "dressing_drafts"

    data class Draft(
        val step: Int,
        val photoUri: String?,
        val necrotic: Float,
        val slough: Float,
        val granulation: Float,
        val epithelial: Float,
        val exudate: String,
        val exudateType: String,
        val painDetected: Boolean,
        val painNrs: Int,
        val primary: String,
        val primaryOther: String,
        val secondary: String,
        val secondaryOther: String,
        val notes: String,
        val eventDate: Long?,
        // Foto aggiuntive (angolazioni 2..4) come URI stringa; le bozze vecchie
        // senza questo campo vengono lette come lista vuota.
        val extraPhotoUris: List<String> = emptyList(),
        // Misure della lesione a questa medicazione (testo grezzo dai campi) + unità profondità.
        val measLength: String = "",
        val measWidth: String = "",
        val measDepth: String = "",
        val measDepthUnit: String = "mm"
    )

    // Come in AppPreferences: l'istanza cifrata si crea una volta sola (Keystore + Tink),
    // qui l'autosalvataggio della bozza scrive di continuo mentre si compila il flusso.
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
        throw SecurityException("Keystore non disponibile: impossibile cifrare le bozze", e)
    }

    private fun key(assessmentId: Long) = "draft_$assessmentId"

    fun save(context: Context, assessmentId: Long, draft: Draft) {
        val json = JSONObject().apply {
            put("step", draft.step)
            put("photoUri", draft.photoUri ?: JSONObject.NULL)
            put("necrotic", draft.necrotic.toDouble())
            put("slough", draft.slough.toDouble())
            put("granulation", draft.granulation.toDouble())
            put("epithelial", draft.epithelial.toDouble())
            put("exudate", draft.exudate)
            put("exudateType", draft.exudateType)
            put("painDetected", draft.painDetected)
            put("painNrs", draft.painNrs)
            put("primary", draft.primary)
            put("primaryOther", draft.primaryOther)
            put("secondary", draft.secondary)
            put("secondaryOther", draft.secondaryOther)
            put("notes", draft.notes)
            draft.eventDate?.let { put("eventDate", it) }
            put("extraPhotoUris", JSONArray(draft.extraPhotoUris))
            put("measLength", draft.measLength)
            put("measWidth", draft.measWidth)
            put("measDepth", draft.measDepth)
            put("measDepthUnit", draft.measDepthUnit)
        }
        prefs(context).edit().putString(key(assessmentId), json.toString()).apply()
    }

    fun load(context: Context, assessmentId: Long): Draft? {
        val raw = prefs(context).getString(key(assessmentId), null) ?: return null
        return try {
            val o = JSONObject(raw)
            Draft(
                step = o.optInt("step", 0),
                photoUri = if (o.isNull("photoUri")) null else o.optString("photoUri", null),
                necrotic = o.optDouble("necrotic", 0.0).toFloat(),
                slough = o.optDouble("slough", 0.0).toFloat(),
                granulation = o.optDouble("granulation", 0.0).toFloat(),
                epithelial = o.optDouble("epithelial", 0.0).toFloat(),
                exudate = o.optString("exudate", ""),
                exudateType = o.optString("exudateType", ""),
                painDetected = o.optBoolean("painDetected", false),
                painNrs = o.optInt("painNrs", 0),
                primary = o.optString("primary", ""),
                primaryOther = o.optString("primaryOther", ""),
                secondary = o.optString("secondary", ""),
                secondaryOther = o.optString("secondaryOther", ""),
                notes = o.optString("notes", ""),
                eventDate = if (o.has("eventDate")) o.optLong("eventDate") else null,
                extraPhotoUris = o.optJSONArray("extraPhotoUris")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        arr.optString(i).takeIf { it.isNotBlank() }
                    }
                } ?: emptyList(),
                measLength = o.optString("measLength", ""),
                measWidth = o.optString("measWidth", ""),
                measDepth = o.optString("measDepth", ""),
                measDepthUnit = o.optString("measDepthUnit", "mm")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context, assessmentId: Long) {
        prefs(context).edit().remove(key(assessmentId)).apply()
    }
}
