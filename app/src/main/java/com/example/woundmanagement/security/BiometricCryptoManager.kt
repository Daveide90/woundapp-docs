package com.example.woundmanagement.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages a Keystore key bound to biometric authentication.
 * On API 30+, provides a CryptoObject for BiometricPrompt to prevent
 * bypass via Frida/Xposed hooks on rooted devices.
 */
object BiometricCryptoManager {

    private const val KEY_NAME = "wound_biometric_auth_key"
    private const val TRANSFORMATION =
        "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    /**
     * Returns a Cipher ready for CryptoObject, or null if unavailable.
     * Only returns non-null on API 30+ (where DEVICE_CREDENTIAL + CryptoObject is supported).
     */
    fun getCipher(): Cipher? {
        if (Build.VERSION.SDK_INT < 30) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher
        } catch (_: KeyPermanentlyInvalidatedException) {
            // Biometric enrollment changed — recreate key
            deleteKey()
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
                cipher
            } catch (_: Exception) {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(30)
    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.getKey(KEY_NAME, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .setUserAuthenticationParameters(
                    0,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
                .build()
        )
        return keyGen.generateKey()
    }

    private fun deleteKey() {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            ks.deleteEntry(KEY_NAME)
        } catch (_: Exception) { /* ignore */ }
    }
}
