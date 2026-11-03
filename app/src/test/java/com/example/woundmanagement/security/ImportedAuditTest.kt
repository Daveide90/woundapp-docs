package com.example.woundmanagement.security

import com.example.woundmanagement.data.db.entity.AuditLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ImportedAuditTest {

    private val riga = AuditLogEntity(
        id = 42, timestamp = 1_700_000_000_000L, action = "DELETE_WOUND",
        entityType = "Wound", entityId = 3, details = "Lesione sacrale"
    )

    @Test
    fun `la riga importata e marcata e perde l'id`() {
        val m = BackupManager.markImported(riga)
        assertEquals(0L, m.id)
        assertEquals("[da backup] Lesione sacrale", m.details)
        assertEquals(riga.action, m.action)
        assertEquals(riga.timestamp, m.timestamp)
    }

    @Test
    fun `una riga gia importata non prende un secondo prefisso`() {
        val due = BackupManager.markImported(BackupManager.markImported(riga))
        assertEquals("[da backup] Lesione sacrale", due.details)
    }

    @Test
    fun `il confronto contro i doppioni ignora il prefisso`() {
        assertEquals(BackupManager.auditKey(riga), BackupManager.auditKey(BackupManager.markImported(riga)))
    }
}
