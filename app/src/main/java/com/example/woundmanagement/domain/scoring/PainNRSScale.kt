package com.example.woundmanagement.domain.scoring

/**
 * Scala NRS (Numeric Rating Scale) per il dolore
 * Valutazione intensità dolore 0-10
 */
object PainNRSScale {

    enum class PainType(val label: String, val labelEn: String) {
        CONTINUO("Continuo", "Continuous"),
        INTERMITTENTE("Intermittente", "Intermittent"),
        PROCEDURALE("Procedurale", "Procedural")
    }

    enum class PainCharacter(val label: String, val labelEn: String) {
        PUNGENTE("Pungente", "Pricking"),
        BRUCIANTE("Bruciante", "Burning"),
        PULSANTE("Pulsante", "Throbbing"),
        SORDO("Sordo", "Dull"),
        LANCINANTE("Lancinante", "Shooting"),
        COMPRESSIVO("Compressivo", "Pressing")
    }

    data class PainNRSScore(
        val painScore: Int,
        val painLocation: String,
        val painType: PainType,
        val painCharacter: PainCharacter
    )

    // NB: la NRS pubblicata definisce solo le ancore 0 ("nessun dolore") e
    // 10 ("peggior dolore possibile"): nessuna descrizione intermedia 1-9.
}
