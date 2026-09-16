package com.ismael.daybyday

import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.TagCatalog
import com.ismael.daybyday.data.TagCategory
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DoseTime
import com.ismael.daybyday.data.MediaItem
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
        assertEquals(DayCard.ACTIVITY, result[result.indexOf(DayCard.SLEEP) + 1])
    }

    @Test
    fun `une cle inconnue est ignoree sans faire disparaitre les cartes`() {
        // Cas d'un retour en arriere : la disposition vient d'une version plus
        // recente et cite une carte que cette version ne connait pas.
        val result = DayCard.order(listOf("carte_d_une_version_future", DayCard.MONEY.key))

        // Toutes les cartes connues sont la, une seule fois, et rien d'autre.
        assertEquals(DayCard.entries.toSet(), result.toSet())
        assertEquals(DayCard.entries.size, result.size)
    }

    @Test
    fun `une carte deplacee garde sa place quand une autre arrive`() {
        // L'argent remonte en deuxieme position, puis une mise a jour apporte
        // le sommeil : l'argent ne doit pas etre renvoye au fond.
        val saved = listOf(DayCard.MOOD, DayCard.MONEY) +
            DayCard.entries.filterNot {
                it == DayCard.MOOD || it == DayCard.MONEY || it == DayCard.SLEEP
            }

        val result = DayCard.order(saved.map { it.key })

        assertEquals(DayCard.MOOD, result[0])
        assertEquals(DayCard.MONEY, result[1])
        assertTrue(DayCard.SLEEP in result)
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
    fun `chaque famille d etiquettes a une carte, et une seule`() {
        // Une famille sans carte serait invisible et impossible a decocher ;
        // deux cartes pour la meme famille afficheraient les memes reperes.
        val familles = DayCard.entries.mapNotNull { it.tagCategory }
        assertEquals(familles.size, familles.toSet().size)

        val utilisees = TagCatalog.tags.map { it.category }.toSet()
        assertTrue(utilisees.all { it in familles.toSet() })
    }

    @Test
    fun `les ressentis ont quitte la carte du corps`() {
        // Pleurer n'est pas un symptome : les ranger a cote du poids revenait a
        // dire le contraire. Chacune des deux cartes doit maintenant avoir de
        // quoi se remplir toute seule.
        val ressentis = TagCatalog.tags.filter { it.category == TagCategory.EMOTION }
        val corps = TagCatalog.tags.filter { it.category == TagCategory.HEALTH }

        assertEquals(TagCategory.EMOTION, DayCard.EMOTION.tagCategory)
        assertEquals(TagCategory.HEALTH, DayCard.HEALTH.tagCategory)
        assertTrue("Trop peu de ressentis : ${ressentis.size}", ressentis.size >= 15)
        assertTrue("Le corps n'a rien a dire : ${corps.size}", corps.size >= 4)

        // « Pleuré » et « Angoisse » gardent leur slug : c'est lui qui relie les
        // journees deja marquees, et le renommer les perdrait. Elles ont
        // maintenant la meme forme que les autres — un mot, un signe — plutot
        // que deux lignes mises a l'ecart en bas de la carte.
        listOf("cried", "anxiety").forEach { slug ->
            val tag = TagCatalog.tags.first { it.slug == slug }
            assertEquals(TagCategory.EMOTION, tag.category)
            assertTrue("« ${tag.name} » n'a pas de signe", tag.emoji.isNotBlank())
        }

        // Et leurs anciens noms sont repris : sans ca, une base qui les porte
        // encore sans identifiant stable les perdrait a la synchronisation.
        val pleure = TagCatalog.tags.first { it.slug == "cried" }
        assertTrue("J'ai pleuré" in pleure.aliases)
        assertTrue("Crise d'angoisse" in TagCatalog.tags.first { it.slug == "anxiety" }.aliases)
    }

    @Test
    fun `chaque etiquette a un identifiant unique`() {
        // Deux entrees du meme slug se recouvriraient a la synchronisation :
        // la seconde ecraserait la premiere, et une des deux disparaitrait de
        // l'ecran sans que rien ne le signale.
        val slugs = TagCatalog.tags.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size)
        assertTrue(slugs.all { it.isNotBlank() })
    }

    @Test
    fun `une journee ou seul le tour de taille est note n est pas vide`() {
        assertTrue(!DayEntry(epochDay = 0, waistCm = 84.0).isEmpty)
    }

    @Test
    fun `un media sans carte reste celui de la journee entiere`() {
        val libre = MediaItem(epochDay = 0, relativePath = "a.jpg", kindKey = 0)
        val repas = MediaItem(
            epochDay = 0,
            relativePath = "b.jpg",
            kindKey = 0,
            cardKey = DayCard.FOOD.key,
        )

        // L'album complet montre les deux, la carte Alimentation seulement le sien.
        val tous = listOf(libre, repas)
        assertEquals(listOf(repas), tous.filter { it.cardKey == DayCard.FOOD.key })
        assertEquals(listOf(libre), tous.filter { it.cardKey == null })
    }

    @Test
    fun `la carte des medias ne se rattache pas a elle-meme`() {
        // "Photos & videos" est l'album de la journee : elle ne propose pas
        // d'y rattacher un media, elle les montre tous.
        assertTrue(!DayCard.MEDIA.canHoldMedia)
        assertTrue(!DayCard.MOOD.canHoldMedia)
        assertTrue(DayCard.FOOD.canHoldMedia)
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
