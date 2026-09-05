package com.ismael.daybyday

import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La mise en forme du journal vit a cote du texte, en intervalles de
 * caracteres. Tout le risque est la : ecrire au milieu d'une phrase deja mise
 * en forme doit deplacer ce qui suit, pas le decaler de travers.
 */
class RichTextTest {

    private val bold = TextStyleKind.BOLD
    private val italic = TextStyleKind.ITALIC

    private fun spans(vararg triples: Triple<Int, Int, TextStyleKind>) =
        triples.map { (start, end, style) -> TextSpan(start, end, style) }

    // --- Poser et retirer -------------------------------------------------

    @Test
    fun `poser un style sur une selection`() {
        val result = RichText.toggle(emptyList(), 0, 5, bold)

        assertEquals(spans(Triple(0, 5, bold)), result)
    }

    @Test
    fun `reappuyer sur un style deja actif le retire`() {
        val once = RichText.toggle(emptyList(), 0, 5, bold)
        val twice = RichText.toggle(once, 0, 5, bold)

        assertTrue(twice.isEmpty())
    }

    @Test
    fun `retirer un style au milieu coupe l intervalle en deux`() {
        val result = RichText.toggle(spans(Triple(0, 10, bold)), 3, 6, bold)

        assertEquals(spans(Triple(0, 3, bold), Triple(6, 10, bold)), result)
    }

    @Test
    fun `un style partiellement pose s etend a toute la selection`() {
        // Seule la moitie est en gras : le bouton doit finir de la mettre,
        // pas enlever ce qui existe deja.
        val result = RichText.toggle(spans(Triple(0, 3, bold)), 0, 10, bold)

        assertEquals(spans(Triple(0, 10, bold)), result)
    }

    @Test
    fun `deux styles differents cohabitent sur le meme texte`() {
        val result = RichText.toggle(spans(Triple(0, 5, bold)), 0, 5, italic)

        assertEquals(setOf(bold, italic), RichText.stylesOn(result, 0, 5))
    }

    @Test
    fun `une couleur chasse la precedente`() {
        val rouge = RichText.toggle(emptyList(), 0, 5, TextStyleKind.COLOR_RED)
        val vert = RichText.toggle(rouge, 0, 5, TextStyleKind.COLOR_GREEN)

        assertEquals(spans(Triple(0, 5, TextStyleKind.COLOR_GREEN)), vert)
    }

    @Test
    fun `une couleur ne chasse pas le gras`() {
        val gras = RichText.toggle(emptyList(), 0, 5, bold)
        val colore = RichText.toggle(gras, 0, 5, TextStyleKind.COLOR_RED)

        assertEquals(setOf(bold, TextStyleKind.COLOR_RED), RichText.stylesOn(colore, 0, 5))
    }

    // --- Ecrire dans un texte deja mis en forme ---------------------------

    @Test
    fun `ecrire avant un passage en gras le decale`() {
        // "monde" est en gras ; on ajoute "Salut " devant.
        val result = RichText.adjust(spans(Triple(0, 5, bold)), "monde", "Salut monde")

        assertEquals(spans(Triple(6, 11, bold)), result)
    }

    @Test
    fun `ecrire au milieu d un passage en gras l allonge`() {
        val result = RichText.adjust(spans(Triple(0, 5, bold)), "monde", "moooonde")

        assertEquals(spans(Triple(0, 8, bold)), result)
    }

    @Test
    fun `ecrire apres un passage en gras ne le teint pas`() {
        // Le texte tape ensuite ne doit pas devenir gras tout seul.
        val result = RichText.adjust(spans(Triple(0, 5, bold)), "monde", "monde entier")

        assertEquals(spans(Triple(0, 5, bold)), result)
    }

