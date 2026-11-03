package com.example.woundmanagement.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Trasformazione visiva per date GG/MM/AAAA.
 *
 * Lo STATO del campo contiene solo cifre (max 8, garantito da onValueChange a monte);
 * gli slash vengono inseriti esclusivamente a livello visivo. Così:
 *  - il cursore non salta a fine campo a ogni ricomposizione (niente riscrittura del value);
 *  - l'input oltre le 8 cifre viene troncato esplicitamente invece che ignorato in silenzio.
 *
 * Layout visivo per le cifre d0..d7:  d0 d1 / d2 d3 / d4 d5 d6 d7
 * (lo slash compare appena digitata la 2ª e la 4ª cifra).
 *
 * Mapping offset (monotono e sempre nel range, per input di lunghezza 0..8):
 *   originale    0 1 2 3 4 5 6 7 8
 *   trasformato  0 1 3 4 6 7 8 9 10   (+1 dopo 2 cifre, +2 dopo 4)
 */
class DateSlashTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(8)
        val transformed = buildString {
            digits.forEachIndexed { index, c ->
                append(c)
                if (index == 1 || index == 3) append('/')
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, digits.length)
                return when {
                    o <= 1 -> o
                    o <= 3 -> o + 1
                    else -> o + 2
                }.coerceIn(0, transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val t = offset.coerceIn(0, transformed.length)
                return when {
                    t <= 2 -> t          // t=2 è la posizione prima dello slash → 2 cifre
                    t <= 5 -> t - 1      // dentro il blocco MM (uno slash alle spalle)
                    else -> t - 2        // dentro il blocco AAAA (due slash alle spalle)
                }.coerceIn(0, digits.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

/**
 * Ricompone la data con gli slash ("GG/MM/AAAA") a partire dalle sole cifre.
 * Per input parziali inserisce gli slash disponibili (es. "0101" → "01/01").
 * Da usare AL CONFINE (validazione con DATE_REGEX, salvataggio su entity):
 * il resto dell'app continua a vedere il formato con slash.
 */
fun dateDigitsToSlashed(digits: String): String = when {
    digits.length <= 2 -> digits
    digits.length <= 4 -> "${digits.substring(0, 2)}/${digits.substring(2)}"
    else -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
}
