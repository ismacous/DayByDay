package com.ismael.daybyday.ui

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.Hashtag

/**
 * Les mots-cles du journal, a l'ecran.
 *
 * La regle du texte brut n'a pas bouge : un `#mood` reste cinq caracteres dans
 * `note`, et rien n'est enregistre a cote. Tout ce qui suit n'est que du
 * **dessin** par-dessus ce texte-la — d'ou deux consequences qui comptent :
 * le curseur n'est jamais decale, et un mot-cle efface disparait tout seul.
 */

/**
 * La couleur d'un mot-cle, prise dans les six teintes de l'application.
 *
 * Elle est calculee a partir du nom ([Hashtag.tint]), donc `#mood` a la meme
 * couleur dans toutes les pages et dans la recherche, aujourd'hui comme apres
 * une restauration. Une couleur tiree au hasard et enregistree aurait demande
 * une table, une migration, et se serait perdue a la premiere sauvegarde.
 */
fun hashtagTint(name: String): Color = Palette[Hashtag.tint(name)]

/**
 * Les pastilles dessinees derriere les mots-cles d'un champ de texte.
 *
 * Compose ne sait pas donner des coins arrondis a un morceau de texte : un
 * `SpanStyle` ne connait qu'un fond rectangulaire, qui se lit comme un
 * surlignage, pas comme une etiquette. On les dessine donc nous-memes a partir
 * de la mise en page du texte.
 *
 * Deux precautions :
 *
 * 1. La mise en page est lue **dans le `drawBehind`**, jamais pendant la
 *    composition : taper une lettre ne doit pas remesurer la page entiere.
 * 2. Le modificateur se pose **apres** la marge du champ, pour que l'origine
 *    du dessin soit celle du texte. Pose avant, toutes les pastilles seraient
 *    decalees de la marge.
 *
 * Un mot-cle coupe en fin de ligne est dessine en deux morceaux, un par ligne :
 * une seule pastille qui traverserait le vide de la marge droite serait pire
 * que pas de pastille du tout.
 */
fun Modifier.hashtagChips(
    layout: State<TextLayoutResult?>,
    /** La taille du texte en points, qui donne la hauteur des pastilles. */
    textSize: Float,
): Modifier = drawBehind {
    val result = layout.value ?: return@drawBehind
    val text = result.layoutInput.text.text
    if (text.isEmpty()) return@drawBehind

    val size = textSize * density
    // La ligne d'ecriture est calee sur le bas de sa bande (voir la hauteur de
    // ligne du journal) : c'est donc du bas qu'on remonte pour trouver les
    // lettres, et pas du haut, qui contient l'interligne.
    val descent = size * DESCENT
    val ascent = size * ASCENT
    val padding = size * PADDING
    val radius = CornerRadius((ascent + descent + 2 * padding) / 2f)

    Hashtag.rangesIn(text).forEach { range ->
        val start = range.first
        val end = range.last + 1
        val tint = hashtagTint(text.substring(start + 1, end))

        val firstLine = result.getLineForOffset(start)
        val lastLine = result.getLineForOffset(end - 1)
        for (line in firstLine..lastLine) {
            val from = maxOf(start, result.getLineStart(line))
            val to = minOf(end, result.getLineEnd(line, visibleEnd = true))
            if (to <= from) continue

            val left = result.getHorizontalPosition(from, usePrimaryDirection = true)
            val right = result.getHorizontalPosition(to, usePrimaryDirection = true)
            val bottom = result.getLineBottom(line)
            drawRoundRect(
                color = tint.copy(alpha = CHIP_ALPHA),
                topLeft = Offset(left - padding, bottom - descent - ascent - padding),
                size = Size(
                    width = (right - left) + 2 * padding,
                    height = ascent + descent + 2 * padding,
                ),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * Ce qui depasse au-dessus et au-dessous de la ligne d'ecriture, en fraction de
 * la taille du texte. Ce sont des proportions moyennes de police : la pastille
 * est une decoration, personne ne mesure si elle deborde d'un demi-point.
 */
private const val ASCENT = 0.80f
private const val DESCENT = 0.22f

/** L'air laisse autour des lettres dans la pastille. */
private const val PADDING = 0.22f

/**
 * La pastille reste **pale**. Le mot-cle se lit d'abord comme un mot de la
 * phrase, et seulement ensuite comme une etiquette : c'est ce qui differencie
 * un journal d'un formulaire.
 */
private const val CHIP_ALPHA = 0.18f

/**
 * Le trait d'une citation, dessine le long de son paragraphe.
 *
 * Pourquoi le dessiner plutot que l'ecrire : un caractere ajoute devant chaque
 * ligne se retrouverait dans la recherche, dans l'export de l'annee et dans
 * l'apercu de la carte, et il faudrait le retirer partout. Le trait n'existe
 * qu'a l'ecran, et la citation reste exactement le texte qu'on a ecrit.
 *
 * Il court du haut de la premiere ligne au bas de la derniere, dans la marge
 * que le paragraphe s'est reservee ([QUOTE_INDENT]).
 */
fun Modifier.quoteBars(
    layout: State<TextLayoutResult?>,
    /** Les citations et la couleur de chacune, relues a chaque dessin. */
    quotes: () -> List<QuoteBar>,
): Modifier = drawBehind {
    val result = layout.value ?: return@drawBehind
    val length = result.layoutInput.text.length
    if (length == 0) return@drawBehind

    val width = BAR_WIDTH.toPx()
    val radius = CornerRadius(width / 2f)
    quotes().forEach { quote ->
        val start = quote.range.first.coerceIn(0, length - 1)
        val end = quote.range.last.coerceIn(start, length - 1)
        val firstLine = result.getLineForOffset(start)
        val lastLine = result.getLineForOffset(end)
        val top = result.getLineTop(firstLine)
        val bottom = result.getLineBottom(lastLine)
        drawRoundRect(
            color = quote.color,
            topLeft = Offset(result.getLineLeft(firstLine), top + BAR_INSET.toPx()),
            size = Size(width, (bottom - top - 2 * BAR_INSET.toPx()).coerceAtLeast(width)),
            cornerRadius = radius,
        )
    }
}

/** Une citation : l'intervalle qu'elle couvre, et la couleur de son trait. */
data class QuoteBar(val range: IntRange, val color: Color)

/** Assez large pour se voir, assez fin pour ne pas devenir une barre. */
private val BAR_WIDTH = 3.dp

/**
 * Le trait ne touche ni le haut ni le bas de sa bande : une ligne fait 28
 * points de haut alors que les lettres en font seize, donc un trait sur toute
 * la bande deborderait nettement du texte qu'il accompagne.
 */
private val BAR_INSET = 5.dp
