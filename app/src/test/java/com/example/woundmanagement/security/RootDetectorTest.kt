package com.example.woundmanagement.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RootDetectorTest {

    @Test
    fun `non-rooted emulator returns false`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        // Robolectric emulates a clean, non-rooted device
        val result = RootDetector.isDeviceCompromised(context)
        assertFalse("Clean Robolectric device must not be detected as rooted", result)
    }

    @Test
    fun `isDeviceCompromised does not crash with null build tags`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        // Robolectric has null/empty Build.TAGS — verify no crash
        RootDetector.isDeviceCompromised(context)
    }
}
