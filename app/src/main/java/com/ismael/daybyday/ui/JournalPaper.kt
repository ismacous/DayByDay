package com.ismael.daybyday.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.Placement
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextStyleKind

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
    /**
     * Une teinte du papier, poussee vers son encre.
     *
     * Toujours **opaque** : un fond translucide laisse passer le lignage et les
     * photos, et ce qui est pose dessus ne se lit plus pareil selon l'endroit
     * de la page ou il se trouve. Un objet pose sur le papier a sa propre
     * surface.
     */
    fun shade(paper: Color, amount: Float): Color {
        val target = ink(paper)
        return Color(
            red = paper.red + (target.red - paper.red) * amount,
            green = paper.green + (target.green - paper.green) * amount,
            blue = paper.blue + (target.blue - paper.blue) * amount,
        )
    }

    fun ink(paper: Color): Color =
        if (paper.red + paper.green + paper.blue > 1.5f) Color(0xFF1B1B1B) else Color(0xFFECEFF3)

    /**
     * La police de base de la page.
     *
     * Elle est rangee sous le **code** du style, pas sous son rang dans la
     * liste : ajouter une police plus tard ne doit pas changer celle des pages
     * deja ecrites. Un code inconnu — une police retiree — retombe sur la
     * police de l'application.
     */
    fun font(code: String): TextStyleKind =
        TextStyleKind.fromCode(code)?.takeIf { it.family == StyleFamily.FONT }
            ?: TextStyleKind.FONT_MODERN

    /**
     * Les tailles proposees, en sp.
     *
     * Toutes tiennent sous l'ecart entre deux lignes ([LINE_SPACING], 28
     * points) : le texte grandit, le lignage ne bouge pas, et les mots restent
     * poses dessus. C'est aussi ce qui borne la liste par le haut.
     */
    val sizes: List<Pair<String, Int>> = listOf(
        "Petit" to 14,
        "Normal" to 16,
        "Grand" to 19,
        "Très grand" to 22,
    )

    fun sizeLabel(size: Int): String =
        sizes.firstOrNull { it.second == size }?.first ?: "$size points"
}

/**
 * Les lignes d'ecriture, dessinees derriere le texte, exactement au pas de sa
 * hauteur de ligne pour que les mots se posent dessus.
 *
 * Un titre est plus haut qu'une ligne : le texte qui le suit se decale alors
 * du lignage. C'est le prix d'un vrai lignage sur un texte a tailles
 * variables, et ca reste juste tant qu'on ecrit au fil de la plume.
 *
 * [measuredLine] est la hauteur d'une ligne **telle que le texte a fini par
 * etre pose**, en pixels, ou `0` tant qu'on ne l'a pas encore mesuree. Ce
 * n'est pas un detail de reglage, c'est la correction d'un vrai defaut : la
 * hauteur demandee (28 points) tombe presque toujours sur un nombre de pixels
 * a virgule, que le moteur de texte arrondit ligne par ligne. Un quart de
 * pixel d'ecart ne se voit pas sur trois lignes ; sur deux cents, le texte a
 * glisse d'une demi-ligne et les traits passent au milieu des mots. On dessine
 * donc le lignage au pas **reel** du texte, et non a celui qu'on avait demande.
 */
