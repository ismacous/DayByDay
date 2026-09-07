package com.ismael.daybyday

import com.ismael.daybyday.data.Hashtag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les mots-cles du journal.
 *
 * Trois choses a proteger : ce qui compte comme mot-cle (un `#` colle a un mot
 * n'en est pas un), le fait que deux ecritures d'un meme mot n'en font qu'un,
 * et la stabilite des couleurs — `#mood` doit garder la sienne pour toujours,
 * y compris apres une restauration sur un telephone neuf.
 */
class HashtagTest {

    @Test
    fun `un mot-cle simple est reconnu`() {
        assertEquals(listOf("mood"), Hashtag.namesIn("journée calme #mood"))
    }

    @Test
    fun `plusieurs mots-cles dans l'ordre`() {
        assertEquals(
            listOf("sport", "maman"),
            Hashtag.namesIn("#sport ce matin, puis #maman l'après-midi"),
        )
    }

    @Test
    fun `un diese colle a un mot n'est pas un mot-cle`() {
        // Sinon une adresse, un accord de musique ou un identifiant deviendrait
        // un mot-cle sans qu'on l'ait voulu.
        assertEquals(emptyList<String>(), Hashtag.namesIn("do#5 et C#"))
    }

    @Test
    fun `un diese suivi de chiffres seuls est un numero`() {
        assertEquals(emptyList<String>(), Hashtag.namesIn("appartement #3"))
    }

    @Test
    fun `un diese seul n'est pas un mot-cle`() {
        assertEquals(emptyList<String>(), Hashtag.namesIn("# et # encore"))
    }

    @Test
    fun `le mot-cle s'arrete a la ponctuation`() {
        val text = "fini #mood, enfin."
        val range = Hashtag.rangesIn(text).single()
        assertEquals("#mood", text.substring(range.first, range.last + 1))
    }

    @Test
    fun `les accents et les majuscules ne font pas deux mots-cles`() {
        assertEquals(listOf("Été"), Hashtag.namesIn("#Été puis #ete encore #été"))
        assertEquals(Hashtag.key("Été"), Hashtag.key("ete"))
    }

    @Test
    fun `un mot-cle peut porter un tiret ou un souligne`() {
        assertEquals(listOf("bonne-journée", "mal_dormi"), Hashtag.namesIn("#bonne-journée #mal_dormi"))
    }

    @Test
    fun `la teinte est stable et tient dans la palette`() {
        repeat(3) {
            assertEquals(Hashtag.tint("mood"), Hashtag.tint("#MOOD"))
        }
        listOf("mood", "sport", "maman", "a", "zzzzzzzz", "été").forEach { name ->
            val tint = Hashtag.tint(name)
            assertTrue("teinte hors palette pour $name", tint in 0 until Hashtag.TINTS)
        }
    }

    @Test
    fun `le comptage classe du plus employe au moins employe`() {
        val counts = Hashtag.counted(
            listOf(
                "#mood #sport",
                "#mood encore #mood le même jour",
                "#mood",
                "#sport",
            )
        )
        assertEquals(listOf("mood", "sport"), counts.map { it.key })
        // Trois journees pour « mood », pas quatre : deux fois dans la meme
        // journee, ca reste une journee.
        assertEquals(3, counts.first().count)
        assertEquals(2, counts.last().count)
    }

    @Test
    fun `plusieurs mots-cles se lisent et`() {
        val text = "#sport et #maman"
        assertTrue(Hashtag.containsAll(text, setOf("sport", "maman")))
        assertFalse(Hashtag.containsAll(text, setOf("sport", "vacances")))
        assertTrue(Hashtag.containsAll(text, emptySet()))
    }
}
