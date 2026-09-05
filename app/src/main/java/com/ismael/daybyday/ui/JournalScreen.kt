package com.ismael.daybyday.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayCard
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

    // Le panneau d'outils prend la place du clavier : on retient la hauteur
    // que le clavier occupait pour que le texte ne bouge pas quand on echange
    // l'un pour l'autre.
    var openPanel by remember { mutableStateOf<ToolPanel?>(null) }
    val density = LocalDensity.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val navHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    var lastKeyboardHeight by remember { mutableStateOf(300.dp) }
    if (imeHeight > 120.dp) lastKeyboardHeight = imeHeight

    // Le panneau et le clavier n'avaient pas la meme taille, et l'ecran se
    // decalait a chaque bascule : le clavier recouvre la barre de navigation,
    // le panneau se posait au-dessus. La colonne retire deja le plus grand des
    // deux encarts du bas ; le panneau ne prend donc que ce qui manque pour
    // atteindre la hauteur du clavier. Comme le calcul suit l'animation du
    // clavier image par image, le panneau grandit exactement au rythme ou le
    // clavier s'en va : le total ne bouge jamais.
    val panelHeight = (lastKeyboardHeight - maxOf(imeHeight, navHeight)).coerceAtLeast(0.dp)

    // Meme chose dans l'autre sens : en refermant le panneau on garde sa place
    // au chaud, le temps que le clavier remonte la prendre.
    var awaitingKeyboard by remember { mutableStateOf(false) }
    if (imeHeight > 120.dp) awaitingKeyboard = false

    // Demander poliment au clavier de se cacher ne suffit pas : tant que le
    // champ garde le focus, Android le fait revenir. Le panneau et le clavier
    // s'empilaient donc, et la page sautait a chaque bascule. Retirer le focus
    // le ferme pour de bon ; le rendre le rouvre, curseur intact.
    val bodyFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun showPanel(panel: ToolPanel?) {
        openPanel = panel
        if (panel == null) {
            awaitingKeyboard = true
            runCatching { bodyFocus.requestFocus() }
        } else {
            awaitingKeyboard = false
            focusManager.clearFocus()
        }
    }

    // Si le clavier ne vient pas — focus refuse, clavier physique — la place
    // reservee ne doit pas rester vide indefiniment.
    LaunchedEffect(awaitingKeyboard) {
        if (awaitingKeyboard) {
            kotlinx.coroutines.delay(800)
            awaitingKeyboard = false
        }
    }

    val mediaItems by remember(date) { repository.observeMediaForDay(date) }
        .collectAsStateWithLifecycle(emptyList())
    val journalMedia = mediaItems.filter { it.cardKey == DayCard.JOURNAL.key }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isNotEmpty()) {
            app.appScope.launch {
                uris.forEach { uri -> repository.addMedia(date, uri, DayCard.JOURNAL) }
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
        if (hasSelection) {
            spans = RichText.toggle(spans, start, end, style)
            return
        }

        // Sans selection, un titre ne peut pas deviner ce qu'il doit habiller.
        // Il pose donc son propre exemple, deja selectionne : ecrire par-dessus
        // le remplace. Prendre la ligne entiere mettait tout un paragraphe en
        // titre des qu'il n'y avait pas de retour a la ligne.
        if (style.takesWholeLine) {
            val example = style.label
            val needsBreak = start > 0 && body.text.getOrNull(start - 1) != '\n'
            val prefix = if (needsBreak) "\n" else ""
            val inserted = prefix + example + "\n"
            val updated = body.text.substring(0, start) + inserted + body.text.substring(start)
            val from = start + prefix.length
            val to = from + example.length
            spans = RichText.applyAll(
                RichText.adjust(spans, body.text, updated),
                from,
                to,
                setOf(style),
            )
            body = body.copy(text = updated, selection = TextRange(from, to))
            return
        }

        typing = when {
            style in typing -> typing - style
            style.family != StyleFamily.MARK ->
                typing.filterNot { it.family == style.family }.toSet() + style
            else -> typing + style
        }
    }

    /** Repasse en texte normal la selection, ou la ligne du curseur. */
    fun clearHeading() {
        val line = RichText.lineRange(body.text, start, end)
        val from = if (hasSelection) start else line.first
        val to = if (hasSelection) end else line.last + 1
        spans = RichText.clearFamily(spans, from, to, StyleFamily.HEADING)
    }

    /** Revient a la police d'origine sur la selection, ou pour la suite tapee. */
    fun clearFont() {
        if (hasSelection) {
            spans = RichText.clearFamily(spans, start, end, StyleFamily.FONT)
        } else {
            typing = typing.filterNot { it.family == StyleFamily.FONT }.toSet()
        }
    }

    /** Ajoute une puce, un numero ou une lettre en tete de ligne : du vrai texte. */
    fun prefixLine(marker: String) {
        val line = RichText.lineRange(body.text, start, end)
        val at = line.first
        val updated = body.text.substring(0, at) + marker + body.text.substring(at)
        spans = RichText.adjust(spans, body.text, updated)
        body = body.copy(
            text = updated,
            selection = TextRange(
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
                .padding(top = innerPadding.calculateTopPadding())
                // Le clavier contient deja la barre de navigation : prendre le
                // plus grand des deux, pas leur somme, sinon une bande vide
                // reste entre la barre d'outils et le clavier.
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
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
                    .selectWordOnDoubleTap({ title }) { title = title.copy(selection = it) }
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
                    .focusRequester(bodyFocus)
                    .selectWordOnDoubleTap({ body }) { body = body.copy(selection = it) }
                    .testTag("day-note-field"),
            )

            if (journalMedia.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    journalMedia.forEach { item ->
                        MediaThumb(
                            item = item,
                            onClick = { viewerIndex = journalMedia.indexOf(item) },
                            modifier = Modifier.width(96.dp),
                        )
                    }
                }
            }

            JournalToolbar(
                active = active,
                openPanel = openPanel,
                onTogglePanel = { panel ->
                    // Le panneau prend la place du clavier : l'un se ferme pour
                    // que l'autre s'ouvre, et la hauteur totale ne bouge pas.
                    showPanel(if (openPanel == panel) null else panel)
                },
                onStyle = { style ->
                    applyStyle(style)
                    if (openPanel != null) showPanel(null)
                },
                onClearHeading = {
                    clearHeading()
                    showPanel(null)
                },
                onClearFont = {
                    clearFont()
                    showPanel(null)
                },
                onList = { marker ->
                    prefixLine(marker.marker)
                    showPanel(null)
                },
                onAddPhoto = {
                    // On referme le panneau sans rendre le focus : le
                    // selecteur de photos passe devant, inutile de rappeler le
                    // clavier juste avant.
                    openPanel = null
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                panelHeight = panelHeight,
            )

            // Le clavier remonte : on tient sa place jusqu'a ce qu'il y soit.
            if (openPanel == null && awaitingKeyboard && panelHeight > 0.dp) {
                Spacer(Modifier.height(panelHeight))
            }
        }
    }

    val index = viewerIndex
    if (index != null && index in journalMedia.indices) {
        MediaViewerDialog(
            items = journalMedia,
            startIndex = index,
            onDismiss = { viewerIndex = null },
            onDelete = { item ->
                viewerIndex = null
                app.appScope.launch { repository.deleteMedia(item) }
            },
        )
    }
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
