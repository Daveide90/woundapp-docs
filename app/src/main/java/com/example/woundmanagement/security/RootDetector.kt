package com.example.woundmanagement.security

import android.content.Context
import java.io.File

/**
 * Basic root/compromise detection.
 * Not bulletproof — provides a warning to the user.
 */
object RootDetector {

    fun isDeviceCompromised(context: Context): Boolean =
        hasSuBinary() || hasRootManagementApps(context) || hasTestKeys()

    private fun hasSuBinary(): Boolean {
        val paths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su"
        )
        return paths.any { File(it).exists() }
    }

    @Suppress("DEPRECATION")
    private fun hasRootManagementApps(context: Context): Boolean {
        val packages = listOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "me.phh.superuser"
        )
        val pm = context.packageManager
        return packages.any {
            try { pm.getPackageInfo(it, 0); true } catch (_: Exception) { false }
        }
    }

    private fun hasTestKeys(): Boolean = try {
        val buildTags = android.os.Build.TAGS
        buildTags != null && buildTags.contains("test-keys")
    } catch (_: Exception) {
        false
    }
}
