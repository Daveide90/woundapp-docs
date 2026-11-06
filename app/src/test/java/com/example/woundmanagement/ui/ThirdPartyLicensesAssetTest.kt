package com.example.woundmanagement.ui

import com.example.woundmanagement.ui.screens.THIRD_PARTY_LICENSES_ASSET
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Gli avvisi di terzi devono viaggiare dentro l'APK: la BSD di SQLCipher e di
 * ThreeTen chiede il testo nella distribuzione binaria, la Apache 2.0 chiede la
 * licenza e i NOTICE. La prova legge l'asset dal sorgente, allo stesso percorso
 * che apre l'app (le prove girano nella cartella del modulo app).
 */
class ThirdPartyLicensesAssetTest {

    private val text: String by lazy {
        File("src/main/assets/$THIRD_PARTY_LICENSES_ASSET").readText()
    }

    @Test
    fun containsFullApacheLicense() {
        assertTrue(text.contains("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION"))
        assertTrue(text.contains("END OF TERMS AND CONDITIONS"))
    }

    @Test
    fun containsBsdNoticesRequiredInBinaries() {
        assertTrue(text.contains("ZETETIC LLC"))
        assertTrue(text.contains("Stephen Colebourne & Michael Nascimento Santos"))
        assertTrue(text.contains("Redistributions in binary form must reproduce"))
    }

    @Test
    fun containsApacheNoticeFiles() {
        assertTrue(text.contains("in this case for the Kotlin Compiler distribution"))
        assertTrue(text.contains("in this case for the kotlinx.coroutines library"))
    }

    @Test
    fun namesEveryLibraryFamilyInTheApk() {
        listOf("AndroidX", "Kotlin Standard Library", "kotlinx.coroutines", "Gson", "Tink",
            "Guava ListenableFuture", "JSpecify", "JetBrains Java Annotations", "SQLCipher", "LibTomCrypt")
            .forEach { assertTrue("manca $it", text.contains(it)) }
    }
}
