package com.example.woundmanagement.security

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BackupIntentTest {

    private val contenuto = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2Fx.woundbackup")

    @Test
    fun `VIEW con content viene accettato`() {
        assertEquals(contenuto, BackupIntent.backupUri(Intent(Intent.ACTION_VIEW, contenuto)))
    }

    @Test
    fun `SEND con content viene accettato`() {
        val i = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, contenuto)
        assertEquals(contenuto, BackupIntent.backupUri(i))
    }

    @Test
    fun `file e https vengono rifiutati`() {
        assertNull(BackupIntent.backupUri(Intent(Intent.ACTION_VIEW, Uri.parse("file:///sdcard/Download/x.woundbackup"))))
        assertNull(BackupIntent.backupUri(Intent(Intent.ACTION_VIEW, Uri.parse("https://esempio.it/x.woundbackup"))))
    }

    @Test
    fun `il manifest non apre i backup da link del browser ne da file`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.intent.category.BROWSABLE"))
        assertFalse(manifest.contains("android:scheme=\"file\""))
    }
}
