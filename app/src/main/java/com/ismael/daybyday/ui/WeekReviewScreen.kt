package com.ismael.daybyday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.WeekReview
import com.ismael.daybyday.data.WeekReviewBuilder
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale

/**
 * Le bilan d'une semaine.
 *
 * L'ecran ou l'application parle, au lieu d'attendre qu'on vienne la
 * consulter. Il s'ouvre par la notification du lundi matin, et depuis le
 * bilan.
 *
 * Le ton est celui de tout le reste : on decrit, on ne note pas. « Tu as bougé
 * trois jours » et pas « tu n'as bougé que trois jours » ; une semaine plus
 * difficile que la precedente est une semaine plus difficile, pas un echec.
 */
@Composable
fun WeekReviewScreen(onBack: () -> Unit, onDayClick: (LocalDate) -> Unit) {
    val repository = LocalContext.current.dayByDayApp.repository

    // On regarde la semaine **ecoulee**, celle qui vient de se terminer : le
    // lundi matin, la semaine en cours n'a encore rien a raconter.
    var mondayEpochDay by rememberSaveable {
        mutableLongStateOf(WeekReviewBuilder.mondayOf(LocalDate.now()).minusWeeks(1).toEpochDay())
    }
    val monday = LocalDate.ofEpochDay(mondayEpochDay)
    var review by remember { mutableStateOf<WeekReview?>(null) }

    LaunchedEffect(mondayEpochDay) {
        review = withContext(Dispatchers.IO) { repository.weekReview(monday) }
    }

    ScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))

            ScreenTitle(
                text = "Ma",
                accent = "semaine",
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
                    label = "Semaine précédente",
                    onClick = { mondayEpochDay = monday.minusWeeks(1).toEpochDay() },
                )
                Text(
                    text = weekLabel(monday),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                val thisWeek = WeekReviewBuilder.mondayOf(LocalDate.now())
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    label = "Semaine suivante",
                    onClick = {
                        val next = monday.plusWeeks(1)
                        if (!next.isAfter(thisWeek)) mondayEpochDay = next.toEpochDay()
                    },
                )
            }

            Spacer(Modifier.height(18.dp))

            val current = review
            if (current == null) {
                Spacer(Modifier.height(40.dp))
            } else if (!current.hasData) {
                Appear(index = 0) {
                    SoftCard {
                        Text(
                            "Rien de noté cette semaine-là.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Ce n'est pas grave — il y a des semaines qu'on ne raconte pas. " +
                                "Les flèches en haut permettent de remonter aux précédentes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                WeekBody(review = current, onDayClick = onDayClick)
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun WeekBody(review: WeekReview, onDayClick: (LocalDate) -> Unit) {
    // La carte forte : les sept jours en couleurs, la moyenne, et la phrase.
    Appear(index = 0) {
        HeroCard {
            val ink = Color.White
            Text(
                text = headline(review),
                style = MaterialTheme.typography.headlineSmall,
                color = ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = comparison(review),
                style = MaterialTheme.typography.bodyMedium,
                color = ink.copy(alpha = 0.88f),
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                review.colors.forEachIndexed { index, dayColor ->
                    val date = review.start.plusDays(index.toLong())
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (dayColor != null) {
                                        Brush.linearGradient(dayColor.gradient)
                                    } else {
                                        Brush.linearGradient(
                                            listOf(
                                                ink.copy(alpha = 0.16f),
                                                ink.copy(alpha = 0.16f),
                                            )
                                        )
                                    }
                                )
                                .clickable { onDayClick(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = dayColor?.let { readableOn(it.color) }
                                    ?: ink.copy(alpha = 0.7f),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Dates.weekDayInitials[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = ink.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Des faits, pas des rapprochements : sur sept jours, comparer « les jours
    // où tu as bougé » aux autres ne veut rien dire. Ces comparaisons-la vivent
    // dans le bilan, qui travaille sur des mois.
    Appear(index = 1) {
        SectionCard(title = "Ce que tu as fait") {
            CountLine("🏃", "Bougé", review.movedDays)
            CountLine("🚪", "Sorti", review.wentOutDays)
            CountLine("🥗", "Bien mangé", review.ateWellDays)
            CountLine("✍️", "Écrit", review.writtenDays)
            if (review.photos > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (review.photos == 1) {
                        "Et une photo gardée."
                    } else {
                        "Et ${review.photos} photos gardées."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    val hasMarkers = review.sleepAverageMinutes != null ||
        review.stepsAverage != null ||
        review.screenAverageMinutes != null
    if (hasMarkers) {
        Spacer(Modifier.height(16.dp))
        Appear(index = 2) {
            SectionCard(title = "Tes repères") {
                review.sleepAverageMinutes?.let { minutes ->
                    MarkerLine(
                        emoji = "🌙",
                        label = "Nuits",
                        value = formatDuration(minutes),
                        delta = review.previousSleepAverageMinutes?.let { minutes - it },
                        format = { formatDuration(kotlin.math.abs(it)) },
                    )
                }
                review.stepsAverage?.let { steps ->
                    MarkerLine(
                        emoji = "👟",
                        label = "Pas par jour",
                        value = groupThousands(steps),
                        delta = review.previousStepsAverage?.let { steps - it },
                        format = { groupThousands(kotlin.math.abs(it)) },
                    )
                }
                review.screenAverageMinutes?.let { minutes ->
                    MarkerLine(
                        emoji = "📱",
                        label = "Écran par jour",
                        value = formatDuration(minutes),
                        delta = review.previousScreenAverageMinutes?.let { minutes - it },
                        format = { formatDuration(kotlin.math.abs(it)) },
                        // Moins d'ecran, c'est le sens qu'on regarde comme une
                        // amelioration — l'application ne juge pas, mais la
                        // fleche doit au moins pointer du bon cote.
                        lessIsMore = true,
                    )
                }
            }
        }
    }

    if (review.topTags.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Appear(index = 3) {
            SectionCard(title = "Ce qui revient") {
                review.topTags.forEach { (tag, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tag.display, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (count == 1) "1 jour" else "$count jours",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    val money = review.money
    if (money.incomeCents != 0L || money.expenseCents != 0L) {
        Spacer(Modifier.height(16.dp))
        Appear(index = 4) {
            SectionCard(title = "Argent") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Rentré",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatMoney(money.incomeCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DayColor.GREEN.color,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Dépensé",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatMoney(money.spentCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DayColor.RED.color,
                        )
                    }
                }
            }
        }
    }

    val brightest = review.brightest
    val hardest = review.hardest
    if (brightest != null && brightest.title.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        Appear(index = 5) {
            SectionCard(title = "La journée la plus claire") {
                DayHighlight(
                    date = LocalDate.ofEpochDay(brightest.epochDay),
                    color = brightest.color,
                    title = brightest.title,
                    onClick = { onDayClick(LocalDate.ofEpochDay(brightest.epochDay)) },
                )
            }
        }
    }
    if (hardest != null && hardest.epochDay != brightest?.epochDay && hardest.title.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        Appear(index = 6) {
            SectionCard(title = "La plus difficile") {
                DayHighlight(
                    date = LocalDate.ofEpochDay(hardest.epochDay),
                    color = hardest.color,
                    title = hardest.title,
                    onClick = { onDayClick(LocalDate.ofEpochDay(hardest.epochDay)) },
                )
            }
        }
    }
}

@Composable
private fun DayHighlight(
    date: LocalDate,
    color: DayColor?,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        color?.let {
            ColorDot(color = it.color, size = 10.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                Dates.dayMedium(date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CountLine(emoji: String, label: String, days: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Text(
            text = when (days) {
                0 -> "aucun jour"
                1 -> "1 jour"
                7 -> "les 7 jours"
                else -> "$days jours"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (days == 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun MarkerLine(
    emoji: String,
    label: String,
    value: String,
    delta: Int?,
    format: (Int) -> String,
    lessIsMore: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (delta != null && delta != 0) {
                val better = if (lessIsMore) delta < 0 else delta > 0
                Text(
                    text = (if (delta > 0) "+" else "−") + format(delta),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (better) DayColor.GREEN.color else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** « Semaine 36 · 1 au 7 septembre ». */
private fun weekLabel(monday: LocalDate): String {
    val sunday = monday.plusDays(6)
    val sameMonth = monday.monthValue == sunday.monthValue
    val from = if (sameMonth) {
        monday.dayOfMonth.toString()
    } else {
        Dates.dayMedium(monday).substringAfter(' ')
    }
    return "Semaine ${com.ismael.daybyday.data.Stats.weekNumber(monday)} · " +
        "$from au ${Dates.dayMedium(sunday).substringAfter(' ')}"
}

/** La phrase du haut. Elle decrit, elle ne felicite pas et ne gronde pas. */
private fun headline(review: WeekReview): String {
    val average = review.summary.average
        ?: return "Une semaine sans couleur posée"
    val formatted = String.format(Locale.FRANCE, "%.1f", average)
    return when {
        average >= 2.5 -> "Une semaine plutôt claire · $formatted / 3"
        average >= 1.8 -> "Une semaine en demi-teinte · $formatted / 3"
        average >= 1.0 -> "Une semaine difficile · $formatted / 3"
        else -> "Une semaine très sombre · $formatted / 3"
    }
}

private fun comparison(review: WeekReview): String {
    val filled = review.summary.filledDays
    val noted = when (filled) {
        0 -> "Aucune journée notée."
        1 -> "Une journée notée sur sept."
        7 -> "Les sept journées notées."
        else -> "$filled journées notées sur sept."
    }
    val delta = review.delta ?: return "$noted Pas de semaine précédente à comparer."
    val movement = when {
        delta > 0.4 -> "Nettement plus claire que la semaine d'avant."
        delta > 0.12 -> "Un peu plus claire que la semaine d'avant."
        delta < -0.4 -> "Nettement plus difficile que la semaine d'avant."
        delta < -0.12 -> "Un peu plus difficile que la semaine d'avant."
        else -> "À peu près comme la semaine d'avant."
    }
    return "$noted $movement"
}

private fun groupThousands(value: Int): String =
    value.toString().reversed().chunked(3).joinToString(" ").reversed()
