package com.example.woundmanagement.ui

import com.example.woundmanagement.ui.screens.CF_REGEX
import com.example.woundmanagement.ui.screens.DATE_REGEX
import com.example.woundmanagement.ui.screens.invalidEditBlocksSave
import org.junit.Assert.*
import org.junit.Test

class PatientEditValidationTest {

    // CF finto in stile backup demo: 19 caratteri, cifre nelle prime 6 posizioni.
    private val demoCf = "DEMO001MRA40A41H501"

    @Test
    fun `CF preesistente non valido ma invariato non blocca il salvataggio`() {
        assertFalse(invalidEditBlocksSave(demoCf, demoCf, CF_REGEX))
    }

    @Test
    fun `CF modificato in valore non valido blocca il salvataggio`() {
        assertTrue(invalidEditBlocksSave("ABC", demoCf, CF_REGEX))
    }

    @Test
    fun `CF modificato in valore valido non blocca`() {
        assertFalse(invalidEditBlocksSave("RSSMRA40A41H501X", demoCf, CF_REGEX))
    }

    @Test
    fun `CF svuotato non blocca`() {
        assertFalse(invalidEditBlocksSave("", demoCf, CF_REGEX))
    }

    @Test
    fun `data preesistente non valida ma invariata non blocca`() {
        assertFalse(invalidEditBlocksSave("1940-05-12", "1940-05-12", DATE_REGEX))
    }

    @Test
    fun `data modificata in formato non valido blocca`() {
        assertTrue(invalidEditBlocksSave("12/13/1940", "12/05/1940", DATE_REGEX))
    }

    @Test
    fun `data modificata in formato valido non blocca`() {
        assertFalse(invalidEditBlocksSave("12/05/1940", "1940-05-12", DATE_REGEX))
    }
}
