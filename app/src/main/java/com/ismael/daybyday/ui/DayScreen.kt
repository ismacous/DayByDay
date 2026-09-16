package com.ismael.daybyday.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
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
import com.ismael.daybyday.data.Badge
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.coach.CoachEngine
import com.ismael.daybyday.coach.CoachRule
import com.ismael.daybyday.coach.CoachSnapshot
import com.ismael.daybyday.coach.Nudge
import com.ismael.daybyday.coach.NudgeSurface
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
import com.ismael.daybyday.data.Brushing
import com.ismael.daybyday.data.Prayer
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Tag
import com.ismael.daybyday.data.Treatment
import com.ismael.daybyday.data.TagCategory
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.health.HealthConnectSource
import com.ismael.daybyday.health.ScreenTimeSource
import com.ismael.daybyday.ui.theme.Brand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private const val SAVE_DEBOUNCE_MS = 400L

/**
 * Les deux etiquettes qui disent comment la nuit s'est passee. Elles
 * s'excluent : c'est un choix, pas deux faits, et l'ecran l'impose maintenant
 * — auparavant rien n'empechait de cocher « bien dormi » et « mal dormi ».
 */
private val SLEEP_QUALITY = listOf("sleep_good", "sleep_bad")

/**
 * Les gens qu'on a pu voir. Il n'y a pas de « personne aujourd'hui » : ne rien
 * cocher le dit deja.
 */
private val SEEN = listOf("girlfriend", "friends", "family")

/**
 * Ce qui fait du bien, dans l'ordre du catalogue. La carte separe les deux
 * familles parce qu'une liste ou « Joie » et « Honte » se suivent oblige a
 * lire chaque pastille avant de la toucher.
 */
private val FEELINGS_LIGHT = listOf(
    "joy", "laugh", "calm", "excited", "proud", "grateful", "loved", "motivated", "relief",
)

/**
 * Ce qui pese. Aucune n'est un reproche : ce sont des faits, comme les autres.
 *
 * « Pleuré » et « Angoisse » y sont, au milieu des autres. Elles avaient leur
 * propre section, en cases a cocher sans emoji, et le resultat disait le
 * contraire de ce qu'on voulait : deux cas graves ranges a part, en bas de la
 * carte. Elles ont la meme forme que la tristesse et la colere.
 */
private val FEELINGS_HEAVY = listOf(
    "sad", "stress", "anger", "fear", "cried", "anxiety",
    "lonely", "guilt", "shame", "bored", "overwhelmed", "empty",
)

/**
 * Ce que le corps a dit de la journee. Ce sont des signes, pas des
 * diagnostics : l'application note « mal de tête », elle n'en conclut rien.
 */
private val BODY_SIGNS = listOf(
    "body_good", "body_tired", "headache", "belly", "pain", "sick",
)

/**
 * Les reperes des deux barres de mesure. Ce ne sont **pas** des objectifs :
 * l'application ne demande rien et ne felicite de rien. Ils servent seulement a
 * situer un chiffre — un nombre de pas seul ne dit pas s'il est grand.
 */
private const val STEPS_REFERENCE = 6000

private const val SCREEN_REFERENCE = 6

/** Le nombre de verres qui remplit la rangee. */
private const val WATER_FULL = 8

/** Le nombre de candidatures sur sept jours qui vaut une medaille. */
private const val WEEK_APPLICATIONS_GOAL = 5

/**
 * Le trajet du reflet sur une tuile d'humeur, en largeurs de tuile.
 *
 * Les bornes sont volontairement larges : a chaque bout, la bande doit avoir
 * entierement quitte la zone dessinee — rotation comprise — sinon la boucle
 * saute au lieu de se refermer.
 */
private const val SHINE_FROM = -0.9f

private const val SHINE_TO = 2.0f