@Composable
fun PaperLines(color: Color, modifier: Modifier = Modifier, measuredLine: Float = 0f) {
    Canvas(modifier = modifier) {
        val spacing = if (measuredLine > 0f) measuredLine else JournalPaper.LINE_SPACING.toPx()
        if (spacing <= 0f) return@Canvas
        val margin = JournalPaper.SIDE_MARGIN.toPx()

        // La marge du haut est un espace dans la page, et un espace est pose au
        // pixel entier : le lignage part du meme endroit, pas d'un demi-pixel
        // plus bas.
        var y = kotlin.math.round(JournalPaper.TOP_PADDING.toPx()) + spacing
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


/**
 * Les parametres de la page : le papier, les lignes, la police, la taille et
 * l'aimant des photos.
 *
 * C'est un **panneau** qui monte du bas, et pas une boite de dialogue. La
 * raison tient en une phrase : une boite de dialogue assombrit la page et la
 * cache derriere, alors qu'ici chaque reglage se voit sur la page. Le panneau
 * la laisse visible, et il porte en plus un apercu — le vrai papier, les
 * vraies lignes, la vraie police, a la vraie taille. On choisit ce qu'on voit.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PaperSettingsSheet(
    ruled: Boolean,
    paperIndex: Int,
    lineIndex: Int,
    snapToGrid: Boolean,
    fontCode: String,
    textSize: Int,
    onRuled: (Boolean) -> Unit,
    onPaper: (Int) -> Unit,
    onLine: (Int) -> Unit,
    onSnap: (Boolean) -> Unit,
    onFont: (String) -> Unit,
    onTextSize: (Int) -> Unit,
    /** Copie le lien de cette page, a coller dans une autre. */
    onCopyLink: () -> Unit,
    /** Le lien lui-meme, montre en clair : on colle ce qu'on a vu. */
    linkText: String,
    /** Enregistre la page en PDF, avec ses polices, ses couleurs et ses photos. */
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val font = JournalPaper.font(fontCode)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ScreenTitle(
                text = "Paramètres",
                accent = "de la page",
                subtitle = "Tout se voit tout de suite sur l'aperçu.",
            )

            PagePreview(
                paper = JournalPaper.paper(paperIndex),
                lineColor = JournalPaper.line(lineIndex),
                ruled = ruled,
                font = font,
                textSize = textSize,
            )

            SettingSwitch(
                label = "Lignes d'écriture",
                hint = "Le lignage de la feuille, dessiné derrière le texte.",
                checked = ruled,
                onChange = onRuled,
            )

            SettingSection(label = "Papier", value = JournalPaper.papers[paperIndex.coerceIn(JournalPaper.papers.indices)].first) {
                SwatchRow(
                    colors = JournalPaper.papers,
                    selected = paperIndex,
                    onPick = onPaper,
                )
            }

            // Choisir la couleur des lignes quand il n'y en a pas n'a pas de
            // sens : la section disparait en glissant plutot qu'en sautant.
            AnimatedVisibility(visible = ruled) {
                SettingSection(
                    label = "Couleur des lignes",
                    value = JournalPaper.lines[lineIndex.coerceIn(JournalPaper.lines.indices)].first,
                ) {
                    SwatchRow(
                        colors = JournalPaper.lines,
                        selected = lineIndex,
                        onPick = onLine,
                    )
                }
            }

            SettingSection(label = "Police de base", value = font.label) {
                // Chaque pastille est ecrite dans sa propre police : on lit le
                // choix au lieu de lire son nom.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    TextStyleKind.fonts.forEach { candidate ->
                        ChoicePill(
                            selected = candidate == font,
                            onClick = { onFont(candidate.code) },
                        ) { color ->
                            Text(
                                text = candidate.label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = candidate.fontFamily(),
                                ),
                                color = color,
                            )
                        }
                    }
                }
            }

            SettingSection(
                label = "Taille du texte",
                value = JournalPaper.sizeLabel(textSize),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    JournalPaper.sizes.forEach { (name, size) ->
                        ChoicePill(
                            selected = size == textSize,
                            onClick = { onTextSize(size) },
                        ) { color ->
                            // La pastille est ecrite a la taille qu'elle
                            // propose : la difference se voit avant d'appuyer.
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = size.sp,
                                    lineHeight = (size + 6).sp,
                                ),
                                color = color,
                            )
                        }
                    }
                }
            }

            SettingSection(label = "Cette page", value = linkText) {
                // Le lien est du texte : colle dans une autre journee, il y
                // devient un nom souligne sur lequel on appuie. Rien n'est
                // enregistre nulle part, donc rien ne peut pointer dans le vide.
                // Deux actions du meme rang : elles partagent la ligne en deux
                // parts egales. Laissees a la taille de leur texte, l'une etait
                // plus large que l'autre sans que rien ne le justifie — et deux
                // boutons voisins de tailles differentes se lisent comme deux
                // boutons d'importance differente.
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ChoicePill(
                        selected = false,
                        onClick = onCopyLink,
                        modifier = Modifier.weight(1f),
                    ) { color ->
                        Text(
                            text = "Copier le lien",
                            style = MaterialTheme.typography.bodyLarge,
                            color = color,
                        )
                    }
                    ChoicePill(
                        selected = false,
                        onClick = onExportPdf,
                        modifier = Modifier.weight(1f),
                    ) { color ->
                        Text(
                            text = "Exporter en PDF",
                            style = MaterialTheme.typography.bodyLarge,
                            color = color,
                        )
                    }
                }
            }

            SettingSwitch(
                label = "Aimanter les photos",
                // La grille des photos n'a rien a voir avec le lignage : on
                // peut ecrire sur page blanche et garder l'aimant.
                hint = "Les photos se calent sur une grille invisible quand tu les déplaces.",
                checked = snapToGrid,
                onChange = onSnap,
            )
        }
    }
}

