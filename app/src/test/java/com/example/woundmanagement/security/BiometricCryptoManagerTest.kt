package com.example.woundmanagement.security

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BiometricCryptoManagerTest {

    @Test
    @Config(sdk = [28])
    fun `getCipher returns null on API 28`() {
        val cipher = BiometricCryptoManager.getCipher()
        assertNull("CryptoObject must not be available on API < 30", cipher)
    }

    @Test
    @Config(sdk = [29])
    fun `getCipher returns null on API 29`() {
        val cipher = BiometricCryptoManager.getCipher()
        assertNull("CryptoObject must not be available on API < 30", cipher)
    }

    @Test
    @Config(sdk = [26])
    fun `getCipher returns null on API 26 (minSdk)`() {
        val cipher = BiometricCryptoManager.getCipher()
        assertNull("CryptoObject must not be available on API < 30", cipher)
    }
}