/** Demi-largeur de la bande claire, en largeurs de tuile. */
private const val SHINE_HALF_WIDTH = 0.28f

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
    // Le journal n'est plus recopie ici : il est **regarde**. « Ma journee » ne
    // fait que l'afficher en apercu, l'ecran du journal est le seul a l'ecrire.
    // Quand les deux en gardaient chacun une copie, le dernier a enregistrer
    // gagnait — et effacer une page puis revenir remettait l'ancien texte.
    var sportLevel by remember { mutableStateOf<Int?>(null) }
    var foodLevel by remember { mutableStateOf<Int?>(null) }
    var wentOut by remember { mutableStateOf<Boolean?>(null) }
    var weightText by remember { mutableStateOf("") }
    var waistText by remember { mutableStateOf("") }
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
    var snackNote by remember { mutableStateOf("") }
    var medicalWith by remember { mutableStateOf("") }
    var medicalNote by remember { mutableStateOf("") }
    var prayerMask by remember { mutableStateOf<Int?>(null) }
    var showered by remember { mutableStateOf<Boolean?>(null) }
    var brushMask by remember { mutableStateOf<Int?>(null) }
    var jumua by remember { mutableStateOf<Boolean?>(null) }
    var jobApplications by remember { mutableStateOf<Int?>(null) }
    var editingTreatment by remember { mutableStateOf<Treatment?>(null) }
    var creatingTreatment by remember { mutableStateOf(false) }

    // Les cartes deja relues pour cette journee. Elles vivent dans la base et
    // non dans les preferences : c'est une propriete de **cette journee-la**,
    // pas un reglage de l'application.
    var checkedCards by remember { mutableStateOf(emptySet<String>()) }

    // Les cartes dont le fond est ouvert : celui qui montre la semaine. L'etat
    // vit ici et non dans les preferences — ouvrir le fond d'une carte est un
    // coup d'oeil, pas un reglage.
    var expandedCards by remember { mutableStateOf(emptySet<DayCard>()) }

    // La celebration en cours, ou null. Elle ne se declenche qu'au **passage**
    // d'un etat a l'autre, jamais a l'affichage : sinon rouvrir la journee la
    // rejouerait chaque fois.
    var celebration by remember { mutableStateOf<Badge?>(null) }

    /**
     * La fete de la journee bouclee.
     *
     * Elle vit ici et non dans [CoachHost] parce qu'elle repond a un **geste**
     * — la derniere carte qu'on vient de verifier — et non a l'ouverture d'un
     * ecran. Meme regle que les medailles : au passage, jamais a l'affichage.
     */
    var completion by remember { mutableStateOf<Nudge?>(null) }
    var everythingCheckedBefore by remember { mutableStateOf<Boolean?>(null) }

    // Les pas ne sont pas saisis : ils montent tout seuls pendant qu'on
    // regarde. Pour feter le passage et non l'arrivee sur la page, on retient
    // la derniere valeur **vue** — la premiere lecture ne declenche jamais
    // rien, meme si elle est deja au-dessus du repere.
    var stepsSeen by remember(epochDay) { mutableStateOf<Int?>(null) }

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

    // Le journal, **observe** et non recopie : l'apercu se met a jour a la
    // seconde ou l'ecran du journal enregistre, sans attendre un rechargement.
    //
    // Le flux porte le jour auquel il repond, et pas seulement la journee
    // trouvee. Sans ca, il n'y a aucun moyen de distinguer « cette journee n'a
    // pas de texte » de « la reponse n'est pas encore arrivee » : les deux
    // valent `null`. C'est exactement ce qui faisait rejouer la medaille.
    val journalState by remember(epochDay) {
        repository.observeDay(date).map { epochDay to it }
    }.collectAsStateWithLifecycle(null)
    val journalLoaded = journalState?.first == epochDay
    val journalDay = journalState?.takeIf { it.first == epochDay }?.second
    val title = journalDay?.title.orEmpty()
    val note = journalDay?.note.orEmpty()
    val noteSpansEncoded = journalDay?.noteSpans.orEmpty()

    // Les sept jours qui menent a celui-ci, pour le fond des cartes. Une seule
    // requete sert a tout le monde : sommeil, pas, prieres, candidatures.
    val weekDays by remember(epochDay) {
        repository.observeDaysBetween(date.minusDays(6), date)
    }.collectAsStateWithLifecycle(emptyMap())
    val selectedTagIds = dayTags.map { it.id }.toSet()

    fun currentEntry(day: Long) = DayEntry(
        epochDay = day,
        colorKey = colorKey,
        // Ces trois champs sont ignores a l'enregistrement
        // (`saveDayKeepingJournal` les relit dans la base) : ils ne sont la que
        // parce que `DayEntry` les porte.
        title = "",
        note = "",
        noteSpans = "",
        sportLevel = sportLevel,
        foodLevel = foodLevel,
        wentOut = wentOut,
        weightKg = weightText.replace(',', '.').toDoubleOrNull(),
        waistCm = waistText.replace(',', '.').toDoubleOrNull(),
        steps = stepsValue,
        screenMinutes = screenValue,
        sleepStartMinutes = sleepStart,
        sleepEndMinutes = sleepEnd,
        sleepFromDevice = sleepFromDevice,
        waterGlasses = waterGlasses,
        mealsNote = mealsNote.trim(),
        snackNote = snackNote.trim(),
        medicalWith = medicalWith.trim(),
        medicalNote = medicalNote.trim(),
        jobApplications = jobApplications,
        partMorning = parts[DayPart.MORNING],
        partAfternoon = parts[DayPart.AFTERNOON],
        partEvening = parts[DayPart.EVENING],
        partNight = parts[DayPart.NIGHT],
        colorManual = colorManual,
        prayerMask = prayerMask,
        showered = showered,
        brushMask = brushMask,
        jumua = jumua,
        checkedCards = checkedCards.sorted().joinToString(","),
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
        sportLevel = entry?.sportLevel
        foodLevel = entry?.foodLevel
        wentOut = entry?.wentOut
        weightText = entry?.weightKg?.let { String.format(Locale.FRANCE, "%.1f", it) }.orEmpty()
        waistText = entry?.waistCm?.let { String.format(Locale.FRANCE, "%.1f", it) }.orEmpty()
        stepsValue = entry?.steps
        screenValue = entry?.screenMinutes
        sleepStart = entry?.sleepStartMinutes
        sleepEnd = entry?.sleepEndMinutes
        sleepFromDevice = entry?.sleepFromDevice ?: false
        waterGlasses = entry?.waterGlasses
        mealsNote = entry?.mealsNote.orEmpty()
        snackNote = entry?.snackNote.orEmpty()
        medicalWith = entry?.medicalWith.orEmpty()
        medicalNote = entry?.medicalNote.orEmpty()
        prayerMask = entry?.prayerMask
        showered = entry?.showered
        brushMask = entry?.brushMask
        jobApplications = entry?.jobApplications
        jumua = entry?.jumua
        checkedCards = entry?.checkedCardKeys.orEmpty()
        expandedCards = emptySet()
        loadedFor = epochDay
    }

    /**
     * Toutes les cartes affichees viennent-elles d'etre verifiees ?
     *
     * Le garde qui compte est `loadedFor != epochDay` : avant que la journee ne
     * revienne de la base, `checkedCards` est vide, et le passage
     * « vide -> tout coche » du chargement ressemble trait pour trait a celui
     * du dernier geste. C'est exactement le piege des medailles.
     *
     * `everythingCheckedBefore` repart a `null` en changeant de jour : ouvrir
     * une journee deja bouclee ne doit rien rejouer.
     */
    LaunchedEffect(epochDay, loadedFor, checkedCards, visibleCards) {
        if (loadedFor != epochDay) return@LaunchedEffect
        val complete = visibleCards.isNotEmpty() && visibleCards.all { it.key in checkedCards }
        val before = everythingCheckedBefore
        everythingCheckedBefore = complete
        if (before == false && complete) {
            completion = CoachEngine.forRule(
                snapshot = CoachSnapshot.build(
                    today = date,
                    hourOfDay = LocalTime.now().hour,
                    firstName = app.prefs.firstName,
                    birthDate = app.prefs.birthDate,
                    entries = emptyList(),
                    tags = emptyList(),
                    links = emptyList(),
                ),
                memory = app.coach,
                rule = CoachRule.DAY_COMPLETE,
            )
        }
    }

    LaunchedEffect(epochDay) { everythingCheckedBefore = null }

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

    // Le journal s'ecrit sur un autre ecran ; on ne voit revenir que son
    // resultat. Meme regle que pour les pas : la premiere valeur vue ne fete
    // rien, c'est le passage du vide au texte qui compte.
    //
    // Le piege est toujours le meme, et il a deux etages : les champs partent
    // vides et se remplissent une fraction de seconde plus tard, donc ouvrir
    // une journee deja ecrite ressemble trait pour trait a l'ecrire. La
    // premiere version se gardait de ca en attendant `loadedFor` — le
    // chargement des **autres** champs — ce qui laissait passer le cas ou le
    // journal, lui, n'avait pas encore repondu.
    var noteSeen by remember(epochDay) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(note, title, journalLoaded) {
        // On attend la **premiere reponse du journal lui-meme**, et non le
        // chargement des autres champs. Les deux allaient chercher la meme
        // journee chacun de son cote, et rien ne disait lequel arriverait le
        // premier : quand le chargement gagnait, le journal etait encore vide a
        // cet instant, on notait « pas de texte », le flux repondait une
        // fraction de seconde plus tard — et le passage du vide au texte
        // relancait la medaille. D'ou une medaille qui revenait au hasard, en
        // passant d'une journee a l'autre avec les fleches.
        if (!journalLoaded) return@LaunchedEffect
        val written = note.isNotBlank() || title.isNotBlank()
        val before = noteSeen
        noteSeen = written
        if (before == false && written) celebration = Badge.JOURNAL
    }

    LaunchedEffect(stepsValue, loadedFor) {
        if (loadedFor != epochDay) return@LaunchedEffect
        val steps = stepsValue ?: return@LaunchedEffect
        val before = stepsSeen
        stepsSeen = steps
        if (before != null && before < STEPS_REFERENCE && steps >= STEPS_REFERENCE &&
            date == LocalDate.now()
        ) {
            celebration = Badge.STEPS
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
        sportLevel,
        foodLevel,
        wentOut,
        weightText,
        waistText,
        stepsValue,
        screenValue,
        sleepStart,
        sleepEnd,
        waterGlasses,
        mealsNote,
        snackNote,
        medicalWith,
        medicalNote,
        jobApplications,
        checkedCards,
        // Ces trois-la n'etaient enregistres qu'en quittant l'ecran : une
        // priere cochee juste avant qu'Android ne ferme l'application etait
        // perdue. Elles suivent maintenant le meme chemin que le reste.
        prayerMask,
        showered,
        brushMask,
        jumua,
    ) {
        if (loadedFor != epochDay) return@LaunchedEffect
        delay(SAVE_DEBOUNCE_MS)
        // Sans toucher au journal : c'est l'ecran du journal qui le possede.
        repository.saveDayKeepingJournal(currentEntry(epochDay))
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
                app.appScope.launch { repository.saveDayKeepingJournal(snapshot) }
            }
        }
    }

    // La carte depuis laquelle la photo a ete demandee : elle lui reste
    // attachee. Depuis "Photos & videos", le media appartient a la journee
    // entiere et ne va sous aucune carte.
    var mediaTarget by remember { mutableStateOf<DayCard?>(null) }

    // Le jour est relu **au moment du geste** : on peut changer de journee
    // entre deux photos, et la photo doit suivre celle qu'on regarde.
    val addPhoto = rememberAddPhoto(
        dayOf = { LocalDate.ofEpochDay(epochDay) },
        onPicked = { uris ->
            val card = mediaTarget
            mediaTarget = null
            val targetDate = LocalDate.ofEpochDay(epochDay)
            app.appScope.launch {
                uris.forEach { uri -> repository.addMedia(targetDate, uri, card) }
            }
        },
        onCaptured = { path ->
            val card = mediaTarget
            mediaTarget = null
            val targetDate = LocalDate.ofEpochDay(epochDay)
            app.appScope.launch { repository.adoptPhoto(targetDate, path, card) }
        },
    )

    fun addMediaTo(card: DayCard?) {
        mediaTarget = card
        addPhoto.ask()
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

    fun toggleTag(tag: Tag, on: Boolean) {
        scope.launch { repository.toggleTag(date, tag, on) }
    }

    /**
     * Les reperes d'une famille, en tuiles de largeur egale.
     *
     * En pastilles libres, trois reperes formaient un escalier colle a gauche —
     * « Marche », puis « Dehors » a cote, puis « Menage, rangement » tout seul
     * a la ligne. Rien ne disait qu'ils allaient ensemble. En tuiles, la rangee
     * est un objet : elle est centree, symetrique, et se lit d'un coup.
     */
    val tagTiles: @Composable (TagCategory, Color) -> Unit = { family, tint ->
        val tags = allTags.filter { it.group == family }
        MultiTiles(
            options = tags.map { Segment(it.emoji, it.name) },
            selected = tags.indices.filter { tags[it].id in selectedTagIds }.toSet(),
            tint = tint,
            onToggle = { index ->
                val tag = tags[index]
                toggleTag(tag, tag.id !in selectedTagIds)
            },
        )
    }

    /**
     * Une liste de reperes, en pastilles qui se replient sur plusieurs lignes.
     * On en coche autant qu'on veut : une journee n'a pas **une** emotion, elle
     * en a cinq qui se contredisent — et un corps a rarement un seul signe.
     *
     * Elle prend des slugs et non une famille : la carte des ressentis en
     * affiche deux listes separees, tirees de la meme famille.
     */
    val tagCloud: @Composable (List<String>, Color) -> Unit = { slugs, tint ->
        val tags = slugs.mapNotNull { tagOf(it) }
        ChipCloud(
            options = tags.map { Segment(it.emoji, it.name) },
            selected = tags.indices.filter { tags[it].id in selectedTagIds }.toSet(),
            tint = tint,
            onToggle = { index ->
                val tag = tags[index]
                toggleTag(tag, tag.id !in selectedTagIds)
            },
        )
    }

    /**
     * Les reperes d'une famille, en lignes a cocher : une liste de choses
     * faites.
     *
     * [emoji] se coupe pour ce qui est dur a dire. Un petit visage qui pleure a
     * cote de « J'ai pleuré » transforme un fait en mise en scene ; la case et
     * les trois mots suffisent.
     */
    val tagChecks: @Composable (TagCategory, Color, Boolean) -> Unit = { family, tint, emoji ->
        Column {
            allTags.filter { it.group == family }.forEachIndexed { index, tag ->
                if (index > 0) Spacer(Modifier.height(8.dp))
                val selected = tag.id in selectedTagIds
                CheckRow(
                    label = if (emoji) tag.display else tag.name,
                    checked = selected,
                    tint = tint,
                    onClick = { toggleTag(tag, !selected) },
                )
            }
        }
    }

    // Les sept jours dans l'ordre, du plus ancien a aujourd'hui, et leurs
    // initiales. C'est la matiere de tous les « voir la semaine ».
    val week = (6 downTo 0).map { back -> weekDays[date.minusDays(back.toLong()).toEpochDay()] }
    val weekLabels = (6 downTo 0).map { back ->
        Dates.weekDayInitials[date.minusDays(back.toLong()).dayOfWeek.value - 1]
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
            when (wentOut) {
                true -> "sorti"
                false -> "resté à la maison"
                null -> null
            },
            SportLevel.fromKey(sportLevel)?.label?.lowercase(Locale.FRANCE),
        ).joinToString(" · ").ifBlank { null }
        DayCard.FOOD -> listOfNotNull(
            FoodLevel.fromKey(foodLevel)?.label,
            waterGlasses?.let { "$it verre(s)" },
        ).joinToString(" · ").ifBlank { null }
        DayCard.HEALTH -> listOfNotNull(
            weightText.takeIf { it.isNotBlank() }?.let { "$it kg" },
            waistText.takeIf { it.isNotBlank() }?.let { "$it cm" },
            allTags.filter { it.group == TagCategory.HEALTH && it.id in selectedTagIds }
                .joinToString(", ") { it.name.lowercase(Locale.FRANCE) }
                .ifBlank { null },
        ).joinToString(" · ").ifBlank { null }
        DayCard.EMOTION -> allTags
            .filter { it.group == TagCategory.EMOTION && it.id in selectedTagIds }
            .joinToString(", ") { it.name }
            .ifBlank { null }
        DayCard.TREATMENT -> {
            val expected = treatments.filter { it.active }.sumOf { it.times.size }
            listOfNotNull(
                medicalWith.takeIf { it.isNotBlank() },
                if (expected > 0) "${dosesTaken.size} prise(s) sur $expected" else null,
            ).joinToString(" · ").ifBlank { null }
        }
        DayCard.SOCIAL -> SEEN.filter { tagOn(it) }.mapNotNull { tagOf(it)?.name }
            .joinToString(", ").ifBlank { null }
        DayCard.WORK -> listOfNotNull(
            jobApplications?.let { "$it candidature(s)" },
            allTags.count { it.group == TagCategory.WORK && it.id in selectedTagIds }
                .takeIf { it > 0 }?.let { "$it démarche(s)" },
        ).joinToString(" · ").ifBlank { null }
        DayCard.OUTSIDE -> screenValue?.let { formatScreenTime(it) }
        DayCard.MONEY -> dayMoney.takeIf { it.isNotEmpty() }
            ?.let { formatSignedMoney(it.sumOf { entry -> entry.amountCents }) }
        DayCard.PRAYER -> prayerMask?.let { mask ->
            "${Prayer.entries.count { mask and it.bit != 0 }} sur ${Prayer.entries.size}"
        }
        DayCard.HYGIENE -> {
            val brushed = Brushing.entries.count { (brushMask ?: 0) and it.bit != 0 }
            when {
                showered == true && brushed > 0 -> "Douché · $brushed/${Brushing.entries.size}"
                showered == true -> "Douché"
                brushed > 0 -> "$brushed/${Brushing.entries.size} brossage(s)"
                else -> null
            }
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

            // Ou en est la relecture de cette journee. Une ligne, rien de plus :
            // pas de bouton a cote, qui faisait a lui seul toute la hauteur de
            // la rangee et repoussait la page entiere vers le bas. On decoche
            // une carte par sa coche, la ou elle est.
            //
            // Elle ne s'affiche qu'une fois la premiere carte verifiee : tant
            // qu'on n'a rien marque, un « 0 sur 11 » en haut de chaque journee
            // ressemblerait a un devoir a rendre, et ce n'en est pas un.
            val checkedCount = visibleCards.count { it.key in checkedCards }
            if (checkedCount > 0) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Verified,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (checkedCount == visibleCards.size) {
                            "Journée validée en entier"
                        } else {
                            "$checkedCount carte(s) validée(s) sur ${visibleCards.size}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Verified,
                    )
                }
                // Le mode d'emploi, et seulement au debut. Une carte validee ne
                // repond plus au doigt : sans un mot, on croit a une panne. Deux
                // cartes suffisent a comprendre, donc la ligne s'efface ensuite
                // plutot que de rester en decor au-dessus de chaque journee.
                if (checkedCount <= 2) {
                    Text(
                        text = "Une carte validée ne se modifie plus. Reglisse-la pour la rouvrir.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
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
                // Un bouton au gabarit reduit : au gabarit normal il reserve
                // quarante-huit points de haut et pousse toute la page vers le
                // bas, pour trois mots.
                TextButton(
                    onClick = { epochDay = LocalDate.now().toEpochDay() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .height(34.dp),
                ) {
                    Text("Aller à aujourd'hui", style = MaterialTheme.typography.labelLarge)
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
                    checked = card.key in checkedCards,
                    onCheckedChange = { verified ->
                        checkedCards = if (verified) {
                            checkedCards + card.key
                        } else {
                            checkedCards - card.key
                        }
                    },
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
                    onOrganize = onOrganizeCards,
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
                            // Trois mots, et seulement dans le cas qui ne se
                            // devine pas. Une couleur calculee n'a rien a
                            // annoncer : les moments sont juste en dessous.
                            // Expliquer en plus comment revenir en arriere
                            // prenait deux lignes pour apprendre un geste qui
                            // s'apprend en le faisant une fois.
                            if (parts.isNotEmpty() && colorManual) {
                                Text(
                                    text = "Choisie à la main",
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
                            Spacer(Modifier.height(12.dp))
                            DayPart.entries.forEach { part ->
                                PartRow(
                                    part = part,
                                    selectedKey = parts[part],
                                    onPick = { key -> setPart(part, key) },
                                )
                            }
                        }
                        DayCard.EMOTION -> {
                            // Deux listes et non une seule. Melangees, « Joie »
                            // et « Honte » se suivent, et il faut lire chaque
                            // pastille avant de la toucher — ce qui n'est pas
                            // rien un jour ou l'on n'a deja plus de force.
                            CardSection("Ce qui t'a fait du bien", tint) {
                                tagCloud(FEELINGS_LIGHT, tint)
                            }
                            Spacer(Modifier.height(18.dp))
                            CardSection("Ce qui a pesé", tint) {
                                tagCloud(FEELINGS_HEAVY, tint)
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
                            }
                            CardWeek(
                                card = card,
                                tint = tint,
                                expanded = card in expandedCards,
                                onToggle = { expandedCards = expandedCards.toggle(card) },
                            ) {
                                MiniBars(
                                    values = week.map { entry ->
                                        entry?.sleepMinutes?.div(60f)
                                    },
                                    labels = weekLabels,
                                    captions = week.map { entry ->
                                        entry?.sleepMinutes?.let { "${it / 60} h" }
                                    },
                                    tint = tint,
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
                            // La semaine se lit juste sous le chiffre qu'elle
                            // met en perspective, pas en bas de la carte : sept
                            // barres de pas rangees apres les questions sur le
                            // sport auraient eu l'air de les commenter.
                            CardWeek(
                                card = card,
                                tint = tint,
                                expanded = card in expandedCards,
                                onToggle = { expandedCards = expandedCards.toggle(card) },
                            ) {
                                MiniBars(
                                    values = week.map { it?.steps?.toFloat() },
                                    labels = weekLabels,
                                    captions = week.map { entry ->
                                        entry?.steps?.let { steps ->
                                            if (steps >= 1000) "${steps / 1000}k" else "$steps"
                                        }
                                    },
                                    tint = tint,
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            // Sortir ou rester chez soi a rejoint cette carte.
                            // C'etait un « dehors » coince entre le menage et
                            // les courses d'un cote, et un temps d'ecran de
                            // l'autre : deux endroits ou ca ne voulait rien
                            // dire. Ici, c'est la premiere chose qu'on note
                            // quand on parle de ce qu'on a fait de son corps.
                            CardSection("Tu es sorti ?", tint) {
                                SegmentedChoice(
                                    options = listOf(
                                        Segment("🚪", "Sorti"),
                                        Segment("🏠", "Resté à la maison"),
                                    ),
                                    selectedIndex = when (wentOut) {
                                        true -> 0
                                        false -> 1
                                        null -> null
                                    },
                                    tint = tint,
                                    onSelect = { chosen ->
                                        val out = when (chosen) {
                                            0 -> true
                                            1 -> false
                                            else -> null
                                        }
                                        if (out == true && wentOut != true) {
                                            celebration = Badge.OUTSIDE
                                        }
                                        wentOut = out
                                    },
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            CardSection("Tu as bougé ?", tint) {
                                SegmentedChoice(
                                    options = SportLevel.entries.map { Segment(it.emoji, it.label) },
                                    selectedIndex = SportLevel.entries
                                        .indexOfFirst { it.key == sportLevel }
                                        .takeIf { it >= 0 },
                                    tint = tint,
                                    onSelect = { chosen ->
                                        val level = chosen?.let { SportLevel.entries[it] }
                                        if (level == SportLevel.GOOD && sportLevel != level.key) {
                                            celebration = Badge.WORKOUT
                                        }
                                        sportLevel = level?.key
                                    },
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            CardSection("Ce que tu as fait", tint) {
                                tagTiles(TagCategory.ACTIVITY, tint)
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
                                    onChange = { glasses ->
                                        if (glasses == WATER_FULL && waterGlasses != WATER_FULL) {
                                            celebration = Badge.WATER
                                        }
                                        waterGlasses = glasses
                                    },
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
                            OutlinedTextField(
                                value = mealsNote,
                                onValueChange = { mealsNote = it.take(500) },
                                label = { Text("Ce que tu as mangé (optionnel)") },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp),
                            )
                            Spacer(Modifier.height(18.dp))
                            // Le grignotage n'est plus une etiquette parmi
                            // quatre. « Cuisine maison », « fast-food » et
                            // « alcool » ne disaient rien que « compliquée /
                            // correcte / bien mangé » ne dise deja ; le
                            // grignotage, si — et il merite qu'on puisse ecrire
                            // ce que c'etait, ce qu'une etiquette ne permet pas.
                            val snacked = tagOn("snacking")
                            CardSection("Grignotage", tint) {
                                CheckRow(
                                    label = "🍫 J'ai grignoté",
                                    checked = snacked,
                                    tint = tint,
                                    onClick = { setTag("snacking", !snacked) },
                                )
                                if (snacked) {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = snackNote,
                                        onValueChange = { snackNote = it.take(300) },
                                        label = { Text("Quoi, et à quel moment ?") },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                        DayCard.HEALTH -> {
                            // Deux mesures, cote a cote, et pas une seule.
                            //
                            // Le poids tout seul ne dit pas grand-chose : il
                            // monte et descend avec l'eau, les repas, l'heure
                            // de la pesee, et deux kilos d'ecart en trois jours
                            // ne veulent rien dire. Le tour de taille bouge
                            // lentement et dans un seul sens a la fois. Les
                            // deux ensemble font une mesure ; l'un sans l'autre
                            // fait un chiffre qu'on regarde avec inquietude.
                            //
                            // Rien n'est obligatoire, et il n'y a **aucun
                            // objectif** : l'application ne dit jamais ce que
                            // ces chiffres devraient valoir.
                            CardSection("Tes mensurations", tint) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = { input ->
                                            weightText = input
                                                .filter { it.isDigit() || it == ',' || it == '.' }
                                                .take(6)
                                        },
                                        label = { Text("Poids") },
                                        suffix = { Text("kg") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = waistText,
                                        onValueChange = { input ->
                                            waistText = input
                                                .filter { it.isDigit() || it == ',' || it == '.' }
                                                .take(5)
                                        },
                                        label = { Text("Tour de taille") },
                                        suffix = { Text("cm") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                // L'IMC se calcule avec la taille du profil. Il
                                // est donne **sans categorie** : « 26,4 » est
                                // un nombre, « surpoids » est un jugement, et
                                // ce n'est pas le role de cette application d'en
                                // porter un.
                                val imc = app.prefs.bodyMassIndex(
                                    weightText.replace(',', '.').toDoubleOrNull()
                                )
                                if (imc != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "IMC ${String.format(Locale.FRANCE, "%.1f", imc)}, " +
                                            "pour ${app.prefs.heightCm} cm.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            CardWeek(
                                card = card,
                                tint = tint,
                                expanded = card in expandedCards,
                                onToggle = { expandedCards = expandedCards.toggle(card) },
                            ) {
                                val weights = week.map { it?.weightKg?.toFloat() }
                                if (weights.all { it == null }) {
                                    Text(
                                        "Aucun poids noté cette semaine.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    // Les barres partent du poids le plus bas
                                    // de la semaine, pas de zero : sur des
                                    // valeurs autour de quatre-vingts kilos,
                                    // partir de zero ferait sept barres
                                    // identiques.
                                    val floor = weights.filterNotNull().min() - 1f
                                    MiniBars(
                                        values = weights.map { it?.minus(floor) },
                                        labels = weekLabels,
                                        captions = weights.map {
                                            it?.let { kg -> String.format(Locale.FRANCE, "%.0f", kg) }
                                        },
                                        tint = tint,
                                    )
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            // La moitie qui manquait a la carte. Avec le seul
                            // poids, elle ne servait qu'un jour sur dix ; un
                            // corps a quelque chose a dire tous les jours, et
                            // c'est ce qui se compare ensuite au sommeil et a
                            // l'alimentation dans le Bilan.
                            CardSection("Ce que ton corps a dit", tint) {
                                tagCloud(BODY_SIGNS, tint)
                            }
                        }
                        DayCard.TREATMENT -> {
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
                            Spacer(Modifier.height(18.dp))
                            // Cocher « rendez-vous médical » disait qu'il y en
                            // avait eu un — ce qui ne sert a rien six mois plus
                            // tard, quand on cherche lequel. Le nom du medecin
                            // et le motif, eux, se retrouvent par la recherche.
                            val appointment = tagOn("appointment")
                            CardSection("Rendez-vous", tint) {
                                CheckRow(
                                    label = "🩺 Rendez-vous médical",
                                    checked = appointment,
                                    tint = tint,
                                    onClick = { setTag("appointment", !appointment) },
                                )
                                if (appointment) {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = medicalWith,
                                        onValueChange = { medicalWith = it.take(120) },
                                        label = { Text("Chez qui ?") },
                                        placeholder = { Text("Dentiste, Dr Martin…") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = medicalNote,
                                        onValueChange = { medicalNote = it.take(300) },
                                        label = { Text("Pour quoi ?") },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                        DayCard.PRAYER -> {
                            PrayerCardBody(
                                mask = prayerMask,
                                tint = tint,
                                date = date,
                                jumua = jumua,
                                onJumua = { jumua = it },
                                onToggle = { prayer, done ->
                                    val before = prayerMask ?: 0
                                    val after = currentEntry(epochDay).withPrayer(prayer, done)
                                    prayerMask = after
                                    // La fete se declenche au **passage** a
                                    // cinq, pas a l'affichage de cinq : rouvrir
                                    // la journee demain ne la rejouera pas.
                                    if (before != Prayer.ALL_DONE && after == Prayer.ALL_DONE) {
                                        celebration = Badge.PRAYERS
                                    }
                                },
                            )
                            CardWeek(
                                card = card,
                                tint = tint,
                                expanded = card in expandedCards,
                                onToggle = { expandedCards = expandedCards.toggle(card) },
                            ) {
                                MiniBars(
                                    values = week.map { entry ->
                                        entry?.prayerMask?.let { mask ->
                                            Prayer.entries.count { mask and it.bit != 0 }.toFloat()
                                        }
                                    },
                                    labels = weekLabels,
                                    captions = week.map { entry ->
                                        entry?.prayerMask?.let { mask ->
                                            "${Prayer.entries.count { mask and it.bit != 0 }}/5"
                                        }
                                    },
                                    tint = tint,
                                )
                            }
                        }
                        DayCard.HYGIENE -> {
                            HygieneCardBody(
                                showered = showered,
                                brushMask = brushMask,
                                tint = tint,
                                onShower = { done ->
                                    // La fete se declenche au **passage**, pas
                                    // a l'affichage : rouvrir la journee demain
                                    // ne la rejouera pas.
                                    val before = showered == true
                                    showered = done
                                    if (!before && done) celebration = Badge.SHOWER
                                },
                                onBrushing = { brushing, done ->
                                    val before = brushMask ?: 0
                                    val after = currentEntry(epochDay)
                                        .withBrushing(brushing, done)
                                    brushMask = after
                                    if (before != Brushing.ALL_DONE &&
                                        after == Brushing.ALL_DONE
                                    ) {
                                        celebration = Badge.TEETH
                                    }
                                },
                            )
                            CardWeek(
                                card = card,
                                tint = tint,
                                expanded = card in expandedCards,
                                onToggle = { expandedCards = expandedCards.toggle(card) },
                            ) {
                                MiniBars(
                                    values = week.map { entry ->
                                        entry?.brushMask?.let { mask ->
                                            Brushing.entries
                                                .count { mask and it.bit != 0 }
                                                .toFloat()
                                        }
                                    },
                                    labels = weekLabels,
                                    captions = week.map { entry ->
                                        entry?.brushMask?.let { mask ->
                                            val n = Brushing.entries.count { mask and it.bit != 0 }
                                            "$n/3"
                                        }
                                    },
                                    tint = tint,
                                )
                            }
                        }
                        DayCard.SOCIAL -> {
                            // Plus de « personne aujourd'hui » : ne rien cocher
                            // le dit deja, et une case pour dire qu'il ne s'est
                            // rien passe est une case de trop — elle demandait
                            // en plus de penser a la decocher le lendemain.
                            CardSection("Qui tu as vu", tint) {
                                tagTiles(TagCategory.SOCIAL, tint)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = SEEN.filter { tagOn(it) }
                                        .mapNotNull { tagOf(it)?.name }
                                        .joinToString(", ")
                                        .ifBlank { "Personne de noté aujourd'hui." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        DayCard.WORK -> {
                            // Chercher du travail n'est pas une case a cocher.
                            // « Recherche d'emploi » cochee ne disait rien : un
                            // jour a une candidature et un jour a six se
                            // ressemblaient. Le nombre, lui, se cumule sur la
                            // semaine, et c'est le cumul qui se regarde quand on
                            // cherche.
                            val weekTotal = week.sumOf { it?.jobApplications ?: 0 }
                            Counter(
                                value = jobApplications,
                                tint = tint,
                                label = "Candidatures envoyées",
                                caption = if (weekTotal > 0) {
                                    "$weekTotal sur les sept derniers jours"
                                } else {
                                    "Aujourd'hui"
                                },
                                onChange = { sent ->
                                    val before = jobApplications ?: 0
                                    val after = sent ?: 0
                                    if (after > before) {
                                        val weekBefore = weekTotal
                                        val weekAfter = weekBefore - before + after
                                        celebration = when {
                                            weekBefore < WEEK_APPLICATIONS_GOAL &&
                                                weekAfter >= WEEK_APPLICATIONS_GOAL ->
                                                Badge.WEEK_APPLICATIONS
                                            before == 0 -> Badge.APPLICATION
                                            else -> null
                                        }
                                    }
                                    jobApplications = sent
                                },
                            )
                            Spacer(Modifier.height(18.dp))
                            CardSection("Ce que tu as avancé", tint) {
                                tagChecks(TagCategory.WORK, tint, true)
                            }
                            CardWeek(
                                card = card,
                                tint = tint,
                                expanded = card in expandedCards,
                                onToggle = { expandedCards = expandedCards.toggle(card) },
                            ) {
                                MiniBars(
                                    values = week.map { it?.jobApplications?.toFloat() },
                                    labels = weekLabels,
                                    captions = week.map { it?.jobApplications?.toString() },
                                    tint = tint,
                                )
                            }
                        }
                        DayCard.OUTSIDE -> {
                            MetricRow(
                                emoji = "📱",
                                label = "Temps sur le téléphone",
                                value = screenValue?.let { formatScreenTime(it) },
                                caption = when {
                                    screenValue != null -> "Sur une échelle de $SCREEN_REFERENCE h."
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
                                tagTiles(TagCategory.SCREENS, tint)
                            }
                            CardWeek(
                                card = card,
                                tint = tint,
                                expanded = card in expandedCards,
                                onToggle = { expandedCards = expandedCards.toggle(card) },
                            ) {
                                MiniBars(
                                    values = week.map { it?.screenMinutes?.div(60f) },
                                    labels = weekLabels,
                                    captions = week.map { entry ->
                                        entry?.screenMinutes?.let { "${it / 60} h" }
                                    },
                                    tint = tint,
                                )
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
                            // Sur la carte des traitements, une photo est une
                            // ordonnance neuf fois sur dix : le bouton le dit,
                            // au lieu de laisser deviner ce qu'on peut y mettre.
                            label = if (card == DayCard.TREATMENT) {
                                "Photographier une ordonnance"
                            } else {
                                "Ajouter une photo"
                            },
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

        // La fete passe **par-dessus** la page qui defile, pas dedans : elle ne
        // pousse rien et ne fait sauter aucune carte.
        celebration?.let { badge ->
            Celebration(badge = badge, onDone = { celebration = null })
        }

        completion?.let { nudge ->
            CoachPopup(
                nudge = nudge,
                onDismiss = {
                    CoachEngine.markShown(
                        memory = app.coach,
                        nudge = nudge,
                        surface = NudgeSurface.DAY,
                        epochDay = date.toEpochDay(),
                    )
                    completion = null
                },
            )
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
 * Le bonus d'une carte : sa semaine.
 *
 * La carte repond a « aujourd'hui ». Ce bouton ouvre la seule question que la
 * carte ne pouvait pas poser sans doubler de hauteur : « et les jours
 * d'avant ? ». C'est ce qui justifiait un deuxieme niveau — un detail de plus
 * sur aujourd'hui aurait simplement rallonge la carte.
 */
@Composable
private fun CardWeek(
    card: DayCard,
    tint: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Spacer(Modifier.height(6.dp))
    MoreButton(expanded = expanded, tint = tint, onClick = onToggle)
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(tween(Motion.NORMAL)) + fadeIn(tween(Motion.NORMAL)),
        exit = shrinkVertically(tween(Motion.QUICK)) + fadeOut(tween(Motion.QUICK)),
        label = "semaine-${card.key}",
    ) {
        Column {
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** Ajoute ou retire une carte de l'ensemble, sans le muter. */
private fun Set<DayCard>.toggle(card: DayCard): Set<DayCard> =
    if (card in this) this - card else this + card

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
    label: String,
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
                .clickable(onClickLabel = label, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (items.isEmpty()) {
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Ligne d'un moment de la journee : le libelle et les quatre couleurs. */
@Composable
private fun PartRow(part: DayPart, selectedKey: Int?, onPick: (Int?) -> Unit) {
    val partBorder = MaterialTheme.colorScheme.onSurface
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
    //
    // La boucle doit etre **invisible a ses deux bouts**, sinon elle saute. La
    // premiere version partait de -0,4 pour aller a 1,4 avec un axe de degrade
    // diagonal : hors de l'intervalle, un degrade se prolonge par sa couleur de
    // bord, et comme l'axe etait oblique, le coin bas-droit de la tuile tombait
    // encore dans la partie claire a l'arrivee. Le reflet s'y arretait net puis
    // reapparaissait en haut. L'axe est donc horizontal et le trajet assez
    // large pour que la bande soit entierement sortie aux deux extremites ;
    // l'obliquite vient maintenant d'une rotation du dessin, qui ne change rien
    // au calcul.
    val shine = if (selected) {
        val loop = rememberInfiniteTransition(label = "reflet")
        loop.animateFloat(
            initialValue = SHINE_FROM,
            targetValue = SHINE_TO,
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
                    val half = size.width * SHINE_HALF_WIDTH
                    rotate(degrees = 22f) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.42f),
                                    Color.Transparent,
                                ),
                                // Axe horizontal : au-dela de la bande, le
                                // degrade se prolonge par du transparent des
                                // deux cotes, donc la boucle se referme
                                // exactement sur elle-meme.
                                start = Offset(x - half, 0f),
                                end = Offset(x + half, 0f),
                            ),
                            // Le dessin deborde largement : la rotation fait
                            // sortir les coins de la tuile, et le decoupage de
                            // la tuile s'occupe du reste.
                            topLeft = Offset(-size.width, -size.height),
                            size = androidx.compose.ui.geometry.Size(
                                size.width * 3f,
                                size.height * 3f,
                            ),
                        )
                    }
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
        MoodEmoji(dayColor = dayColor, selected = selected, modifier = Modifier.fillMaxSize())
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
