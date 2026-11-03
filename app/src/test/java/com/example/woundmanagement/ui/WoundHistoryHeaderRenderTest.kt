package com.example.woundmanagement.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.ui.screens.WoundHistoryHeader
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.theme.WoundManagementTheme
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

/**
 * Intestazione della lesione nello Storico (pulsante «Confronta»).
 *
 * Disegna la versione della build 53 e quella corretta con nomi e tipi reali, su schermi
 * da 360 e 393 dp e col carattere al 100% e al 130%, e salva le immagini in
 * app/build/snapshot-confronta/ per il controllo a occhio.
 *
 * Controllo automatico: il pulsante può aggiungere al massimo la sua riga (48 dp), mai
 * le centinaia di dp della build 53, dove «Confronta» si spezzava una lettera per riga.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class WoundHistoryHeaderRenderTest {

    private data class Case(val name: String, val type: String?, val count: Int, val lang: AppLanguage = AppLanguage.IT)

    private val cases = listOf(
        // Nome suggerito corto
        Case("Sacro 1", "Ulcera da pressione", 4),
        // Nome suggerito reale + tipo lungo: il tipo occupa quasi tutta la riga
        Case("Rachide/Spinose vertebrali 1", "Dermatite associata a umidità (MASD/IAD)", 7),
        // Nome scritto a mano, lungo: va a capo
        Case("Lesione sacrale con tunnellizzazione a ore 6", "Ulcera da pressione", 12),
        // Spagnolo: tipo più lungo delle 4 lingue
        Case("Raquis/Apófisis espinosas 1", "Dermatitis asociada a la humedad (MASD/IAD)", 3, AppLanguage.ES)
    )

    @Test
    fun `card con Confronta resta proporzionata al testo`() {
        val outDir = File("build/snapshot-confronta").apply { mkdirs() }
        val problems = mutableListOf<String>()
        for (widthDp in listOf(360, 393)) {
            for (fontScale in listOf(1.0f, 1.3f)) {
                val tag = "w${widthDp}_font${(fontScale * 100).toInt()}"
                val heights = render(widthDp, fontScale, File(outDir, "$tag.png"))
                val density = heights.getValue("density") / 1000f
                // Il pulsante può aggiungere al massimo la sua riga: 48 dp di bersaglio tattile
                val tolerance = (48 * density).toInt() + 1
                cases.indices.forEach { i ->
                    val with = heights.getValue("new_$i")
                    val without = heights.getValue("newNo_$i")
                    if (with - without > tolerance) {
                        problems += "$tag caso ${i + 1}: con Confronta ${with}px, senza ${without}px"
                    }
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    /** Disegna i casi e restituisce le altezze in px; "density" è la densità × 1000. */
    private fun render(widthDp: Int, fontScale: Float, file: File): Map<String, Int> {
        RuntimeEnvironment.setQualifiers("w${widthDp}dp-h2400dp-xxhdpi")
        RuntimeEnvironment.setFontScale(fontScale)
        val heights = mutableMapOf<String, Int>()
        var contentBottom = 0
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create()
        val activity = controller.get()
        // Niente barra del titolo nelle immagini (va chiesto prima di impostare il contenuto)
        activity.window.requestFeature(Window.FEATURE_NO_TITLE)
        activity.setContent {
            WoundManagementTheme {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        // come il contentPadding della LazyColumn dello Storico
                        .padding(16.dp)
                        .onGloballyPositioned { contentBottom = it.boundsInWindow().bottom.toInt() },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "$widthDp dp · carattere ${(fontScale * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall
                    )
                    cases.forEachIndexed { i, c ->
                        CompositionLocalProvider(LocalLanguage provides c.lang) {
                            Text("Caso ${i + 1} (${c.lang.name}) · prima, build 53", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Box(Modifier.onGloballyPositioned { heights["old_$i"] = it.size.height }) {
                                OldWoundHeader(c.name, c.type, c.count, c.lang)
                            }
                            Text("Caso ${i + 1} (${c.lang.name}) · dopo, con e senza Confronta", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Box(Modifier.onGloballyPositioned { heights["new_$i"] = it.size.height }) {
                                WoundHistoryHeader(c.name, c.type, c.count, showCompare = true, onCompare = {})
                            }
                            Box(Modifier.onGloballyPositioned { heights["newNo_$i"] = it.size.height }) {
                                WoundHistoryHeader(c.name, c.type, c.count, showCompare = false, onCompare = {})
                            }
                        }
                    }
                }
            }
        }
        controller.start().resume().visible()
        repeat(5) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)) }

        val root = activity.window.decorView
        val full = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(full))
        val cropHeight = contentBottom.coerceIn(1, full.height)
        val cropped = Bitmap.createBitmap(full, 0, 0, full.width, cropHeight)
        file.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
        heights["density"] = (activity.resources.displayMetrics.density * 1000).toInt()
        controller.pause().stop().destroy()
        return heights
    }
}

/** Copia fedele del layout della build 53 (Column senza peso misurata prima del pulsante). */
@Composable
private fun OldWoundHeader(woundName: String, type: String?, count: Int, lang: AppLanguage) {
    val countText = if (lang == AppLanguage.ES) "$count curas" else "$count medicazioni"
    val compare = if (lang == AppLanguage.ES) "Comparar" else "Confronta"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(woundName, style = MaterialTheme.typography.titleSmall)
                if (!type.isNullOrBlank()) {
                    Text(type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(countText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = {}) {
                Icon(Icons.Default.Compare, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(compare)
            }
        }
    }
}
