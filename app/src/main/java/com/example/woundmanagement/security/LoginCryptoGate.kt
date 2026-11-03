package com.example.woundmanagement.security

/**
 * Il controllo crittografico del login (revisione esterna del 3/11, W2). Da Android 11 (API 30)
 * la chiave Keystore legata all'autenticazione è obbligatoria: senza cipher, o con doFinal
 * fallito, il login si ferma. Prima di Android 11 il CryptoObject con DEVICE_CREDENTIAL non
 * esiste e conta solo che nulla sia fallito.
 */
object LoginCryptoGate {
    fun mayProceed(sdkInt: Int, cipherPrepared: Boolean, doFinalSucceeded: Boolean): Boolean =
        if (sdkInt >= 30) cipherPrepared && doFinalSucceeded else doFinalSucceeded
}
