package com.example.woundmanagement.ui

import com.example.woundmanagement.domain.model.PrimaryDressing
import com.example.woundmanagement.domain.model.SecondaryDressing
import com.example.woundmanagement.ui.screens.primaryDressingMenu
import com.example.woundmanagement.ui.screens.secondaryDressingMenu
import com.example.woundmanagement.ui.strings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tendine medicazione: "Non specificata" e "Nessuna" in cima, poi A→Z, in tutte le lingue. */
class DressingMenuOrderTest {

    @Test
    fun `primaria - Non specificata e Nessuna in cima, poi ordine alfabetico`() {
        for (lang in AppLanguage.entries) {
            val menu = primaryDressingMenu(lang)
            assertEquals(lang.name, PrimaryDressing.NON_SPECIFICATA, menu[0])
            assertEquals(lang.name, PrimaryDressing.NESSUNA, menu[1])
            val rest = menu.drop(2).map { it.label(lang).lowercase() }
            assertEquals(lang.name, rest.sorted(), rest)
            assertEquals(lang.name, PrimaryDressing.entries.size, menu.toSet().size)
        }
    }

    @Test
    fun `secondaria - Non specificata e Nessuna in cima, poi ordine alfabetico`() {
        for (lang in AppLanguage.entries) {
            val menu = secondaryDressingMenu(lang)
            assertEquals(lang.name, SecondaryDressing.NON_SPECIFICATA, menu[0])
            assertEquals(lang.name, SecondaryDressing.NESSUNA, menu[1])
            val rest = menu.drop(2).map { it.label(lang).lowercase() }
            assertEquals(lang.name, rest.sorted(), rest)
            assertEquals(lang.name, SecondaryDressing.entries.size, menu.toSet().size)
        }
    }
}
