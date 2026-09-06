package com.ismael.daybyday.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DayPart
import com.ismael.daybyday.data.DoseTime
import com.ismael.daybyday.data.FoodLevel
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaKind
import com.ismael.daybyday.data.Memory
import com.ismael.daybyday.data.MoneyCategory
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.Prayer
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Treatment
import com.ismael.daybyday.data.TagCategory
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.health.HealthConnectSource
import com.ismael.daybyday.health.ScreenTimeSource
import com.ismael.daybyday.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

private const val SAVE_DEBOUNCE_MS = 400L

/**
 * Les deux etiquettes qui disent comment la nuit s'est passee. Elles
 * s'excluent : c'est un choix, pas deux faits, et l'ecran l'impose maintenant
 * — auparavant rien n'empechait de cocher « bien dormi » et « mal dormi ».
 */
private val SLEEP_QUALITY = listOf("sleep_good", "sleep_bad")

/** Les gens qu'on a pu voir. « Personne aujourd'hui » vit a part, et les efface. */
private val SEEN = listOf("girlfriend", "friends", "family")

/**
 * Les reperes des deux barres de mesure. Ce ne sont **pas** des objectifs :
 * l'application ne demande rien et ne felicite de rien. Ils servent seulement a
 * situer un chiffre — un nombre de pas seul ne dit pas s'il est grand.
 */
private const val STEPS_REFERENCE = 6000

private const val SCREEN_REFERENCE = 6

