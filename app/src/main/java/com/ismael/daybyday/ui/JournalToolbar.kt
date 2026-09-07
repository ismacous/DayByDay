package com.ismael.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.R
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.BlockAlign
import com.ismael.daybyday.data.TextStyleKind
import java.io.File

/**
 * Taille des boutons de la barre. Sept tiennent ainsi sur la largeur d'un
 * telephone courant, sans que le dernier soit rogne.
 */
private val BUTTON_SIZE = 38.dp

/** Les panneaux qui peuvent s'ouvrir sous la barre, a la place du clavier. */
enum class ToolPanel(val label: String) {
    /** Tout ce que l'editeur sait faire, range par famille, comme un menu. */
    ALL("Tous les outils"),
    COLORS("Couleur du texte"),
    HIGHLIGHTS("Surlignage"),
}

/**
 * L'encre et le papier de la page, disponibles partout dans la barre d'outils.
 *
 * Passer les deux couleurs a chacun des huit boutons et des quatre panneaux
 * aurait fait douze parametres a trainer. La barre les annonce une fois, et
 * tout ce qu'elle contient les lit — c'est la meme idee que l'encre des cartes
 * de « Ma journee », pour la meme raison : une surface coloree ne peut pas
 * laisser son contenu prendre les couleurs du theme.
 */
private val LocalPaperInk = compositionLocalOf { Color.Black }

private val LocalPaperSurface = compositionLocalOf { Color.White }

/** L'encre principale de la barre : celle du texte de la page. */
@Composable
private fun paperInk(): Color = LocalPaperInk.current

/** Un fond pose sur le papier : l'encre a peine posee dessus. */
@Composable
private fun paperFill(alpha: Float): Color = LocalPaperInk.current.copy(alpha = alpha)

/**
 * L'accent, ajuste au papier.
 *
 * Le violet de l'application se perd sur un papier noir. On garde donc sa
 * teinte, mais on la ramene vers l'encre quand le papier est sombre : ce qui
 * est actif reste visible, quelle que soit la page.
 */
@Composable
private fun paperAccent(): Color {
    val accent = MaterialTheme.colorScheme.primary
    val dark = LocalPaperSurface.current.luminance() < 0.4f
    return if (dark) blendTowards(accent, Color.White, 0.45f) else accent
}

private fun blendTowards(from: Color, to: Color, amount: Float): Color = Color(
    red = from.red + (to.red - from.red) * amount,
    green = from.green + (to.green - from.green) * amount,
    blue = from.blue + (to.blue - from.blue) * amount,
    alpha = from.alpha,
)

/** Les trois façons de numeroter une liste. */
enum class ListMarker(val label: String, val sample: String, val marker: String) {
    BULLET("Liste à puces", "•", "• "),
    NUMBER("Liste numérotée", "1.", "1. "),
    LETTER("Liste à lettres", "a.", "a. "),
}

/**
 * La barre de mise en forme du journal.
 *
 * Elle ne garde que ce qu'on utilise en ecrivant : la photo, les quatre
 * effets de base, la couleur et le surlignage. Tout le reste — titres,
 * listes, polices — vit derriere le "+", un menu unique plutot que quatre
 * boutons de plus a caser. La barre tient ainsi sur une ligne, sans defiler.
 *
 * Les panneaux s'ouvrent a la place du clavier, jamais au-dessus : la hauteur
 * occupee en bas de l'ecran est la meme dans les deux cas, donc le texte ne
 * saute pas quand on passe de l'un a l'autre.
 */
