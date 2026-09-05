package com.ismael.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * L'ecran d'ecriture du journal : rien d'autre que la page du jour.
 *
 * Le journal vivait dans une carte, au milieu d'un ecran qui defilait deja.
 * Trois consequences : la barre de mise en forme partait vers le haut des
 * qu'on ecrivait un peu, le clavier laissait une bande vide, et il ne restait
 * qu'une lucarne pour ecrire. Ici la page entiere est a l'ecriture, et la
 * barre reste collee au clavier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(date: LocalDate, onBack: () -> Unit) {
    val app = LocalContext.current.dayByDayApp
    val repository = app.repository

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var spans by remember { mutableStateOf<List<TextSpan>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(date) {
        val entry = repository.dayOnce(date)
        title = TextFieldValue(entry?.title.orEmpty())
        body = TextFieldValue(entry?.note.orEmpty())
        spans = RichText.decode(entry?.noteSpans, entry?.note?.length ?: 0)
        loaded = true
    }

    // Enregistrement au depart de l'ecran : on ne touche qu'au journal, le
    // reste de la journee est relu au moment d'ecrire pour ne rien ecraser.
    val current = rememberUpdatedState(Triple(title.text, body.text, spans))
    DisposableEffect(date, loaded) {
        onDispose {
            if (!loaded) return@onDispose
            val (savedTitle, savedBody, savedSpans) = current.value
            app.appScope.launch {
                val existing = repository.dayOnce(date)
                    ?: com.ismael.daybyday.data.DayEntry(epochDay = date.toEpochDay())
                repository.saveDay(
                    existing.copy(
                        title = savedTitle.trim(),
                        note = savedBody,
                        noteSpans = RichText.encode(savedSpans),
                    )
                )
            }
        }
    }

    val selection = body.selection
    val start = minOf(selection.start, selection.end)
    val end = maxOf(selection.start, selection.end)
    val hasSelection = end > start

    // Le style courant du curseur : il suit la frappe, ce qui permet de
    // continuer a ecrire en gras apres un mot en gras, et d'appuyer sur "gras"
    // avant d'ecrire. Un seul mecanisme, donc pas de desaccord possible.
    var typing by remember { mutableStateOf<Set<TextStyleKind>>(emptySet()) }

    val active = if (hasSelection) RichText.stylesOn(spans, start, end) else typing

    fun applyStyle(style: TextStyleKind) {
        if (style.takesWholeLine) {
            val line = RichText.lineRange(body.text, start, end)
            spans = RichText.toggle(spans, line.first, line.last + 1, style)
            return
        }
        if (hasSelection) {
            spans = RichText.toggle(spans, start, end, style)
            return
        }
        typing = when {
            style in typing -> typing - style
            style.family != StyleFamily.MARK ->
                typing.filterNot { it.family == style.family }.toSet() + style
            else -> typing + style
        }
    }

    /** Ajoute une puce ou un numero en tete de ligne : du vrai texte, pas un décor. */
    fun prefixLine(marker: String) {
        val line = RichText.lineRange(body.text, start, end)
        val at = line.first
        val updated = body.text.substring(0, at) + marker + body.text.substring(at)
        spans = RichText.adjust(spans, body.text, updated)
        body = body.copy(
            text = updated,
            selection = androidx.compose.ui.text.TextRange(
                (body.selection.start + marker.length).coerceAtMost(updated.length)
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Dates.dayMedium(date)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { field ->
                    Box {
                        if (title.text.isEmpty()) {
                            Text(
                                "Titre de la journée",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        field()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("day-title-field"),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            BasicTextField(
                value = body,
                onValueChange = { updated ->
                    if (updated.text == body.text) {
                        // Deplacement du curseur : on adopte le style de
                        // l'endroit ou il arrive, comme un traitement de texte.
                        val caret = updated.selection.start
                        typing = RichText.stylesOn(spans, caret, caret)
                        body = updated
                        return@BasicTextField
                    }

                    val moved = RichText.adjust(spans, body.text, updated.text)
                    val edit = RichText.diff(body.text, updated.text)
                    spans = if (edit.newEnd > edit.start && typing.isNotEmpty()) {
                        RichText.applyAll(moved, edit.start, edit.newEnd, typing)
                    } else {
                        moved
                    }
                    body = updated
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 26.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = remember(spans) { SpanTransformation(spans) },
                decorationBox = { field ->
                    Box {
                        if (body.text.isEmpty()) {
                            Text(
                                "Écris ce que tu veux, comme tu veux.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        field()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag("day-note-field"),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            FormattingBar(
                active = active,
                onStyle = ::applyStyle,
                onBullet = { prefixLine("• ") },
                onNumber = { prefixLine("1. ") },
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

/**
 * La barre de mise en forme, collee au clavier. Elle defile horizontalement :
 * il y a plus d'outils que de largeur d'ecran, et les empiler sur deux lignes
 * mangerait la place d'ecriture.
 */
@Composable
private fun FormattingBar(
    active: Set<TextStyleKind>,
    onStyle: (TextStyleKind) -> Unit,
    onBullet: () -> Unit,
    onNumber: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextStyleKind.marks.forEach { style ->
            ToolButton(selected = style in active, label = style.label, onClick = { onStyle(style) }) {
                MarkGlyph(style)
            }
        }

        Separator()

        TextStyleKind.headings.forEach { style ->
            ToolButton(selected = style in active, label = style.label, onClick = { onStyle(style) }) {
                Text(
                    text = "T${style.ordinal - TextStyleKind.TITLE_1.ordinal + 1}",
                    fontWeight = FontWeight.Bold,
                    fontSize = (17 - (style.ordinal - TextStyleKind.TITLE_1.ordinal)).sp,
                )
            }
        }

        ToolButton(selected = false, label = "Liste à puces", onClick = onBullet) {
            Text("•—", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        ToolButton(selected = false, label = "Liste numérotée", onClick = onNumber) {
            Text("1—", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Separator()

        TextStyleKind.colors.forEach { style ->
            ToolButton(selected = style in active, label = style.label, onClick = { onStyle(style) }) {
                // La lettre elle-meme porte la teinte : on voit ce qu'on obtient.
                Text(
                    "A",
                    color = Color(style.argb),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Separator()

        TextStyleKind.highlights.forEach { style ->
            ToolButton(selected = style in active, label = style.label, onClick = { onStyle(style) }) {
                // Un "A" reellement surligne : l'icone montre son effet.
                Text(
                    text = "A",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1B),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(style.argb))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
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
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
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

/** L'apercu du journal sur la carte de la journee : lecture seule. */
@Composable
fun JournalPreview(
    title: String,
    body: String,
    spans: List<TextSpan>,
    onOpen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClickLabel = "Ouvrir le journal", onClick = onOpen)
            .padding(vertical = 4.dp)
            .testTag("journal-preview"),
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
        }

        if (body.isBlank()) {
            Text(
                text = "Rien d'écrit pour l'instant. Appuie pour ouvrir ta page.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = buildAnnotatedStringWithSpans(body, spans),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 8,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Appuie pour lire et écrire en grand",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