/**
 * L'apercu de la page.
 *
 * Ce n'est pas une miniature : c'est la page elle-meme, a sa vraie echelle,
 * simplement coupee apres quelques lignes. Une miniature aurait menti sur la
 * taille du texte, qui est justement l'un des reglages.
 */
@Composable
private fun PagePreview(
    paper: Color,
    lineColor: Color,
    ruled: Boolean,
    font: TextStyleKind,
    textSize: Int,
) {
    val ink = JournalPaper.ink(paper)
    val background by animateColorAsState(paper, tween(Motion.NORMAL), label = "papier")
    val rhythm = with(LocalDensity.current) { JournalPaper.LINE_SPACING.toSp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PREVIEW_HEIGHT)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        if (ruled) {
            PaperLines(color = lineColor, modifier = Modifier.matchParentSize())
        }
        Text(
            text = "Aujourd'hui, j'ai pris le temps de m'asseoir et d'écrire.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = ink,
                fontFamily = font.fontFamily(),
                fontSize = textSize.sp,
                lineHeight = rhythm,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Bottom,
                    trim = LineHeightStyle.Trim.None,
                ),
            ),
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = JournalPaper.TOP_PADDING,
            ),
        )
    }
}

/** Le titre d'une section, sa valeur en clair, et le choix en dessous. */
@Composable
private fun SettingSection(
    label: String,
    value: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            SectionLabelText(label)
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        content()
    }
}

/** Un reglage qui n'a que deux etats : l'interrupteur le dit tout seul. */
@Composable
private fun SettingSwitch(
    label: String,
    hint: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClickLabel = label) { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * Une pastille de choix dont le contenu est libre — c'est ce qui permet
 * d'ecrire chaque police dans sa propre police, et chaque taille a sa taille.
 */
@Composable
private fun ChoicePill(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val background by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(Motion.NORMAL),
        label = "fond",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) readableOn(accent) else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(Motion.NORMAL),
        label = "encre",
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = background,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            content(foreground)
        }
    }
}

/**
 * Les couleurs proposees.
 *
 * La pastille choisie **grandit** et prend un anneau : deux signaux plutot
 * qu'un, parce qu'un anneau seul se perd sur les papiers tres clairs, ou il a
 * presque la couleur du fond.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(
    colors: List<Pair<String, Color>>,
    selected: Int,
    onPick: (Int) -> Unit,
) {
    // En ligne simple, la derniere pastille etait rognee par le bord du
    // panneau : on la voyait comme un trait.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        colors.forEachIndexed { index, (name, color) ->
            val isSelected = index == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.84f,
                animationSpec = tween(Motion.NORMAL),
                label = "taille",
            )
            Box(
                modifier = Modifier
                    .size(SWATCH_SIZE)
                    .clip(CircleShape)
                    .clickable(onClickLabel = name) { onPick(index) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        // L'echelle est lue dans graphicsLayer : lue dans la
                        // composition, elle remesurerait la rangee entiere a
                        // chaque image de l'animation.
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .size(SWATCH_SIZE)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = CircleShape,
                        ),
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(SWATCH_SIZE)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
        }
    }
}

private val SWATCH_SIZE = 40.dp

/**
 * Quatre lignes de la page, pas plus : l'apercu doit montrer le rythme du
 * lignage sans prendre la moitie du panneau.
 */
private val PREVIEW_HEIGHT = 126.dp
