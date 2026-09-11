package com.ismael.daybyday

import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les cartes verifiees d'une journee.
 *
 * « Verifiee » veut dire « je l'ai relue », et rien d'autre : ca ne remplit
 * rien, ca ne compte pas dans la note, et surtout ca survit — une journee ou il
 * ne s'est rien passe mais qu'on a relue reste une journee.
 */
class CheckedCardsTest {

    private val day = DayEntry(epochDay = 20_000)

    @Test
    fun `une journee neuve n a aucune carte verifiee`() {
        assertEquals("", day.checkedCards)
        assertEquals(emptySet<String>(), day.checkedCardKeys)
    }

    @Test
    fun `marquer une carte ne touche pas les autres`() {
        val after = day.withCheckedCard(DayCard.PRAYER.key, true)

        assertEquals(setOf(DayCard.PRAYER.key), after.checkedCardKeys)
        assertFalse(DayCard.MOOD.key in after.checkedCardKeys)
    }

    @Test
    fun `le meme geste enleve la marque`() {
        val marked = day.withCheckedCard(DayCard.MONEY.key, true)
        val undone = marked.withCheckedCard(DayCard.MONEY.key, false)

        assertEquals(emptySet<String>(), undone.checkedCardKeys)
        assertEquals("", undone.checkedCards)
    }

    @Test
    fun `marquer deux fois la meme carte ne change rien`() {
        val once = day.withCheckedCard(DayCard.SLEEP.key, true)
        val twice = once.withCheckedCard(DayCard.SLEEP.key, true)

        assertEquals(once, twice)
    }

    @Test
    fun `les cles sont toujours rangees dans le meme ordre`() {
        // Deux journees identiques doivent s'ecrire pareil, sinon une
        // sauvegarde change a chaque export sans qu'il se soit rien passe.
        val a = day.withCheckedCard("b", true).withCheckedCard("a", true)
        val b = day.withCheckedCard("a", true).withCheckedCard("b", true)

        assertEquals(a.checkedCards, b.checkedCards)
        assertEquals("a,b", a.checkedCards)
    }

    @Test
    fun `une journee relue n est pas une journee vide`() {
        // Sinon le depot l'effacerait en quittant l'ecran, et « j'ai verifie,
        // il n'y a rien a mettre » ne voudrait plus rien dire le lendemain.
        assertTrue(day.isEmpty)
        assertFalse(day.withCheckedCard(DayCard.PRAYER.key, true).isEmpty)
    }

    @Test
    fun `toutes les cartes de l application ont une cle utilisable`() {
        DayCard.entries.forEach { card ->
            assertTrue(card.key.isNotBlank())
            assertFalse("une cle ne peut pas contenir de virgule", card.key.contains(','))
        }
    }
}
