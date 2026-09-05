package com.ismael.daybyday.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.Treatment
import com.ismael.daybyday.data.TagCategory
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.health.HealthConnectSource
import com.ismael.daybyday.health.ScreenTimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

private const val SAVE_DEBOUNCE_MS = 400L

/** Les pas et le temps d'ecran sont relus a ce rythme sur la journee en cours. */
private const val MEASURE_REFRESH_MS = 60_000L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DayScreen(
    initialDate: LocalDate,
    onBack: () -> Unit,
    onOrganizeCards: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.dayByDayApp
    val repository = app.repository
    val scope = rememberCoroutineScope()

    var epochDay by rememberSaveable { mutableLongStateOf(initialDate.toEpochDay()) }
    val date = LocalDate.ofEpochDay(epochDay)
    val birthday = app.prefs.birthDate
    val isBirthday = date.dayOfMonth == birthday.dayOfMonth && date.monthValue == birthday.monthValue

    var colorKey by remember { mutableStateOf<Int?>(null) }
    var colorManual by remember { mutableStateOf(false) }
    var parts by remember { mutableStateOf<Map<DayPart, Int>>(emptyMap()) }
    var title by remember { mutableStateOf("") }
    var noteValue by remember { mutableStateOf(TextFieldValue("")) }
    var noteSpans by remember { mutableStateOf<List<TextSpan>>(emptyList()) }
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
        note = noteValue.text,
        noteSpans = RichText.encode(noteSpans),
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
        val loadedNote = entry?.note.orEmpty()
        noteValue = TextFieldValue(loadedNote)
        noteSpans = RichText.decode(entry?.noteSpans, loadedNote.length)
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
        noteValue.text,
        noteSpans,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ma journée") },
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
                // imePadding avant verticalScroll : la zone visible se reduit
                // quand le clavier s'ouvre, donc le curseur reste au-dessus.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { epochDay -= 1 }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Jour précédent",
                    )
                }
                Text(
                    text = Dates.dayLong(date),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { epochDay += 1 }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Jour suivant",
                    )
                }
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

            Spacer(Modifier.height(8.dp))

            // Chaque partie de la journee est une carte : repliable, masquable,
            // deplacable. La disposition vit dans les preferences, pas ici.
            visibleCards.forEach { card ->
                DayCardShell(
                    card = card,
                    collapsed = card in collapsedCards,
                    onToggleCollapse = {
                        val current = app.prefs.collapsedDayCards
                        app.prefs.collapsedDayCards =
                            if (card in current) current - card else current + card
                        layoutTick += 1
                    },
                ) {
                    when (card) {
                        DayCard.MOOD -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                DayColor.entries.forEach { dayColor ->
                                    ColorChoice(
                                        dayColor = dayColor,
                                        selected = colorKey == dayColor.key,
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
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = DayColor.fromKey(colorKey)?.label ?: "Aucune couleur pour l'instant",
                                style = MaterialTheme.typography.bodyLarge,
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

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

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
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Titre de la journée") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("day-title-field"),
                            )
                            Spacer(Modifier.height(12.dp))
                            JournalEditor(
                                value = noteValue,
                                spans = noteSpans,
                                onValueChange = { updated, updatedSpans ->
                                    noteValue = updated
                                    noteSpans = updatedSpans
                                },
                                modifier = Modifier.testTag("day-note-field"),
                            )
                        }
                        DayCard.SLEEP -> {
                            SleepCardBody(
                                startMinutes = sleepStart,
                                endMinutes = sleepEnd,
                                fromDevice = sleepFromDevice,
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
                        }
                        DayCard.ACTIVITY -> {
                            MeasureRow(
                                emoji = "👟",
                                label = if (date == LocalDate.now()) "Pas aujourd'hui" else "Pas ce jour-là",
                                value = stepsValue?.let { "${formatSteps(it)} pas" },
                                hint = when {
                                    !healthAvailable ->
                                        "Health Connect n'est pas installé sur ce téléphone."
                                    !stepsGranted -> "Appuie pour autoriser Health Connect."
                                    else -> "Autorisé, mais aucun pas enregistré pour l'instant."
                                },
                                onClick = { openSystemScreen(context, HealthConnectSource.settingsIntent()) },
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Une vraie séance ?",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SportLevel.entries.forEach { level ->
                                    ChoiceChip(
                                        label = "${level.emoji} ${level.label}",
                                        selected = sportLevel == level.key,
                                        onClick = {
                                            sportLevel = if (sportLevel == level.key) null else level.key
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = weightText,
                                onValueChange = { input ->
                                    weightText = input.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
                                },
                                label = { Text("Poids du jour (optionnel)") },
                                suffix = { Text("kg") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        DayCard.FOOD -> {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FoodLevel.entries.forEach { level ->
                                    ChoiceChip(
                                        label = "${level.emoji} ${level.label}",
                                        selected = foodLevel == level.key,
                                        onClick = {
                                            foodLevel = if (foodLevel == level.key) null else level.key
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = mealsNote,
                                onValueChange = { mealsNote = it.take(500) },
                                label = { Text("Ce que tu as mangé (optionnel)") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp),
                            )
                            Spacer(Modifier.height(14.dp))
                            WaterRow(
                                glasses = waterGlasses,
                                onChange = { waterGlasses = it },
                            )
                        }
                        DayCard.HEALTH -> {
                            TreatmentsCardBody(
                                treatments = treatments,
                                taken = dosesTaken,
                                onToggle = { treatment, time, checked ->
                                    scope.launch {
                                        repository.setDoseTaken(date, treatment.id, time, checked)
                                    }
                                },
                                onEdit = { editingTreatment = it },
                                onAdd = { creatingTreatment = true },
                            )
                        }
                        DayCard.SOCIAL, DayCard.WORK -> {
                            // Ces deux cartes ne sont faites que de leurs
                            // reperes : le bloc commun ci-dessous les affiche.
                        }
                        DayCard.OUTSIDE -> {
                            Text(
                                "Tu es sorti aujourd'hui ?",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ChoiceChip(
                                    label = "🚪 Oui, je suis sorti",
                                    selected = wentOut == true,
                                    onClick = { wentOut = if (wentOut == true) null else true },
                                )
                                ChoiceChip(
                                    label = "🛋️ Resté à la maison",
                                    selected = wentOut == false,
                                    onClick = { wentOut = if (wentOut == false) null else false },
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            MeasureRow(
                                emoji = "📱",
                                label = "Temps sur le téléphone",
                                value = screenValue?.let { formatScreenTime(it) },
                                hint = if (screenGranted) {
                                    "Autorisé, mais rien de mesuré pour l'instant."
                                } else {
                                    "Appuie pour autoriser l'accès aux données d'utilisation."
                                },
                                onClick = {
                                    openSystemScreen(context, ScreenTimeSource.settingsIntent(context))
                                },
                            )
                        }
                        DayCard.MONEY -> {
                            if (dayMoney.isEmpty()) {
                                Text(
                                    "Rien noté ce jour-là. Ce que tu ajoutes ici remonte tout de suite dans l'onglet Argent.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                dayMoney.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { editingMoney = entry }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(entry.category?.emoji ?: if (entry.isIncome) "➕" else "➖")
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = entry.displayLabel,
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Text(
                                            text = formatSignedMoney(entry.amountCents),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (entry.isIncome) DayColor.GREEN.color else DayColor.RED.color,
                                        )
                                    }
                                }
                                val total = dayMoney.sumOf { it.amountCents }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("Bilan du jour", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = formatSignedMoney(total),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (total < 0) DayColor.RED.color else DayColor.GREEN.color,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { addingMoney = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add-day-money"),
                            ) {
                                Text("Ajouter une dépense ou une rentrée")
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
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Ajouter une photo ou une vidéo")
                            }
                        }
                    }

                    // Les medias rattaches a cette carte, et de quoi en ajouter.
                    if (card.canHoldMedia) {
                        val cardMedia = mediaItems.filter { it.cardKey == card.key }
                        Spacer(Modifier.height(14.dp))
                        CardMediaRow(
                            items = cardMedia,
                            onOpen = { item -> viewerIndex = mediaItems.indexOf(item) },
                            onAdd = { addMediaTo(card) },
                        )
                    }

                    // Les reperes rapides de cette carte, sous la question
                    // qu'ils precisent plutot que dans une liste a part.
                    val cardTags = card.tagCategory?.let { family ->
                        allTags.filter { it.group == family }
                    }.orEmpty()
                    if (cardTags.isNotEmpty()) {
                        if (card != DayCard.SOCIAL && card != DayCard.WORK) {
                            Spacer(Modifier.height(14.dp))
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            cardTags.forEach { tag ->
                                val selected = tag.id in selectedTagIds
                                ChoiceChip(
                                    label = tag.display,
                                    selected = selected,
                                    onClick = {
                                        scope.launch {
                                            repository.toggleTag(date, tag, !selected)
                                        }
                                    },
                                )
                            }
                        }
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
    TextButton(onClick = onAdd) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (items.isEmpty()) "Ajouter une photo" else "Ajouter une autre photo")
    }
}

/** Donnee relevee automatiquement par le telephone, en lecture seule. */
@Composable
private fun MeasureRow(
    emoji: String,
    label: String,
    value: String?,
    hint: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (value == null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Ligne d'un moment de la journee : le libelle et les quatre couleurs. */
@Composable
private fun PartRow(part: DayPart, selectedKey: Int?, onPick: (Int?) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = "${part.emoji} ${part.label}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(dayColor.color)
                        .border(
                            BorderStroke(
                                if (selected) 3.dp else 1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                },
                            ),
                            RoundedCornerShape(12.dp),
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

@Composable
private fun ColorChoice(
    dayColor: DayColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(dayColor.color)
            .border(
                BorderStroke(
                    if (selected) 3.dp else 1.dp,
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    },
                ),
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .testTag("color-${dayColor.name}"),
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

@Composable
private fun MediaThumb(
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
