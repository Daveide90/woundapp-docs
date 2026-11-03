package com.example.woundmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.woundmanagement.ui.theme.BrandIndigo
import com.example.woundmanagement.ui.theme.BrandIndigoDark
import com.example.woundmanagement.ui.theme.BrandIndigoLight

/**
 * Componenti grafici condivisi del "restyling" (header a gradiente, avatar con iniziali,
 * chip di stato). Punto unico di verità così le tre schermate restano coerenti.
 */

/** Gradiente diagonale del brand: indaco → indaco chiaro. */
val brandGradient: Brush
    get() = Brush.linearGradient(
        colors = listOf(BrandIndigo, BrandIndigoLight),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

/** Variante più scura per gli header di dettaglio. */
val brandGradientDeep: Brush
    get() = Brush.linearGradient(
        colors = listOf(BrandIndigoDark, BrandIndigoLight),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

private val HeaderShape: Shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)

/**
 * Header a gradiente con angoli inferiori arrotondati e due "bolle" decorative traslucide.
 * Il contenuto (titolo, sottotitolo, eventuale campo di ricerca) va nello slot [content].
 */
@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    deep: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeaderShape)
            .background(if (deep) brandGradientDeep else brandGradient)
    ) {
        // Bolle decorative: disegnate su Canvas per non intercettare i tocchi.
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            drawIntoCanvas {
                val white = Color.White
                drawCircle(
                    color = white.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.92f, size.height * 0.05f)
                )
                drawCircle(
                    color = white.copy(alpha = 0.07f),
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.04f, size.height * 0.55f)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
            content = content
        )
    }
}

/** Titolo dell'header (bianco, bold). */
@Composable
fun BrandHeaderTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

/** Sottotitolo dell'header (bianco tenue). */
@Composable
fun BrandHeaderSubtitle(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.85f),
        style = MaterialTheme.typography.bodySmall
    )
}

/** Avatar quadrato-arrotondato con iniziali su gradiente indaco. */
@Composable
fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(brandGradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp
        )
    }
}

/**
 * Tab segmentate "a pillole": la voce selezionata ha il gradiente del brand, le altre
 * sono tenui. Sostituisce la TabRow standard mantenendo lo stesso indice di stato.
 */
@Composable
fun BrandSegmentedTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEEF0FB))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (sel) Modifier.background(brandGradient) else Modifier)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (sel) Color.White else Color(0xFF7C839C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// NB: qui viveva anche uno StatusChip (attivo/monitoraggio/neutro), rimosso perché non
// usato da nessuna schermata: "monitoraggio" è per giunta un termine che questo fork
// evita di proposito (vincolo non-DM).
