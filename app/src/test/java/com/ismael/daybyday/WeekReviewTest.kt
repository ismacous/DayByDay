package com.ismael.daybyday

import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DayTagCrossRef
import com.ismael.daybyday.data.FoodLevel
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Tag
import com.ismael.daybyday.data.TagCategory
import com.ismael.daybyday.data.WeekReviewBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Le bilan du lundi matin.
 *
 * Ce qui se joue ici tient en deux choses : la semaine doit etre la **bonne**
 * (du lundi au dimanche, et celle qui vient de finir), et un bilan sans rien
 * dedans ne doit pas s'annoncer.
 */
class WeekReviewTest {

    private val monday = LocalDate.of(2026, 8, 31)

    private fun day(
        offset: Long,
        color: DayColor? = null,
        sport: SportLevel? = null,
        out: Boolean? = null,
        title: String = "",
    ) = DayEntry(
        epochDay = monday.plusDays(offset).toEpochDay(),
        colorKey = color?.key,
        sportLevel = sport?.key,
        wentOut = out,
        title = title,
    )

    private fun build(
        days: List<DayEntry>,
        media: Map<Long, Int> = emptyMap(),
        tags: List<Tag> = emptyList(),
        links: List<DayTagCrossRef> = emptyList(),
        money: List<MoneyEntry> = emptyList(),
    ) = WeekReviewBuilder.build(
        monday = monday,
        days = days.associateBy { it.epochDay },
        mediaCounts = media,
        tags = tags,
        links = links,
        money = money,
    )

    @Test
    fun `la semaine commence un lundi, quel que soit le jour donne`() {
        // Le dimanche est le jour a verifier : il termine la semaine, il ne
        // l'ouvre pas. Une convention « la semaine commence le dimanche » le
        // rattacherait au lundi qui suit, et tout le bilan glisserait d'un jour.
        val sunday = LocalDate.of(2026, 9, 6)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(monday, WeekReviewBuilder.mondayOf(sunday))

        assertEquals(monday, WeekReviewBuilder.mondayOf(monday))
        assertEquals(monday, WeekReviewBuilder.mondayOf(monday.plusDays(3)))
    }

    @Test
    fun `les sept couleurs sont dans l ordre des jours`() {
        val review = build(
            listOf(
                day(0, DayColor.GREEN),
                day(3, DayColor.RED),
                day(6, DayColor.ORANGE),
            )
        )

        assertEquals(7, review.colors.size)
        assertEquals(DayColor.GREEN, review.colors[0])
        assertNull(review.colors[1])
        assertEquals(DayColor.RED, review.colors[3])
        assertEquals(DayColor.ORANGE, review.colors[6])
    }

    @Test
    fun `une semaine vide ne s annonce pas`() {
        assertFalse(build(emptyList()).hasData)
    }

    @Test
    fun `une semaine sans couleur mais avec du texte s annonce quand meme`() {
        // On peut tenir son journal sans poser de couleur : ce n'est pas une
        // semaine vide.
        val review = build(listOf(day(2, title = "Une journée à raconter")))

        assertTrue(review.hasData)
        assertEquals(1, review.writtenDays)
    }

    @Test
    fun `la comparaison porte sur la semaine precedente`() {
        val previous = monday.minusWeeks(1)
        val review = build(
            listOf(
                day(0, DayColor.GREEN),
                day(1, DayColor.GREEN),
                DayEntry(epochDay = previous.toEpochDay(), colorKey = DayColor.RED.key),
                DayEntry(epochDay = previous.plusDays(1).toEpochDay(), colorKey = DayColor.RED.key),
            )
        )

        assertEquals(3.0, review.summary.average!!, 0.001)
        assertEquals(1.0, review.previous.average!!, 0.001)
        assertEquals(2.0, review.delta!!, 0.001)
    }

