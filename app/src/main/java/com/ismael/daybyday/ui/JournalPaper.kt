package com.ismael.daybyday.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.Placement

/**
 * L'apparence de la page du journal : le papier, et les lignes d'ecriture.
 *
 * Les lignes ne sont pas la grille des photos. La grille sert a aligner des
 * images et n'apparait que pendant qu'on en deplace une ; les lignes, elles,
 * restent tout le temps, comme sur une feuille de classeur. Couper les lignes
 * ne coupe pas l'aimantation : ce sont deux choses separees.
 */
object JournalPaper {

    /**
     * L'ecart entre deux lignes, en points.
     *
     * C'est aussi la hauteur de ligne du texte : les deux valeurs sont la
     * meme, exprimee en points et non en taille de police, sinon agrandir les
     * caracteres dans les reglages d'Android decalerait le texte de ses
     * lignes.
     */
    val LINE_SPACING: Dp = Placement.lineSpacing.dp

    /**
     * La marge en haut du texte, a laquelle commence le lignage.
     *
     * C'est un pas de grille exactement, et l'ecart entre deux lignes en vaut
     * deux : chaque ligne d'ecriture tombe donc pile sur une ligne de la
     * grille des photos. Sans ca, les deux quadrillages se croisent de travers
     * des qu'on ouvre la grille.
     */
    val TOP_PADDING: Dp = Placement.topMargin.dp

    /** La marge de gauche, et le trait vertical qui la marque. */
    val SIDE_MARGIN: Dp = 20.dp

    /** Les papiers proposes, du plus neutre au plus marque. */
    val papers: List<Pair<String, Color>> = listOf(
        "Blanc" to Color(0xFFFFFFFF),
        "Ivoire" to Color(0xFFFBF5E6),
        "Sable" to Color(0xFFF3EADB),
        "Gris clair" to Color(0xFFEDEEF0),
        "Bleu pâle" to Color(0xFFEAF1F8),
        "Vert d'eau" to Color(0xFFE9F2EC),
        "Ardoise" to Color(0xFF23262B),
    )

    /** Les couleurs de lignes. */
    val lines: List<Pair<String, Color>> = listOf(
        "Bleu cahier" to Color(0xFF9DB6D4),
        "Gris" to Color(0xFFB6BBC2),
        "Sépia" to Color(0xFFCBB79A),
        "Vert" to Color(0xFFA8C7B0),
        "Rose" to Color(0xFFE3B5C6),
        "Presque noir" to Color(0xFF6A6F77),
    )

    fun paper(index: Int): Color = papers[index.coerceIn(papers.indices)].second

    fun line(index: Int): Color = lines[index.coerceIn(lines.indices)].second

    /**
     * Le texte doit rester lisible sur le papier choisi : sur l'ardoise, il
     * passe en clair. On regarde la luminosite plutot que de tenir une
     * deuxieme liste a jour.
     */
    fun ink(paper: Color): Color =
        if (paper.red + paper.green + paper.blue > 1.5f) Color(0xFF1B1B1B) else Color(0xFFECEFF3)
}

/**
 * Les lignes d'ecriture, dessinees derriere le texte, exactement au pas de sa
 * hauteur de ligne pour que les mots se posent dessus.
 *
 * Un titre est plus haut qu'une ligne : le texte qui le suit se decale alors
 * du lignage. C'est le prix d'un vrai lignage sur un texte a tailles
 * variables, et ca reste juste tant qu'on ecrit au fil de la plume.
 */
@Composable
fun PaperLines(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val spacing = JournalPaper.LINE_SPACING.toPx()
        if (spacing <= 0f) return@Canvas
        val margin = JournalPaper.SIDE_MARGIN.toPx()

        var y = JournalPaper.TOP_PADDING.toPx() + spacing
        while (y < size.height) {
            drawLine(
                color = color,
                start = Offset(margin, y),
                end = Offset(size.width - margin, y),
                strokeWidth = 1f,
            )
            y += spacing
        }

        // Le trait de marge. C'est lui qui fait vraiment la feuille de
        // classeur : sans lui, des lignes horizontales seules ressemblent a du
        // papier a musique.
        val marginX = margin * 0.65f
        drawLine(
            color = color.copy(alpha = color.alpha * 0.9f),
            start = Offset(marginX, 0f),
            end = Offset(marginX, size.height),
            strokeWidth = 2f,
        )
    }
}

/** Le menu des trois points : tout ce qui touche a l'allure de la page. */
@Composable
fun PaperSettingsDialog(
    ruled: Boolean,
    paperIndex: Int,
    lineIndex: Int,
    snapToGrid: Boolean,
    onRuled: (Boolean) -> Unit,
    onPaper: (Int) -> Unit,
    onLine: (Int) -> Unit,
    onSnap: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        title = { Text("Allure de la page") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SettingLine(
                    label = "Lignes d'écriture",
                    value = if (ruled) "Affichées" else "Masquées",
                    onClick = { onRuled(!ruled) },
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Papier", style = MaterialTheme.typography.labelLarge)
                    SwatchRow(
                        colors = JournalPaper.papers,
                        selected = paperIndex,
                        onPick = onPaper,
                    )
                }

                if (ruled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lignes", style = MaterialTheme.typography.labelLarge)
                        SwatchRow(
                            colors = JournalPaper.lines,
                            selected = lineIndex,
                            onPick = onLine,
                        )
                    }
                }

                SettingLine(
                    label = "Aimanter les photos",
                    // La grille des photos n'a rien a voir avec le lignage :
                    // on peut ecrire sur page blanche et garder l'aimant.
                    value = if (snapToGrid) "Activé" else "Coupé",
                    onClick = { onSnap(!snapToGrid) },
                )
            }
        },
    )
}

@Composable
private fun SettingLine(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(
    colors: List<Pair<String, Color>>,
    selected: Int,
    onPick: (Int) -> Unit,
) {
    // En ligne simple, la derniere pastille etait rognee par le bord de la
    // boite de dialogue : on la voyait comme un trait.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        colors.forEachIndexed { index, (name, color) ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (index == selected) 3.dp else 1.dp,
                        color = if (index == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        },
                        shape = CircleShape,
                    )
                    .clickable(onClickLabel = name) { onPick(index) },
            )
        }
    }
}
