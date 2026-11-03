package com.example.woundmanagement.ui.strings

import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val code: String, val label: String) {
    IT("it", "Italiano"),
    EN("en", "English"),
    FR("fr", "Français"),
    ES("es", "Español")
}

val LocalLanguage = compositionLocalOf { AppLanguage.IT }

/**
 * Giurisdizione legale scelta dall'utente (indipendente dalla lingua).
 * EU  = profilo UE/SEE (MDR + GDPR);
 * US  = profilo Stati Uniti (FDA + HIPAA, testo dedicato);
 * ROW = profilo Resto del mondo (testo neutro).
 */
enum class Jurisdiction { EU, US, ROW }

val LocalJurisdiction = compositionLocalOf { Jurisdiction.EU }
