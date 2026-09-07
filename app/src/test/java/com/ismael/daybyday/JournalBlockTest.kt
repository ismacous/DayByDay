package com.ismael.daybyday

import com.ismael.daybyday.data.BlockKind
import com.ismael.daybyday.data.JournalBlock
import com.ismael.daybyday.data.JournalBlocks
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les blocs de la page.
 *
 * Ce qu'il faut protéger, et qui n'est pas evident : le texte a plat que
 * lisent la recherche, l'export de l'annee et les apercus est **recalcule** a
 * partir des blocs. Si le calcul se decale d'un retour a la ligne, tout ce qui
 * lit une page a plat se decale avec — y compris les intervalles de mise en
 * forme, qui ne pointeraient plus sur les bons mots.
 */
class JournalBlockTest {

    private val day = 20000L

    private fun text(text: String, spans: List<TextSpan> = emptyList()) = JournalBlock(
        epochDay = day,
        position = 0,
        kindCode = BlockKind.TEXT.code,
        text = text,
        spans = RichText.encode(spans),
    )

    private fun quote(text: String, bar: TextStyleKind? = null) = JournalBlock(
        epochDay = day,
        position = 0,
        kindCode = BlockKind.QUOTE.code,
        text = text,
        barCode = bar?.code.orEmpty(),
    )

    private fun rule(style: TextStyleKind) = JournalBlock(
        epochDay = day,
        position = 0,
        kindCode = BlockKind.RULE.code,
        ruleCode = style.code,
    )

    // --- Le texte a plat ---------------------------------------------------

    @Test
    fun `deux paragraphes se collent par un retour a la ligne`() {
        val flat = JournalBlocks.flatten(listOf(text("bonjour"), text("le monde")))

        assertEquals("bonjour\nle monde", flat.text)
    }

    @Test
    fun `un vocal n'ecrit rien dans le texte`() {
        val voice = JournalBlock(
            epochDay = day,
            position = 1,
            kindCode = BlockKind.VOICE.code,
            voiceId = 7,
        )
        val flat = JournalBlocks.flatten(listOf(text("avant"), voice, text("apres")))

        // Un enregistrement n'est pas du texte : le faire apparaitre dans la
        // recherche n'aurait aucun sens, et le compter comme un paragraphe
        // decalerait tous les intervalles qui suivent.
        assertEquals("avant\napres", flat.text)
    }

    @Test
    fun `un trait occupe une ligne vide, et une seule`() {
        val flat = JournalBlocks.flatten(
            listOf(text("abc"), rule(TextStyleKind.RULE_THIN), text("def"))
        )

        assertEquals("abc\n\ndef", flat.text)
        val mark = flat.spans.single { it.style.isRule }
        // Le style est pose sur le saut de ligne qui **termine** la ligne vide.
        assertEquals('\n', flat.text[mark.start])
        assertEquals(4, mark.start)
        assertEquals(5, mark.end)
    }

    @Test
    fun `une citation porte son style sur tout son texte`() {
        val flat = JournalBlocks.flatten(
            listOf(text("avant"), quote("la citation", TextStyleKind.QUOTE_SKY))
        )

        assertEquals("avant\nla citation", flat.text)
        val mark = flat.spans.single { it.style == TextStyleKind.QUOTE }
        assertEquals(6, mark.start)
        assertEquals(flat.text.length, mark.end)
        // La couleur du trait couvre exactement la meme etendue : c'est ce qui
        // permet au PDF de la retrouver.
        val bar = flat.spans.single { it.style == TextStyleKind.QUOTE_SKY }
        assertEquals(mark.start, bar.start)
        assertEquals(mark.end, bar.end)
    }

    @Test
    fun `la mise en forme d'un bloc est decalee de la place du bloc`() {
        val flat = JournalBlocks.flatten(
            listOf(
                text("abc"),
                text("gras ici", listOf(TextSpan(0, 4, TextStyleKind.BOLD))),
            )
        )

        assertEquals("abc\ngras ici", flat.text)
        val bold = flat.spans.single { it.style == TextStyleKind.BOLD }
        assertEquals("gras", flat.text.substring(bold.start, bold.end))
    }

    // --- Le decoupage ------------------------------------------------------

