package com.example.woundmanagement.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.woundmanagement.ui.strings.AppLanguage
import com.example.woundmanagement.ui.strings.LocalLanguage
import com.example.woundmanagement.ui.strings.loc4
import com.example.woundmanagement.ui.theme.*

data class TissueData(
    val label: String,
    val percent: Float,
    val color: Color
)

@Composable
fun TissuePercentageChart(
    necrotic: Float,
    slough: Float,
    granulation: Float,
    epithelial: Float,
    modifier: Modifier = Modifier
) {
    val data = listOf(
        TissueData(loc4(it = "Necrosi", en = "Necrosis", fr = "Nécrose", es = "Necrosis"), necrotic, TissueBlack),
        TissueData(loc4(it = "Slough", en = "Slough", fr = "Fibrine", es = "Esfacelo"), slough, TissueYellow),
        TissueData(loc4(it = "Granulazione", en = "Granulation", fr = "Granulation", es = "Granulación"), granulation, TissueRed),
        TissueData(loc4(it = "Epitelizzazione", en = "Epithelialization", fr = "Épithélialisation", es = "Epitelización"), epithelial, TissuePink)
    )

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text(loc4(it = "Composizione Tessuto", en = "Tissue Composition", fr = "Composition tissulaire", es = "Composición del tejido"), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Stacked horizontal bar
        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
            var xOffset = 0f
            val barHeight = size.height
            data.forEach { tissue ->
                val width = size.width * tissue.percent / 100f
                if (width > 0f) {
                    drawRoundRect(
                        color = tissue.color,
                        topLeft = Offset(xOffset, 0f),
                        size = Size(width, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    xOffset += width
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        data.forEach { tissue ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = tissue.color)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "${tissue.label}: %.1f%%".format(tissue.percent),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
