package com.ismael.daybyday.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaLayer
import com.ismael.daybyday.data.Placement
import com.ismael.daybyday.data.PageLink
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun JournalScreen(
    date: LocalDate,
    onBack: () -> Unit,
    /** Ouvre la journee visee par un lien ecrit dans la page. */
    onOpenDay: (LocalDate) -> Unit = {},
) {
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
    // La hauteur du clavier, retenue pour que le panneau prenne exactement sa
    // place. On garde le **maximum** vu depuis l'ouverture de l'ecran, et c'est
    // tout l'interet : en se fermant, l'encart du clavier passe par toutes les
    // valeurs intermediaires, et retenir la derniere au-dessus d'un seuil
    // gardait 130 au lieu de 330. Le panneau retrecissait donc a chaque
    // aller-retour, jusqu'a n'etre plus qu'un bandeau ecrase en bas de l'ecran.
    var measuredKeyboard by remember { mutableStateOf(0.dp) }
    if (imeHeight > measuredKeyboard) measuredKeyboard = imeHeight
    // Tant que le clavier n'a jamais ete vu, une hauteur d'attente plausible.
    val keyboardHeight = if (measuredKeyboard > 150.dp) measuredKeyboard else 300.dp

    // Le panneau et le clavier n'avaient pas la meme taille, et l'ecran se
    // decalait a chaque bascule : le clavier recouvre la barre de navigation,
    // le panneau se posait au-dessus. La colonne retire deja le plus grand des
    // deux encarts du bas ; le panneau ne prend donc que ce qui manque pour
    // atteindre la hauteur du clavier. Comme le calcul suit l'animation du
    // clavier image par image, le panneau grandit exactement au rythme ou le
    // clavier s'en va : le total ne bouge jamais.
    val panelHeight = (keyboardHeight - maxOf(imeHeight, navHeight)).coerceAtLeast(0.dp)

    // Meme chose dans l'autre sens : en refermant le panneau on garde sa place
    // au chaud, le temps que le clavier remonte la prendre.
    var awaitingKeyboard by remember { mutableStateOf(false) }
    if (imeHeight > 150.dp) awaitingKeyboard = false

    // Demander poliment au clavier de se cacher ne suffit pas : tant que le
    // champ garde le focus, Android le fait revenir. Le panneau et le clavier
    // s'empilaient donc, et la page sautait a chaque bascule. Retirer le focus
    // le ferme pour de bon ; le rendre le rouvre, curseur intact.
    val bodyFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Retirer le focus ferme le clavier, mais efface aussi la selection : on ne
    // pouvait donc pas colorer un texte deja ecrit, il se deselectionnait a
    // l'ouverture du panneau. La selection est mise de cote ici avant de lacher
    // le focus, et c'est elle qui sert de cible tant qu'un panneau est ouvert.
    var heldSelection by remember { mutableStateOf<TextRange?>(null) }

    fun showPanel(panel: ToolPanel?) {
        if (panel == null) {
            openPanel = null
            // On rend la selection au champ avant de lui rendre le focus :
            // le mot colore reste visiblement selectionne.
            heldSelection?.let { body = body.copy(selection = it) }
            heldSelection = null
            awaitingKeyboard = true
            runCatching { bodyFocus.requestFocus() }
        } else {
            // Passer d'un panneau a l'autre ne doit pas relire une selection
            // deja perdue : on garde celle mise de cote au premier passage.
            heldSelection = (heldSelection ?: body.selection).takeIf { it.start != it.end }
            openPanel = panel
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
    val storedMedia = mediaItems.filter { it.cardKey == DayCard.JOURNAL.key }

    // --- Les photos posees sur la page ---------------------------------
    val pageScroll = rememberScrollState()

    // La mise en page du texte, et de quoi convertir des points en pixels : les
    // deux servent a savoir ou se trouve le curseur dans la page.
    // La mise en page est gardee dans un `State` et pas derriere un `by` : les
    // pastilles des mots-cles la lisent **au dessin**, et il leur faut donc
    // l'objet, pas sa valeur du moment.
    val bodyLayout = remember { mutableStateOf<TextLayoutResult?>(null) }
    val textTopPadding = remember(density) {
        with(density) { JournalPaper.TOP_PADDING.toPx() }
    }
    var pageWidth by remember { mutableStateOf(0f) }
    var selectedPhotoId by remember { mutableStateOf<Long?>(null) }

    val clipboard = LocalClipboardManager.current

    // --- L'allure de la page, gardee d'une journee a l'autre -------------
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
    val rhythm = with(density) { JournalPaper.LINE_SPACING.toSp() }
    // La police et la taille de base de la page. Les mises en forme posees sur
    // un morceau de texte passent par-dessus : ce style-ci n'est que le point
    // de depart, celui de tout ce qui n'a rien de particulier.
    val baseFont = JournalPaper.font(fontCode).fontFamily()

    // Pendant qu'un doigt deplace une photo, sa nouvelle place vit ici : on
    // n'ecrit pas dans la base a chaque image de l'animation.
    var draft by remember { mutableStateOf<MediaItem?>(null) }
    val journalMedia = storedMedia.map { item ->
        if (item.id == draft?.id) draft ?: item else item
    }
    val selectedPhoto = journalMedia.firstOrNull { it.id == selectedPhotoId }

    val pendingDraft = draft
    LaunchedEffect(pendingDraft) {
        if (pendingDraft != null) {
            // Une fois les doigts immobiles, on enregistre.
            kotlinx.coroutines.delay(350)
            repository.updateMedia(pendingDraft)
        }
    }

    fun selectPhoto(id: Long?) {
        selectedPhotoId = id
        draft = null
        if (id != null) {
            // On ne tape pas et on manipule une image : le clavier n'a plus
            // rien a faire la, et un panneau ouvert non plus.
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

    // Une photo qu'on vient de choisir dans la galerie n'a pas encore de place,
    // et celles ajoutees avant le placement libre non plus. Elles se rangent
    // la ou on regarde, a leurs proportions, et la derniere est deja choisie
    // pour qu'on puisse la deplacer tout de suite.
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

    val selection = heldSelection ?: body.selection
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
            selection = TextRange((start + marker.length).coerceAtMost(updated.length)),
        )
    }

    /**
     * Insere un `#` au curseur.
     *
     * Avec une espace devant s'il est colle a un mot : un `#` accroche au mot
     * precedent n'est pas un mot-cle (voir `Hashtag.rangesIn`), et le bouton
     * ne doit pas fabriquer quelque chose qui ne marchera pas. Le curseur reste
     * juste apres, prêt pour le mot.
     */
    fun insertHashtag() {
        val at = if (hasSelection) end else start
        val before = body.text.getOrNull(at - 1)
        val inserted = if (before != null && (before.isLetterOrDigit() || before == '_')) " #" else "#"
        val updated = body.text.substring(0, at) + inserted + body.text.substring(at)
        spans = RichText.adjust(spans, body.text, updated)
        body = body.copy(
            text = updated,
            selection = TextRange((at + inserted.length).coerceAtMost(updated.length)),
        )
    }

    Scaffold(
        // Le papier prend tout l'ecran. Une page teintee dans un cadre blanc
        // ne ressemblait a rien : c'est le carnet entier qui a une couleur,
        // pas une feuille posee dessus.
        containerColor = paper,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = ink,
                    navigationIconContentColor = ink,
                    actionIconContentColor = ink,
                ),
                title = { Text(Dates.dayMedium(date)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
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
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = ink,
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
                                color = ink.copy(alpha = 0.45f),
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

            // La page : le texte et les photos defilent ensemble, dans un seul
            // conteneur. Les positions des photos sont donc des positions dans
            // la page, pas dans l'ecran — sinon tout se decalerait au premier
            // defilement.
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

                // Le curseur ne doit jamais passer sous le clavier. Le champ
                // ne peut pas s'en charger : il n'a pas de defilement a lui, il
                // s'etend sur toute la page et c'est la page qui bouge. On
                // calcule donc nous-memes ou est le curseur, et on amene la
                // page a lui — avec une marge, pour qu'on voie aussi la ligne
                // qui suit et non le curseur colle au bord.
                LaunchedEffect(body.selection, bodyLayout.value, viewportHeight) {
                    val layout = bodyLayout.value ?: return@LaunchedEffect
                    val caret = body.selection.end
                        .coerceIn(0, layout.layoutInput.text.length)
                    val rect = runCatching { layout.getCursorRect(caret) }.getOrNull()
                        ?: return@LaunchedEffect
                    val top = rect.top + textTopPadding
                    val bottom = rect.bottom + textTopPadding
                    val viewport = with(density) { viewportHeight.toPx() }
                    val current = pageScroll.value.toFloat()
                    val target = when {
                        bottom + CARET_MARGIN > current + viewport ->
                            bottom + CARET_MARGIN - viewport
                        top - CARET_MARGIN < current -> top - CARET_MARGIN
                        else -> null
                    }
                    if (target != null) {
                        pageScroll.animateScrollTo(
                            target.toInt().coerceIn(0, pageScroll.maxValue)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(pageScroll),
                ) {
                    // Les lignes d'ecriture restent tout le temps ; la grille
                    // des photos, elle, n'apparait que pendant qu'on en
                    // deplace une. Deux choses differentes, deux durees de vie.
                    //
                    // matchParentSize, et non une hauteur calculee : c'est le
                    // texte qui decide de la hauteur de la page, et une hauteur
                    // fixee d'avance laissait le bas de la page sans lignes des
                    // que le texte depassait — d'ou les lignes qui semblaient
                    // s'arreter apres un titre, ou disparaitre a l'ouverture du
                    // clavier, qui reduit la page visible.
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
                                // champ qui est devant. On la reprend par la liste
                                // "Photos de la page".
                                onSelect = null,
                            )
                        }

                    BasicTextField(
                        value = body,
                        onValueChange = { updated ->
                            // L'utilisateur reprend la main sur le texte : la selection
                            // mise de cote pour le panneau n'a plus lieu d'etre. Mais
                            // seulement panneau ferme : en perdant le focus, le champ
                            // annonce lui-meme une selection vide, et il ne faut pas la
                            // prendre pour un geste de l'utilisateur.
                            if (openPanel == null) heldSelection = null
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
                            color = ink,
                            fontFamily = baseFont,
                            fontSize = textSize.sp,
                            // La hauteur de ligne vient d'une mesure en points,
                            // pas d'une taille de police : agrandir les
                            // caracteres dans les reglages d'Android decalerait
                            // sinon le texte de ses lignes.
                            lineHeight = rhythm,
                            // Sans ca, Compose repartit l'espace d'une ligne
                            // autour du texte, et rogne meme celui de la
                            // premiere : le texte flottait au-dessus de ses
                            // lignes, d'un ecart different a chaque ligne. Cale
                            // en bas et sans rognage, chaque ligne fait
                            // exactement le pas du lignage, texte pose dessus.
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Bottom,
                                trim = LineHeightStyle.Trim.None,
                            ),
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        // La mise en page du texte, gardee pour savoir **ou** est
                        // le curseur. C'est la seule facon de le suivre ici : le
                        // champ ne defile pas lui-meme, il grandit, et c'est la
                        // page autour de lui qui defile.
                        onTextLayout = { bodyLayout.value = it },
                        visualTransformation = run {
                            // Sans focus, le champ ne peint plus la selection : on la
                            // dessine nous-memes, sinon on colore a l'aveugle.
                            val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            remember(spans, heldSelection, tint, rhythm) {
                                SpanTransformation(spans, heldSelection, tint, rhythm)
                            }
                        },
                        decorationBox = { field ->
                            Box {
                                if (body.text.isEmpty()) {
                                    Text(
                                        "Écris ce que tu veux, comme tu veux.",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = baseFont,
                                            fontSize = textSize.sp,
                                        ),
                                        color = ink.copy(alpha = 0.45f),
                                    )
                                }
                                field()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            // La page entiere est a l'ecriture : appuyer n'importe ou
                            // dessous pose le curseur, meme loin sous le dernier mot.
                            .heightIn(min = pageHeight)
                            .padding(horizontal = 20.dp, vertical = JournalPaper.TOP_PADDING)
                            // Apres la marge, pas avant : l'origine du dessin
                            // doit etre celle du texte, sinon les pastilles
                            // sont decalees de toute la marge.
                            .openPageLinkOnTap(bodyLayout, { body.text }, onOpenDay)
                            .quoteBars(bodyLayout) {
                                // Relu au dessin : poser une citation ou en
                                // changer la couleur ne doit pas remesurer la
                                // page entiere.
                                spans.filter { it.style == TextStyleKind.QUOTE && !it.isEmpty }
                                    .map { quote ->
                                        val tint = spans.firstOrNull {
                                            it.style.family == StyleFamily.COLOR &&
                                                it.start <= quote.start && it.end >= quote.end
                                        }
                                        QuoteBar(
                                            range = quote.start until quote.end,
                                            color = tint?.let { Color(it.style.argb) } ?: ink,
                                        )
                                    }
                            }
                            .hashtagChips(bodyLayout, textSize.toFloat())
                            .focusRequester(bodyFocus)
                            .onFocusChanged { state ->
                                // Retourner ecrire referme le panneau : sinon il reste
                                // sous le clavier qui remonte, et les deux s'empilent.
                                // C'est l'appui de l'utilisateur qui decide du curseur,
                                // donc on ne rend pas la selection mise de cote.
                                if (state.isFocused && openPanel != null) {
                                    openPanel = null
                                    heldSelection = null
                                    awaitingKeyboard = false
                                }
                            }
                            .selectWordOnDoubleTap({ body }) { body = body.copy(selection = it) }
                            .testTag("day-note-field"),
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

                    // Le cadre de manipulation passe par-dessus tout, meme sur une
                    // photo de fond que le texte recouvre.
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
                        // Le panneau prend la place du clavier : l'un se ferme pour
                        // que l'autre s'ouvre, et la hauteur totale ne bouge pas.
                        showPanel(if (openPanel == panel) null else panel)
                    },
                    onStyle = { style ->
                        // Le panneau reste ouvert : on essaie rarement une seule
                        // nuance, et le refermer a chaque essai obligeait a le
                        // rouvrir pour comparer. Il ne se ferme que quand
                        // l'action a insere du texte — la, il n'y a plus rien a
                        // reessayer, et il faut voir ou on en est.
                        val inserted = !hasSelection && style.takesWholeLine
                        applyStyle(style)
                        if (inserted) showPanel(null)
                    },
                    onClearHeading = { clearHeading() },
                    onClearFont = { clearFont() },
                    onList = { marker ->
                        prefixLine(marker.marker)
                        showPanel(null)
                    },
                    onHashtag = {
                        // Le panneau se referme et le clavier revient : on vient
                        // d'ouvrir un mot, il faut pouvoir l'ecrire.
                        showPanel(null)
                        insertHashtag()
                        bodyFocus.requestFocus()
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
