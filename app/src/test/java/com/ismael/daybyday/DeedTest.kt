package com.ismael.daybyday

import com.ismael.daybyday.data.Brushing
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.Deed
import com.ismael.daybyday.data.Note
import com.ismael.daybyday.data.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La note : le ressenti, plus ce qu'on a fait.
 *
 * Ce qu'il faut protéger avant tout : **rien ne se perd**. Un geste manqué ne
 * doit jamais retirer de points, et masquer une carte ne doit jamais en coûter.
 * Ce sont les deux promesses faites à l'utilisateur, et les seules qu'un calcul
 * de note peut trahir sans que personne ne s'en aperçoive.
 */
class DeedTest {

    private val day = DayEntry(epochDay = 20_000)

    @Test
    fun `un geste manque ne retire jamais de points`() {
        // Une journee verte sans aucun geste garde son ressenti entier.
        val vide = Note(moodAverage = DayColor.GREEN.score.toDouble(), deedsDone = 0, deedsPossible = 9)

        assertEquals(10.0, vide.mood!!, 0.001)
        assertEquals(0.0, vide.actions!!, 0.001)
        assertEquals(10.0, vide.total!!, 0.001)
        assertEquals(20, vide.outOf)
    }

    @Test
    fun `tout fait sur une journee verte donne le maximum`() {
        val plein = Note(DayColor.GREEN.score.toDouble(), deedsDone = 9, deedsPossible = 9)

        assertEquals(20.0, plein.total!!, 0.001)
    }

    @Test
    fun `masquer une carte ne coute rien`() {
        // Le meme comportement, avec et sans la carte Hygiene affichee : deux
        // gestes sur deux possibles vaut autant que quatre sur quatre.
        val avec = Note(DayColor.GREEN.score.toDouble(), deedsDone = 4, deedsPossible = 4)
        val sans = Note(DayColor.GREEN.score.toDouble(), deedsDone = 2, deedsPossible = 2)

        assertEquals(avec.total!!, sans.total!!, 0.001)
    }

    @Test
    fun `sans carte a geste la note reste sur dix`() {
        // Il n'y a alors rien a compter, et pretendre le contraire plafonnerait
        // la note a la moitie sans que rien ne l'explique.
        val note = Note(DayColor.GREEN.score.toDouble(), deedsDone = 0, deedsPossible = 0)

        assertEquals(10, note.outOf)
        assertEquals(10.0, note.total!!, 0.001)
        assertNull(note.actions)
    }

    @Test
    fun `une journee sans couleur n'a pas de note`() {
        val note = Note(moodAverage = null, deedsDone = 5, deedsPossible = 9)

        assertNull(note.total)
    }

    @Test
    fun `les cartes masquees retirent leurs gestes des deux cotes`() {
        val toutes = Deed.possibleWith(emptySet())
        val sansHygiene = Deed.possibleWith(setOf(DayCard.HYGIENE))

        assertEquals(2, toutes.size - sansHygiene.size)
        assertTrue(sansHygiene.none { it.card == DayCard.HYGIENE })
    }

    // --- Les gestes eux-memes ---------------------------------------------

    @Test
    fun `les prieres ne comptent qu'une fois les cinq faites`() {
        assertTrue(Deed.PRAYERS.doneOn(day.copy(prayerMask = Prayer.ALL_DONE)))
        assertTrue(!Deed.PRAYERS.doneOn(day.copy(prayerMask = Prayer.FAJR.bit)))
    }

    @Test
    fun `les trois brossages ne comptent qu'une fois les trois faits`() {
        assertTrue(Deed.TEETH.doneOn(day.copy(brushMask = Brushing.ALL_DONE)))
        assertTrue(!Deed.TEETH.doneOn(day.copy(brushMask = Brushing.MORNING.bit)))
    }

    @Test
    fun `la douche se coche, et ne pas la cocher vaut non`() {
        assertTrue(Deed.SHOWER.doneOn(day.copy(showered = true)))
        assertTrue(!Deed.SHOWER.doneOn(day.copy(showered = null)))
        assertTrue(!Deed.SHOWER.doneOn(day.copy(showered = false)))
    }

    @Test
    fun `chaque geste a un badge, et chaque badge le meme geste`() {
        // Les deux repondent a la meme question. Les faire diverger donnerait
        // une medaille sans point, ou un point sans medaille.
        Deed.entries.forEach { deed ->
            assertTrue("${deed.key} sans badge", deed.badge != null)
        }
    }

    @Test
    fun `le compte des gestes suit la journee`() {
        val possible = Deed.possibleWith(emptySet())
        val journee = day.copy(
            prayerMask = Prayer.ALL_DONE,
            showered = true,
            note = "quelque chose d'écrit",
        )

        assertEquals(3, Deed.doneCount(journee, possible))
    }
}
