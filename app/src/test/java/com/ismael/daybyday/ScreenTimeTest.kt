package com.ismael.daybyday

import android.app.usage.UsageEvents
import com.ismael.daybyday.health.ScreenTimeSource
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Le temps d'ecran se calcule a partir des evenements d'usage d'Android. Ces
 * tests fixent l'ordre reel des evenements envoyes par le systeme, celui qui
 * faisait compter une nuit entiere comme une heure.
 */
class ScreenTimeTest {

    private val minute = 60_000L
    private val hour = 60 * minute

    private fun moment(type: Int, at: Long, activity: String = "app.a/A") =
        ScreenTimeSource.Moment(type, at, activity)

    private fun resumed(at: Long, activity: String) =
        moment(UsageEvents.Event.ACTIVITY_RESUMED, at, activity)

    private fun paused(at: Long, activity: String) =
        moment(UsageEvents.Event.ACTIVITY_PAUSED, at, activity)

    private fun stopped(at: Long, activity: String) =
        moment(UsageEvents.Event.ACTIVITY_STOPPED, at, activity)

    @Test
    fun `une seule application au premier plan`() {
        val moments = listOf(
            resumed(0, "tiktok/Main"),
            paused(2 * hour, "tiktok/Main"),
        )

        assertEquals(2 * hour, ScreenTimeSource.foregroundMillis(moments, 0, 6 * hour))
    }

    @Test
    fun `le stop tardif de l application precedente ne coupe pas la suivante`() {
        // Ordre reel envoye par Android quand on passe de A a B : le STOPPED
        // de A arrive apres le RESUMED de B. Il ne doit pas fermer B.
        val moments = listOf(
            resumed(0, "claude/Main"),
            paused(10 * minute, "claude/Main"),
            resumed(10 * minute, "tiktok/Main"),
            stopped(10 * minute + 500, "claude/Main"),
            paused(4 * hour, "tiktok/Main"),
        )

        assertEquals(4 * hour, ScreenTimeSource.foregroundMillis(moments, 0, 6 * hour))
    }

    @Test
    fun `les applications qui se relaient ne comptent qu une fois`() {
        val moments = listOf(
            resumed(0, "app.a/A"),
            resumed(30 * minute, "app.b/B"),
            paused(30 * minute, "app.a/A"),
            paused(hour, "app.b/B"),
        )

        assertEquals(hour, ScreenTimeSource.foregroundMillis(moments, 0, 6 * hour))
    }

    @Test
    fun `ecran eteint ferme la periode meme sans pause`() {
        val moments = listOf(
            resumed(0, "app.a/A"),
            moment(UsageEvents.Event.SCREEN_NON_INTERACTIVE, 20 * minute),
            moment(UsageEvents.Event.KEYGUARD_SHOWN, 21 * minute),
            resumed(3 * hour, "app.a/A"),
            paused(3 * hour + 5 * minute, "app.a/A"),
        )

        assertEquals(25 * minute, ScreenTimeSource.foregroundMillis(moments, 0, 6 * hour))
    }

    @Test
    fun `une session encore ouverte compte jusqu a maintenant`() {
        val moments = listOf(resumed(5 * hour, "app.a/A"))

        assertEquals(hour, ScreenTimeSource.foregroundMillis(moments, 0, 6 * hour))
    }

    @Test
    fun `le total ne depasse jamais le temps ecoule`() {
        val moments = listOf(
            resumed(0, "app.a/A"),
            paused(10 * hour, "app.a/A"),
        )

        assertEquals(6 * hour, ScreenTimeSource.foregroundMillis(moments, 0, 6 * hour))
    }

    @Test
    fun `aucun evenement donne zero`() {
        assertEquals(0L, ScreenTimeSource.foregroundMillis(emptyList(), 0, 6 * hour))
    }
}
