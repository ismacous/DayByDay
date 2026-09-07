package com.ismael.daybyday

import com.ismael.daybyday.data.PageLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Les liens d'une page vers une autre.
 *
 * Ce qu'il faut protéger : qu'une adresse de courriel n'en devienne jamais un,
 * et qu'une date qui n'existe pas reste du texte plutôt qu'un lien mort.
 */
class PageLinkTest {

    @Test
    fun `un lien est reconnu et rend sa journee`() {
        val link = PageLink.linksIn("voir @07/09/2026 pour la suite").single()
        assertEquals(LocalDate.of(2026, 9, 7), link.date)
        assertEquals(5, link.range.first)
        assertEquals(15, link.range.last)
    }

    @Test
    fun `la forme ecrite se relit`() {
        val date = LocalDate.of(2026, 1, 3)
        val link = PageLink.linksIn(PageLink.format(date)).single()
        assertEquals(date, link.date)
    }

    @Test
    fun `une adresse de courriel n'est pas un lien`() {
        assertEquals(emptyList<Any>(), PageLink.linksIn("ismael@07/09/2026"))
    }

    @Test
    fun `une date qui n'existe pas reste du texte`() {
        assertEquals(emptyList<Any>(), PageLink.linksIn("@31/02/2026"))
        assertEquals(emptyList<Any>(), PageLink.linksIn("@00/09/2026"))
    }

    @Test
    fun `une date collee a des chiffres n'est pas un lien`() {
        assertEquals(emptyList<Any>(), PageLink.linksIn("@07/09/20261"))
    }

    @Test
    fun `plusieurs liens dans une phrase`() {
        val dates = PageLink.linksIn("de @01/01/2026 à @31/12/2026").map { it.date }
        assertEquals(listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)), dates)
    }

    @Test
    fun `le lien sous une position`() {
        val text = "voir @07/09/2026 ici"
        assertEquals(LocalDate.of(2026, 9, 7), PageLink.at(text, 5)?.date)
        assertEquals(LocalDate.of(2026, 9, 7), PageLink.at(text, 15)?.date)
        assertNull(PageLink.at(text, 4))
        assertNull(PageLink.at(text, 16))
    }
}
