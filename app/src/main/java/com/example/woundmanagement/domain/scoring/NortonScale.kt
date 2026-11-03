package com.example.woundmanagement.domain.scoring

/**
 * Scala di Norton
 * Valutazione rischio lesioni da pressione
 * 5 items valutati 1-4, totale 5-20
 * Più basso = più rischio
 */
object NortonScale {

    data class NortonScore(
        val condizioneFisica: Int,
        val statoMentale: Int,
        val attivita: Int,
        val mobilita: Int,
        val incontinenza: Int
    ) {
        val total: Int get() = condizioneFisica + statoMentale + attivita + mobilita + incontinenza
    }

    object Descriptions {
        fun condizioneFisica(en: Boolean) = if (en) mapOf(
            1 to "Very Bad",
            2 to "Poor",
            3 to "Fair",
            4 to "Good"
        ) else mapOf(
            1 to "Pessima: allettato, cachessia",
            2 to "Scadente: magro, debole",
            3 to "Discreta: adeguato stato nutrizionale",
            4 to "Buona: ben nutrito"
        )

        fun statoMentale(en: Boolean) = if (en) mapOf(
            1 to "Stuporous/Comatose",
            2 to "Confused/Disoriented",
            3 to "Apathetic/Passive",
            4 to "Alert/Oriented"
        ) else mapOf(
            1 to "Stuporoso/comatoso",
            2 to "Confuso/disorientato",
            3 to "Apatico/passivo",
            4 to "Vigile/orientato"
        )

        fun attivita(en: Boolean) = if (en) mapOf(
            1 to "Bedfast",
            2 to "Chairbound",
            3 to "Walks with help",
            4 to "Ambulant"
        ) else mapOf(
            1 to "Allettato",
            2 to "In poltrona",
            3 to "Cammina con aiuto",
            4 to "Deambula autonomamente"
        )

        fun mobilita(en: Boolean) = if (en) mapOf(
            1 to "Immobile",
            2 to "Very limited",
            3 to "Slightly limited",
            4 to "Full"
        ) else mapOf(
            1 to "Immobile",
            2 to "Molto limitata",
            3 to "Leggermente limitata",
            4 to "Completa"
        )

        fun incontinenza(en: Boolean) = if (en) mapOf(
            1 to "Doubly incontinent",
            2 to "Usually urinary",
            3 to "Occasional",
            4 to "Not incontinent/catheterized"
        ) else mapOf(
            1 to "Doppia incontinenza",
            2 to "Abitualmente urinaria",
            3 to "Occasionale",
            4 to "Assente/catetere"
        )
    }
}
