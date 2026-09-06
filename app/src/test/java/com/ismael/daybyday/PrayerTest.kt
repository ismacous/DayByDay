package com.ismael.daybyday

import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les cinq prieres, rangees en masque de bits.
 *
 * Deux choses a protéger : les bits ne bougent pas (ce sont eux qui sont dans
 * la base), et `null` ne veut pas dire zero — une journee d'avant cette
 * version est une journee dont on ne sait rien, pas une journee sans priere.
 */
class PrayerTest {

    private val day = DayEntry(epochDay = 20_000)

    @Test
    fun `les bits sont distincts et ne changent pas`() {
        assertEquals(1, Prayer.FAJR.bit)
        assertEquals(2, Prayer.DHUHR.bit)
        assertEquals(4, Prayer.ASR.bit)
        assertEquals(8, Prayer.MAGHRIB.bit)
        assertEquals(16, Prayer.ISHA.bit)
        assertEquals(31, Prayer.ALL_DONE)
    }

    @Test
    fun `cocher une priere n en coche pas d autre`() {
        val mask = day.withPrayer(Prayer.ASR, true)
        val after = day.copy(prayerMask = mask)

        assertTrue(after.isPrayerDone(Prayer.ASR))
        Prayer.entries.filter { it != Prayer.ASR }.forEach {
            assertFalse(after.isPrayerDone(it))
        }
        assertEquals(1, after.prayersDone)
    }

    @Test
    fun `decocher ne touche que la priere visee`() {
        var entry = day
        Prayer.entries.forEach { entry = entry.copy(prayerMask = entry.withPrayer(it, true)) }
        assertEquals(5, entry.prayersDone)

        entry = entry.copy(prayerMask = entry.withPrayer(Prayer.DHUHR, false))

        assertEquals(4, entry.prayersDone)
        assertFalse(entry.isPrayerDone(Prayer.DHUHR))
        assertTrue(entry.isPrayerDone(Prayer.FAJR))
        assertTrue(entry.isPrayerDone(Prayer.ISHA))
    }

    @Test
    fun `cocher deux fois la meme priere ne change rien`() {
        val once = day.withPrayer(Prayer.MAGHRIB, true)
        val twice = day.copy(prayerMask = once).withPrayer(Prayer.MAGHRIB, true)

        assertEquals(once, twice)
    }

    @Test
    fun `rien de coche n est pas la meme chose que rien de connu`() {
        assertNull(day.prayerMask)
        assertEquals(0, day.prayersDone)
        assertFalse(day.isPrayerDone(Prayer.FAJR))
    }

    @Test
    fun `une journee avec une priere cochee n est pas vide`() {
        // Sinon le depot la supprimerait en la croyant sans contenu, et la
        // priere cochee disparaitrait en sortant de l'ecran.
        assertTrue(day.isEmpty)
        assertFalse(day.copy(prayerMask = Prayer.FAJR.bit).isEmpty)
    }

    @Test
    fun `tout decocher rend la journee vide a nouveau`() {
        val entry = day.copy(prayerMask = 0)
        assertTrue(entry.isEmpty)
    }
}
