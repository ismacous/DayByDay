package com.ismael.daybyday

import com.ismael.daybyday.work.ReminderAlarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Le rappel du soir n'est jamais arrive, trois versions de suite. Ces tests
 * fixent la regle qui a manque a chaque fois : **une alarme vise un instant,
 * pas un delai.**
 */
class ReminderAlarmTest {

    private val paris = ZoneId.of("Europe/Paris")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, paris)

    private fun instantOf(millis: Long): ZonedDateTime =
        ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), paris)

    @Test
    fun `avant l heure, le rappel est pour ce soir`() {
        // Le coeur du bug d'origine : ouvrir l'application a 20 h repoussait le
        // rappel au lendemain, parce que c'etait un compte a rebours. Ici, on
        // vise 21 h — celui de ce soir.
        val next = ReminderAlarm.nextDaily(21, 0, now = at(2026, 9, 7, 20, 0))
        assertEquals(at(2026, 9, 7, 21, 0), instantOf(next))
    }

    @Test
    fun `apres l heure, le rappel est pour demain`() {
        val next = ReminderAlarm.nextDaily(21, 0, now = at(2026, 9, 7, 21, 30))
        assertEquals(at(2026, 9, 8, 21, 0), instantOf(next))
    }

    @Test
    fun `pile a l heure, on vise le lendemain`() {
        // Sinon l'alarme se reprogrammerait sur elle-meme et sonnerait en
        // boucle : c'est ce receveur qui rearme, juste apres avoir sonne.
        val next = ReminderAlarm.nextDaily(21, 0, now = at(2026, 9, 7, 21, 0))
        assertEquals(at(2026, 9, 8, 21, 0), instantOf(next))
    }

    @Test
    fun `rearmer plusieurs fois ne repousse rien`() {
        // La regle qui manquait : reprogrammer doit etre sans effet. Trois
        // ouvertures de l'application dans l'apres-midi visent toutes le meme
        // instant.
        val premiere = ReminderAlarm.nextDaily(21, 0, now = at(2026, 9, 7, 14, 0))
        val deuxieme = ReminderAlarm.nextDaily(21, 0, now = at(2026, 9, 7, 17, 30))
        val troisieme = ReminderAlarm.nextDaily(21, 0, now = at(2026, 9, 7, 20, 59))
        assertEquals(premiere, deuxieme)
        assertEquals(deuxieme, troisieme)
    }

    @Test
    fun `le bilan vise le lundi suivant`() {
        // Mardi 8 septembre 2026 : le prochain lundi est le 14.
        val next = ReminderAlarm.nextWeekly(9, 0, now = at(2026, 9, 8, 10, 0))
        assertEquals(at(2026, 9, 14, 9, 0), instantOf(next))
    }

    @Test
    fun `un lundi avant l heure, le bilan est pour le matin meme`() {
        val next = ReminderAlarm.nextWeekly(9, 0, now = at(2026, 9, 7, 7, 0))
        assertEquals(at(2026, 9, 7, 9, 0), instantOf(next))
    }

    @Test
    fun `l heure visee est toujours dans le futur`() {
        // Quelle que soit l'heure qu'il est, une alarme posee dans le passe ne
        // sonnerait jamais — ou sonnerait immediatement, ce qui est pire.
        (0..23).forEach { hour ->
            val now = at(2026, 9, 7, hour, 30)
            val next = ReminderAlarm.nextDaily(21, 0, now = now)
            assertTrue("à $hour h", instantOf(next).isAfter(now))
        }
    }
}
