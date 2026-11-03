package com.example.woundmanagement.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PhotoManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `loadBitmap rejects file without enc extension`() {
        // Create a non-encrypted file in the encrypted directory
        val dir = PhotoManager.getEncryptedDir(context)
        val plainFile = File(dir, "photo.jpg")
        plainFile.writeBytes(ByteArray(100))

        val result = PhotoManager.loadBitmap(context, "photo.jpg")
        assertNull("Non-.enc files must be rejected", result)

        plainFile.delete()
    }

    @Test
    fun `loadBitmap rejects path traversal with dot-dot`() {
        val result = PhotoManager.loadBitmap(context, "../etc/passwd")
        assertNull("Path traversal with .. must be rejected", result)
    }

    @Test
    fun `loadBitmap rejects path traversal with slash`() {
        val result = PhotoManager.loadBitmap(context, "subdir/file.enc")
        assertNull("Path with / must be rejected", result)
    }

    @Test
    fun `loadBitmap rejects absolute path outside encrypted dir`() {
        val result = PhotoManager.loadBitmap(context, "/data/local/tmp/evil.jpg.enc")
        assertNull("Absolute path outside encrypted dir must be rejected", result)
    }

    @Test
    fun `loadBitmap returns null for non-existent file`() {
        val result = PhotoManager.loadBitmap(context, "nonexistent.jpg.enc")
        assertNull("Non-existent file must return null", result)
    }

    @Test
    fun `getEncryptedDir creates directory`() {
        val dir = PhotoManager.getEncryptedDir(context)
        assertTrue("Encrypted dir must exist", dir.exists())
        assertTrue("Encrypted dir must be a directory", dir.isDirectory)
        assertEquals("wound_photos_enc", dir.name)
    }
}