    @Test
    fun `les jours de la semaine d avant ne sont pas comptes dans celle-ci`() {
        val review = build(
            listOf(
                day(0, DayColor.GREEN, sport = SportLevel.GOOD),
                DayEntry(
                    epochDay = monday.minusDays(1).toEpochDay(),
                    colorKey = DayColor.GREEN.key,
                    sportLevel = SportLevel.GOOD.key,
                ),
            )
        )

        assertEquals(1, review.summary.filledDays)
        assertEquals(1, review.movedDays)
    }

    @Test
    fun `bouger compte des le niveau leger`() {
        val review = build(
            listOf(
                day(0, sport = SportLevel.NONE),
                day(1, sport = SportLevel.LIGHT),
                day(2, sport = SportLevel.GOOD),
            )
        )

        assertEquals(2, review.movedDays)
    }

    @Test
    fun `les etiquettes de la semaine sont classees par frequence`() {
        val sommeil = Tag(id = 1, name = "Bien dormi", slug = "bien-dormi")
        val amis = Tag(id = 2, name = "Vu des amis", slug = "vu-des-amis")
        val links = listOf(
            DayTagCrossRef(monday.toEpochDay(), 1),
            DayTagCrossRef(monday.plusDays(1).toEpochDay(), 1),
            DayTagCrossRef(monday.plusDays(2).toEpochDay(), 1),
            DayTagCrossRef(monday.plusDays(3).toEpochDay(), 2),
            // Celle-ci est hors semaine : elle ne doit pas compter.
            DayTagCrossRef(monday.minusDays(2).toEpochDay(), 2),
        )

        val review = build(
            days = (0..3).map { day(it.toLong(), DayColor.GREEN) },
            tags = listOf(sommeil, amis),
            links = links,
        )

        assertEquals(2, review.topTags.size)
        assertEquals(sommeil to 3, review.topTags[0])
        assertEquals(amis to 1, review.topTags[1])
    }

    @Test
    fun `l argent de la semaine ignore le reste du mois`() {
        val review = build(
            days = listOf(day(0, DayColor.GREEN)),
            money = listOf(
                MoneyEntry(epochDay = monday.toEpochDay(), amountCents = -1500),
                MoneyEntry(epochDay = monday.plusDays(2).toEpochDay(), amountCents = 20000),
                MoneyEntry(epochDay = monday.minusDays(3).toEpochDay(), amountCents = -99999),
            ),
        )

        assertEquals(20000L, review.money.incomeCents)
        assertEquals(1500L, review.money.spentCents)
    }

    @Test
    fun `bien manger ne compte que le meilleur niveau`() {
        val review = build(
            listOf(
                day(0).copy(foodLevel = FoodLevel.GOOD.key),
                day(1).copy(foodLevel = FoodLevel.OK.key),
                day(2).copy(foodLevel = FoodLevel.HARD.key),
            )
        )

        assertEquals(1, review.ateWellDays)
    }

    @Test
    fun `la journee la plus claire et la plus difficile sont les bonnes`() {
        val review = build(
            listOf(
                day(0, DayColor.RED, title = "Dur"),
                day(1, DayColor.GREEN, title = "Bien"),
                day(2, DayColor.ORANGE, title = "Bof"),
            )
        )

        assertEquals("Bien", review.brightest?.title)
        assertEquals("Dur", review.hardest?.title)
    }

    @Test
    fun `les photos comptees sont celles de la semaine`() {
        val review = build(
            days = listOf(day(0, DayColor.GREEN)),
            media = mapOf(
                monday.toEpochDay() to 2,
                monday.plusDays(4).toEpochDay() to 1,
                monday.minusDays(1).toEpochDay() to 5,
            ),
        )

        assertEquals(3, review.photos)
    }

    @Test
    fun `une categorie d etiquette n est pas requise pour le comptage`() {
        // Garde-fou : le catalogue evolue, le bilan ne doit pas en dependre.
        val tag = Tag(id = 7, name = "Test", slug = "test", category = TagCategory.OTHER.key)
        val review = build(
            days = listOf(day(0, DayColor.GREEN)),
            tags = listOf(tag),
            links = listOf(DayTagCrossRef(monday.toEpochDay(), 7)),
        )

        assertEquals(1, review.topTags.size)
    }
}
