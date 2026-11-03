package com.example.woundmanagement.export

import com.example.woundmanagement.data.db.entity.DressingEventEntity
import com.example.woundmanagement.domain.export.PatientReportPdf
import com.example.woundmanagement.ui.strings.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class DressingTextTest {

    private fun event(primary: String, secondary: String, other: String = "") = DressingEventEntity(
        id = 1, assessmentId = 1, photoUri = "", roiData = "",
        tissueBlackPercent = 0f, tissueYellowPercent = 0f,
        tissueRedPercent = 50f, tissuePinkPercent = 50f,
        exudateLevel = "ASSENTE", exudateType = "NON_SPECIFICATO",
        painNrs = null, dressingType = other,
        primaryDressing = primary, secondaryDressing = secondary,
        createdAt = 1700000000000L
    )

    @Test
    fun `secondaria Nessuna non viene stampata dopo la primaria`() {
        assertEquals("Idrocolloide", PatientReportPdf.dressingText(event("IDROCOLLOIDE", "NESSUNA"), AppLanguage.IT))
    }

    @Test
    fun `primaria Nessuna non viene stampata prima della secondaria`() {
        assertEquals("Garza", PatientReportPdf.dressingText(event("NESSUNA", "GARZA"), AppLanguage.IT))
    }

    @Test
    fun `entrambe Nessuna stampa Nessuna una volta sola`() {
        assertEquals("Nessuna", PatientReportPdf.dressingText(event("NESSUNA", "NESSUNA"), AppLanguage.IT))
    }

    @Test
    fun `primaria Nessuna e secondaria non specificata stampa Nessuna`() {
        assertEquals("Nessuna", PatientReportPdf.dressingText(event("NESSUNA", "NON_SPECIFICATA"), AppLanguage.IT))
    }

    @Test
    fun `coppia normale resta composta con il piu`() {
        assertEquals("Idrocolloide + Film trasparente", PatientReportPdf.dressingText(event("IDROCOLLOIDE", "FILM"), AppLanguage.IT))
    }

    @Test
    fun `non specificate entrambe resta vuoto`() {
        assertEquals("", PatientReportPdf.dressingText(event("NON_SPECIFICATA", "NON_SPECIFICATA"), AppLanguage.IT))
    }
}
