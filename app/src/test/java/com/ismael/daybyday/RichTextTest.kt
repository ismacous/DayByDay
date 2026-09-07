package com.ismael.daybyday

import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        // Chaque style appartient a une famille et une seule. La somme se fait
        // sur StyleFamily.entries et non sur une liste ecrite a la main :
        // ajouter une famille sans toucher au test cassait le test, jamais le
        // code, et l'echec ne disait rien du vrai probleme.
        assertEquals(
            TextStyleKind.entries.size,
            StyleFamily.entries.sumOf { TextStyleKind.of(it).size },
        )
        // Une famille vide est une famille qu'on a declaree puis oubliee.
        StyleFamily.entries.forEach {
            assertTrue("la famille $it n'a aucun style", TextStyleKind.of(it).isNotEmpty())
        }
    }

    @Test
    fun `les codes deja enregistres n ont pas bouge`() {
        // Les changer rendrait illisible la mise en forme deja ecrite.
        assertEquals("b", TextStyleKind.BOLD.code)
        assertEquals("i", TextStyleKind.ITALIC.code)
        assertEquals("u", TextStyleKind.UNDERLINE.code)
        assertEquals("s", TextStyleKind.STRIKETHROUGH.code)
        assertEquals("h", TextStyleKind.HIGHLIGHT.code)
        assertEquals("cr", TextStyleKind.COLOR_RED.code)
        assertEquals("cg", TextStyleKind.COLOR_GREEN.code)
        assertEquals("cb", TextStyleKind.COLOR_BLUE.code)
        assertEquals("cv", TextStyleKind.COLOR_VIOLET.code)
        assertEquals("co", TextStyleKind.COLOR_ORANGE.code)
    }

    @Test
    fun `une citation et un trait prennent la ligne entiere`() {
        // Comme un titre : ni l'un ni l'autre ne se pose sur trois mots au
        // milieu d'une phrase.
        TextStyleKind.blocks.forEach { style ->
            assertTrue(style.label, style.takesWholeLine)
        }
        assertTrue(TextStyleKind.RULE_THIN.isRule)
        assertTrue(TextStyleKind.RULE_BOLD.isRule)
        assertTrue(TextStyleKind.RULE_SHORT.isRule)
        // La citation n'est pas un trait : elle habille du texte, elle ne le
        // remplace pas.
        assertFalse(TextStyleKind.QUOTE.isRule)
    }

    @Test
    fun `un surlignage chasse le precedent mais pas la couleur du texte`() {
        val jaune = RichText.toggle(emptyList(), 0, 5, TextStyleKind.HIGHLIGHT)
        val rouge = RichText.toggle(jaune, 0, 5, TextStyleKind.COLOR_RED)
        val vert = RichText.toggle(rouge, 0, 5, TextStyleKind.HIGHLIGHT_GREEN)

        val actifs = RichText.stylesOn(vert, 0, 5)
        assertTrue(TextStyleKind.HIGHLIGHT_GREEN in actifs)
        assertTrue(TextStyleKind.HIGHLIGHT !in actifs)
        assertTrue(TextStyleKind.COLOR_RED in actifs)
    }

    @Test
    fun `un titre remplace le niveau precedent`() {
        val un = RichText.toggle(emptyList(), 0, 5, TextStyleKind.TITLE_1)
        val deux = RichText.toggle(un, 0, 5, TextStyleKind.TITLE_2)

        assertEquals(spans(Triple(0, 5, TextStyleKind.TITLE_2)), deux)
    }

    // --- Trouver la ligne ------------------------------------------------

    @Test
    fun `la ligne du curseur va d un retour a l autre`() {
        val texte = "premiere\nseconde\ntroisieme"

        // Un titre prend la ligne entiere, meme si le curseur est au milieu.
        assertEquals(9..15, RichText.lineRange(texte, 12))
    }

    @Test
    fun `la premiere ligne commence a zero`() {
        assertEquals(0..7, RichText.lineRange("premiere\nseconde", 3))
    }

    @Test
    fun `la derniere ligne va jusqu au bout du texte`() {
        val texte = "premiere\nseconde"
        assertEquals(9..15, RichText.lineRange(texte, 16))
    }

    @Test
    fun `une selection sur plusieurs lignes les prend toutes`() {
        val texte = "premiere\nseconde\ntroisieme"
        assertEquals(0..25, RichText.lineRange(texte, 3, 20))
    }

    @Test
    fun `un journal sans retour a la ligne n est qu une seule ligne`() {
        // C'est pour ca qu'un titre ne s'applique plus a "la ligne" quand rien
        // n'est selectionne : sur un paragraphe d'un bloc, il prenait tout.
        val paragraphe = "Alors aujourd'hui j'etais chez Dune, et je suis rentre tard."
        assertEquals(0 until paragraphe.length, RichText.lineRange(paragraphe, 20))
    }

    @Test
    fun `un texte vide donne une ligne vide`() {
        assertTrue(RichText.lineRange("", 0).isEmpty())
    }

    // --- Le mot sous le curseur (double appui) ----------------------------

    @Test
    fun `le double appui prend le mot autour du curseur`() {
        val texte = "Journee tranquille et douce"
        assertEquals(8 until 18, RichText.wordAt(texte, 12))
    }

    @Test
    fun `le curseur colle a la fin d un mot prend ce mot`() {
        // C'est le cas courant : le premier appui pose le curseur juste apres
        // la derniere lettre touchee.
        val texte = "Journee tranquille"
        assertEquals(0 until 7, RichText.wordAt(texte, 7))
    }

    @Test
    fun `le curseur au debut d un mot prend ce mot`() {
        assertEquals(8 until 18, RichText.wordAt("Journee tranquille", 8))
    }

    @Test
    fun `un espace ne selectionne rien`() {
        // Curseur entre deux espaces : rien a selectionner, et surtout pas la
        // phrase entiere.
        assertNull(RichText.wordAt("bien  dormi", 5))
    }

    @Test
    fun `la ponctuation n est jamais prise dans le mot`() {
        val texte = "Fatigue, mais content."
        assertEquals(0 until 7, RichText.wordAt(texte, 4))
        assertEquals(14 until 21, RichText.wordAt(texte, 16))
    }

    @Test
    fun `l apostrophe coupe le mot`() {
        // "l'ami" : on veut "ami", comme le fait le telephone.
        assertEquals(2 until 5, RichText.wordAt("l'ami", 3))
    }

    @Test
    fun `les accents font partie du mot`() {
        val texte = "une journee eprouvante"
        assertEquals(4 until 11, RichText.wordAt(texte.replace("journee", "journée"), 6))
    }

    @Test
    fun `les chiffres font partie du mot`() {
        assertEquals(0 until 5, RichText.wordAt("12h30 debout", 2))
    }

    @Test
    fun `un texte vide ne selectionne rien`() {
        assertNull(RichText.wordAt("", 0))
    }

    @Test
    fun `un curseur hors du texte ne fait pas tomber l application`() {
        assertEquals(0 until 4, RichText.wordAt("bien", 900))
        assertEquals(0 until 4, RichText.wordAt("bien", -5))
    }

    // --- Revenir au normal ------------------------------------------------

    @Test
    fun `effacer une famille enleve aussi ce qui ne couvre qu un bout`() {
        // Un titre pose sur la moitie de la selection n'est pas "actif" au
        // sens du bouton : il faut quand meme le retirer pour revenir au
        // texte normal, sinon "Texte normal" ne fait rien de visible.
        val depart = spans(
            Triple(0, 5, TextStyleKind.TITLE_1),
            Triple(0, 20, bold),
        )

        val resultat = RichText.clearFamily(depart, 0, 20, StyleFamily.HEADING)

        assertEquals(spans(Triple(0, 20, bold)), resultat)
    }

    @Test
    fun `effacer une famille laisse ce qui est en dehors`() {
        val depart = spans(Triple(0, 30, TextStyleKind.FONT_HAND))

        val resultat = RichText.clearFamily(depart, 10, 20, StyleFamily.FONT)

        assertEquals(
            spans(
                Triple(0, 10, TextStyleKind.FONT_HAND),
                Triple(20, 30, TextStyleKind.FONT_HAND),
            ),
            resultat,
        )
    }

    // --- Les polices ------------------------------------------------------

    @Test
    fun `une police remplace la precedente et ne touche pas au gras`() {
        val depart = spans(
            Triple(0, 10, bold),
            Triple(0, 10, TextStyleKind.FONT_SERIF),
        )

        val resultat = RichText.toggle(depart, 0, 10, TextStyleKind.FONT_HAND)

        assertEquals(
            spans(
                Triple(0, 10, bold),
                Triple(0, 10, TextStyleKind.FONT_HAND),
            ),
            resultat,
        )
    }

    @Test
    fun `une police se relit apres enregistrement`() {
        val depart = spans(Triple(3, 9, TextStyleKind.FONT_MODERN))
        assertEquals(depart, RichText.decode(RichText.encode(depart), 20))
    }
}
