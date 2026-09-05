package com.ismael.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind

/**
 * Le champ d'ecriture du journal, avec sa barre de mise en forme.
 *
 * La mise en forme est purement decorative : elle n'ajoute ni ne retire un
 * seul caractere. C'est ce qui permet de la poser avec une simple
 * [VisualTransformation] et une correspondance de positions identique — le
 * curseur, la selection et le clavier continuent de travailler sur le texte
 * brut, sans decalage possible.
 */
@Composable
fun JournalEditor(
    value: TextFieldValue,
    spans: List<TextSpan>,
    onValueChange: (TextFieldValue, List<TextSpan>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Styles choisis alors que rien n'est selectionne : ils habilleront les
    // prochains caracteres tapes. Sans cela, appuyer sur "gras" puis ecrire
    // ne ferait rien, et il faudrait ecrire avant de mettre en forme.
    var pending by remember { mutableStateOf<Set<TextStyleKind>>(emptySet()) }

    val selection = value.selection
    val start = minOf(selection.start, selection.end)
    val end = maxOf(selection.start, selection.end)
    val hasSelection = end > start

    val active = if (hasSelection) {
        RichText.stylesOn(spans, start, end)
    } else {
        RichText.stylesOn(spans, start, start) + pending
    }

    fun toggle(style: TextStyleKind) {
        if (hasSelection) {
            onValueChange(value, RichText.toggle(spans, start, end, style))
            return
        }
        // Sans selection, on note l'intention pour la suite de la frappe.
        pending = when {
            style in pending -> pending - style
            style.isColor -> pending.filterNot { it.isColor }.toSet() + style
            else -> pending + style
        }
    }

    JournalToolbar(active = active, onToggle = ::toggle)

    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = { updated ->
            if (updated.text == value.text) {
                // Simple deplacement du curseur : l'intention de mise en forme
                // ne suit pas ailleurs dans le texte.
                if (updated.selection.start != value.selection.start) pending = emptySet()
                onValueChange(updated, spans)
                return@OutlinedTextField
            }

            val moved = RichText.adjust(spans, value.text, updated.text)
            val edit = RichText.diff(value.text, updated.text)
            val continued = RichText.stylesOn(spans, value.selection.start, value.selection.start)
            val toApply = pending + continued

            val decorated = if (edit.newEnd > edit.start && toApply.isNotEmpty()) {
                RichText.applyAll(moved, edit.start, edit.newEnd, toApply)
            } else {
                moved
            }
            onValueChange(updated, decorated)
        },
        label = { Text("Ce que tu as vécu") },
        placeholder = { Text("Écris ce que tu veux, comme tu veux.") },
        shape = RoundedCornerShape(16.dp),
        visualTransformation = remember(spans) { SpanTransformation(spans) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp),
    )
}

/**
 * Pose les intervalles sur le texte affiche. Les positions ne changent pas,
 * donc la correspondance est l'identite : aucun risque de curseur decale.
 */
private class SpanTransformation(private val spans: List<TextSpan>) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (spans.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val styled = buildAnnotatedStringWithSpans(text.text, spans)
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

/** Le texte habille de ses intervalles, pour l'edition comme pour la relecture. */
fun buildAnnotatedStringWithSpans(text: String, spans: List<TextSpan>): AnnotatedString =
    AnnotatedString(
        text = text,
        spanStyles = spans
            .filter { it.start < text.length && it.end <= text.length && !it.isEmpty }
            .map { span ->
                AnnotatedString.Range(span.style.toSpanStyle(), span.start, span.end)
            },
    )

/**
 * Les teintes sont choisies moyennes exprès : elles restent lisibles sur fond
 * clair comme sur fond sombre, sans avoir a suivre le theme.
 */
private fun TextStyleKind.toSpanStyle(): SpanStyle = when (this) {
    TextStyleKind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    TextStyleKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    TextStyleKind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    TextStyleKind.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    TextStyleKind.HIGHLIGHT -> SpanStyle(background = Color(0x66FFD54F))
    TextStyleKind.COLOR_RED -> SpanStyle(color = Color(0xFFE1483F))
    TextStyleKind.COLOR_ORANGE -> SpanStyle(color = Color(0xFFF2A93B))
    TextStyleKind.COLOR_GREEN -> SpanStyle(color = Color(0xFF3FBF6A))
    TextStyleKind.COLOR_BLUE -> SpanStyle(color = Color(0xFF4A9BE8))
    TextStyleKind.COLOR_VIOLET -> SpanStyle(color = Color(0xFF9B6BD6))
}

/** La couleur d'une puce de teinte, ou null pour une mise en forme sans couleur. */
private fun TextStyleKind.swatch(): Color? =
    toSpanStyle().color.takeIf { it != Color.Unspecified }

@Composable
private fun JournalToolbar(active: Set<TextStyleKind>, onToggle: (TextStyleKind) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextStyleKind.marks.forEach { style ->
            MarkButton(
                style = style,
                selected = style in active,
                onClick = { onToggle(style) },
            )
        }

        Spacer(Modifier.width(4.dp))

        TextStyleKind.colors.forEach { style ->
            ColorButton(
                color = style.swatch() ?: MaterialTheme.colorScheme.onSurface,
                label = style.label,
                selected = style in active,
                onClick = { onToggle(style) },
            )
        }
    }
}

@Composable
private fun MarkButton(style: TextStyleKind, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                }
            )
            .clickable(onClickLabel = style.label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Le bouton porte son propre effet : le gras est ecrit en gras.
        Text(
            text = when (style) {
                TextStyleKind.BOLD -> "G"
                TextStyleKind.ITALIC -> "I"
                TextStyleKind.UNDERLINE -> "S"
                TextStyleKind.STRIKETHROUGH -> "B"
                else -> "‎▮"
            },
            fontSize = 16.sp,
            fontWeight = if (style == TextStyleKind.BOLD) FontWeight.Bold else FontWeight.Medium,
            fontStyle = if (style == TextStyleKind.ITALIC) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when (style) {
                TextStyleKind.UNDERLINE -> TextDecoration.Underline
                TextStyleKind.STRIKETHROUGH -> TextDecoration.LineThrough
                else -> null
            },
            color = if (style == TextStyleKind.HIGHLIGHT) {
                Color(0xFFFFD54F)
            } else if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun ColorButton(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                }
            )
            .clickable(onClickLabel = label, onClick = onClick)
            // La puce n'a pas de texte : sans cela, elle serait annoncee
            // "bouton" sans dire laquelle.
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 22.dp else 18.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
