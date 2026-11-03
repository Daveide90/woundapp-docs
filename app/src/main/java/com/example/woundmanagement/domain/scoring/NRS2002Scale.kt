package com.example.woundmanagement.domain.scoring

/**
 * NRS-2002 - Nutritional Risk Screening
 * Riproduce la SOLA componente "final screening" dello strumento pubblicato;
 * il pre-screening iniziale a 4 domande NON è incluso.
 * 3 componenti, totale 0-7:
 *  - punteggio dello stato nutrizionale (0-3)
 *  - punteggio della gravità di malattia (0-3)
 *  - aggiustamento per età (+1 se >= 70 anni)
 *
 * Descrizioni parafrasate dalle linee guida ESPEN
 * (Kondrup J, Allison SP, Elia M, Vellas B, Plauth M. ESPEN guidelines for
 * nutrition screening 2002. Clin Nutr. 2003;22(4):415-421) — NON copia verbatim.
 *
 * NOTA (MDR/non-DM): questo oggetto fornisce SOLO il punteggio grezzo e le descrizioni
 * dei singoli item per facilitarne la scelta. Non classifica il paziente, non applica
 * cutoff e non fornisce interpretazione del rischio.
 */
object NRS2002Scale {

    data class NRS2002Score(
        val nutritionalScore: Int,      // 0-3
        val diseaseSeverityScore: Int,  // 0-3
        val ageOver70: Boolean
    ) {
        val total: Int get() = nutritionalScore + diseaseSeverityScore + (if (ageOver70) 1 else 0)
    }

    object Descriptions {
        fun nutritionalScore(en: Boolean) = if (en) mapOf(
            0 to "Normal nutritional status",
            1 to "Weight loss > 5% in 3 months, or food intake at 50-75% of the usual requirement over the past week",
            2 to "Weight loss > 5% in 2 months, or BMI 18.5-20.5 with reduced general condition, or food intake at 25-60% of requirement",
            3 to "Weight loss > 5% in 1 month (or > 15% in 3 months), or BMI < 18.5 with reduced general condition, or food intake at 0-25% of requirement"
        ) else mapOf(
            0 to "Stato nutrizionale normale",
            1 to "Calo ponderale > 5% in 3 mesi, oppure intake alimentare al 50-75% del fabbisogno nell'ultima settimana",
            2 to "Calo ponderale > 5% in 2 mesi, oppure BMI 18,5-20,5 con condizioni generali ridotte, oppure intake al 25-60% del fabbisogno",
            3 to "Calo ponderale > 5% in 1 mese (o > 15% in 3 mesi), oppure BMI < 18,5 con condizioni generali ridotte, oppure intake allo 0-25% del fabbisogno"
        )

        fun diseaseSeverityScore(en: Boolean) = if (en) mapOf(
            0 to "Normal nutritional requirements",
            1 to "Mild (e.g. hip fracture, chronic patient with acute complication, COPD, diabetes, oncology)",
            2 to "Moderate (e.g. major abdominal surgery, stroke, severe pneumonia, haematological malignancy)",
            3 to "Severe (e.g. head injury, bone-marrow transplant, critically ill patient in intensive care)"
        ) else mapOf(
            0 to "Fabbisogni nutrizionali normali",
            1 to "Lieve (es. frattura d'anca, paziente cronico con complicanza acuta, BPCO, diabete, oncologia)",
            2 to "Moderato (es. chirurgia addominale maggiore, ictus, polmonite grave, neoplasia ematologica)",
            3 to "Grave (es. trauma cranico, trapianto di midollo, paziente critico in terapia intensiva)"
        )

        fun ageAdjustment(en: Boolean) = if (en)
            "Age >= 70 years (+1)"
        else
            "Età >= 70 anni (+1)"
    }

}