@Composable
fun JournalToolbar(
    active: Set<TextStyleKind>,
    openPanel: ToolPanel?,
    onTogglePanel: (ToolPanel) -> Unit,
    onStyle: (TextStyleKind) -> Unit,
    /** L'alignement du bloc courant, et de quoi en changer. */
    align: BlockAlign,
    onAlign: (BlockAlign) -> Unit,
    onClearHeading: () -> Unit,
    /** Revient a la taille de base : « normale » est l'absence de style. */
    onClearSize: () -> Unit,
    onClearFont: () -> Unit,
    /** Retire le fond d'une citation : « sans fond » est l'absence de style. */
    onClearQuoteFill: () -> Unit,
    onList: (ListMarker) -> Unit,
    onAddPhoto: () -> Unit,
    /** Insere un `#` au curseur, et rouvre le clavier pour ecrire le mot. */
    onHashtag: () -> Unit,
    /** Vrai pendant qu'un vocal s'enregistre : le bouton dit alors « arrêter ». */
    recording: Boolean,
    onRecord: () -> Unit,
    photos: List<MediaItem>,
    photoFile: (MediaItem) -> File,
    onPickPhoto: (MediaItem) -> Unit,
    panelHeight: Dp,
    /**
     * Le papier de la page, et son encre.
     *
     * La barre d'outils est **posee sur la page**, pas a cote : sur un papier
     * noir, des boutons blancs du theme faisaient une bande lumineuse en bas de
     * l'ecran ; sur un papier creme, ils tranchaient en froid. Tout ce que la
     * barre dessine se teinte donc a partir de ces deux couleurs, et l'accent
     * de l'application ne sert plus qu'a dire ce qui est **actif**.
     */
    paper: Color,
    ink: Color,
) {
    CompositionLocalProvider(
        LocalPaperInk provides ink,
        LocalPaperSurface provides paper,
        // Et l'encre courante avec : sans ca, les lettres des boutons (« G »,
        // « I »…) et les icones gardent la couleur du theme, donc du sombre
        // sur le papier ardoise. Les fonds s'adaptaient, pas ce qu'il y a
        // dessus — c'est-a-dire la seule chose qu'on regarde.
        LocalContentColor provides ink,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = paperFill(0.12f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GroupButton(
                    label = "Tous les outils",
                    open = openPanel == ToolPanel.ALL,
                    marked = active.any {
                        it.family == StyleFamily.HEADING || it.family == StyleFamily.FONT
                    },
                    onClick = { onTogglePanel(ToolPanel.ALL) },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                }

                ToolButton(selected = false, label = "Ajouter une photo", onClick = onAddPhoto) {
                    Icon(
                        painterResource(R.drawable.ic_photo),
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                }

                // Le mot-cle n'est pas une mise en forme : c'est du texte, un
                // `#` de plus dans la phrase. Il a quand meme son bouton, parce
                // qu'une possibilite qu'on ne voit nulle part n'existe pas.
                // Le micro est dans la rangee principale et pas dans le menu :
                // un vocal se prend quand l'idee passe, pas apres deux appuis.
                ToolButton(
                    selected = recording,
                    label = if (recording) "Arrêter l'enregistrement" else "Enregistrer un vocal",
                    onClick = onRecord,
                ) {
                    if (recording) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE1483F)),
                        )
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_mic),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                ToolButton(selected = false, label = "Ajouter un mot-clé", onClick = onHashtag) {
                    Text("#", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }

                Separator()

                TextStyleKind.marks.forEach { style ->
                    ToolButton(
                        selected = style in active,
                        label = style.label,
                        onClick = { onStyle(style) },
                    ) { MarkGlyph(style) }
                }

                Separator()

                GroupButton(
                    label = ToolPanel.COLORS.label,
                    open = openPanel == ToolPanel.COLORS,
                    marked = active.any { it.family == StyleFamily.COLOR },
                    onClick = { onTogglePanel(ToolPanel.COLORS) },
                ) {
                    ColorGlyph(active.firstOrNull { it.family == StyleFamily.COLOR })
                }

                GroupButton(
                    label = ToolPanel.HIGHLIGHTS.label,
                    open = openPanel == ToolPanel.HIGHLIGHTS,
                    marked = active.any { it.family == StyleFamily.HIGHLIGHT },
                    onClick = { onTogglePanel(ToolPanel.HIGHLIGHTS) },
                ) {
                    val chosen = active.firstOrNull { it.family == StyleFamily.HIGHLIGHT }
                    Text(
                        "A",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(chosen?.argb ?: TextStyleKind.HIGHLIGHT.argb))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }

            if (openPanel != null) {
                HorizontalDivider(color = paperFill(0.12f))
                ToolPanelContent(
                    panel = openPanel,
                    active = active,
                    onStyle = onStyle,
                    align = align,
                    onAlign = onAlign,
                    onClearHeading = onClearHeading,
                    onClearSize = onClearSize,
                    onClearFont = onClearFont,
                    onClearQuoteFill = onClearQuoteFill,
                    onList = onList,
                    onAddPhoto = onAddPhoto,
                    photos = photos,
                    photoFile = photoFile,
                    onPickPhoto = onPickPhoto,
                    height = panelHeight,
                )
            }
        }
    }
}

