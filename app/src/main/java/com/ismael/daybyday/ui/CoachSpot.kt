package com.ismael.daybyday.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.coach.CoachEngine
import com.ismael.daybyday.coach.CoachSnapshot
import com.ismael.daybyday.coach.Nudge
import com.ismael.daybyday.coach.NudgeSurface
import com.ismael.daybyday.coach.NudgeTone
import com.ismael.daybyday.dayByDayApp
import java.time.LocalDate
import java.time.LocalTime

/** Fenetre d'historique lue par l'algorithme : un peu plus d'un an. */
private const val COACH_WINDOW_DAYS = 400L

/**
 * L'endroit ou le coup de pouce a le droit de parler.
 *
 * A poser une seule fois par ecran. Le message se choisit tout seul a partir
 * de ce qui est coche dans les cartes, reste le meme toute la journee, et
 * disparait jusqu'au lendemain si on le ferme.
 */
@Composable
fun CoachSpot(
    surface: NudgeSurface,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.dayByDayApp
    if (!app.prefs.coachEnabled) return

    val snapshot = rememberCoachSnapshot() ?: return
    val memory = app.coach
    var closed by remember { mutableStateOf(false) }

    val nudge = remember(snapshot, closed, surface) {
        if (closed) null else CoachEngine.choose(snapshot, memory, surface)
    }

    // Le dernier message reste en memoire d'affichage : sans ca, la bulle
    // disparaitrait d'un coup au lieu de se replier.
    var displayed by remember { mutableStateOf(nudge) }

    // On retient l'affichage une fois le message reellement a l'ecran : c'est
    // ce qui fait tourner les formulations d'une fois sur l'autre.
    LaunchedEffect(nudge) {
        val shown = nudge ?: return@LaunchedEffect
        displayed = shown
        CoachEngine.markShown(memory, shown, surface, snapshot.todayEpochDay)
    }

    AnimatedVisibility(
        visible = nudge != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        displayed?.let { shown ->
            CoachBubble(
                nudge = shown,
                onDismiss = {
                    closed = true
                    memory.dismiss(surface, snapshot.todayEpochDay)
                },
            )
        }
    }
}

/**
 * Le message lui-meme : une carte a part, dans le fil de l'ecran.
 *
 * Volontairement une carte et non un bandeau en haut : un bandeau se lit comme
 * une alerte du systeme, et ce n'en est pas une. Elle se ferme d'une croix
 * discrete, jamais d'un bouton qui demanderait a etre remarque.
 */
@Composable
private fun CoachBubble(
    nudge: Nudge,
    onDismiss: () -> Unit,
) {
    // Le ton ne change que la nuance de la carte : le soutien et les
    // propositions se posent sur la couleur de l'application, les simples
    // remarques restent en retrait.
    val discreet = nudge.tone == NudgeTone.SOFT
    val background = if (discreet) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val foreground = if (discreet) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    SoftCard(
        color = background,
        padding = 14.dp,
        modifier = Modifier.testTag("coach-message"),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(nudge.tone.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Text(
                text = nudge.text,
                style = MaterialTheme.typography.bodyLarge,
                color = foreground,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 1.dp),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Masquer ce message",
                    tint = foreground,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
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

/** Apercu utilise par les reglages : il n'enregistre rien et ne se ferme pas. */
@Composable
fun CoachPreviewBubble(nudge: Nudge) {
    CoachBubble(nudge = nudge, onDismiss = {})
}
