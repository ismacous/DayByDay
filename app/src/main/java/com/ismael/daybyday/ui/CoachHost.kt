package com.ismael.daybyday.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.ismael.daybyday.coach.CoachEngine
import com.ismael.daybyday.coach.CoachSnapshot
import com.ismael.daybyday.coach.Nudge
import com.ismael.daybyday.coach.NudgeSurface
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

/** Fenetre d'historique lue par l'algorithme : un peu plus d'un an. */
private const val COACH_WINDOW_DAYS = 400L

/**
 * Le temps qu'on laisse a l'ecran avant de parler.
 *
 * Les cartes entrent en scene de facon decalee (voir [Appear]) et la derniere
 * finit vers quatre cents millisecondes. Parler pendant ce temps-la donnerait
 * une boite de dialogue de demarrage, ce qu'on ne lit jamais. On attend que la
 * page soit posee et qu'on ait eu le temps de la regarder.
 */
private const val SETTLE_MS = 1400L

/**
 * L'endroit ou le coup de pouce a le droit d'apparaitre.
 *
 * Pose **une seule fois**, autour de la navigation : la bulle peut donc
 * arriver sur n'importe quel ecran — le mois, une journee, le bilan, l'argent
 * — et c'est la regle elle-meme qui dit ou elle a du sens. Une remarque sur
 * l'argent au milieu du calendrier ne se lit pas ; la meme dans l'onglet
 * Argent, si.
 *
 * @param surface l'ecran courant, ou `null` quand on est sur un ecran qui ne
 *   doit jamais etre interrompu (les reglages, le journal, la recherche).
 */
@Composable
fun CoachHost(surface: NudgeSurface?) {
    val app = LocalContext.current.dayByDayApp
    if (!app.prefs.coachEnabled || surface == null) return

    val snapshot = rememberCoachSnapshot()
    var shown by remember { mutableStateOf<Nudge?>(null) }

    // Le choix se fait **apres** que la page se soit posee, et il est refait si
    // l'on change d'ecran : c'est ce qui permet a une remarque sur l'argent
    // d'attendre qu'on ouvre l'onglet Argent.
    LaunchedEffect(surface, snapshot) {
        if (snapshot == null || shown != null) return@LaunchedEffect
        delay(SETTLE_MS)
        val nudge = CoachEngine.choose(snapshot, app.coach, surface) ?: return@LaunchedEffect
        CoachEngine.markShown(app.coach, nudge, surface, snapshot.todayEpochDay)
        shown = nudge
    }

    shown?.let { nudge ->
        CoachPopup(nudge = nudge, onDismiss = { shown = null })
    }
}

/**
 * Rassemble ce que l'algorithme a besoin de savoir. Les requetes sont bornees
 * dans le temps pour rester legeres, et rien d'autre n'est lu : ni le titre,
 * ni le texte du journal.
 */
@Composable
fun rememberCoachSnapshot(): CoachSnapshot? {
    val app = LocalContext.current.dayByDayApp
    val repository = app.repository
    val today = LocalDate.now()
    val start = today.minusDays(COACH_WINDOW_DAYS)

    val days by remember { repository.observeDaysBetween(start, today) }
        .collectAsStateWithLifecycle(emptyMap())
    val tags by remember { repository.observeTags() }
        .collectAsStateWithLifecycle(emptyList())
    val links by remember { repository.observeAllDayTags() }
        .collectAsStateWithLifecycle(emptyList())
    val money by remember { repository.observeMoneyBetween(start, today) }
        .collectAsStateWithLifecycle(emptyList())
    val mediaToday by remember { repository.observeMediaCounts(today, today) }
        .collectAsStateWithLifecycle(emptyMap())
    val treatments by remember { repository.observeTreatments() }
        .collectAsStateWithLifecycle(emptyList())
    val doses by remember { repository.observeDosesBetween(today.minusDays(30), today) }
        .collectAsStateWithLifecycle(emptyList())

    if (tags.isEmpty()) return null

    val hiddenCards = app.prefs.hiddenDayCards
    return remember(days, tags, links, money, mediaToday, treatments, doses, hiddenCards) {
        CoachSnapshot.build(
            today = today,
            hourOfDay = LocalTime.now().hour,
            firstName = app.prefs.firstName,
            birthDate = app.prefs.birthDate,
            entries = days.values.toList(),
            tags = tags,
            links = links,
            mediaCounts = mediaToday,
            money = money,
            treatments = treatments,
            doses = doses,
            hiddenCards = hiddenCards,
        )
    }
}

/**
 * L'ecran courant, pour le coup de pouce.
 *
 * Les ecrans qui demandent de la concentration — les reglages, le journal, la
 * recherche, l'organisation des cartes — rendent `null` : on n'interrompt pas
 * quelqu'un en train de regler quelque chose ou d'ecrire.
 */
fun coachSurfaceFor(route: String?): NudgeSurface? = when {
    route == null -> null
    route == "calendar" -> NudgeSurface.MONTH
    route == "year" -> NudgeSurface.YEAR
    route == "stats" -> NudgeSurface.STATS
    route == "money" -> NudgeSurface.MONEY
    route.startsWith("day/") -> NudgeSurface.DAY
    else -> null
}
