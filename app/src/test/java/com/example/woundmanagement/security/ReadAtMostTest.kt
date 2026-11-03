package com.example.woundmanagement.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ReadAtMostTest {

    @Test
    fun `entro il tetto legge tutto`() {
        val letti = BackupManager.readAtMost(ByteArrayInputStream(ByteArray(10) { it.toByte() }), 10)
        assertEquals(10, letti.size)
        assertEquals(9.toByte(), letti[9])
    }

    @Test
    fun `oltre il tetto si ferma con un errore`() {
        assertThrows(IOException::class.java) {
            BackupManager.readAtMost(ByteArrayInputStream(ByteArray(11)), 10)
        }
    }
}
