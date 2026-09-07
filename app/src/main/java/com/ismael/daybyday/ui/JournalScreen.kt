package com.ismael.daybyday.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.BlockKind
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.JournalBlocks
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaLayer
import com.ismael.daybyday.data.PageLink
import com.ismael.daybyday.data.Placement
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import com.ismael.daybyday.data.VoiceNote
import com.ismael.daybyday.R
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * L'ecran d'ecriture du journal : rien d'autre que la page du jour.
 *
 * **La page est une suite de blocs.** Elle etait un seul long champ de texte,
 * avec les vocaux ranges dessous et les citations dessinees a partir de la
 * mise en page. Trois choses en decoulaient, et c'etaient les trois reproches :
 * rien ne pouvait se glisser entre deux paragraphes, il n'y avait rien a
 * attraper pour deplacer quoi que ce soit, et un trait de citation dessine a
 * partir d'un intervalle de caracteres n'appartenait a personne. Un bloc, lui,
 * est une chose : il a une place, une hauteur, un bord, et on peut le prendre.
 *
 * **Ecrire n'a pas change pour autant.** Un bloc de texte n'est pas un
 * paragraphe : Entree fait un retour a la ligne comme partout, et une page
 * ordinaire n'a qu'un seul champ. Un deuxieme bloc n'apparait que quand on
 * pose une citation, un trait ou un vocal au milieu du texte.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun JournalScreen(
    date: LocalDate,
    onBack: () -> Unit,
    /** Ouvre la journee visee par un lien ecrit dans la page. */
    onOpenDay: (LocalDate) -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.dayByDayApp
    val repository = app.repository
    val epochDay = date.toEpochDay()

    var title by remember { mutableStateOf(TextFieldValue("")) }
    var blocks by remember { mutableStateOf(listOf(PageBlocks.text())) }
    var focusedKey by remember { mutableStateOf<Long?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val history = remember { PageHistory() }

    LaunchedEffect(date) {
        val entry = repository.dayOnce(date)
        title = TextFieldValue(entry?.title.orEmpty())
        val stored = repository.blocksOnce(date)
        val voices = repository.voiceNotesOnce(date)
        // Une page qui a du texte mais pas de blocs ne devrait pas exister — la
        // migration les a tous decoupes. Si ca arrive quand meme, on decoupe a
        // l'ouverture plutot que de montrer une page vide sur un texte qui est
        // bien la.
        val base = stored.ifEmpty {
            val text = entry?.note.orEmpty()
            if (text.isEmpty()) {
                emptyList()
            } else {
                JournalBlocks.split(text, RichText.decode(entry?.noteSpans, text.length), epochDay)
            }
        }
        blocks = PageBlocks.tidy(
            PageBlocks.from(JournalBlocks.reconcile(base, voices.map { it.id }, epochDay))
        )
        history.reset()
        loaded = true
    }

    // Enregistrement au depart de l'ecran : on ne touche qu'au journal, le
    // reste de la journee est relu au moment d'ecrire pour ne rien ecraser.
    val current = rememberUpdatedState(title.text to blocks)
    DisposableEffect(date, loaded) {
        onDispose {
            if (!loaded) return@onDispose
            val (savedTitle, savedBlocks) = current.value
            app.appScope.launch {
                repository.saveJournal(
                    date = date,
                    title = savedTitle.trim(),
                    blocks = PageBlocks.toStored(PageBlocks.tidy(savedBlocks), epochDay),
                )
            }
        }
    }

    // --- Le clavier et le panneau d'outils --------------------------------
    var openPanel by remember { mutableStateOf<ToolPanel?>(null) }
    val density = LocalDensity.current
    val imeHeight = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val navHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    // La hauteur du clavier, retenue pour que le panneau prenne exactement sa
    // place. On garde le **maximum** vu depuis l'ouverture de l'ecran : en se
    // fermant, l'encart du clavier passe par toutes les valeurs
    // intermediaires, et retenir la derniere au-dessus d'un seuil gardait le
    // seuil — le panneau retrecissait a chaque aller-retour.
    var measuredKeyboard by remember { mutableStateOf(0.dp) }
    if (imeHeight > measuredKeyboard) measuredKeyboard = imeHeight
    val keyboardHeight = if (measuredKeyboard > 150.dp) measuredKeyboard else 300.dp
    val panelHeight = (keyboardHeight - maxOf(imeHeight, navHeight)).coerceAtLeast(0.dp)

    var awaitingKeyboard by remember { mutableStateOf(false) }
    if (imeHeight > 150.dp) awaitingKeyboard = false

    val focusManager = LocalFocusManager.current
    // Le bloc a qui rendre le focus : apres un decoupage, apres un « annuler »,
    // apres avoir pose une citation. Un `LaunchedEffect` par bloc le reclame
    // quand son tour vient.
    var pendingFocus by remember { mutableStateOf<Long?>(null) }

    // Retirer le focus ferme le clavier, mais efface aussi la selection : on ne
    // pouvait donc pas colorer un texte deja ecrit. La selection est mise de
    // cote ici avant de lacher le focus, et c'est elle qui sert de cible tant
    // qu'un panneau est ouvert.
    var heldSelection by remember { mutableStateOf<TextRange?>(null) }

    fun focusedBlock(): PageBlock? = blocks.firstOrNull { it.key == focusedKey }

    fun showPanel(panel: ToolPanel?) {
        if (panel == null) {
            openPanel = null
            heldSelection?.let { held ->
                val key = focusedKey
                blocks = blocks.map {
                    if (it.key == key) it.copy(value = it.value.copy(selection = held)) else it
                }
            }
            heldSelection = null
            awaitingKeyboard = true
            pendingFocus = focusedKey
        } else {
            heldSelection = (heldSelection ?: focusedBlock()?.value?.selection)
                ?.takeIf { it.start != it.end }
            openPanel = panel
            awaitingKeyboard = false
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(awaitingKeyboard) {
        if (awaitingKeyboard) {
            kotlinx.coroutines.delay(800)
            awaitingKeyboard = false
        }
    }

    // --- L'allure de la page, gardee d'une journee a l'autre --------------
    val prefs = app.prefs
    var ruled by remember { mutableStateOf(prefs.journalRuled) }
    var paperIndex by remember { mutableStateOf(prefs.journalPaperIndex) }
    var lineIndex by remember { mutableStateOf(prefs.journalLineIndex) }
    var snapToGrid by remember { mutableStateOf(prefs.journalSnapToGrid) }
    var fontCode by remember { mutableStateOf(prefs.journalFontCode) }
    var textSize by remember { mutableStateOf(prefs.journalTextSize) }
    var showPaperSettings by remember { mutableStateOf(false) }
    val paper = JournalPaper.paper(paperIndex)
    val ink = JournalPaper.ink(paper)
    val accent = MaterialTheme.colorScheme.primary
    val rhythm = with(density) { JournalPaper.LINE_SPACING.toSp() }
    val baseFont = JournalPaper.font(fontCode).fontFamily()
    val pageStyle = PageStyle(
        ink = ink,
        paper = paper,
        accent = accent,
        baseFont = baseFont,
        textSize = textSize,
        rhythm = rhythm,
    )

    // --- Les photos posees sur la page ------------------------------------
    val pageScroll = rememberScrollState()
    val mediaItems by remember(date) { repository.observeMediaForDay(date) }
        .collectAsStateWithLifecycle(emptyList())
    val storedMedia = mediaItems.filter { it.cardKey == DayCard.JOURNAL.key }
    var pageWidth by remember { mutableStateOf(0f) }
    var selectedPhotoId by remember { mutableStateOf<Long?>(null) }
    var draft by remember { mutableStateOf<MediaItem?>(null) }
    val journalMedia = storedMedia.map { item ->
        if (item.id == draft?.id) draft ?: item else item
    }
    val selectedPhoto = journalMedia.firstOrNull { it.id == selectedPhotoId }
    val clipboard = LocalClipboardManager.current

    val pendingDraft = draft
    LaunchedEffect(pendingDraft) {
        if (pendingDraft != null) {
            kotlinx.coroutines.delay(350)
            repository.updateMedia(pendingDraft)
        }
    }

    fun selectPhoto(id: Long?) {
        selectedPhotoId = id
        draft = null
        if (id != null) {
            openPanel = null
            heldSelection = null
            awaitingKeyboard = false
            focusManager.clearFocus()
        }
    }

    fun changePhoto(item: MediaItem) {
        draft = null
        app.appScope.launch { repository.updateMedia(item) }
    }

    val unplacedCount = storedMedia.count { !it.isPlaced }
    LaunchedEffect(unplacedCount, pageWidth) {
        if (pageWidth <= 0f || unplacedCount == 0) return@LaunchedEffect
        val placedAlready = storedMedia.count { it.isPlaced }
        val topY = with(density) { pageScroll.value.toDp().value } + 32f
        storedMedia.filter { !it.isPlaced }.forEachIndexed { index, item ->
            val ratio = MediaLoader.aspectRatio(
                repository.media.file(item.relativePath),
                item.kind,
            )
            repository.updateMedia(
                Placement.autoPlace(item, placedAlready + index, pageWidth, topY, ratio)
            )
        }
        selectPhoto(storedMedia.last { !it.isPlaced }.id)
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        if (uris.isNotEmpty()) {
            app.appScope.launch {
                uris.forEach { uri -> repository.addMedia(date, uri, DayCard.JOURNAL) }
            }
        }
    }

    // --- Modifier la page -------------------------------------------------

    /**
     * Le style courant du curseur : il suit la frappe, ce qui permet de
     * continuer a ecrire en gras apres un mot en gras, et d'appuyer sur
     * « gras » avant d'ecrire. Un seul mecanisme, donc pas de desaccord.
     */
    var typing by remember { mutableStateOf<Set<TextStyleKind>>(emptySet()) }

    fun snapshot(): PageSnapshot = PageSnapshot(title.text, blocks)

    /**
     * Pose la page, en retenant celle d'avant.
     *
     * Tout passe par ici — c'est ce qui rend « annuler » complet sans avoir a y
     * penser a chaque endroit. [structural] dit qu'il ne s'agit pas d'une
     * lettre tapee mais d'un bloc pose, deplace ou efface : ces pas-la ne se
     * fondent jamais dans le precedent.
     */
    fun setBlocks(structural: Boolean, updated: List<PageBlock>) {
        history.record(snapshot(), structural)
        blocks = updated
    }

    /** Ce que le champ d'un bloc vient de rendre : texte, curseur, styles. */
    fun changeBlock(key: Long, updated: TextFieldValue) {
        val index = blocks.indexOfFirst { it.key == key }
        if (index < 0) return
        val block = blocks[index]

        // L'utilisateur reprend la main : la selection mise de cote pour le
        // panneau n'a plus lieu d'etre. Mais seulement panneau ferme — en
        // perdant le focus, le champ annonce lui-meme une selection vide, et il
        // ne faut pas la prendre pour un geste.
        if (openPanel == null) heldSelection = null

        if (updated.text == block.text) {
            // Deplacement du curseur : on adopte le style de l'endroit ou il
            // arrive, comme un traitement de texte.
            val caret = updated.selection.start
            typing = RichText.stylesOn(block.spans, caret, caret)
            blocks = blocks.toMutableList().also { it[index] = block.copy(value = updated) }
            return
        }

        val edit = RichText.diff(block.text, updated.text)

        // Entree : le paragraphe se coupe en deux. C'est ce qui permet de
        // fabriquer un deuxieme paragraphe, donc d'en deplacer un seul. On le
        // reconnait au fait que **exactement** un retour a la ligne vient
        // d'etre ecrit — coller un texte qui en contient plusieurs reste du
        // texte colle.
        if (updated.text.substring(edit.start, edit.newEnd) == "\n" && block.isText) {
            history.record(snapshot(), structural = true)
            val (split, next) = PageBlocks.splitAt(blocks, key, edit.start)
            blocks = split
            pendingFocus = next
            return
        }

        history.record(snapshot(), structural = false)
        val moved = RichText.adjust(block.spans, block.text, updated.text)
        val spans = if (edit.newEnd > edit.start && typing.isNotEmpty()) {
            RichText.applyAll(moved, edit.start, edit.newEnd, typing)
        } else {
            moved
        }
        blocks = blocks.toMutableList().also {
            it[index] = block.copy(value = updated, spans = spans)
        }
    }

    /**
     * Effacer au tout debut d'un bloc : il rejoint celui d'au-dessus.
     *
     * Sans ca, la page ne ferait que se decouper : on pourrait creer des
     * paragraphes a l'infini sans jamais pouvoir en recoller deux.
     */
    fun backspaceAtStart(key: Long): Boolean {
        val index = blocks.indexOfFirst { it.key == key }
        if (index <= 0) return false
        val previous = blocks[index - 1]

        // Au-dessus, autre chose que du texte — un vocal, un trait, une
        // citation : on ne le happe pas dans le paragraphe. Effacer par
        // megarde un enregistrement en tapant sur la touche d'effacement
        // serait le pire des raccourcis.
        if (!previous.isText) return false

        history.record(snapshot(), structural = true)
        val (merged, target) = PageBlocks.mergeBack(blocks, key)
        if (target == null) return false
        blocks = merged
        pendingFocus = target
        return true
    }

    /** Pose un bloc au curseur, en coupant le paragraphe en deux s'il le faut. */
    fun insertBlock(inserted: PageBlock, focusIt: Boolean = false) {
        setBlocks(structural = true, updated = PageBlocks.insertAt(blocks, focusedKey, inserted))
        if (focusIt) pendingFocus = inserted.key
    }

    val selection = heldSelection ?: focusedBlock()?.value?.selection ?: TextRange.Zero
    val start = minOf(selection.start, selection.end)
    val end = maxOf(selection.start, selection.end)
    val hasSelection = end > start

    val focused = focusedBlock()
    val activeStyles = if (hasSelection && focused != null) {
        RichText.stylesOn(focused.spans, start, end)
    } else {
        typing
    }
    // Une citation n'est plus un style pose sur du texte, c'est un bloc — mais
    // le bouton doit quand meme s'allumer quand on ecrit dedans.
    val active = if (focused?.kind == BlockKind.QUOTE) {
        activeStyles + TextStyleKind.QUOTE
    } else {
        activeStyles
    }

    fun applyStyle(style: TextStyleKind) {
        // Une citation devient un **bloc**. Avec une selection, c'est elle qui
        // part dans la citation et le paragraphe se coupe autour ; sans
        // selection, une citation vide se pose au curseur, prête a ecrire.
        if (style == TextStyleKind.QUOTE) {
            val (updated, key) = PageBlocks.toQuote(blocks, focusedKey)
            setBlocks(structural = true, updated = updated)
            if (key != null) pendingFocus = key
            return
        }

        // Un trait est un bloc lui aussi : il ne se pose pas *sur* du texte, il
        // prend sa place entre deux paragraphes.
        if (style.isRule) {
            insertBlock(PageBlocks.rule(style))
            return
        }

        // La couleur du trait d'une citation et son fond sont des reglages du
        // bloc, pas des intervalles de texte.
        if (style.family == StyleFamily.QUOTE_BAR) {
            val key = focusedKey ?: return
            history.record(snapshot(), structural = true)
            blocks = blocks.map {
                if (it.key == key && it.kind == BlockKind.QUOTE) {
                    it.copy(bar = if (it.bar == style) null else style)
                } else {
                    it
                }
            }
            return
        }
        if (style.family == StyleFamily.QUOTE_FILL) {
            val key = focusedKey ?: return
            history.record(snapshot(), structural = true)
            blocks = blocks.map {
                if (it.key == key && it.kind == BlockKind.QUOTE) {
                    it.copy(fill = if (it.fill == style) null else style)
                } else {
                    it
                }
            }
            return
        }

        val block = focusedBlock() ?: return

        if (hasSelection) {
            history.record(snapshot(), structural = false)
            blocks = blocks.map {
                if (it.key == block.key) {
                    it.copy(spans = RichText.toggle(it.spans, start, end, style))
                } else {
                    it
                }
            }
            return
        }

        // Sans selection, un titre ne peut pas deviner ce qu'il doit habiller.
        // Il pose donc son propre exemple, deja selectionne : ecrire par-dessus
        // le remplace.
        if (style.takesWholeLine) {
            history.record(snapshot(), structural = true)
            val example = style.label
            val needsBreak = start > 0 && block.text.getOrNull(start - 1) != '\n'
            val prefix = if (needsBreak) "\n" else ""
            val inserted = prefix + example + "\n"
            val updated = block.text.substring(0, start) + inserted + block.text.substring(start)
            val from = start + prefix.length
            val to = from + example.length
            blocks = blocks.map {
                if (it.key == block.key) {
                    it.copy(
                        value = TextFieldValue(updated, TextRange(from, to)),
                        spans = RichText.applyAll(
                            RichText.adjust(it.spans, block.text, updated),
                            from,
                            to,
                            setOf(style),
                        ),
                    )
                } else {
                    it
                }
            }
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
        val block = focusedBlock() ?: return
        val line = RichText.lineRange(block.text, start, end)
        val from = if (hasSelection) start else line.first
        val to = if (hasSelection) end else line.last + 1
        history.record(snapshot(), structural = false)
        blocks = blocks.map {
            if (it.key == block.key) {
                it.copy(spans = RichText.clearFamily(it.spans, from, to, StyleFamily.HEADING))
            } else {
                it
            }
        }
    }

    /** Retire le fond d'une citation : « sans fond » est l'absence de style. */
    fun clearQuoteFill() {
        val key = focusedKey ?: return
        history.record(snapshot(), structural = true)
        blocks = blocks.map { if (it.key == key) it.copy(fill = null) else it }
    }

    /** Revient a la police d'origine sur la selection, ou pour la suite tapee. */
    fun clearFont() {
        val block = focusedBlock()
        if (hasSelection && block != null) {
            history.record(snapshot(), structural = false)
            blocks = blocks.map {
                if (it.key == block.key) {
                    it.copy(spans = RichText.clearFamily(it.spans, start, end, StyleFamily.FONT))
                } else {
                    it
                }
            }
        } else {
            typing = typing.filterNot { it.family == StyleFamily.FONT }.toSet()
        }
    }

    /** Ecrit du texte au curseur du bloc courant, et deplace le curseur apres. */
    fun insertText(at: Int, inserted: String) {
        val block = focusedBlock() ?: return
        history.record(snapshot(), structural = false)
        val updated = block.text.substring(0, at) + inserted + block.text.substring(at)
        blocks = blocks.map {
            if (it.key == block.key) {
                it.copy(
                    value = TextFieldValue(
                        updated,
                        TextRange((at + inserted.length).coerceAtMost(updated.length)),
                    ),
                    spans = RichText.adjust(it.spans, block.text, updated),
                )
            } else {
                it
            }
        }
    }

    /** Ajoute une puce, un numero ou une lettre en tete de ligne : du vrai texte. */
    fun prefixLine(marker: String) {
        val block = focusedBlock() ?: return
        insertText(RichText.lineRange(block.text, start, end).first, marker)
    }

    /**
     * Insere un `#` au curseur, avec une espace devant s'il est colle a un mot.
     *
     * Un `#` accroche au mot precedent n'est pas un mot-cle (voir
     * `Hashtag.rangesIn`), et le bouton ne doit pas fabriquer quelque chose qui
     * ne marchera pas.
     */
    fun insertHashtag() {
        val block = focusedBlock() ?: return
        val at = if (hasSelection) end else start
        val before = block.text.getOrNull(at - 1)
        val glued = before != null && (before.isLetterOrDigit() || before == '_')
        insertText(at, if (glued) " #" else "#")
    }

    fun undo() {
        val restored = history.undo(snapshot()) ?: return
        title = title.copy(text = restored.title)
        blocks = restored.blocks
        focusedKey = restored.blocks.firstOrNull { it.isText }?.key
    }

    fun redo() {
        val restored = history.redo(snapshot()) ?: return
        title = title.copy(text = restored.title)
        blocks = restored.blocks
        focusedKey = restored.blocks.firstOrNull { it.isText }?.key
    }

    // --- Les vocaux -------------------------------------------------------
    // Les messages du bas de l'ecran. Declares avant les vocaux : c'est eux qui
    // s'en servent pour dire qu'un micro est pris.
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val voiceNotes by remember(date) { repository.observeVoiceNotes(date) }
        .collectAsStateWithLifecycle(emptyList())
    val recorder = remember { VoiceRecorder() }
    val player = remember { VoicePlayer() }
    var recordingPath by remember { mutableStateOf<String?>(null) }
    /** Le bloc en cours d'enregistrement : un bloc vocal qui n'a pas encore de son. */
    var recordingKey by remember { mutableStateOf<Long?>(null) }
    var recordingMs by remember { mutableStateOf(0L) }
    var recordingWave by remember { mutableStateOf("") }
    // L'avancee de la lecture. Dans un `State` et lue au dessin : lue pendant
    // la composition, elle recomposerait la page vingt fois par seconde.
    val playProgress = remember { mutableStateOf(0f) }
    var playingPath by remember { mutableStateOf<String?>(null) }

    // Le micro et le haut-parleur se rendent en quittant l'ecran. Sans ca, un
    // enregistrement continue en fond et le telephone garde le micro ouvert.
    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            player.stop()
        }
    }

    // L'horloge de l'enregistrement. Elle ne tourne que pendant : une horloge
    // permanente ferait recomposer la page toutes les deux dixiemes de seconde
    // pour un chiffre que personne ne regarde le reste du temps.
    LaunchedEffect(recordingPath) {
        while (recordingPath != null) {
            // Le meme battement sert au chiffre qui defile et a la mesure du
            // micro qui dessine la silhouette du son, en direct.
            recordingMs = recorder.tick()
            recordingWave = recorder.waveform()
            kotlinx.coroutines.delay(100)
        }
        recordingMs = 0L
        recordingWave = ""
    }

    LaunchedEffect(playingPath) {
        while (playingPath != null) {
            playProgress.value = player.progress()
            kotlinx.coroutines.delay(60)
        }
        playProgress.value = 0f
    }

    fun startRecording() {
        val relativePath = repository.media.newVoicePath(epochDay)
        when (recorder.start(context, repository.media.file(relativePath))) {
            RecordStart.OK -> {
                player.stop()
                playingPath = null
                recordingPath = relativePath
                // Le bloc est pose **tout de suite**, au curseur : la barre
                // rouge est deja a la place ou le vocal restera. Rien ne bougera
                // quand on arretera — c'est le meme bloc, il changera juste de
                // couleur et de bouton.
                val placeholder = PageBlock(key = PageBlocks.newKey(), kind = BlockKind.VOICE)
                recordingKey = placeholder.key
                insertBlock(placeholder)
                focusManager.clearFocus()
                openPanel = null
            }
            // Pendant un appel, `MediaRecorder` demarre sans broncher et
            // enregistre du silence : mieux vaut refuser et le dire.
            RecordStart.BUSY -> scope.launch {
                snackbar.showSnackbar("Le micro est déjà pris — un appel en cours ?")
            }
            RecordStart.FAILED -> scope.launch {
                snackbar.showSnackbar("Impossible d'ouvrir le micro.")
            }
        }
    }

    fun stopRecording() {
        val relativePath = recordingPath ?: return
        val key = recordingKey
        recordingPath = null
        recordingKey = null
        val duration = recorder.stop()
        val heard = recorder.heardSomething()
        val file = repository.media.file(relativePath)
        if (duration == null || !heard) {
            // Trop court, ou le micro n'a rien entendu : on ne garde pas un
            // vocal qui ne contient rien. Un enregistrement vide qui a l'air
            // reussi est pire qu'un refus.
            file.delete()
            if (key != null) blocks = PageBlocks.tidy(blocks.filterNot { it.key == key })
            if (duration != null) {
                scope.launch { snackbar.showSnackbar("Rien n'a été entendu — vocal non gardé.") }
            }
            return
        }
        val shape = recorder.waveform()
        app.appScope.launch {
            val id = repository.addVoiceNote(date, relativePath, duration, shape)
            // Le bloc etait deja la ; il apprend seulement quel son il montre.
            blocks = blocks.map { if (it.key == key) it.copy(voiceId = id) else it }
        }
    }

    val askMicrophone = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
    }

    fun recordOrAsk() {
        if (recordingPath != null) {
            stopRecording()
            return
        }
        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (allowed) startRecording() else askMicrophone.launch(Manifest.permission.RECORD_AUDIO)
    }

    // --- Choisir, puis supprimer un bloc -----------------------------------
    /**
     * Le bloc choisi sans qu'on ecrive dedans : un vocal, un trait.
     *
     * Declare **ici**, avec ce qui s'en sert, et non plus avec le deplacement
     * plus bas : les fonctions d'un composable ne voient que ce qui est ecrit
     * au-dessus d'elles.
     */
    var selectedBlock by remember { mutableStateOf<Long?>(null) }

    /** Le bloc dont la poignee a ete effleuree : son petit menu est ouvert. */
    var menuFor by remember { mutableStateOf<Long?>(null) }

    /** Le vocal dont on demande confirmation avant d'effacer le son. */
    var confirmDelete by remember { mutableStateOf<Long?>(null) }

    fun deleteBlockNow(key: Long) {
        val block = blocks.firstOrNull { it.key == key }
        val note = block?.voiceId?.let { id -> voiceNotes.firstOrNull { it.id == id } }
        if (note != null) {
            if (playingPath == note.relativePath) {
                player.stop()
                playingPath = null
            }
            app.appScope.launch { repository.deleteVoiceNote(note) }
        }
        menuFor = null
        confirmDelete = null
        selectedBlock = null
        setBlocks(structural = true, updated = PageBlocks.tidy(blocks.filterNot { it.key == key }))
    }

    /**
     * Demande la suppression d'un bloc.
     *
     * Un paragraphe, une citation, un trait s'effacent tout de suite : ils sont
     * dans « annuler », et rien n'est perdu. Un vocal, non — supprimer son bloc
     * **efface le fichier son**, et aucune annulation ne le rendra. Il est le
     * seul a demander confirmation, et c'est cette difference-la qui merite une
     * question, pas le fait d'effacer quelque chose.
     */
    fun requestDeleteBlock(key: Long) {
        val block = blocks.firstOrNull { it.key == key } ?: return
        if (block.kind == BlockKind.VOICE && block.voiceId != null) {
            menuFor = null
            confirmDelete = key
        } else {
            deleteBlockNow(key)
        }
    }

    // --- L'export PDF -----------------------------------------------------
    val exportPdf = rememberLauncherForActivityResult(
        // On laisse l'utilisateur choisir ou ranger le fichier : rien ne sort du
        // telephone sans qu'il ait dit ou.
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            // Le PDF est dessine a partir de la page **a plat** : il ne sait
            // rien des blocs, comme la recherche et l'export de l'annee.
            val flat = JournalBlocks.flatten(
                PageBlocks.toStored(PageBlocks.tidy(blocks), epochDay)
            )
            scope.launch {
                val result = runCatching {
                    PagePdf.write(
                        context = context,
                        target = uri,
                        date = date,
                        title = title.text,
                        body = flat.text,
                        spans = flat.spans,
                        photos = journalMedia,
                        voiceNotes = voiceNotes,
                        photoFile = { repository.media.file(it.relativePath) },
                        paper = paper,
                        ink = ink,
                        lineColor = JournalPaper.line(lineIndex),
                        accent = accent,
                        ruled = ruled,
                        baseFont = baseFont,
                        textSize = textSize,
                        pageWidth = pageWidth,
                    )
                }
                snackbar.showSnackbar(
                    result.fold(
                        onSuccess = { if (it > 1) "Page exportée ($it feuilles)." else "Page exportée." },
                        onFailure = { "Échec de l'export : ${it.message}" },
                    )
                )
            }
        }
    }

    // --- Deplacer un bloc -------------------------------------------------
    // La hauteur et la place de chaque bloc, mesurees a la volee. On ne s'en
    // sert que pendant un deplacement : c'est ce qui permet de savoir quand le
    // doigt a franchi le voisin, sans rien mesurer pendant le geste.
    val blockHeights = remember { mutableStateMapOf<Long, Float>() }
    val blockTops = remember { mutableStateMapOf<Long, Float>() }
    val layouts = remember { mutableMapOf<Long, MutableState<TextLayoutResult?>>() }
    var dragKey by remember { mutableStateOf<Long?>(null) }
    var dragDy by remember { mutableStateOf(0f) }

    /**
     * Le bloc suit le doigt, et la page se reorganise sous lui.
     *
     * La liste est reordonnee **pendant** le geste : le trou s'ouvre a la
     * bonne place, donc il n'y a pas d'indicateur a dessiner — le trou *est*
     * l'indicateur. A chaque franchissement on retranche la hauteur du voisin
     * de l'ecart accumule, sinon le bloc s'echapperait du doigt.
     *
     * La boucle s'arrete sur une hauteur nulle : un bloc pas encore mesure
     * rendrait la condition vraie sans que rien ne bouge, et on tournerait
     * pour toujours. C'est exactement le piege d'« Organiser ma journee ».
     */
    fun dragBy(delta: Float) {
        val key = dragKey ?: return
        var offset = dragDy + delta
        var list = blocks
        var index = list.indexOfFirst { it.key == key }
        if (index < 0) return

        while (true) {
            if (offset > 0f && index < list.lastIndex) {
                val height = blockHeights[list[index + 1].key] ?: break
                if (height <= 0f || offset < height / 2f) break
                list = list.toMutableList().also { it.add(index + 1, it.removeAt(index)) }
                offset -= height
                index += 1
            } else if (offset < 0f && index > 0) {
                val height = blockHeights[list[index - 1].key] ?: break
                if (height <= 0f || -offset < height / 2f) break
                list = list.toMutableList().also { it.add(index - 1, it.removeAt(index)) }
                offset += height
                index -= 1
            } else {
                break
            }
        }

        blocks = list
        dragDy = offset
    }

    fun startDrag(key: Long) {
        history.record(snapshot(), structural = true)
        focusManager.clearFocus()
        openPanel = null
        selectedBlock = null
        dragKey = key
        dragDy = 0f
    }

    fun endDrag() {
        dragKey = null
        dragDy = 0f
        // Deux paragraphes qui se retrouvent cote a cote n'ont plus de raison
        // d'etre separes : ils se recollent.
        blocks = PageBlocks.tidy(blocks)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        // Le papier prend tout l'ecran. Une page teintee dans un cadre blanc ne
        // ressemblait a rien : c'est le carnet entier qui a une couleur.
        containerColor = paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = ink,
                    navigationIconContentColor = ink,
                    actionIconContentColor = ink,
                ),
                // Pas de titre ici : la date est **sur la page**, comme
                // l'en-tete d'une lettre. Coincee entre la fleche de retour et
                // trois boutons, elle passait a la ligne — et une date qui
                // tient sur deux lignes dans une barre n'est pas une barre bien
                // remplie, c'est une date au mauvais endroit.
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    // Annuler et refaire vivent **ici**, pas dans la barre du
                    // bas : on en a besoin surtout quand quelque chose vient de
                    // mal se passer, et a ce moment-la le clavier est souvent
                    // deja parti — donc la barre du bas aussi.
                    IconButton(onClick = { undo() }, enabled = history.canUndo) {
                        Icon(
                            painter = painterResource(R.drawable.ic_undo),
                            contentDescription = "Annuler",
                            tint = ink.copy(alpha = if (history.canUndo) 0.9f else 0.25f),
                        )
                    }
                    IconButton(onClick = { redo() }, enabled = history.canRedo) {
                        Icon(
                            painter = painterResource(R.drawable.ic_redo),
                            contentDescription = "Refaire",
                            tint = ink.copy(alpha = if (history.canRedo) 0.9f else 0.25f),
                        )
                    }
                    IconButton(onClick = { showPaperSettings = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Paramètres de la page")
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
            // Le surtitre : la date, en petites capitales espacees. Elle etait
            // dans la barre du haut, ou elle se disputait la place avec quatre
            // boutons. Ici elle appartient a la page, et le titre qu'on ecrit
            // devient enfin le premier mot de la feuille.
            Text(
                text = Dates.dayMedium(date).uppercase(Locale.FRENCH),
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.45f),
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(start = TEXT_INDENT, end = 20.dp),
            )

            Spacer(Modifier.height(2.dp))

            BasicTextField(
                value = title,
                onValueChange = {
                    history.record(snapshot(), structural = false)
                    title = it
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = ink,
                    fontWeight = FontWeight.Bold,
                ),
                cursorBrush = SolidColor(accent),
                decorationBox = { field ->
                    Box {
                        if (title.text.isEmpty()) {
                            Text(
                                "Titre de la journée",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = ink.copy(alpha = 0.28f),
                            )
                        }
                        field()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = TEXT_INDENT, end = 20.dp, bottom = 14.dp)
                    .selectWordOnDoubleTap({ title }) { title = title.copy(selection = it) }
                    .testTag("day-title-field"),
            )

            // Un trait a peine pose : il separe l'en-tete de la page qui defile
            // dessous, sans couper la feuille en deux.
            HorizontalDivider(color = ink.copy(alpha = 0.07f))

            // La page : les blocs et les photos defilent ensemble, dans un seul
            // conteneur. Les positions des photos sont donc des positions dans
            // la page, pas dans l'ecran.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val viewportHeight = maxHeight
                val measuredWidth = maxWidth
                LaunchedEffect(measuredWidth) { pageWidth = measuredWidth.value }

                // La page descend au moins jusqu'au bas de la photo la plus
                // basse : sans ca, une image posee en bas serait inatteignable.
                val pageHeight = maxOf(
                    viewportHeight,
                    (Placement.lowestEdge(journalMedia) + 160f).dp,
                )

                val focusedLayout = layouts[focusedKey]?.value
                val focusedSelection = focusedBlock()?.value?.selection

                // Le curseur ne doit jamais passer sous le clavier. Aucun champ
                // ne peut s'en charger : ils n'ont pas de defilement a eux, ils
                // grandissent, et c'est la page qui bouge. On calcule donc ou
                // est le curseur — dans son bloc, plus la place du bloc dans la
                // page — et on amene la page a lui.
                LaunchedEffect(focusedSelection, focusedLayout, viewportHeight, focusedKey) {
                    val key = focusedKey ?: return@LaunchedEffect
                    val layout = focusedLayout ?: return@LaunchedEffect
                    val blockTop = blockTops[key] ?: return@LaunchedEffect
                    val caret = (focusedSelection?.end ?: 0)
                        .coerceIn(0, layout.layoutInput.text.length)
                    val rect = runCatching { layout.getCursorRect(caret) }.getOrNull()
                        ?: return@LaunchedEffect
                    val top = rect.top + blockTop
                    val bottom = rect.bottom + blockTop
                    val viewport = with(density) { viewportHeight.toPx() }
                    val at = pageScroll.value.toFloat()
                    val target = when {
                        bottom + CARET_MARGIN > at + viewport -> bottom + CARET_MARGIN - viewport
                        top - CARET_MARGIN < at -> top - CARET_MARGIN
                        else -> null
                    }
                    if (target != null) {
                        pageScroll.animateScrollTo(target.toInt().coerceIn(0, pageScroll.maxValue))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScroll),
                ) {
                    // Le lignage reste tout le temps ; la grille des photos
                    // n'apparait que pendant qu'on en deplace une. Deux choses
                    // differentes, deux durees de vie. `matchParentSize` et non
                    // une hauteur calculee : c'est le contenu qui decide de la
                    // hauteur de la page.
                    if (ruled) {
                        PaperLines(
                            color = JournalPaper.line(lineIndex),
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    if (selectedPhoto != null && snapToGrid) {
                        PhotoGrid(modifier = Modifier.matchParentSize())
                    }

                    // Sous le texte : le fond, puis le milieu.
                    journalMedia
                        .filter { it.layer != MediaLayer.FRONT && it.isPlaced }
                        .sortedBy { it.layerKey }
                        .forEach { item ->
                            PlacedPhoto(
                                item = item,
                                file = repository.media.file(item.relativePath),
                                // Sous le texte, un appui va au texte : c'est le
                                // champ qui est devant. On la reprend par la
                                // liste « Photos de la page ».
                                onSelect = null,
                            )
                        }

                    PageColumn(
                        blocks = blocks,
                        style = pageStyle,
                        pageHeight = pageHeight,
                        lineHeight = JournalPaper.LINE_SPACING,
                        voiceNotes = voiceNotes,
                        focusedKey = focusedKey,
                        pendingFocus = pendingFocus,
                        heldSelection = heldSelection,
                        recordingKey = recordingKey,
                        recordingMs = recordingMs,
                        recordingWave = recordingWave,
                        selectedBlock = selectedBlock,
                        dragKey = dragKey,
                        dragOffset = { dragDy },
                        layouts = layouts,
                        playingPath = playingPath,
                        playProgress = { playProgress.value },
                        onValueChange = ::changeBlock,
                        onFocused = { key, focused ->
                            if (focused) {
                                focusedKey = key
                                selectedBlock = null
                                if (openPanel != null) {
                                    openPanel = null
                                    heldSelection = null
                                    awaitingKeyboard = false
                                }
                            }
                        },
                        onFocusHandled = { pendingFocus = null },
                        onPlaced = { key, top, height ->
                            blockTops[key] = top
                            blockHeights[key] = height
                        },
                        onDragStart = ::startDrag,
                        onDrag = ::dragBy,
                        onDragEnd = ::endDrag,
                        onOpenDay = onOpenDay,
                        onBackspaceAtStart = ::backspaceAtStart,
                        onSelectBlock = { selectedBlock = it },
                        onDeleteBlock = ::requestDeleteBlock,
                        menuFor = menuFor,
                        onMenu = { menuFor = it },
                        onQuoteBar = { key, style ->
                            history.record(snapshot(), structural = true)
                            blocks = blocks.map { if (it.key == key) it.copy(bar = style) else it }
                        },
                        onQuoteFill = { key, style ->
                            history.record(snapshot(), structural = true)
                            blocks = blocks.map { if (it.key == key) it.copy(fill = style) else it }
                        },
                        onStopRecording = { stopRecording() },
                        onPlayVoice = { note ->
                            player.toggle(
                                file = repository.media.file(note.relativePath),
                                key = note.relativePath,
                            ) { playingPath = null }
                            playingPath = player.playing
                        },
                        onToggleVoiceWidth = { note ->
                            app.appScope.launch {
                                repository.updateVoiceNote(note.copy(wide = !note.wide))
                            }
                        },
                        onTapBelow = {
                            // Appuyer sous le dernier bloc doit poser le curseur
                            // au bout du texte, comme appuyer dans une page
                            // blanche. Sans ca, tout le bas de la page est mort.
                            val last = blocks.lastOrNull()
                            if (last != null && last.isText) {
                                blocks = blocks.map {
                                    if (it.key == last.key) {
                                        it.copy(
                                            value = it.value.copy(
                                                selection = TextRange(it.text.length),
                                            )
                                        )
                                    } else {
                                        it
                                    }
                                }
                                pendingFocus = last.key
                            } else {
                                val added = PageBlocks.text()
                                setBlocks(structural = true, updated = blocks + added)
                                pendingFocus = added.key
                            }
                        },
                    )

                    // Devant le texte : ces photos-la se prennent directement.
                    journalMedia
                        .filter { it.layer == MediaLayer.FRONT && it.isPlaced }
                        .forEach { item ->
                            PlacedPhoto(
                                item = item,
                                file = repository.media.file(item.relativePath),
                                onSelect = { selectPhoto(item.id) },
                            )
                        }

                    // Le cadre de manipulation passe par-dessus tout, meme sur
                    // une photo de fond que le texte recouvre.
                    selectedPhoto?.let { photo ->
                        PhotoHandle(
                            item = photo,
                            pageWidth = pageWidth,
                            snapToGrid = snapToGrid,
                            onChange = { draft = it },
                        )
                    }
                }
            }

            confirmDelete?.let { key ->
                AlertDialog(
                    onDismissRequest = { confirmDelete = null },
                    title = { Text("Supprimer ce vocal ?") },
                    // On dit ce qui est irreversible, et rien de plus : c'est
                    // le fichier son qui part, et « annuler » ne le ramenera
                    // pas. Une question qui n'explique pas ce qu'elle protege
                    // n'est qu'un clic de plus.
                    text = { Text("L'enregistrement sera effacé du téléphone. On ne pourra pas le récupérer.") },
                    confirmButton = {
                        TextButton(onClick = { deleteBlockNow(key) }) {
                            Text("Supprimer", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = null }) { Text("Garder") }
                    },
                )
            }

            if (showPaperSettings) {
                PaperSettingsSheet(
                    ruled = ruled,
                    paperIndex = paperIndex,
                    lineIndex = lineIndex,
                    snapToGrid = snapToGrid,
                    fontCode = fontCode,
                    textSize = textSize,
                    onRuled = { ruled = it; prefs.journalRuled = it },
                    onPaper = { paperIndex = it; prefs.journalPaperIndex = it },
                    onLine = { lineIndex = it; prefs.journalLineIndex = it },
                    onSnap = { snapToGrid = it; prefs.journalSnapToGrid = it },
                    onFont = { fontCode = it; prefs.journalFontCode = it },
                    onTextSize = { textSize = it; prefs.journalTextSize = it },
                    linkText = PageLink.format(date),
                    onExportPdf = {
                        showPaperSettings = false
                        exportPdf.launch("DayByDay ${Dates.dayMedium(date)}.pdf")
                    },
                    onCopyLink = {
                        clipboard.setText(AnnotatedString(PageLink.format(date)))
                        showPaperSettings = false
                    },
                    onDismiss = { showPaperSettings = false },
                )
            }

            if (selectedPhoto != null) {
                val photo = selectedPhoto
                PhotoToolsBar(
                    item = photo,
                    snapToGrid = snapToGrid,
                    // Le panneau vit **sur** la page : il prend ses couleurs du
                    // papier, comme la barre d'outils du journal. Le blanc et
                    // le violet du theme tombaient sur un papier ivoire comme
                    // un morceau d'une autre application.
                    paper = paper,
                    ink = ink,
                    onShape = { changePhoto(photo.copy(shapeKey = it.key)) },
                    onOutline = { changePhoto(photo.copy(stickerOutline = it)) },
                    onLayer = { changePhoto(photo.copy(layerKey = it.key)) },
                    onSnap = {
                        snapToGrid = it
                        prefs.journalSnapToGrid = it
                    },
                    onDelete = {
                        selectPhoto(null)
                        app.appScope.launch { repository.deleteMedia(photo) }
                    },
                    onDone = { selectPhoto(null) },
                )
            } else {
                JournalToolbar(
                    active = active,
                    openPanel = openPanel,
                    onTogglePanel = { panel ->
                        showPanel(if (openPanel == panel) null else panel)
                    },
                    onStyle = { style ->
                        // Le panneau reste ouvert : on essaie rarement une seule
                        // nuance. Il ne se ferme que quand l'action a pose
                        // quelque chose — la, il n'y a plus rien a reessayer, et
                        // il faut voir ou on en est.
                        val posed = style.isRule ||
                            style == TextStyleKind.QUOTE ||
                            (!hasSelection && style.takesWholeLine)
                        applyStyle(style)
                        if (posed) showPanel(null)
                    },
                    onClearHeading = { clearHeading() },
                    onClearFont = { clearFont() },
                    onClearQuoteFill = { clearQuoteFill() },
                    onList = { marker ->
                        prefixLine(marker.marker)
                        showPanel(null)
                    },
                    recording = recordingPath != null,
                    onRecord = { recordOrAsk() },
                    onHashtag = {
                        // Le panneau se referme et le clavier revient : on vient
                        // d'ouvrir un mot, il faut pouvoir l'ecrire.
                        showPanel(null)
                        insertHashtag()
                    },
                    onAddPhoto = {
                        // On referme le panneau sans rendre le focus : le
                        // selecteur de photos passe devant.
                        openPanel = null
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    photos = journalMedia,
                    photoFile = { repository.media.file(it.relativePath) },
                    onPickPhoto = { selectPhoto(it.id) },
                    panelHeight = panelHeight,
                    paper = paper,
                    ink = ink,
                )

                // Le clavier remonte : on tient sa place jusqu'a ce qu'il y soit.
                if (openPanel == null && awaitingKeyboard && panelHeight > 0.dp) {
                    Spacer(Modifier.height(panelHeight))
                }
            }
        }
    }
}

/**
 * La page : ses blocs, les uns sous les autres.
 *
 * Elle ne decide de rien — pas plus que les champs de texte. Elle place, elle
 * mesure, elle previent. Toute la logique reste dans l'ecran, a un seul
 * endroit : c'est ce qui permet a « annuler » de tout couvrir sans avoir a y
 * penser bloc par bloc.
 */
@Composable
private fun PageColumn(
    blocks: List<PageBlock>,
    style: PageStyle,
    pageHeight: Dp,
    lineHeight: Dp,
    voiceNotes: List<VoiceNote>,
    focusedKey: Long?,
    pendingFocus: Long?,
    heldSelection: TextRange?,
    recordingKey: Long?,
    recordingMs: Long,
    recordingWave: String,
    selectedBlock: Long?,
    dragKey: Long?,
    dragOffset: () -> Float,
    layouts: MutableMap<Long, MutableState<TextLayoutResult?>>,
    playingPath: String?,
    playProgress: () -> Float,
    onValueChange: (Long, TextFieldValue) -> Unit,
    onFocused: (Long, Boolean) -> Unit,
    onFocusHandled: () -> Unit,
    onPlaced: (Long, Float, Float) -> Unit,
    onDragStart: (Long) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onBackspaceAtStart: (Long) -> Boolean,
    onSelectBlock: (Long?) -> Unit,
    onDeleteBlock: (Long) -> Unit,
    menuFor: Long?,
    onMenu: (Long?) -> Unit,
    onQuoteBar: (Long, TextStyleKind?) -> Unit,
    onQuoteFill: (Long, TextStyleKind?) -> Unit,
    onStopRecording: () -> Unit,
    onPlayVoice: (VoiceNote) -> Unit,
    onToggleVoiceWidth: (VoiceNote) -> Unit,
    onTapBelow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = pageHeight)
            // A gauche, juste de quoi loger la colonne des poignees ; le texte
            // se decale d'autant et retombe sur `TEXT_INDENT`, celui du titre.
            .padding(start = PAGE_START, end = 20.dp),
    ) {
        // La marge du haut est un espace, pas un `padding` : les places des
        // blocs sont mesurees dans cette colonne, et un `padding` les
        // decalerait toutes de sa hauteur sans que rien ne le dise.
        Spacer(Modifier.height(JournalPaper.TOP_PADDING))

        blocks.forEach { block ->
            key(block.key) {
                val layout = layouts.getOrPut(block.key) { mutableStateOf<TextLayoutResult?>(null) }
                val focusRequester = remember { FocusRequester() }

                LaunchedEffect(pendingFocus) {
                    if (pendingFocus == block.key) {
                        runCatching { focusRequester.requestFocus() }
                        onFocusHandled()
                    }
                }

                val current = focusedKey == block.key || selectedBlock == block.key
                val grip = Modifier.blockDrag(
                    onStart = { onDragStart(block.key) },
                    onDrag = onDrag,
                    onEnd = onDragEnd,
                )

                // La couche est posee **toujours**, jamais seulement pendant le
                // geste. C'est le bug qui a rendu la premiere version
                // inutilisable : ajouter un `graphicsLayer` au demarrage du
                // deplacement changeait la forme de la chaine de modificateurs,
                // Compose recreait le detecteur d'appui, et le geste en cours
                // etait annule aussitot — le bloc sursautait puis restait sur
                // place. Une chaine de forme constante ne peut plus le faire.
                val slot = Modifier
                    .fillMaxWidth()
                    .reportPlacement { top, height -> onPlaced(block.key, top, height) }
                    .graphicsLayer {
                        val moving = dragKey == block.key
                        // Lu **dans** la couche : lu pendant la composition, le
                        // decalage remesurerait la page a chaque image.
                        translationY = if (moving) dragOffset() else 0f
                        scaleX = if (moving) 1.02f else 1f
                        scaleY = if (moving) 1.02f else 1f
                        shadowElevation = if (moving) 14f else 0f
                        alpha = if (moving) 0.97f else 1f
                    }

                Column(modifier = slot) {
                    Row(verticalAlignment = Alignment.Top) {
                        // La poignee, dans la marge. Elle est la pour **tous**
                        // les blocs, y compris les paragraphes : c'est elle qui
                        // rend le systeme visible, et sans elle une page en
                        // blocs ressemble trait pour trait a une page qui n'en
                        // a pas.
                        Box {
                            BlockGutter(
                                current = current,
                                ink = style.ink,
                                lineHeight = lineHeight,
                                dragModifier = grip,
                                onTap = { onMenu(block.key) },
                            )
                            if (menuFor == block.key) {
                                BlockMenu(
                                    paper = style.paper,
                                    ink = style.ink,
                                    onDelete = { onDeleteBlock(block.key) },
                                    onDismiss = { onMenu(null) },
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (block.kind) {
                                BlockKind.TEXT -> BlockTextField(
                                    value = block.value,
                                    spans = block.spans,
                                    style = style,
                                    layout = layout,
                                    placeholder = "Écris ce que tu veux, comme tu veux."
                                        .takeIf { blocks.size == 1 },
                                    heldSelection = heldSelection
                                        .takeIf { focusedKey == block.key },
                                    onValueChange = { onValueChange(block.key, it) },
                                    onLayout = { layout.value = it },
                                    onFocus = { onFocused(block.key, it) },
                                    onOpenDay = onOpenDay,
                                    onBackspaceAtStart = { onBackspaceAtStart(block.key) },
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .testTag("day-note-field"),
                                )

                                BlockKind.QUOTE -> QuoteBlockView(
                                    value = block.value,
                                    spans = block.spans,
                                    bar = block.bar,
                                    fill = block.fill,
                                    style = style,
                                    layout = layout,
                                    heldSelection = heldSelection
                                        .takeIf { focusedKey == block.key },
                                    onValueChange = { onValueChange(block.key, it) },
                                    onLayout = { layout.value = it },
                                    onFocus = { onFocused(block.key, it) },
                                    onOpenDay = onOpenDay,
                                    // Le trait de la citation est une deuxieme
                                    // prise, en plus de la poignee : c'est deja
                                    // ce qu'on montre du doigt pour designer
                                    // une citation.
                                    dragModifier = grip,
                                    modifier = Modifier.focusRequester(focusRequester),
                                )

                                BlockKind.RULE -> Row(
                                    modifier = Modifier.clickable(
                                        onClickLabel = "Choisir ce trait",
                                        onClick = {
                                            onSelectBlock(
                                                if (selectedBlock == block.key) null else block.key
                                            )
                                        },
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RuleBlockView(
                                        rule = block.rule,
                                        style = style,
                                        lineHeight = lineHeight,
                                        dragModifier = grip,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (selectedBlock == block.key) {
                                        IconButton(
                                            onClick = { onDeleteBlock(block.key) },
                                            modifier = Modifier.size(32.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Supprimer ce trait",
                                                tint = style.ink.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }

                                BlockKind.VOICE -> {
                                    val note = voiceNotes.firstOrNull { it.id == block.voiceId }
                                    when {
                                        block.key == recordingKey -> RecordingRow(
                                            elapsedMs = recordingMs,
                                            waveform = recordingWave,
                                            paper = style.paper,
                                            lineHeight = lineHeight,
                                            onStop = onStopRecording,
                                        )

                                        note != null -> VoiceNoteRow(
                                            note = note,
                                            playing = playingPath == note.relativePath,
                                            progress = playProgress,
                                            paper = style.paper,
                                            lineHeight = lineHeight,
                                            selected = selectedBlock == block.key,
                                            onSelect = {
                                                onSelectBlock(
                                                    if (selectedBlock == block.key) {
                                                        null
                                                    } else {
                                                        block.key
                                                    }
                                                )
                                            },
                                            onPlay = { onPlayVoice(note) },
                                            onDelete = { onDeleteBlock(block.key) },
                                            onToggleWidth = { onToggleVoiceWidth(note) },
                                            dragModifier = grip,
                                        )

                                        // Un bloc dont le son a disparu : il
                                        // s'en ira a la prochaine ouverture
                                        // (voir JournalBlocks.reconcile).
                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }

                    // Les reglages de la citation, **sous la citation**. Ils
                    // existaient deja, ranges au fond d'un panneau qui ne
                    // s'ouvrait qu'au bon endroit — donc introuvables. Ici ils
                    // apparaissent a cote de ce qu'ils changent.
                    if (block.kind == BlockKind.QUOTE &&
                        focusedKey == block.key &&
                        dragKey == null
                    ) {
                        QuotePalette(
                            bar = block.bar,
                            fill = block.fill,
                            style = style,
                            onBar = { onQuoteBar(block.key, it) },
                            onFill = { onQuoteFill(block.key, it) },
                            modifier = Modifier.padding(start = GUTTER_WIDTH),
                        )
                    }
                }
            }
        }

        // Le bas de la page repond au doigt, comme une page blanche : sans ca,
        // tout ce qui est sous le dernier bloc est mort, et il faut viser la
        // derniere ligne pour reprendre l'ecriture.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(TAIL_HEIGHT)
                .clickable(onClickLabel = "Écrire à la suite", onClick = onTapBelow),
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
                // Ici les mots-cles portent leur couleur eux-memes : il n'y a
                // pas de pastille dessinee sur une carte.
                text = buildAnnotatedStringWithSpans(body, spans, hashtagColors = true),
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

/**
 * L'air garde entre le curseur et le bord de la page quand elle defile toute
 * seule. Sans marge, le curseur se colle au bas de l'ecran et on ecrit sans
 * voir la ligne suivante.
 */
private const val CARET_MARGIN = 120f

/** Le blanc laisse sous le dernier bloc, pour pouvoir ecrire a la suite. */
private val TAIL_HEIGHT = 220.dp

/** La marge de la page a gauche, avant la colonne des poignees. */
private val PAGE_START = 6.dp

/**
 * Ou commence le texte, marge des poignees comprise.
 *
 * Le surtitre, le titre et les blocs partagent ce decalage : sans lui, le titre
 * commencerait a gauche des paragraphes et la page aurait deux bords gauches.
 *
 * Declare **apres** `PAGE_START` et ce n'est pas un detail : les proprietes de
 * fichier s'initialisent dans l'ordre ou elles sont ecrites, donc l'inverse
 * aurait additionne un zero sans que rien ne le signale.
 */
private val TEXT_INDENT = PAGE_START + GUTTER_WIDTH
