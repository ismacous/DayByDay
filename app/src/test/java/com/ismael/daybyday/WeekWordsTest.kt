package com.ismael.daybyday

import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.WeekReview
import com.ismael.daybyday.data.WeekReviewBuilder
import com.ismael.daybyday.data.WeekWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Le texte du lundi matin.
 *
 * Il arrive sans qu'on l'ait demande, un jour ou tout peut aller mal. Ce qui
 * est teste ici n'est pas la beaute des phrases, c'est ce qu'elles n'ont pas le
 * droit de faire : ouvrir sur un chiffre, feliciter une semaine noire, ou
 * proposer un objectif a quelqu'un qui n'en peut plus.
 */
class WeekWordsTest {

    private val monday = LocalDate.of(2026, 8, 31)

    private fun day(
        offset: Long,
        color: DayColor,
        sport: SportLevel? = null,
        out: Boolean? = null,
        title: String = "",
    ) = DayEntry(
        epochDay = monday.plusDays(offset).toEpochDay(),
        colorKey = color.key,
        sportLevel = sport?.key,
        wentOut = out,
        title = title,
    )

    private fun review(days: List<DayEntry>, before: List<DayEntry> = emptyList()): WeekReview =
        WeekReviewBuilder.build(
            monday = monday,
            days = (days + before).associateBy { it.epochDay },
            mediaCounts = emptyMap(),
            tags = emptyList(),
            links = emptyList(),
            money = emptyList(),
        )

    private fun previous(offset: Long, color: DayColor) = DayEntry(
        epochDay = monday.minusWeeks(1).plusDays(offset).toEpochDay(),
        colorKey = color.key,
    )

    @Test
    fun `la premiere phrase n'est jamais un chiffre`() {
        // Le reproche d'Ismael, mot pour mot : « aucune ame, aucun
        // encouragement, juste des stats ». C'est cette ligne-la qu'on lit dans
        // le bandeau, sans derouler.
        val moods = listOf(
            (0..6).map { day(it.toLong(), DayColor.BLACK) },
            (0..6).map { day(it.toLong(), DayColor.ORANGE) },
            (0..6).map { day(it.toLong(), DayColor.GREEN) },
            listOf(day(0, DayColor.GREEN)),
        )
        moods.forEach { days ->
            val words = WeekWords.of(review(days), "Ismael", variant = 0)
            assertTrue("Commence par un chiffre : ${words.short}", words.short.first().isLetter())
            assertTrue("Trop courte : ${words.short}", words.short.length >= 40)
            assertTrue("Ne finit pas par une ponctuation : ${words.short}", words.short.last() in ".!?")
        }
    }

    @Test
    fun `une semaine noire n'est jamais felicitee`() {
        val noire = review((0..6).map { day(it.toLong(), DayColor.BLACK) })
        val words = WeekWords.of(noire, "Ismael", variant = 0)

        assertEquals(WeekWords.Mood.DARK, WeekWords.moodOf(noire))

        val forbidden = listOf("bravo", "chapeau", "félicitations", "savoure", "profite")
        forbidden.forEach {
            assertFalse("« $it » dans une semaine noire : ${words.long}", words.long.lowercase().contains(it))
        }
    }

    @Test
    fun `une semaine noire ne recoit pas d'objectif`() {
        // Proposer une marche a quelqu'un qui n'arrive pas a se lever, c'est la
        // meilleure facon de lui faire desinstaller l'application.
        val words = WeekWords.of(
            review((0..6).map { day(it.toLong(), DayColor.BLACK) }),
            "Ismael",
            variant = 1,
        )
        assertFalse(words.long.contains("Et si tu sortais"))
        assertFalse(words.long.contains("Une marche"))
    }

    @Test
    fun `une semaine qui remonte le dit`() {
        val words = WeekWords.of(
            review(
                days = (0..6).map { day(it.toLong(), DayColor.GREEN) },
                before = (0..6).map { previous(it.toLong(), DayColor.RED) },
            ),
            "Ismael",
            variant = 0,
        )
        assertEquals("Ça remonte Ismael", words.title)
    }

