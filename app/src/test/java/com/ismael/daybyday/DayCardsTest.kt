package com.ismael.daybyday

import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DoseTime
import com.ismael.daybyday.data.Treatment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La disposition des cartes est enregistree par cle. Ces tests fixent ce qui
 * doit survivre a une mise a jour : un ordre choisi, et une carte nouvelle qui
 * apparait sans effacer le reste.
 */
class DayCardsTest {

    @Test
    fun `un ordre complet est rendu tel quel`() {
        val chosen = listOf(DayCard.MONEY, DayCard.MOOD) +
            DayCard.entries.filterNot { it == DayCard.MONEY || it == DayCard.MOOD }

        val result = DayCard.order(chosen.map { it.key })

        assertEquals(chosen, result)
    }

    @Test
    fun `une carte ajoutee par une mise a jour revient a sa place d origine`() {
        // L'utilisateur avait rangé sa journée avant que le sommeil n'existe.
        val saved = DayCard.entries.filterNot { it == DayCard.SLEEP }.map { it.key }

        val result = DayCard.order(saved)

        assertEquals(DayCard.entries.size, result.size)
        assertTrue(DayCard.SLEEP in result)
        // Elle se glisse entre le journal et l'activité, comme prévu au départ.
        assertEquals(DayCard.JOURNAL, result[result.indexOf(DayCard.SLEEP) - 1])
    }

    @Test
    fun `une cle inconnue est ignoree sans faire disparaitre les cartes`() {
        val result = DayCard.order(listOf("carte_d_une_version_future", DayCard.MONEY.key))

        assertEquals(DayCard.entries.size, result.size)
        assertEquals(DayCard.MONEY, result.first())
    }

    @Test
    fun `un ordre vide donne la disposition par defaut`() {
        assertEquals(DayCard.entries.toList(), DayCard.order(emptyList()))
    }

    @Test
    fun `la carte de l humeur ne peut pas etre masquee`() {
        assertTrue(DayCard.MOOD.essential)
        assertTrue(DayCard.entries.count { it.essential } == 1)
    }

    @Test
    fun `les cles des cartes sont uniques et stables`() {
        val keys = DayCard.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { it.isNotBlank() })
    }
}

/** Le sommeil se compte a cheval sur minuit : c'est le cas normal, pas l'exception. */
class SleepTest {

    private fun night(start: Int?, end: Int?) =
        DayEntry(epochDay = 0, sleepStartMinutes = start, sleepEndMinutes = end)

    @Test
    fun `une nuit qui passe minuit est comptee entierement`() {
        // Couché à 23h30, levé à 7h15 : 7 h 45, pas une durée négative.
        assertEquals(7 * 60 + 45, night(23 * 60 + 30, 7 * 60 + 15).sleepMinutes)
    }

    @Test
    fun `une nuit dans la meme journee se compte normalement`() {
        assertEquals(6 * 60, night(1 * 60, 7 * 60).sleepMinutes)
    }

    @Test
    fun `une nuit incomplete ne donne aucune duree`() {
        assertNull(night(23 * 60, null).sleepMinutes)
        assertNull(night(null, 8 * 60).sleepMinutes)
        assertNull(night(null, null).sleepMinutes)
    }

    @Test
    fun `un coucher et un lever identiques ne valent pas vingt-quatre heures`() {
        assertNull(night(8 * 60, 8 * 60).sleepMinutes)
    }

    @Test
    fun `une journee avec seulement une nuit notee n est pas vide`() {
        assertTrue(!night(23 * 60, 7 * 60).isEmpty)
    }

    @Test
    fun `une journee avec seulement de l eau ou un repas n est pas vide`() {
        assertTrue(!DayEntry(epochDay = 0, waterGlasses = 3).isEmpty)
        assertTrue(!DayEntry(epochDay = 0, mealsNote = "Pâtes").isEmpty)
    }
}

/** Les moments de prise tiennent dans un masque de bits. */
class TreatmentTest {

    @Test
    fun `un traitement ne repond que pour ses moments`() {
        val treatment = Treatment(
            name = "Test",
            timesMask = DoseTime.MORNING.bit or DoseTime.EVENING.bit,
        )

        assertTrue(treatment.isDueAt(DoseTime.MORNING))
        assertTrue(treatment.isDueAt(DoseTime.EVENING))
        assertTrue(!treatment.isDueAt(DoseTime.NOON))
        assertEquals(listOf(DoseTime.MORNING, DoseTime.EVENING), treatment.times)
    }

    @Test
    fun `les bits des quatre moments ne se recouvrent pas`() {
        val bits = DoseTime.entries.map { it.bit }
        assertEquals(bits.size, bits.toSet().size)
        assertEquals(0b1111, bits.reduce { a, b -> a or b })
    }
}