    @Test
    fun `effacer le texte en gras efface sa mise en forme`() {
        val result = RichText.adjust(spans(Triple(6, 11, bold)), "Salut monde", "Salut ")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `effacer autour laisse la mise en forme sur ce qui reste`() {
        // "Salut monde entier", "monde" en gras, on efface " entier".
        val result = RichText.adjust(spans(Triple(6, 11, bold)), "Salut monde entier", "Salut monde")

        assertEquals(spans(Triple(6, 11, bold)), result)
    }

    @Test
    fun `tout effacer ne laisse aucune mise en forme`() {
        val result = RichText.adjust(spans(Triple(0, 5, bold)), "monde", "")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `remplacer une selection en gras garde le gras sur le remplacement`() {
        // Selectionner "monde" et taper "terre" : ce qui remplace herite de la
        // mise en forme, comme dans n'importe quel traitement de texte.
        val result = RichText.adjust(spans(Triple(0, 5, bold)), "monde", "terre")

        assertEquals(spans(Triple(0, 5, bold)), result)
    }

    @Test
    fun `un intervalle ne depasse jamais la fin du texte`() {
        val result = RichText.adjust(spans(Triple(0, 20, bold)), "un texte plutot long", "court")

        assertTrue(result.all { it.end <= "court".length })
    }

    @Test
    fun `ecrire juste avant un passage en gras ne le teint pas`() {
        // Au point exact de l'insertion, le debut de l'intervalle se pousse a
        // droite et sa fin suit : le texte ajoute reste hors du gras.
        val result = RichText.adjust(spans(Triple(0, 5, bold)), "monde", "Xmonde")

        assertEquals(spans(Triple(1, 6, bold)), result)
    }

    // --- Continuer a ecrire dans le style courant -------------------------

    @Test
    fun `le curseur en fin de gras signale le gras`() {
        // C'est ce qui permet de continuer a taper en gras.
        assertEquals(setOf(bold), RichText.stylesOn(spans(Triple(0, 5, bold)), 5, 5))
    }

    @Test
    fun `le curseur juste apres la fin du gras ne signale rien`() {
        assertTrue(RichText.stylesOn(spans(Triple(0, 5, bold)), 6, 6).isEmpty())
    }

    @Test
    fun `poser un style sur le texte qui vient d etre ecrit`() {
        val edit = RichText.diff("monde", "monde !")
        val result = RichText.applyAll(
            spans(Triple(0, 5, bold)),
            edit.inserted.first,
            edit.inserted.last + 1,
            setOf(bold),
        )

        assertEquals(spans(Triple(0, 7, bold)), result)
    }

    // --- Situer une modification -----------------------------------------

    @Test
    fun `situer un ajout au milieu`() {
        val edit = RichText.diff("abcdef", "abcXdef")

        assertEquals(3, edit.start)
        assertEquals(3, edit.oldEnd)
        assertEquals(4, edit.newEnd)
        assertEquals(1, edit.delta)
    }

    @Test
    fun `situer une suppression`() {
        val edit = RichText.diff("abcdef", "abdef")

        assertEquals(2, edit.start)
        assertEquals(3, edit.oldEnd)
        assertEquals(2, edit.newEnd)
        assertEquals(-1, edit.delta)
    }

    @Test
    fun `un texte identique ne bouge pas`() {
        val avant = spans(Triple(0, 5, bold))

        assertEquals(avant, RichText.adjust(avant, "monde", "monde"))
    }

    // --- Enregistrer et relire -------------------------------------------

    @Test
    fun `la mise en forme survit a un aller-retour`() {
        val avant = RichText.merge(
            spans(Triple(0, 5, bold), Triple(3, 9, italic), Triple(2, 4, TextStyleKind.COLOR_RED))
        )

        val apres = RichText.decode(RichText.encode(avant), textLength = 20)

        assertEquals(avant, apres)
    }

    @Test
    fun `une mise en forme abimee n empeche pas de relire le texte`() {
        // Ce cas ne devrait pas arriver, mais une note ne doit jamais devenir
        // illisible a cause de sa decoration.
        val result = RichText.decode("nimportequoi;0,5,b;3,x,i;9", textLength = 10)

        assertEquals(spans(Triple(0, 5, bold)), result)
    }

    @Test
    fun `relire borne les intervalles au texte present`() {
        val result = RichText.decode("0,99,b", textLength = 4)

        assertEquals(spans(Triple(0, 4, bold)), result)
    }

    @Test
    fun `aucune mise en forme donne une liste vide`() {
        assertTrue(RichText.decode(null, 10).isEmpty())
        assertTrue(RichText.decode("", 10).isEmpty())
        assertTrue(RichText.encode(emptyList()).isEmpty())
    }

    // --- Recoller ---------------------------------------------------------

    @Test
    fun `deux intervalles du meme style qui se touchent n en font qu un`() {
        val result = RichText.merge(spans(Triple(0, 5, bold), Triple(5, 10, bold)))

        assertEquals(spans(Triple(0, 10, bold)), result)
    }

    @Test
    fun `deux intervalles separes restent separes`() {
        val result = RichText.merge(spans(Triple(0, 5, bold), Triple(7, 10, bold)))

        assertEquals(spans(Triple(0, 5, bold), Triple(7, 10, bold)), result)
    }

    @Test
    fun `les codes des styles sont uniques et stables`() {
        val codes = TextStyleKind.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertEquals(TextStyleKind.entries.size, TextStyleKind.marks.size + TextStyleKind.colors.size)
    }
}