@Composable
private fun ToolPanelContent(
    panel: ToolPanel,
    active: Set<TextStyleKind>,
    onStyle: (TextStyleKind) -> Unit,
    /** L'alignement du bloc courant, et de quoi en changer. */
    align: BlockAlign,
    onAlign: (BlockAlign) -> Unit,
    onClearHeading: () -> Unit,
    /** Revient a la taille de base : « normale » est l'absence de style. */
    onClearSize: () -> Unit,
    onClearFont: () -> Unit,
    onClearQuoteFill: () -> Unit,
    onList: (ListMarker) -> Unit,
    onAddPhoto: () -> Unit,
    photos: List<MediaItem>,
    photoFile: (MediaItem) -> File,
    onPickPhoto: (MediaItem) -> Unit,
    height: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(paperFill(0.05f))
            // Le panneau prend exactement la hauteur laissee libre par le
            // clavier qui s'en va : ouvert ou ferme, le bas de l'ecran occupe
            // la meme place, donc le texte au-dessus ne bouge pas d'un pixel.
            .heightIn(min = height, max = height)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        when (panel) {
            ToolPanel.ALL -> AllToolsPanel(
                active = active,
                onStyle = onStyle,
                align = align,
                onAlign = onAlign,
                onClearHeading = onClearHeading,
                onClearSize = onClearSize,
                onClearFont = onClearFont,
                onClearQuoteFill = onClearQuoteFill,
                onList = onList,
                onAddPhoto = onAddPhoto,
                photos = photos,
                photoFile = photoFile,
                onPickPhoto = onPickPhoto,
            )

            ToolPanel.COLORS -> {
                SectionLabel(panel.label)
                SwatchGrid(
                    styles = TextStyleKind.colors,
                    active = active,
                    onStyle = onStyle,
                    highlighted = false,
                )
            }

            ToolPanel.HIGHLIGHTS -> {
                SectionLabel(panel.label)
                SwatchGrid(
                    styles = TextStyleKind.highlights,
                    active = active,
                    onStyle = onStyle,
                    highlighted = true,
                )
            }
        }
    }
}

/**
 * Le menu du "+" : une liste unique, rangee par famille, ou l'on retrouve
 * tout ce qu'on peut poser dans la page. Chaque ligne se montre telle qu'elle
 * sera — un titre en grand, une police dans sa propre police — pour choisir en
 * voyant plutot qu'en lisant un nom.
 */
