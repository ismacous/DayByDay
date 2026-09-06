package com.ismael.daybyday

import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DaySearch
import com.ismael.daybyday.data.FoodLevel
import com.ismael.daybyday.data.SearchFilter
import com.ismael.daybyday.data.SportLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * La recherche croisee.
 *
 * Deux choses a proteger : les accents, que personne ne tape dans une barre de
 * recherche, et le sens des combinaisons — plusieurs couleurs se lisent « ou »,
 * plusieurs etiquettes se lisent « et ».
 */
class DaySearchTest {

    private val start = LocalDate.of(2026, 3, 1)

    private fun day(
        offset: Long,
        color: DayColor? = null,
        title: String = "",
        note: String = "",
        sport: SportLevel? = null,
        out: Boolean? = null,
        food: FoodLevel? = null,
    ) = DayEntry(
        epochDay = start.plusDays(offset).toEpochDay(),
        colorKey = color?.key,
        title = title,
        note = note,
        sportLevel = sport?.key,
        wentOut = out,
        foodLevel = food?.key,
    )

    private fun search(
        days: List<DayEntry>,
        filter: SearchFilter,
        tags: Map<Long, Set<Long>> = emptyMap(),
        media: Map<Long, Int> = emptyMap(),
    ) = DaySearch.apply(days, filter, tags, media)

    @Test
    fun `sans critere, on ne renvoie rien`() {
        // Une recherche vide qui renverrait tout donnerait l'impression d'un
        // resultat alors qu'on n'a rien demande.
        val results = search(listOf(day(0, DayColor.GREEN)), SearchFilter())
        assertTrue(results.isEmpty())
    }

    @Test
    fun `les accents ne comptent pas`() {
        val days = listOf(day(0, title = "Un été magnifique"), day(1, title = "Rien"))

        assertEquals(1, search(days, SearchFilter(text = "ete")).size)
        assertEquals(1, search(days, SearchFilter(text = "ÉTÉ")).size)
        assertEquals(1, search(days, SearchFilter(text = "Été")).size)
    }

    @Test
    fun `le texte est cherche dans le titre et dans la note`() {
        val days = listOf(
            day(0, title = "Cinéma", note = "rien de special"),
            day(1, title = "Rien", note = "On est allés au cinema"),
            day(2, title = "Rien", note = "rien"),
        )

        assertEquals(2, search(days, SearchFilter(text = "cinema")).size)
    }

    @Test
    fun `plusieurs couleurs se lisent ou`() {
        val days = listOf(
            day(0, DayColor.GREEN),
            day(1, DayColor.ORANGE),
            day(2, DayColor.RED),
            day(3, DayColor.BLACK),
        )

        val results = search(days, SearchFilter(colors = setOf(DayColor.GREEN, DayColor.BLACK)))

        assertEquals(2, results.size)
    }

    @Test
    fun `plusieurs etiquettes se lisent et`() {
        val days = listOf(day(0, DayColor.GREEN), day(1, DayColor.GREEN), day(2, DayColor.GREEN))
        val tags = mapOf(
            days[0].epochDay to setOf(1L, 2L),
            days[1].epochDay to setOf(1L),
            days[2].epochDay to setOf(2L),
        )

        assertEquals(3, search(days, SearchFilter(tagIds = emptySet(), colors = setOf(DayColor.GREEN)), tags).size)
        assertEquals(2, search(days, SearchFilter(tagIds = setOf(1L)), tags).size)
        assertEquals(1, search(days, SearchFilter(tagIds = setOf(1L, 2L)), tags).size)
    }

    @Test
    fun `les criteres de nature differente se cumulent`() {
        // « Mes bonnes journees ou j'ai bougé » : la question de depart.
        val days = listOf(
            day(0, DayColor.GREEN, sport = SportLevel.GOOD),
            day(1, DayColor.GREEN, sport = SportLevel.NONE),
            day(2, DayColor.RED, sport = SportLevel.GOOD),
        )

        val results = search(days, SearchFilter(colors = setOf(DayColor.GREEN), moved = true))

        assertEquals(1, results.size)
        assertEquals(days[0].epochDay, results.first().epochDay)
    }

    @Test
    fun `bouger commence au niveau leger`() {
        val days = listOf(
            day(0, sport = SportLevel.NONE),
            day(1, sport = SportLevel.LIGHT),
            day(2, sport = SportLevel.GOOD),
            day(3),
        )

        assertEquals(2, search(days, SearchFilter(moved = true)).size)
    }

    @Test
    fun `bien mange ne retient que le meilleur niveau`() {
        val days = listOf(
            day(0, food = FoodLevel.GOOD),
            day(1, food = FoodLevel.OK),
            day(2, food = FoodLevel.HARD),
        )

        assertEquals(1, search(days, SearchFilter(ateWell = true)).size)
    }

    @Test
    fun `avec photo ne garde que les journees qui en ont`() {
        val days = listOf(day(0, DayColor.GREEN), day(1, DayColor.GREEN))
        val media = mapOf(days[0].epochDay to 3)

        val results = search(days, SearchFilter(withPhoto = true), media = media)

        assertEquals(1, results.size)
        assertEquals(days[0].epochDay, results.first().epochDay)
    }

    @Test
    fun `les resultats vont du plus recent au plus ancien`() {
        val days = listOf(day(0, DayColor.GREEN), day(5, DayColor.GREEN), day(2, DayColor.GREEN))

        val results = search(days, SearchFilter(colors = setOf(DayColor.GREEN)))

        assertEquals(days[1].epochDay, results[0].epochDay)
        assertEquals(days[2].epochDay, results[1].epochDay)
        assertEquals(days[0].epochDay, results[2].epochDay)
    }

    @Test
    fun `mettre a plat garde la longueur du texte`() {
        // C'est ce qui permet a l'extrait affiche autour du mot trouve de
        // tomber au bon endroit : la position dans le texte mis a plat doit
        // etre la meme que dans l'original.
        val original = "Été à Cérès, très éprouvant"

        assertEquals(original.length, DaySearch.fold(original).length)
        assertEquals("ete a ceres, tres eprouvant", DaySearch.fold(original))
    }

    @Test
    fun `le compte de criteres sert a le dire a l ecran`() {
        assertEquals(0, SearchFilter().activeCount)
        assertEquals(
            4,
            SearchFilter(
                text = "plage",
                colors = setOf(DayColor.GREEN, DayColor.ORANGE),
                moved = true,
            ).activeCount,
        )
    }
}
