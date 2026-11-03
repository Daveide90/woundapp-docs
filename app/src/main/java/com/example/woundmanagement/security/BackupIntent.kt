package com.example.woundmanagement.security

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

/** Il backup arrivato da un'altra app: solo content:// (revisione esterna del 3/11, W5). */
object BackupIntent {
    fun backupUri(intent: Intent?): Uri? {
        val uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> @Suppress("DEPRECATION") (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
            else -> null
        }
        return uri?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }
    }
}