@Composable
private fun AllToolsPanel(
    active: Set<TextStyleKind>,
    onStyle: (TextStyleKind) -> Unit,
    /** L'alignement du bloc courant, et de quoi en changer. */
    align: BlockAlign,
    onAlign: (BlockAlign) -> Unit,
    onClearHeading: () -> Unit,
    /** Revient a la taille de base : « normale » est l'absence de style. */
    onClearSize: () -> Unit,
    onClearFont: () -> Unit,
    onClearQuoteFill: () -> Unit,
    onList: (ListMarker) -> Unit,
    onAddPhoto: () -> Unit,
    photos: List<MediaItem>,
    photoFile: (MediaItem) -> File,
    onPickPhoto: (MediaItem) -> Unit,
) {
    SectionLabel("Titres")
    TextStyleKind.headings.forEach { style ->
        PanelRow(
            label = style.label,
            selected = style in active,
            onClick = { onStyle(style) },
        ) {
            Text(
                "A",
                fontSize = when (style) {
                    TextStyleKind.TITLE_1 -> 22.sp
                    TextStyleKind.TITLE_2 -> 19.sp
                    else -> 16.sp
                },
                fontWeight = FontWeight.Bold,
            )
        }
    }
    PanelRow(
        label = "Texte normal",
        selected = active.none { it.family == StyleFamily.HEADING },
        onClick = onClearHeading,
    ) {
        Text("A", fontSize = 16.sp)
    }

    SectionLabel("Taille du texte")
    // A part des titres : ici on grossit un morceau de phrase sans en faire un
    // titre. Les deux ecarts restent modestes — une ligne du lignage fait
    // vingt-huit points, et un texte plus haut sortirait de ses lignes.
    TextStyleKind.sizes.forEach { style ->
        PanelRow(
            label = style.label,
            selected = style in active,
            onClick = { onStyle(style) },
        ) {
            Text(
                "A",
                fontSize = if (style == TextStyleKind.SIZE_SMALL) 12.sp else 19.sp,
            )
        }
    }
    PanelRow(
        label = "Taille normale",
        selected = active.none { it.family == StyleFamily.SIZE },
        onClick = onClearSize,
    ) {
        Text("A", fontSize = 16.sp)
    }

    SectionLabel("Alignement")
    BlockAlign.entries.forEach { option ->
        PanelRow(
            label = option.label,
            selected = align == option,
            onClick = { onAlign(option) },
        ) {
            AlignPreview(option)
        }
    }

    SectionLabel("Blocs")
    TextStyleKind.blocks.forEach { style ->
        PanelRow(
            label = style.label,
            // Un trait s'insere, il ne se coche pas : il n'y a rien a
            // « decocher » sur une ligne qu'on vient de creer.
            selected = !style.isRule && style in active,
            onClick = { onStyle(style) },
        ) {
            // Chaque ligne se montre telle qu'elle sera : un trait vertical
            // suivi de texte pour la citation, le trait lui-meme pour les
            // separations. On choisit en voyant, pas en lisant un nom.
            if (style.isRule) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ruleWidth(style))
                        .height(ruleThickness(style))
                        .clip(RoundedCornerShape(2.dp))
                        .background(paperFill(0.55f)),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(paperAccent()),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("A", fontSize = 15.sp, fontStyle = FontStyle.Italic)
                }
            }
        }
    }

    // Les reglages de la citation n'apparaissent que quand le curseur est
    // dedans : proposer la couleur d'un trait qui n'existe pas ne veut rien
    // dire, et ca allongerait le menu pour tout le monde.
    if (TextStyleKind.QUOTE in active) {
        SectionLabel("Trait de la citation")
        SwatchGrid(
            styles = TextStyleKind.quoteBars,
            active = active,
            onStyle = onStyle,
            highlighted = false,
        )
        SectionLabel("Fond de la citation")
        TextStyleKind.quoteFills.forEach { style ->
            PanelRow(
                label = style.label,
                selected = style in active,
                onClick = { onStyle(style) },
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            paperAccent().copy(
                                alpha = if (style == TextStyleKind.QUOTE_FILL_FULL) 0.30f else 0.12f
                            )
                        ),
                )
            }
        }
        PanelRow(
            label = "Sans fond",
            selected = active.none { it.family == StyleFamily.QUOTE_FILL },
            onClick = { onClearQuoteFill() },
        ) {
            Text("—", fontSize = 15.sp)
        }
    }

    SectionLabel("Listes")
    ListMarker.entries.forEach { marker ->
        PanelRow(label = marker.label, selected = false, onClick = { onList(marker) }) {
            Text(marker.sample, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }

    SectionLabel("Police")
    PanelRow(
        label = "Police par défaut",
        selected = active.none { it.family == StyleFamily.FONT },
        onClick = onClearFont,
    ) {
        Text("Aa", fontSize = 15.sp)
    }
    TextStyleKind.fonts.forEach { style ->
        PanelRow(
            label = style.label,
            selected = style in active,
            onClick = { onStyle(style) },
            labelFont = style,
        ) {
            Text("Aa", fontSize = 15.sp, fontFamily = style.fontFamily())
        }
    }

    SectionLabel("Ajouter")
    PanelRow(label = "Une photo ou une vidéo", selected = false, onClick = onAddPhoto) {
        Icon(
            painterResource(R.drawable.ic_photo),
            contentDescription = null,
            modifier = Modifier.size(19.dp),
        )
    }

    if (photos.isNotEmpty()) {
        // Une photo passee au fond est sous le texte : y toucher, c'est
        // toucher le texte, et aucun geste ne distingue les deux de facon
        // fiable. On la reprend donc par sa vignette.
        SectionLabel("Photos de la page")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            photos.forEach { photo ->
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LocalPaperSurface.current)
                        .clickable(onClickLabel = "Reprendre cette photo") { onPickPhoto(photo) },
                ) {
                    MediaImage(
                        file = photoFile(photo),
                        kind = photo.kind,
                        modifier = Modifier.fillMaxSize(),
                        maxSize = 256,
                    )
                }
            }
        }
    }
}

