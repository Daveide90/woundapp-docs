package com.example.woundmanagement.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginCryptoGateTest {

    @Test
    fun `da Android 11 senza cipher il login si ferma`() {
        assertFalse(LoginCryptoGate.mayProceed(sdkInt = 30, cipherPrepared = false, doFinalSucceeded = true))
        assertFalse(LoginCryptoGate.mayProceed(sdkInt = 35, cipherPrepared = false, doFinalSucceeded = true))
    }

    @Test
    fun `da Android 11 con cipher sbloccato il login prosegue`() {
        assertTrue(LoginCryptoGate.mayProceed(sdkInt = 30, cipherPrepared = true, doFinalSucceeded = true))
    }

    @Test
    fun `cipher non sbloccato ferma sempre il login`() {
        assertFalse(LoginCryptoGate.mayProceed(sdkInt = 30, cipherPrepared = true, doFinalSucceeded = false))
        assertFalse(LoginCryptoGate.mayProceed(sdkInt = 29, cipherPrepared = false, doFinalSucceeded = false))
    }

    @Test
    fun `prima di Android 11 il CryptoObject non c'e e il login prosegue`() {
        assertTrue(LoginCryptoGate.mayProceed(sdkInt = 29, cipherPrepared = false, doFinalSucceeded = true))
        assertTrue(LoginCryptoGate.mayProceed(sdkInt = 26, cipherPrepared = false, doFinalSucceeded = true))
    }
}