    @Test
    fun `les faits se lisent en francais`() {
        // « Bouge 2 jour(s). Sorti 2 jour(s). » etait un releve de compteur.
        val days = (0..6).map {
            day(
                offset = it.toLong(),
                color = DayColor.ORANGE,
                sport = if (it < 2) SportLevel.GOOD else null,
                out = it < 3,
                title = if (it == 0) "Une journée" else "",
            )
        }
        val words = WeekWords.of(review(days), "Ismael", variant = 0)

        assertFalse("Un « (s) » a survecu : ${words.long}", words.long.contains("(s)"))
        // Majuscule : la phrase des faits suit celle de la moyenne.
        assertTrue(words.long.contains("Tu as bougé deux jours"))
        assertTrue(words.long.contains("tu es sorti trois fois"))
        assertTrue(words.long.contains("tu as écrit un jour"))
    }

    @Test
    fun `ce qui n'a pas eu lieu n'est pas reproche`() {
        // Une ligne « Sorti 0 jour(s) » est un reproche deguise.
        val words = WeekWords.of(
            review((0..6).map { day(it.toLong(), DayColor.ORANGE) }),
            "Ismael",
            variant = 0,
        )
        assertFalse(words.long.contains("zéro"))
        assertFalse(words.long.contains(" 0 "))
    }

    @Test
    fun `deux semaines de suite ne se ressemblent pas`() {
        val week = review((0..6).map { day(it.toLong(), DayColor.ORANGE) })
        assertTrue(
            WeekWords.of(week, "Ismael", variant = 4).short !=
                WeekWords.of(week, "Ismael", variant = 5).short,
        )
    }

    @Test
    fun `un prenom vide ne laisse pas de trou`() {
        val words = WeekWords.of(review((0..6).map { day(it.toLong(), DayColor.GREEN) }), "  ", 0)
        assertFalse(words.title.contains("  "))
        assertFalse(words.title.endsWith(" "))
    }

    @Test
    fun `une semaine vide accueille au lieu de reclamer`() {
        val words = WeekWords.of(review(emptyList()), "Ismael", variant = 0)

        assertEquals(WeekWords.Mood.EMPTY, WeekWords.moodOf(review(emptyList())))
        assertTrue(words.long.isNotBlank())
        listOf("tu devrais", "il faut", "pense à").forEach {
            assertFalse("Reproche deguise : ${words.long}", words.long.lowercase().contains(it))
        }
    }

    @Test
    fun `chaque humeur a de quoi parler`() {
        WeekWords.Mood.entries.forEach { mood ->
            val sample = when (mood) {
                WeekWords.Mood.EMPTY -> review(emptyList())
                WeekWords.Mood.DARK -> review((0..6).map { day(it.toLong(), DayColor.BLACK) })
                WeekWords.Mood.DOWN -> review(
                    days = (0..6).map { day(it.toLong(), DayColor.ORANGE) },
                    before = (0..6).map { previous(it.toLong(), DayColor.GREEN) },
                )
                WeekWords.Mood.STEADY -> review((0..6).map { day(it.toLong(), DayColor.ORANGE) })
                WeekWords.Mood.UP -> review(
                    days = (0..6).map { day(it.toLong(), DayColor.GREEN) },
                    before = (0..6).map { previous(it.toLong(), DayColor.RED) },
                )
                WeekWords.Mood.BRIGHT -> review((0..6).map { day(it.toLong(), DayColor.GREEN) })
            }
            // La rotation ne doit jamais tomber a cote d'une liste vide.
            (0..12).forEach { variant ->
                val words = WeekWords.of(sample, "Ismael", variant)
                assertTrue("Vide pour $mood", words.short.isNotBlank() && words.long.isNotBlank())
                assertFalse("Gabarit non rempli pour $mood", words.long.contains("{"))
            }
        }
    }
}