/**
 * Le bouton des couleurs : un "A" pose sur une barre de couleur, comme dans
 * les traitements de texte. Sans couleur choisie, la barre montre un degrade :
 * un "A" noir tout seul ne disait pas de quoi il s'agissait.
 */
@Composable
private fun ColorGlyph(chosen: TextStyleKind?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "A",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = chosen?.let { Color(it.argb) } ?: paperInk(),
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(19.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (chosen != null) {
                        SolidColor(Color(chosen.argb))
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                Color(TextStyleKind.COLOR_RED.argb),
                                Color(TextStyleKind.COLOR_AMBER.argb),
                                Color(TextStyleKind.COLOR_GREEN.argb),
                                Color(TextStyleKind.COLOR_BLUE.argb),
                                Color(TextStyleKind.COLOR_VIOLET.argb),
                            )
                        )
                    }
                ),
        )
    }
}

/**
 * Trois traits, calés comme le sera le texte.
 *
 * Dessinés plutôt qu'importés : le jeu d'icônes de base n'a pas les
 * alignements, et trois rectangles ne valent pas les mégaoctets du jeu complet.
 * Le troisième trait est plus court — c'est lui qui rend l'alignement visible,
 * puisque trois traits de même longueur se ressemblent quel que soit le côté.
 */
@Composable
private fun AlignPreview(align: BlockAlign) {
    val ink = paperFill(0.6f)
    Column(
        modifier = Modifier.width(18.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = when (align) {
            BlockAlign.START -> Alignment.Start
            BlockAlign.CENTER -> Alignment.CenterHorizontally
            BlockAlign.END -> Alignment.End
        },
    ) {
        listOf(1f, 0.7f, 1f, 0.5f).forEach { width ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(width)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(ink),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = paperFill(0.62f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
    )
}

/**
 * Les teintes en grille de deux, chacune avec son nom : un "A" de la couleur
 * qu'elle pose, pour voir le resultat avant de choisir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchGrid(
    styles: List<TextStyleKind>,
    active: Set<TextStyleKind>,
    onStyle: (TextStyleKind) -> Unit,
    highlighted: Boolean,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2,
    ) {
        styles.forEach { style ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (style in active) {
                            paperAccent().copy(alpha = 0.20f)
                        } else {
                            paperFill(0.05f)
                        }
                    )
                    .clickable(onClickLabel = style.label) { onStyle(style) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(26.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (highlighted) {
                        Text(
                            "A",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1B),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(style.argb))
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    } else {
                        Text(
                            "A",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(style.argb),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = style.label.removePrefix("Surligné ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PanelRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    /** Ecrit le nom dans la police qu'il designe, quand la ligne en propose une. */
    labelFont: TextStyleKind? = null,
    glyph: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    paperAccent().copy(alpha = 0.22f)
                } else {
                    paperFill(0.05f)
                }
            )
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) { glyph() }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = labelFont?.fontFamily(),
        )
    }
}

@Composable
private fun GroupButton(
    label: String,
    open: Boolean,
    marked: Boolean,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(BUTTON_SIZE)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    open -> paperAccent().copy(alpha = 0.30f)
                    marked -> paperAccent().copy(alpha = 0.16f)
                    else -> paperFill(0.08f)
                }
            )
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        glyph()
        if (open) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Fermer $label",
                tint = paperAccent(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(12.dp),
            )
        }
    }
}

@Composable
private fun Separator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(paperFill(0.20f)),
    )
}

@Composable
private fun ToolButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(BUTTON_SIZE)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    paperAccent().copy(alpha = 0.22f)
                } else {
                    paperFill(0.08f)
                }
            )
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Le bouton porte son propre effet : le gras est ecrit en gras. */
@Composable
private fun MarkGlyph(style: TextStyleKind) {
    Text(
        text = when (style) {
            TextStyleKind.BOLD -> "G"
            TextStyleKind.ITALIC -> "I"
            TextStyleKind.UNDERLINE -> "S"
            TextStyleKind.STRIKETHROUGH -> "B"
            else -> "A"
        },
        fontSize = 17.sp,
        fontWeight = if (style == TextStyleKind.BOLD) FontWeight.Bold else FontWeight.Medium,
        fontStyle = if (style == TextStyleKind.ITALIC) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when (style) {
            TextStyleKind.UNDERLINE -> TextDecoration.Underline
            TextStyleKind.STRIKETHROUGH -> TextDecoration.LineThrough
            else -> null
        },
    )
}