/** Les pas et le temps d'ecran sont relus a ce rythme sur la journee en cours. */
private const val MEASURE_REFRESH_MS = 60_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayScreen(
    initialDate: LocalDate,
    onBack: () -> Unit,
    onOrganizeCards: () -> Unit,
    onOpenJournal: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    val app = context.dayByDayApp
    val repository = app.repository
    val scope = rememberCoroutineScope()

    var epochDay by rememberSaveable { mutableLongStateOf(initialDate.toEpochDay()) }
    val date = LocalDate.ofEpochDay(epochDay)
    val birthday = app.prefs.birthDate
    val isBirthday = date.dayOfMonth == birthday.dayOfMonth && date.monthValue == birthday.monthValue

    // Le meme jour, les annees d'avant. La recherche remonte jusqu'a douze ans
    // et s'arrete a la premiere annee qui a quelque chose a montrer ; s'il n'y
    // a rien nulle part, il n'y a pas de carte du tout.
    var memory by remember { mutableStateOf<Memory?>(null) }
    LaunchedEffect(epochDay) {
        memory = withContext(Dispatchers.IO) { repository.memoryFor(date) }
    }

    var colorKey by remember { mutableStateOf<Int?>(null) }
    var colorManual by remember { mutableStateOf(false) }
    var parts by remember { mutableStateOf<Map<DayPart, Int>>(emptyMap()) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var noteSpansEncoded by remember { mutableStateOf("") }
    var sportLevel by remember { mutableStateOf<Int?>(null) }
    var foodLevel by remember { mutableStateOf<Int?>(null) }
    var wentOut by remember { mutableStateOf<Boolean?>(null) }
    var weightText by remember { mutableStateOf("") }
    var stepsValue by remember { mutableStateOf<Int?>(null) }
    var screenValue by remember { mutableStateOf<Int?>(null) }
    var stepsGranted by remember { mutableStateOf(false) }
    var screenGranted by remember { mutableStateOf(false) }
    var measuresTick by remember { mutableIntStateOf(0) }
    val healthAvailable = remember { HealthConnectSource.isAvailable(context) }
    var loadedFor by remember { mutableStateOf<Long?>(null) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    var addingMoney by remember { mutableStateOf(false) }
    var editingMoney by remember { mutableStateOf<MoneyEntry?>(null) }
    var sleepStart by remember { mutableStateOf<Int?>(null) }
    var sleepEnd by remember { mutableStateOf<Int?>(null) }
    var sleepFromDevice by remember { mutableStateOf(false) }
    var waterGlasses by remember { mutableStateOf<Int?>(null) }
    var mealsNote by remember { mutableStateOf("") }
    var prayerMask by remember { mutableStateOf<Int?>(null) }
    var editingTreatment by remember { mutableStateOf<Treatment?>(null) }
    var creatingTreatment by remember { mutableStateOf(false) }

    // La disposition des cartes vit dans les preferences : elle est relue a
    // chaque changement pour que l'ecran suive immediatement.
    var layoutTick by remember { mutableIntStateOf(0) }
    val visibleCards = remember(layoutTick) { app.prefs.visibleDayCards }
    val collapsedCards = remember(layoutTick) { app.prefs.collapsedDayCards }

    val treatments by remember { repository.observeTreatments() }
        .collectAsStateWithLifecycle(emptyList())
    val dosesTaken by remember(epochDay) { repository.observeDosesForDay(date) }
        .collectAsStateWithLifecycle(emptyList())

    val mediaItems by remember(epochDay) { repository.observeMediaForDay(date) }
        .collectAsStateWithLifecycle(emptyList())
    val allTags by remember { repository.observeTags() }
        .collectAsStateWithLifecycle(emptyList())
    val dayTags by remember(epochDay) { repository.observeTagsForDay(date) }
        .collectAsStateWithLifecycle(emptyList())
    val dayMoney by remember(epochDay) { repository.observeMoneyBetween(date, date) }
        .collectAsStateWithLifecycle(emptyList())
    val selectedTagIds = dayTags.map { it.id }.toSet()

    fun currentEntry(day: Long) = DayEntry(
        epochDay = day,
        colorKey = colorKey,
        title = title.trim(),
        note = note,
        noteSpans = noteSpansEncoded,
        sportLevel = sportLevel,
        foodLevel = foodLevel,
        wentOut = wentOut,
        weightKg = weightText.replace(',', '.').toDoubleOrNull(),
        steps = stepsValue,
        screenMinutes = screenValue,
        sleepStartMinutes = sleepStart,
        sleepEndMinutes = sleepEnd,
        sleepFromDevice = sleepFromDevice,
        waterGlasses = waterGlasses,
        mealsNote = mealsNote.trim(),
        partMorning = parts[DayPart.MORNING],
        partAfternoon = parts[DayPart.AFTERNOON],
        partEvening = parts[DayPart.EVENING],
        partNight = parts[DayPart.NIGHT],
        colorManual = colorManual,
        prayerMask = prayerMask,
    )

    /** Applique la couleur d'un moment, puis recalcule la couleur du jour. */
    fun setPart(part: DayPart, key: Int?) {
        parts = if (key == null) parts - part else parts + (part to key)
        if (!colorManual) colorKey = averageColorKey(parts.values)
    }

    LaunchedEffect(epochDay) {
        loadedFor = null
        val entry = repository.observeDay(LocalDate.ofEpochDay(epochDay)).first()
        colorKey = entry?.colorKey
        colorManual = entry?.colorManual ?: (entry?.colorKey != null)
        parts = DayPart.entries.mapNotNull { part ->
            entry?.partColorKey(part)?.let { part to it }
        }.toMap()
        title = entry?.title.orEmpty()
        note = entry?.note.orEmpty()
        noteSpansEncoded = entry?.noteSpans.orEmpty()
        sportLevel = entry?.sportLevel
        foodLevel = entry?.foodLevel
        wentOut = entry?.wentOut
        weightText = entry?.weightKg?.let { String.format(Locale.FRANCE, "%.1f", it) }.orEmpty()
        stepsValue = entry?.steps
        screenValue = entry?.screenMinutes
        sleepStart = entry?.sleepStartMinutes
        sleepEnd = entry?.sleepEndMinutes
        sleepFromDevice = entry?.sleepFromDevice ?: false
        waterGlasses = entry?.waterGlasses
        mealsNote = entry?.mealsNote.orEmpty()
        prayerMask = entry?.prayerMask
        loadedFor = epochDay
    }

    // Pas et temps d'ecran, lus en local. Ces deux mesures bougent toute la
    // journee : on les relit a chaque retour dans l'application, et toutes les
    // minutes tant que la journee affichee est celle en cours.
    LaunchedEffect(epochDay, loadedFor, measuresTick) {
        if (loadedFor != epochDay) return@LaunchedEffect
        val day = LocalDate.ofEpochDay(epochDay)
        withContext(Dispatchers.IO) {
            val granted = HealthConnectSource.hasPermission(context)
            val steps = if (granted) HealthConnectSource.stepsFor(context, day) else null
            val screen = ScreenTimeSource.minutesFor(context, day)
            // La nuit lue sur le telephone ne remplace jamais une saisie a la
            // main : elle ne comble que ce qui est encore vide.
            val night = if (granted) HealthConnectSource.nightFor(context, day) else null
            withContext(Dispatchers.Main) {
                stepsGranted = granted
                screenGranted = ScreenTimeSource.hasPermission(context)
                steps?.let { stepsValue = it }
                screen?.let { screenValue = it }
                if (night != null && (sleepStart == null || sleepFromDevice)) {
                    sleepStart = night.startMinutes
                    sleepEnd = night.endMinutes
                    sleepFromDevice = true
                }
            }
        }
    }

    LaunchedEffect(epochDay, loadedFor, measuresTick) {
        if (loadedFor != epochDay) return@LaunchedEffect
        val entry = repository.dayOnce(LocalDate.ofEpochDay(epochDay)) ?: return@LaunchedEffect
        title = entry.title
        note = entry.note
        noteSpansEncoded = entry.noteSpans
    }

    // Relecture a chaque fois que l'ecran redevient visible, puis chaque
    // minute tant qu'on regarde la journee en cours. Rien ne tourne quand
    // l'application passe en arriere-plan.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(epochDay, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            measuresTick += 1
            while (LocalDate.ofEpochDay(epochDay) == LocalDate.now()) {
                delay(MEASURE_REFRESH_MS)
                measuresTick += 1
            }
        }
    }

    LaunchedEffect(
        epochDay,
        loadedFor,
        colorKey,
        colorManual,
        parts,
        title,
        note,
        noteSpansEncoded,
        sportLevel,
        foodLevel,
        wentOut,
        weightText,
        stepsValue,
        screenValue,
        sleepStart,
        sleepEnd,
        waterGlasses,
        mealsNote,
    ) {
        if (loadedFor != epochDay) return@LaunchedEffect
        delay(SAVE_DEBOUNCE_MS)
        repository.saveDay(currentEntry(epochDay))
    }

    DisposableEffect(epochDay, loadedFor) {
        // Le jour est fige ici : au moment du onDispose, epochDay peut deja
        // pointer vers le jour suivant alors que les champs contiennent encore
        // le contenu du jour precedent.
        val dayOfThisEffect = epochDay
        val contentIsLoaded = loadedFor == epochDay
        onDispose {
            if (contentIsLoaded) {
                val snapshot = currentEntry(dayOfThisEffect)
                app.appScope.launch { repository.saveDay(snapshot) }
            }
        }
    }

    // La carte depuis laquelle la photo a ete demandee : elle lui reste
    // attachee. Depuis "Photos & videos", le media appartient a la journee
    // entiere et ne va sous aucune carte.
    var mediaTarget by remember { mutableStateOf<DayCard?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(30)
    ) { uris ->
        val card = mediaTarget
        mediaTarget = null
        if (uris.isNotEmpty()) {
            val targetDate = LocalDate.ofEpochDay(epochDay)
            app.appScope.launch {
                uris.forEach { uri -> repository.addMedia(targetDate, uri, card) }
            }
        }
    }

    fun addMediaTo(card: DayCard?) {
        mediaTarget = card
        pickMedia.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    // Les reperes rapides sont des etiquettes en base, mais l'ecran ne les
    // manipule plus en vrac : il va chercher celle dont il a besoin par son
    // `slug`, qui est stable, et lui donne la forme qui convient a la question.
    fun tagOf(slug: String) = allTags.firstOrNull { it.slug == slug }

    fun tagOn(slug: String): Boolean {
        val id = tagOf(slug)?.id ?: return false
        return id in selectedTagIds
    }

    fun setTag(slug: String, on: Boolean) {
        val tag = tagOf(slug) ?: return
        if ((tag.id in selectedTagIds) == on) return
        scope.launch { repository.toggleTag(date, tag, on) }
    }

    /** Le libelle d'une etiquette, pris dans le catalogue plutot que recopie ici. */
    fun segmentOf(slug: String): Segment {
        val tag = tagOf(slug)
        return Segment(tag?.emoji.orEmpty(), tag?.name ?: slug)
    }

    val tagChips: @Composable (TagCategory, Color) -> Unit = { family, tint ->
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            allTags.filter { it.group == family }.forEach { tag ->
                val selected = tag.id in selectedTagIds
                CardChip(
                    label = tag.display,
                    selected = selected,
                    tint = tint,
                    onClick = {
                        scope.launch { repository.toggleTag(date, tag, !selected) }
                    },
                )
            }
        }
    }

    /**
     * L'etat d'une carte, en quelques mots, montre sous son titre.
     *
     * C'est ce qui rend une carte repliee encore utile : on voit sans ouvrir.
     * Rien a dire vaut `null` — une ligne « aucune donnee » sous chaque titre
     * ferait douze lignes qui ne disent rien.
     */
    fun summaryFor(card: DayCard): String? = when (card) {
        DayCard.MOOD -> DayColor.fromKey(colorKey)?.label
        DayCard.JOURNAL -> title.trim().ifBlank {
            note.trim().replace('\n', ' ').take(40).ifBlank { null }
        }
        DayCard.SLEEP -> {
            val hours = durationMinutes(sleepStart, sleepEnd)?.let { formatDuration(it) }
            val quality = SLEEP_QUALITY.firstOrNull { tagOn(it) }?.let { tagOf(it)?.name }
            listOfNotNull(hours, quality?.lowercase(Locale.FRANCE)).joinToString(" · ")
                .ifBlank { null }
        }
        DayCard.ACTIVITY -> listOfNotNull(
            stepsValue?.let { "${formatSteps(it)} pas" },
            SportLevel.fromKey(sportLevel)?.label?.lowercase(Locale.FRANCE),
        ).joinToString(" · ").ifBlank { null }
        DayCard.FOOD -> listOfNotNull(
            FoodLevel.fromKey(foodLevel)?.label,
            waterGlasses?.let { "$it verre(s)" },
        ).joinToString(" · ").ifBlank { null }
        DayCard.HEALTH -> {
            val expected = treatments.filter { it.active }.sumOf { it.times.size }
            listOfNotNull(
                weightText.takeIf { it.isNotBlank() }?.let { "$it kg" },
                if (expected > 0) "${dosesTaken.size} prise(s) sur $expected" else null,
            ).joinToString(" · ").ifBlank { null }
        }
        DayCard.SOCIAL -> when {
            tagOn("alone") -> "Personne aujourd'hui"
            else -> SEEN.filter { tagOn(it) }.mapNotNull { tagOf(it)?.name }
                .joinToString(", ").ifBlank { null }
        }
        DayCard.WORK -> allTags
            .count { it.group == TagCategory.WORK && it.id in selectedTagIds }
            .takeIf { it > 0 }
            ?.let { "$it avancée(s)" }
        DayCard.OUTSIDE -> listOfNotNull(
            when (wentOut) {
                true -> "Sorti"
                false -> "Resté à la maison"
                null -> null
            },
            screenValue?.let { formatScreenTime(it) },
        ).joinToString(" · ").ifBlank { null }
        DayCard.MONEY -> dayMoney.takeIf { it.isNotEmpty() }
            ?.let { formatSignedMoney(it.sumOf { entry -> entry.amountCents }) }
        DayCard.PRAYER -> prayerMask?.let { mask ->
            "${Prayer.entries.count { mask and it.bit != 0 }} sur ${Prayer.entries.size}"
        }
        DayCard.MEDIA -> mediaItems.size.takeIf { it > 0 }?.let { "$it fichier(s)" }
    }

    ScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // imePadding avant verticalScroll : la zone visible se reduit
                // quand le clavier s'ouvre, donc le curseur reste au-dessus.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))

            ScreenTitle(
                text = "Ma",
                accent = "journée",
                trailing = {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "Retour",
                        onClick = onBack,
                    )
                },
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    label = "Jour précédent",
                    onClick = { epochDay -= 1 },
                )
                Text(
                    text = Dates.dayLong(date),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    label = "Jour suivant",
                    onClick = { epochDay += 1 },
                )
            }

            if (isBirthday) {
                Text(
                    text = "🎂 Ton anniversaire",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            if (date != LocalDate.now()) {
                TextButton(
                    onClick = { epochDay = LocalDate.now().toEpochDay() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Aller à aujourd'hui")
                }
            }

            memory?.let { souvenir ->
                Spacer(Modifier.height(14.dp))
                MemoryCard(
                    memory = souvenir,
                    onOpen = { epochDay = souvenir.date.toEpochDay() },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Chaque partie de la journee est une carte : repliable, masquable,
            // deplacable. La disposition vit dans les preferences, pas ici.
            visibleCards.forEach { card ->
                DayCardShell(
                    card = card,
                    collapsed = card in collapsedCards,
                    summary = summaryFor(card),
                    // Une seule carte porte la couleur : celle de l'humeur, qui
                    // est la raison d'etre de la page. Les autres restent
                    // blanches autour — c'est ce contraste qui donne la
                    // hierarchie, pas la taille des titres.
                    //
                    // Elle porte toujours un degrade : c'est
                    // une carte en degrade qu'on a touchee pour arriver ici, on
                    // doit tomber sur la meme. Elle prend la couleur du jour
                    // des qu'il y en a une, celle de l'application tant qu'il
                    // n'y en a pas — et alors son encre est le blanc des autres
                    // cartes fortes, pas celle que le calcul choisirait.
                    accent = if (card == DayCard.MOOD) {
                        DayColor.fromKey(colorKey)?.gradient ?: Brand.gradient
                    } else {
                        null
                    },
                    accentInk = if (card == DayCard.MOOD && DayColor.fromKey(colorKey) == null) {
                        Color.White
                    } else {
                        null
                    },
                    onToggleCollapse = {
                        val current = app.prefs.collapsedDayCards
                        app.prefs.collapsedDayCards =
                            if (card in current) current - card else current + card
                        layoutTick += 1
                    },
                ) {
                    val tint = cardStyle(card).tint

                    when (card) {
                        DayCard.MOOD -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                DayColor.entries.forEachIndexed { index, dayColor ->
                                    ColorChoice(
                                        dayColor = dayColor,
                                        selected = colorKey == dayColor.key,
                                        anySelected = colorKey != null,
                                        index = index,
                                        onClick = {
                                            if (colorKey == dayColor.key && colorManual) {
                                                colorManual = false
                                                colorKey = averageColorKey(parts.values)
                                            } else {
                                                colorKey = dayColor.key
                                                colorManual = true
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = DayColor.fromKey(colorKey)?.label ?: "Aucune couleur pour l'instant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (parts.isNotEmpty()) {
                                Text(
                                    text = if (colorManual) {
                                        "Choisie à la main. Touche-la à nouveau pour revenir à la moyenne de tes moments."
                                    } else {
                                        "Calculée à partir de tes moments."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )

                            Text(
                                "Moment par moment",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Ton humeur bouge dans la journée : la couleur du jour se calcule à partir d'ici.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            DayPart.entries.forEach { part ->
                                PartRow(
                                    part = part,
                                    selectedKey = parts[part],
                                    onPick = { key -> setPart(part, key) },
                                )
                            }
                        }
                        DayCard.JOURNAL -> {
                            JournalPreview(
                                title = title,
                                body = note,
                                spans = RichText.decode(noteSpansEncoded, note.length),
                                onOpen = { onOpenJournal(date) },
                            )
                        }
                        DayCard.SLEEP -> {
                            SleepCardBody(
                                startMinutes = sleepStart,
                                endMinutes = sleepEnd,
                                fromDevice = sleepFromDevice,
                                tint = tint,
                                onPickStart = {
                                    showClock(context, sleepStart ?: 23 * 60) { minutes ->
                                        sleepStart = minutes
                                        sleepFromDevice = false
                                    }
                                },
                                onPickEnd = {
                                    showClock(context, sleepEnd ?: 8 * 60) { minutes ->
                                        sleepEnd = minutes
                                        sleepFromDevice = false
                                    }
                                },
                                onClear = {
                                    sleepStart = null
                                    sleepEnd = null
                                    sleepFromDevice = false
                                },
                            )
                            Spacer(Modifier.height(18.dp))
                            // Bien dormi et mal dormi ne peuvent pas etre vrais
                            // ensemble : c'est un choix, donc un selecteur. Les
                            // deux etaient auparavant deux pastilles voisines,
                            // et rien n'empechait de cocher les deux.
                            CardSection("Comment tu as dormi", tint) {
                                SegmentedChoice(
                                    options = SLEEP_QUALITY.map { segmentOf(it) },
                                    selectedIndex = SLEEP_QUALITY.indexOfFirst { tagOn(it) }
                                        .takeIf { it >= 0 },
                                    tint = tint,
                                    onSelect = { chosen ->
                                        SLEEP_QUALITY.forEachIndexed { index, option ->
                                            setTag(option, index == chosen)
                                        }
                                    },
                                )
                                Spacer(Modifier.height(10.dp))
                                CardChip(
                                    label = "🌙 Couché tard",
                                    selected = tagOn("sleep_late"),
                                    tint = tint,
                                    onClick = { setTag("sleep_late", !tagOn("sleep_late")) },
                                )
                            }
                        }
                        DayCard.ACTIVITY -> {
                            MetricRow(
                                emoji = "👟",
                                label = if (date == LocalDate.now()) "Pas aujourd'hui" else "Pas ce jour-là",
                                value = stepsValue?.let { formatSteps(it) },
                                caption = when {
                                    stepsValue != null -> "Une journée ordinaire tourne autour de $STEPS_REFERENCE pas."
                                    !healthAvailable -> "Health Connect n'est pas installé sur ce téléphone."
                                    !stepsGranted -> "Appuie pour autoriser Health Connect."
                                    else -> "Autorisé, mais aucun pas enregistré pour l'instant."
                                },
                                progress = stepsValue?.let { it / STEPS_REFERENCE.toFloat() },
                                tint = tint,
                                onClick = {
                                    openSystemScreen(context, HealthConnectSource.settingsIntent())
                                },
                            )
                            Spacer(Modifier.height(18.dp))
                            CardSection("Tu as bougé ?", tint) {
                                SegmentedChoice(
                                    options = SportLevel.entries.map { Segment(it.emoji, it.label) },
                                    selectedIndex = SportLevel.entries
                                        .indexOfFirst { it.key == sportLevel }
                                        .takeIf { it >= 0 },
                                    tint = tint,
                                    onSelect = { chosen ->
                                        sportLevel = chosen?.let { SportLevel.entries[it].key }
                                    },
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            CardSection("Ce que tu as fait", tint) {
                                tagChips(TagCategory.ACTIVITY, tint)
                            }
                        }
                        DayCard.FOOD -> {
                            CardSection("Comment tu as mangé", tint) {
                                SegmentedChoice(
                                    options = FoodLevel.entries.map { Segment(it.emoji, it.label) },
                                    selectedIndex = FoodLevel.entries
                                        .indexOfFirst { it.key == foodLevel }
                                        .takeIf { it >= 0 },
                                    tint = tint,
                                    onSelect = { chosen ->
                                        foodLevel = chosen?.let { FoodLevel.entries[it].key }
                                    },
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            // Huit verres, pas un compteur a fleches : on les
                            // compte d'un coup d'oeil et on les remplit d'un
                            // seul doigt.
                            CardSection("Ce que tu as bu", tint) {
                                WaterGlasses(
                                    count = waterGlasses,
                                    tint = tint,
                                    onChange = { waterGlasses = it },
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = waterGlasses?.let {
                                        "$it verre(s) · environ ${formatLitres(it)}"
                                    } ?: "Touche un verre pour compter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            CardSection("Ce qu'il y a eu", tint) {
                                tagChips(TagCategory.FOOD, tint)
                            }
                            Spacer(Modifier.height(18.dp))
                            OutlinedTextField(
                                value = mealsNote,
                                onValueChange = { mealsNote = it.take(500) },
                                label = { Text("Ce que tu as mangé (optionnel)") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp),
                            )
                        }
                        DayCard.HEALTH -> {
                            // Le poids a quitte l'activite physique : c'est une
                            // mesure du corps, pas une facon d'avoir bouge. Il
                            // n'avait rien a faire entre une seance de sport et
                            // une paire de baskets.
                            CardSection("Ton poids", tint) {
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { input ->
                                        weightText = input
                                            .filter { it.isDigit() || it == ',' || it == '.' }
                                            .take(6)
                                    },
                                    label = { Text("Poids du jour (optionnel)") },
                                    suffix = { Text("kg") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            CardSection("Tes traitements", tint) {
                                TreatmentsCardBody(
                                    treatments = treatments,
                                    taken = dosesTaken,
                                    tint = tint,
                                    onToggle = { treatment, time, checked ->
                                        scope.launch {
                                            repository.setDoseTaken(date, treatment.id, time, checked)
                                        }
                                    },
                                    onEdit = { editingTreatment = it },
                                    onAdd = { creatingTreatment = true },
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            CardSection("Ce qu'il s'est passé", tint) {
                                tagChips(TagCategory.HEALTH, tint)
                            }
                        }
                        DayCard.PRAYER -> {
                            PrayerCardBody(
                                mask = prayerMask,
                                tint = tint,
                                onToggle = { prayer, done ->
                                    prayerMask = currentEntry(epochDay).withPrayer(prayer, done)
                                },
                            )
                        }
                        DayCard.SOCIAL -> {
                            // « Personne aujourd'hui » ne peut pas cohabiter
                            // avec « ma copine » : choisir l'un efface l'autre.
                            // Rien ne l'empechait avant, et une journee pouvait
                            // dire les deux a la fois.
                            CardSection("Qui tu as vu", tint) {
                                MultiTiles(
                                    options = SEEN.map { segmentOf(it) },
                                    selected = SEEN.indices
                                        .filter { tagOn(SEEN[it]) }
                                        .toSet(),
                                    tint = tint,
                                    onToggle = { index ->
                                        val slug = SEEN[index]
                                        val turningOn = !tagOn(slug)
                                        setTag(slug, turningOn)
                                        if (turningOn) setTag("alone", false)
                                    },
                                )
                                Spacer(Modifier.height(10.dp))
                                CheckRow(
                                    label = "🙈 Personne aujourd'hui",
                                    checked = tagOn("alone"),
                                    tint = tint,
                                    onClick = {
                                        val turningOn = !tagOn("alone")
                                        setTag("alone", turningOn)
                                        if (turningOn) SEEN.forEach { setTag(it, false) }
                                    },
                                )
                            }
                        }
                        DayCard.WORK -> {
                            // Une liste de cases cochees se lit comme une liste
                            // de choses faites. Les memes items en pastilles se
                            // lisaient comme un classement.
                            CardSection("Ce que tu as avancé", tint) {
                                val workTags = allTags.filter { it.group == TagCategory.WORK }
                                workTags.forEachIndexed { index, tag ->
                                    if (index > 0) Spacer(Modifier.height(8.dp))
                                    val selected = tag.id in selectedTagIds
                                    CheckRow(
                                        label = tag.display,
                                        checked = selected,
                                        tint = tint,
                                        onClick = {
                                            scope.launch {
                                                repository.toggleTag(date, tag, !selected)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        DayCard.OUTSIDE -> {
                            CardSection("Tu es sorti ?", tint) {
                                SegmentedChoice(
                                    options = listOf(
                                        Segment("🚪", "Sorti"),
                                        Segment("🛋️", "Resté à la maison"),
                                    ),
                                    selectedIndex = when (wentOut) {
                                        true -> 0
                                        false -> 1
                                        null -> null
                                    },
                                    tint = tint,
                                    onSelect = { chosen ->
                                        wentOut = when (chosen) {
                                            0 -> true
                                            1 -> false
                                            else -> null
                                        }
                                    },
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            MetricRow(
                                emoji = "📱",
                                label = "Temps sur le téléphone",
                                value = screenValue?.let { formatScreenTime(it) },
                                caption = when {
                                    screenValue != null ->
                                        "Sur une échelle de $SCREEN_REFERENCE h."
                                    screenGranted -> "Autorisé, mais rien de mesuré pour l'instant."
                                    else -> "Appuie pour autoriser l'accès aux données d'utilisation."
                                },
                                progress = screenValue?.let { it / (SCREEN_REFERENCE * 60f) },
                                tint = tint,
                                onClick = {
                                    openSystemScreen(context, ScreenTimeSource.settingsIntent(context))
                                },
                            )
                            Spacer(Modifier.height(18.dp))
                            CardSection("Sur quoi", tint) {
                                tagChips(TagCategory.SCREENS, tint)
                            }
                        }
                        DayCard.MONEY -> {
                            // Le bilan d'abord, les lignes ensuite. Une liste
                            // sans total oblige a additionner de tete ; c'est
                            // pourtant le total qu'on vient chercher.
                            MoneyDayHeader(entries = dayMoney, tint = tint)
                            if (dayMoney.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                dayMoney.forEach { entry ->
                                    MoneyDayRow(
                                        entry = entry,
                                        tint = tint,
                                        onClick = { editingMoney = entry },
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = { addingMoney = true },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tint,
                                    contentColor = readableOn(tint),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add-day-money"),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ajouter un mouvement")
                            }
                        }
                        DayCard.MEDIA -> {
                            if (mediaItems.isEmpty()) {
                                Text(
                                    "Aucun média pour ce jour. Les fichiers ajoutés sont copiés dans " +
                                        "l'espace privé de l'application.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                mediaItems.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rowItems.forEach { item ->
                                            MediaThumb(
                                                item = item,
                                                onClick = { viewerIndex = mediaItems.indexOf(item) },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { addMediaTo(null) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tint,
                                    contentColor = readableOn(tint),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ajouter une photo ou une vidéo")
                            }
                        }
                    }

                    // Les medias rattaches a cette carte, et de quoi en ajouter.
                    // Les reperes rapides, eux, ne sont plus ajoutes ici en bloc :
                    // chaque carte place les siens sous la question qu'ils
                    // precisent, dans la forme qui convient — pastilles, cases a
                    // cocher ou selecteur.
                    if (card.canHoldMedia) {
                        val cardMedia = mediaItems.filter { it.cardKey == card.key }
                        Spacer(Modifier.height(16.dp))
                        CardMediaRow(
                            items = cardMedia,
                            onOpen = { item -> viewerIndex = mediaItems.indexOf(item) },
                            onAdd = { addMediaTo(card) },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            OutlinedButton(
                onClick = onOrganizeCards,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Organiser ma journée")
            }

            Spacer(Modifier.height(48.dp))
        }
    }

    val index = viewerIndex
    if (index != null && index in mediaItems.indices) {
        MediaViewerDialog(
            items = mediaItems,
            startIndex = index,
            onDismiss = { viewerIndex = null },
            onDelete = { item ->
                viewerIndex = null
                scope.launch { repository.deleteMedia(item) }
            },
        )
    }

    if (creatingTreatment) {
        TreatmentDialog(
            initial = null,
            onDismiss = { creatingTreatment = false },
            onSave = { treatment ->
                creatingTreatment = false
                scope.launch {
                    repository.saveTreatment(treatment.copy(sortOrder = treatments.size))
                }
            },
        )
    }

    editingTreatment?.let { current ->
        TreatmentDialog(
            initial = current,
            onDismiss = { editingTreatment = null },
            onSave = { treatment ->
                editingTreatment = null
                scope.launch { repository.saveTreatment(treatment) }
            },
            onDelete = {
                editingTreatment = null
                scope.launch { repository.deleteTreatment(current) }
            },
        )
    }

    if (addingMoney) {
        MoneyEntryDialog(
            initial = null,
            defaultDate = date,
            allowDateChange = false,
            onDismiss = { addingMoney = false },
            onSave = { entry ->
                addingMoney = false
                scope.launch { repository.saveMoney(entry) }
            },
        )
    }

    editingMoney?.let { current ->
        MoneyEntryDialog(
            initial = current,
            defaultDate = LocalDate.ofEpochDay(current.epochDay),
            onDismiss = { editingMoney = null },
            onSave = { entry ->
                editingMoney = null
                scope.launch { repository.saveMoney(entry) }
            },
            onDelete = {
                editingMoney = null
                scope.launch { repository.deleteMoney(current) }
            },
        )
    }
}

/**
 * Le bilan du jour, en tete de la carte de l'argent.
 *
 * La liste des mouvements venait avant, et le total apres — il fallait donc
 * additionner de tete pour savoir ou l'on en etait, alors que c'est justement
 * ce qu'on vient chercher. Le total passe devant, les lignes le detaillent.
 *
 * Les corrections de solde ne sont comptees ni dans les rentrees ni dans les
 * depenses : sans cela, remettre le total juste apres une depense la compterait
 * une seconde fois, a l'envers.
 */
@Composable
private fun MoneyDayHeader(entries: List<MoneyEntry>, tint: Color) {
    val real = entries.filterNot { it.category == MoneyCategory.ADJUSTMENT }
    val income = real.filter { it.amountCents > 0 }.sumOf { it.amountCents }
    val spent = real.filter { it.amountCents < 0 }.sumOf { it.amountCents }
    val total = income + spent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.09f))
            .padding(16.dp),
    ) {
        Text(
            text = if (entries.isEmpty()) "Rien noté ce jour-là" else "Bilan du jour",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatSignedMoney(total),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                total < 0 -> DayColor.RED.color
                total > 0 -> tint
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (entries.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Ce que tu ajoutes ici remonte tout de suite dans l'onglet Argent.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MoneySide("Rentrées", income, tint)
                MoneySide("Dépenses", spent, DayColor.RED.color)
            }
        }
    }
}

@Composable
private fun MoneySide(label: String, cents: Long, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatMoney(kotlin.math.abs(cents)),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (cents == 0L) MaterialTheme.colorScheme.onSurfaceVariant else color,
        )
    }
}

/** Une ligne de mouvement : son signe, son libelle, son montant. */
@Composable
private fun MoneyDayRow(entry: MoneyEntry, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(entry.category?.emoji ?: if (entry.isIncome) "➕" else "➖")
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = entry.displayLabel,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatSignedMoney(entry.amountCents),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (entry.isIncome) tint else DayColor.RED.color,
        )
    }
}

/**
 * Les photos rattachees a une carte, et le bouton pour en ajouter. Discret
 * quand il n'y en a aucune : une ligne de texte, pas un cadre vide.
 */
@Composable
private fun CardMediaRow(
    items: List<MediaItem>,
    onOpen: (MediaItem) -> Unit,
    onAdd: () -> Unit,
) {
    if (items.isNotEmpty()) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    MediaThumb(
                        item = item,
                        onClick = { onOpen(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    // Une icone plutot qu'un bouton pleine largeur : sur une carte qui a deja
    // son contenu, un bandeau "Ajouter une photo" pesait plus qu'il ne servait.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .clickable(onClickLabel = "Ajouter une photo", onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Ajouter une photo",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (items.isEmpty()) {
            Spacer(Modifier.width(10.dp))
            Text(
                "Ajouter une photo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Ligne d'un moment de la journee : le libelle et les quatre couleurs. */
@Composable
private fun PartRow(part: DayPart, selectedKey: Int?, onPick: (Int?) -> Unit) {
    val partBorder = cardInk()
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = "${part.emoji} ${part.label}",
            style = MaterialTheme.typography.labelLarge,
            color = cardInkSoft(),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DayColor.entries.forEach { dayColor ->
                val selected = selectedKey == dayColor.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        // Un degrade, pas un aplat : chaque couleur de journee
                        // a le meme relief que le reste de l'application.
                        .background(Brush.linearGradient(dayColor.gradient))
                        // La bordure du choix se peint a l'encre de la carte :
                        // le bleu nuit du theme se perdait sur une journee tres
                        // noire, et on ne voyait plus ce qui etait selectionne.
                        .border(
                            BorderStroke(
                                if (selected) 3.dp else 0.dp,
                                if (selected) partBorder else Color.Transparent,
                            ),
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { onPick(if (selected) null else dayColor.key) }
                        .testTag("part-${part.name}-${dayColor.name}"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Sélectionné",
                            tint = readableOn(dayColor.color),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Une des quatre couleurs de la journee.
 *
 * C'est le geste principal de l'application : il devait cesser d'etre quatre
 * carres immobiles. Quatre choses s'y jouent, et aucune n'est decorative.
 *
 * - **L'arrivee en cascade.** Les tuiles montent l'une apres l'autre, a
 *   soixante millisecondes d'ecart. L'ecran se deploie au lieu d'apparaitre.
 * - **Le relief.** Une lumiere en haut a gauche et une ombre teintee de la
 *   couleur : la tuile a une epaisseur, elle n'est pas un aplat colle.
 * - **Le choix qui se sent.** La tuile choisie se souleve, grandit, et les
 *   trois autres reculent — elles palissent et se retrecissent. On voit ce
 *   qu'on a choisi sans avoir a chercher une coche.
 * - **Le reflet.** Un trait de lumiere traverse lentement la tuile choisie, en
 *   boucle. C'est ce qui la rend vivante plutot que simplement allumee, et il
 *   ne dit rien — contrairement a un clignotement, qui aurait l'air d'annoncer
 *   quelque chose.
 *
 * Tout est lu dans la couche graphique ou dans le dessin, jamais dans la
 * composition : rien n'est remesure pendant que ca bouge.
 */
@Composable
private fun ColorChoice(
    dayColor: DayColor,
    selected: Boolean,
    anySelected: Boolean,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        entered = true
    }
    val entrance = animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = Motion.softSpring(),
        label = "arrivee",
    )

    val lift = animateFloatAsState(
        targetValue = when {
            pressed -> 0.92f
            selected -> 1.10f
            anySelected -> 0.93f
            else -> 1f
        },
        animationSpec = Motion.softSpring(),
        label = "relief",
    )
    val presence = animateFloatAsState(
        targetValue = if (!anySelected || selected) 1f else 0.5f,
        animationSpec = tween(Motion.NORMAL),
        label = "presence",
    )

    // Le reflet ne tourne que sur la tuile choisie : trois reflets qui passent
    // en meme temps feraient une vitrine, pas un choix.
    val shine = if (selected) {
        val loop = rememberInfiniteTransition(label = "reflet")
        loop.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, delayMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "passage",
        )
    } else {
        null
    }

    val shape = RoundedCornerShape(22.dp)
    val markScale = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.softSpring(),
        label = "coche",
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                val appear = entrance.value
                alpha = appear * presence.value
                translationY = (1f - appear) * 26.dp.toPx()
                val grow = appear * lift.value
                scaleX = grow
                scaleY = grow
            }
            .brandShadow(elevation = 10.dp, shape = shape, color = dayColor.color)
            .clip(shape)
            .background(Brush.linearGradient(dayColor.gradient))
            // Le liseré blanc, et la raison d'etre de la carte d'humeur.
            // La couleur du jour descend maintenant jusque derriere ces
            // tuiles : sans ce contour, une tuile verte sur une journee verte
            // se fondrait dans le fond, exactement le defaut qu'on avait
            // corrige en raccourcissant le degrade. Le contour separe la tuile
            // de tout ce qui peut passer dessous — c'est ce qui permet au
            // degrade d'etre long.
            .border(BorderStroke(3.dp, Color.White.copy(alpha = 0.9f)), shape)
            .drawWithContent {
                // La lumiere du coin haut-gauche : c'est elle qui donne
                // l'epaisseur. Sans elle, la tuile est un timbre.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(size.width * 0.24f, size.height * 0.18f),
                        radius = size.width * 0.8f,
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.24f, size.height * 0.18f),
                )
                drawContent()
                val pass = shine?.value
                if (pass != null) {
                    val x = size.width * pass
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.42f),
                                Color.Transparent,
                            ),
                            start = Offset(x - size.width * 0.3f, 0f),
                            end = Offset(x + size.width * 0.3f, size.height),
                        )
                    )
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = dayColor.label,
                onClick = onClick,
            )
            .testTag("color-${dayColor.name}"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = if (selected) "Sélectionné" else null,
            tint = readableOn(dayColor.color),
            modifier = Modifier.graphicsLayer {
                scaleX = markScale.value
                scaleY = markScale.value
                alpha = markScale.value
            },
        )
    }
}

/**
 * Le meme jour, il y a un an — ou deux, ou cinq.
 *
 * Une seule regle de conception ici, et c'est celle qui fait la difference
 * entre un plaisir et une nuisance : **la carte n'existe que s'il y a quelque
 * chose**. Pas de « rien noté il y a un an », pas de cadre vide. Quand elle
 * apparait, c'est qu'il y a une couleur, un mot ou une photo, et un appui
 * emmene directement sur cette journee-la.
 *
 * Elle reste volontairement basse de ton : une ligne, une pastille de couleur,
 * une vignette. Ce n'est pas ce qu'on est venu faire — c'est un cadeau au
 * passage.
 */
@Composable
private fun MemoryCard(memory: Memory, onOpen: () -> Unit) {
    val entry = memory.entry
    val preview = entry.title.ifBlank { entry.note }.trim().replace('\n', ' ')

    SoftCard(onClick = onOpen, onClickLabel = "Ouvrir cette journée", padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            memory.photo?.let { photo ->
                MediaThumb(
                    item = photo,
                    onClick = onOpen,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    entry.color?.let { dayColor ->
                        ColorDot(color = dayColor.color, size = 9.dp)
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        text = if (memory.yearsAgo == 1) "Il y a un an" else "Il y a ${memory.yearsAgo} ans",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (preview.isBlank()) {
                        // Il y a forcement quelque chose, sinon la carte
                        // n'existerait pas : ici, ce sont les photos.
                        Dates.dayLong(memory.date)
                    } else {
                        preview
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun MediaThumb(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository = LocalContext.current.dayByDayApp.repository
    val file = remember(item.id) { repository.media.file(item.relativePath) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
    ) {
        MediaImage(
            file = file,
            kind = item.kind,
            modifier = Modifier.fillMaxSize(),
            maxSize = 512,
        )
        if (item.kind == MediaKind.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Vidéo", tint = Color.White)
            }
        }
    }
}

/** Couleur moyenne (arrondie) d'une liste de moments notes. */
private fun averageColorKey(partKeys: Collection<Int>): Int? {
    val colors = partKeys.mapNotNull { DayColor.fromKey(it) }
    if (colors.isEmpty()) return null
    val average = colors.sumOf { it.score }.toDouble() / colors.size
    return DayColor.entries.minByOrNull { kotlin.math.abs(it.score - average) }?.key
}

private fun formatSteps(steps: Int): String =
    steps.toString().reversed().chunked(3).joinToString(" ").reversed()

private fun formatScreenTime(minutes: Int): String =
    "${minutes / 60} h ${String.format(Locale.FRANCE, "%02d", minutes % 60)}"
