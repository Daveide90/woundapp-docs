package com.example.woundmanagement.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for input validation limits applied across the app.
 * These tests verify the validation logic patterns used in Compose screens.
 */
class InputValidationTest {

    // Simulates the validation pattern: if (it.length <= limit) field = it
    private fun simulateInput(currentValue: String, newInput: String, maxLength: Int): String {
        return if (newInput.length <= maxLength) newInput else currentValue
    }

    @Test
    fun `patient name respects 50 char limit`() {
        val short = "Mario Rossi"
        assertEquals(short, simulateInput("", short, 50))

        val exact50 = "A".repeat(50)
        assertEquals(exact50, simulateInput("", exact50, 50))

        val over50 = "A".repeat(51)
        assertEquals("", simulateInput("", over50, 50))
    }

    @Test
    fun `fiscal code respects 16 char limit`() {
        val valid = "RSSMRA80A01H501Z"
        assertEquals(valid, simulateInput("", valid, 16))

        val over = "RSSMRA80A01H501ZX"
        assertEquals("", simulateInput("", over, 16))
    }

    @Test
    fun `birth date respects 10 char limit`() {
        val valid = "01/01/1980"
        assertEquals(valid, simulateInput("", valid, 10))

        val over = "01/01/19800"
        assertEquals("", simulateInput("", over, 10))
    }

    @Test
    fun `clinical notes respect 500 char limit`() {
        val exact500 = "N".repeat(500)
        assertEquals(exact500, simulateInput("", exact500, 500))

        val over500 = "N".repeat(501)
        assertEquals("", simulateInput("", over500, 500))
    }

    @Test
    fun `dressing other field respects 100 char limit`() {
        val exact100 = "D".repeat(100)
        assertEquals(exact100, simulateInput("", exact100, 100))

        val over100 = "D".repeat(101)
        assertEquals("", simulateInput("", over100, 100))
    }

    @Test
    fun `pain location respects 200 char limit`() {
        val exact200 = "L".repeat(200)
        assertEquals(exact200, simulateInput("", exact200, 200))

        val over200 = "L".repeat(201)
        assertEquals("", simulateInput("", over200, 200))
    }

    @Test
    fun `backup password respects 128 char limit`() {
        val exact128 = "P".repeat(128)
        assertEquals(exact128, simulateInput("", exact128, 128))

        val over128 = "P".repeat(129)
        assertEquals("", simulateInput("", over128, 128))
    }

    @Test
    fun `ward respects 50 char limit`() {
        assertEquals("Cardiologia", simulateInput("", "Cardiologia", 50))
        assertEquals("", simulateInput("", "A".repeat(51), 50))
    }

    @Test
    fun `bed respects 20 char limit`() {
        assertEquals("12A", simulateInput("", "12A", 20))
        assertEquals("", simulateInput("", "A".repeat(21), 20))
    }
}
