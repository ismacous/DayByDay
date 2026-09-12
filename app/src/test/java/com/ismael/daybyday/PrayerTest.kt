package com.ismael.daybyday

import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.Jumua
import com.ismael.daybyday.data.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

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

    @Test
    fun `la jumua ne touche ni les bits ni le compte des cinq`() {
        // C'est **la** garantie : la jumu'a est une chose a part, pas une
        // sixieme priere. Le masque, la medaille et les journees deja ecrites
        // ne bougent pas d'un pouce.
        val entry = day.copy(prayerMask = day.withPrayer(Prayer.DHUHR, true), jumua = true)

        assertEquals(5, Prayer.entries.size)
        assertEquals(31, Prayer.ALL_DONE)
        assertEquals(2, Prayer.DHUHR.bit)
        assertEquals(1, entry.prayersDone)
        assertTrue(entry.isPrayerDone(Prayer.DHUHR))
    }

    @Test
    fun `on peut avoir fait le dhuhr sans la jumua`() {
        // Le dhuhr chez soi un vendredi, sans etre alle a la mosquee : les deux
        // se notent separement, c'est tout l'interet du bouton en plus.
        val entry = day.copy(prayerMask = day.withPrayer(Prayer.DHUHR, true), jumua = false)

        assertTrue(entry.isPrayerDone(Prayer.DHUHR))
        assertEquals(false, entry.jumua)
    }

    @Test
    fun `la question ne se pose que le vendredi`() {
        assertTrue(Jumua.concerns(LocalDate.of(2026, 9, 11)))
        assertEquals(DayOfWeek.FRIDAY, Jumua.DAY)

        (12..17).forEach { jour ->
            val date = LocalDate.of(2026, 9, jour)
            assertFalse(Jumua.concerns(date))
        }
    }

    @Test
    fun `un vendredi d avant cette version ne dit pas non`() {
        // `null` et `false` ne veulent pas dire la meme chose : une journee
        // d'avant la colonne est une journee dont on ne sait rien.
        assertNull(day.jumua)
    }

    @Test
    fun `une journee ou il n y a que la jumua n est pas vide`() {
        assertTrue(day.isEmpty)
        assertFalse(day.copy(jumua = true).isEmpty)
        // Repondre « non » n'est pas remplir la journee.
        assertTrue(day.copy(jumua = false).isEmpty)
    }
}
