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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextStyleKind

/**
 * Taille des boutons de la barre. Neuf tiennent ainsi sur la largeur d'un
 * telephone courant, sans que le dernier soit rogne.
 */
private val BUTTON_SIZE = 38.dp

/** Les panneaux qui peuvent s'ouvrir sous la barre, a la place du clavier. */
enum class ToolPanel(val label: String) {
    HEADINGS("Titres"),
    LISTS("Listes"),
    COLORS("Couleur du texte"),
    HIGHLIGHTS("Surlignage"),
}

/** Les trois façons de numeroter une liste. */
enum class ListMarker(val label: String, val sample: String, val marker: String) {
    BULLET("Liste à puces", "•", "• "),
    NUMBER("Liste numérotée", "1.", "1. "),
    LETTER("Liste à lettres", "a.", "a. "),
}

/**
 * La barre de mise en forme du journal.
 *
 * Tout etait aligne sur une seule ligne defilante : il fallait pousser
 * jusqu'au bout pour atteindre une couleur. Les outils nombreux sont donc
 * ranges par famille derriere un bouton, et leur panneau s'ouvre a la place du
 * clavier plutot qu'en poussant le texte vers le haut. La place a l'ecran est
 * la meme, ouverte ou fermee.
 */
@Composable
fun JournalToolbar(
    active: Set<TextStyleKind>,
    openPanel: ToolPanel?,
    onTogglePanel: (ToolPanel) -> Unit,
    onStyle: (TextStyleKind) -> Unit,
    onClearHeading: () -> Unit,
    onList: (ListMarker) -> Unit,
    onAddPhoto: () -> Unit,
    panelHeight: androidx.compose.ui.unit.Dp,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton(selected = false, label = "Ajouter une photo", onClick = onAddPhoto) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
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
                panel = ToolPanel.HEADINGS,
                open = openPanel == ToolPanel.HEADINGS,
                marked = active.any { it.family == StyleFamily.HEADING },
                onClick = { onTogglePanel(ToolPanel.HEADINGS) },
            ) {
                Text("T", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            GroupButton(
                panel = ToolPanel.LISTS,
                open = openPanel == ToolPanel.LISTS,
                marked = false,
                onClick = { onTogglePanel(ToolPanel.LISTS) },
            ) {
                Text("•—", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            GroupButton(
                panel = ToolPanel.COLORS,
                open = openPanel == ToolPanel.COLORS,
                marked = active.any { it.family == StyleFamily.COLOR },
                onClick = { onTogglePanel(ToolPanel.COLORS) },
            ) {
                val chosen = active.firstOrNull { it.family == StyleFamily.COLOR }
                Text(
                    "A",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = chosen?.let { Color(it.argb) } ?: MaterialTheme.colorScheme.onSurface,
                )
            }

            GroupButton(
                panel = ToolPanel.HIGHLIGHTS,
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ToolPanelContent(
                panel = openPanel,
                active = active,
                onStyle = onStyle,
                onClearHeading = onClearHeading,
                onList = onList,
                height = panelHeight,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolPanelContent(
    panel: ToolPanel,
    active: Set<TextStyleKind>,
    onStyle: (TextStyleKind) -> Unit,
    onClearHeading: () -> Unit,
    onList: (ListMarker) -> Unit,
    height: androidx.compose.ui.unit.Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            // Le panneau prend la hauteur du clavier qu'il remplace : le texte
            // au-dessus ne bouge pas quand on l'ouvre ou qu'on le ferme.
            .heightIn(min = height, max = height)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = panel.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        when (panel) {
            ToolPanel.HEADINGS -> {
                PanelRow(
                    label = "Texte normal",
                    selected = active.none { it.family == StyleFamily.HEADING },
                    onClick = onClearHeading,
                ) {
                    Text("A", fontSize = 16.sp)
                }
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
            }

            ToolPanel.LISTS -> ListMarker.entries.forEach { marker ->
                PanelRow(label = marker.label, selected = false, onClick = { onList(marker) }) {
                    Text(marker.sample, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            ToolPanel.COLORS -> SwatchGrid(
                styles = TextStyleKind.colors,
                active = active,
                onStyle = onStyle,
                highlighted = false,
            )

            ToolPanel.HIGHLIGHTS -> SwatchGrid(
                styles = TextStyleKind.highlights,
                active = active,
                onStyle = onStyle,
                highlighted = true,
            )
        }
    }
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surface
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
    glyph: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) { glyph() }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun GroupButton(
    panel: ToolPanel,
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
                    open -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    marked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClickLabel = panel.label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        glyph()
        if (open) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Fermer ${panel.label}",
                tint = MaterialTheme.colorScheme.primary,
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
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
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
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
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