    @Test
    fun `des paragraphes ordinaires ne font qu'un seul bloc`() {
        // C'est ce qui fait qu'ecrire n'a pas change : une page ordinaire n'a
        // qu'un champ, et Entree reste un simple retour a la ligne.
        val blocks = JournalBlocks.split("un\ndeux\ntrois", emptyList(), day)

        assertEquals(1, blocks.size)
        assertEquals("un\ndeux\ntrois", blocks.first().text)
    }

    @Test
    fun `une citation ecrite avant les blocs en devient un`() {
        val text = "avant\nla citation\napres"
        val spans = listOf(
            TextSpan(6, 17, TextStyleKind.QUOTE),
            TextSpan(6, 17, TextStyleKind.QUOTE_MINT),
        )
        val blocks = JournalBlocks.split(text, spans, day)

        assertEquals(3, blocks.size)
        assertEquals(BlockKind.QUOTE, blocks[1].kind)
        assertEquals("la citation", blocks[1].text)
        assertEquals(TextStyleKind.QUOTE_MINT.code, blocks[1].barCode)
        assertEquals("avant", blocks[0].text)
        assertEquals("apres", blocks[2].text)
    }

    @Test
    fun `un texte vide donne quand meme un bloc`() {
        // Il faut bien un endroit ou poser le curseur.
        val blocks = JournalBlocks.split("", emptyList(), day)

        assertEquals(1, blocks.size)
        assertEquals(BlockKind.TEXT, blocks.first().kind)
    }

    // --- L'aller-retour ----------------------------------------------------

    @Test
    fun `decouper puis recomposer rend le texte d'origine`() {
        // C'est l'invariant qui protege la migration : les pages deja ecrites
        // sont decoupees, et ce qu'on relit ensuite doit etre mot pour mot ce
        // qui etait la.
        val text = "premier\nla citation\n\nsuite du texte\ndeuxieme ligne"
        val spans = listOf(
            TextSpan(8, 19, TextStyleKind.QUOTE),
            TextSpan(20, 21, TextStyleKind.RULE_BOLD),
            TextSpan(0, 7, TextStyleKind.BOLD),
        )

        val blocks = JournalBlocks.split(text, spans, day)
        val flat = JournalBlocks.flatten(blocks)

        assertEquals(text, flat.text)
        val bold = flat.spans.single { it.style == TextStyleKind.BOLD }
        assertEquals("premier", flat.text.substring(bold.start, bold.end))
        assertTrue(flat.spans.any { it.style == TextStyleKind.RULE_BOLD })
        assertTrue(flat.spans.any { it.style == TextStyleKind.QUOTE })
    }

    // --- Accorder les blocs et les vocaux ----------------------------------

    @Test
    fun `un vocal sans bloc se range a la fin`() {
        // Le cas reel : l'application est tuee entre l'enregistrement (ecrit
        // tout de suite) et le depart de l'ecran (qui ecrit les blocs).
        val blocks = listOf(text("du texte"))
        val out = JournalBlocks.reconcile(blocks, listOf(42L), day)

        assertEquals(2, out.size)
        assertEquals(BlockKind.VOICE, out[1].kind)
        assertEquals(42L, out[1].voiceId)
    }

    @Test
    fun `un bloc dont le vocal a disparu s'en va`() {
        val orphan = JournalBlock(
            epochDay = day,
            position = 1,
            kindCode = BlockKind.VOICE.code,
            voiceId = 99,
        )
        val out = JournalBlocks.reconcile(listOf(text("du texte"), orphan), emptyList(), day)

        assertEquals(1, out.size)
        assertEquals(BlockKind.TEXT, out.first().kind)
    }

    @Test
    fun `les rangs sont remis a plat`() {
        val out = JournalBlocks.reconcile(
            listOf(text("a").copy(position = 7), text("b").copy(position = 3)),
            emptyList(),
            day,
        )

        assertEquals(listOf(0, 1), out.map { it.position })
    }

    @Test
    fun `ranger jette les blocs de texte vides mais jamais le dernier`() {
        val out = JournalBlocks.tidy(listOf(text(""), quote("citation"), text("")), day)

        assertEquals(1, out.size)
        assertEquals(BlockKind.QUOTE, out.first().kind)

        val alone = JournalBlocks.tidy(listOf(text("")), day)
        assertEquals(1, alone.size)
        assertNull(alone.first().voiceId)
    }
}
