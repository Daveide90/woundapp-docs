package com.example.woundmanagement.security

import com.example.woundmanagement.data.db.entity.DressingEventEntity
import com.example.woundmanagement.data.db.entity.WoundAssessmentEntity
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BackupJsonRoundtripTest {

    private val wound = WoundAssessmentEntity(
        id = 3, patientId = 7, location = "SACRO", name = "Lesione sacrale",
        type = "ULCERA_PRESSIONE", origin = "OSPEDALE",
        lengthCm = 4.5f, widthCm = 2.0f, depthCm = 0.5f, areaCm2 = 9.0f,
        notes = "Presa in carico il 01/07, ustione da acqua bollente",
        createdAt = 1700000000000L
    )

    private val dressing = DressingEventEntity(
        id = 11, assessmentId = 3, photoUri = "foto.enc", roiData = "0.1,0.2;0.3,0.4",
        tissueBlackPercent = 10f, tissueYellowPercent = 20f,
        tissueRedPercent = 30f, tissuePinkPercent = 40f,
        exudateLevel = "MODERATO", exudateType = "SIEROSO",
        painNrs = 4, dressingType = "",
        primaryDressing = "SCHIUMA", secondaryDressing = "FILM",
        notes = "medicazione ok", createdAt = 1700000100000L
    )

    @Test
    fun `wound roundtrip preserva tutti i campi incluse le note`() {
        val out = BackupManager.woundsFromJson(BackupManager.woundsToJson(listOf(wound)))
        assertEquals(listOf(wound), out)
    }

    @Test
    fun `dressing roundtrip preserva campi nuovi e painNrs valorizzato`() {
        val out = BackupManager.dressingsFromJson(BackupManager.dressingsToJson(listOf(dressing)))
        assertEquals(listOf(dressing), out)
    }

    @Test
    fun `dressing roundtrip con painNrs null resta null`() {
        val noPain = dressing.copy(painNrs = null)
        val json = BackupManager.dressingsToJson(listOf(noPain))
        // dolore non rilevato: la chiave viene omessa dal JSON
        assertFalse(json.getJSONObject(0).has("painNrs"))
        val out = BackupManager.dressingsFromJson(json)
        assertNull(out.single().painNrs)
    }

    @Test
    fun `import legacy senza campi nuovi usa i default`() {
        // JSON come lo produceva un backup pre-v11: niente notes/exudateType/primary/secondary,
        // painNrs sempre presente, dressingType composto
        val legacy = JSONArray(
            """[{"id":5,"assessmentId":3,"photoUri":"p.enc","roiData":"",
                 "tissueBlackPercent":0.0,"tissueYellowPercent":0.0,
                 "tissueRedPercent":50.0,"tissuePinkPercent":50.0,
                 "exudateLevel":"SCARSO","painNrs":2,
                 "dressingType":"Schiuma + Film trasparente","notes":"","createdAt":1690000000000}]"""
        )
        val out = BackupManager.dressingsFromJson(legacy).single()
        assertEquals(2, out.painNrs)
        assertEquals("", out.exudateType)
        assertEquals("", out.primaryDressing)
        assertEquals("", out.secondaryDressing)
        assertEquals("Schiuma + Film trasparente", out.dressingType)
    }

    @Test
    fun `import wound legacy senza notes usa stringa vuota`() {
        val json = BackupManager.woundsToJson(listOf(wound))
        json.getJSONObject(0).remove("notes")
        assertEquals("", BackupManager.woundsFromJson(json).single().notes)
    }
}
