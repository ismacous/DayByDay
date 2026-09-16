package com.ismael.daybyday.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.Deed
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.FactorInsight
import com.ismael.daybyday.data.FoodLevel
import com.ismael.daybyday.data.PartSummary
import com.ismael.daybyday.data.PeriodSummary
import com.ismael.daybyday.data.SportLevel
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.data.WeightPoint
import com.ismael.daybyday.dayByDayApp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun StatsScreen(onOpenWeek: () -> Unit = {}) {
    val app = LocalContext.current.dayByDayApp
    val repository = app.repository
    val today = LocalDate.now()
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var showScoreHelp by rememberSaveable { mutableStateOf(false) }

    val allDays by remember { repository.observeAllDays() }
        .collectAsStateWithLifecycle(emptyList())
    val tags by remember { repository.observeTags() }
        .collectAsStateWithLifecycle(emptyList())
    val dayTags by remember { repository.observeAllDayTags() }
        .collectAsStateWithLifecycle(emptyList())
    val weights by remember { repository.observeWeights() }
        .collectAsStateWithLifecycle(emptyList())

    val yearDays = allDays.filter { LocalDate.ofEpochDay(it.epochDay).year == year }
    val yearLength = if (LocalDate.of(year, 1, 1).isLeapYear) 366 else 365
    // Les cartes masquees sortent du calcul des gestes : masquer les
    // traitements quand on n'en prend pas ne doit rien faire perdre.
    val hiddenCards = LocalContext.current.dayByDayApp.prefs.hiddenDayCards
    val yearSummary = Stats.summarize("Année $year", yearDays, yearLength, hiddenCards)
    val allTimeTotalDays = allDays.minOfOrNull { it.epochDay }
        ?.let { (today.toEpochDay() - it + 1).toInt() } ?: 0
    val allTimeSummary = Stats.summarize("Depuis le début", allDays, allTimeTotalDays, hiddenCards)
    val (currentStreak, longestStreak) = Stats.streaks(allDays, today)

    val insights = remember(allDays, tags, dayTags) { Stats.insights(allDays, tags, dayTags) }

    val weekSummaries = remember(yearDays) { weeklySummaries(yearDays, hiddenCards) }
    val bestWeek = weekSummaries.filter { it.second.filledDays >= 3 }
        .maxByOrNull { it.second.average ?: -1.0 }
    val hardestWeek = weekSummaries.filter { it.second.filledDays >= 3 }
        .minByOrNull { it.second.average ?: 99.0 }

    ScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(TAB_HEADER_HEIGHT))

            // La semaine avant l'annee : c'est l'echelle a laquelle on se
            // souvient de quelque chose. L'annee, elle, se regarde de loin.
            Appear(index = 0) {
                SoftCard(onClick = onOpenWeek, onClickLabel = "Ouvrir le bilan de la semaine") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Ma semaine",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Les sept derniers jours, comparés à ceux d'avant.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Le point chaud de l'ecran : la moyenne de l'annee dans un grand
            // anneau qui se remplit a l'ouverture. Un chiffre seul ne dit rien ;
            // le meme chiffre dans un anneau se lit d'un coup d'oeil.
            Appear(index = 1) {
                HeroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundIconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            label = "Année précédente",
                            onClick = { year -= 1 },
                        )
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                        RoundIconButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            label = "Année suivante",
                            onClick = { year += 1 },
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val note = yearSummary.note
                        ScoreRing(
                            progress = ((note.total ?: 0.0) / note.outOf).toFloat(),
                            value = note.total?.let {
                                String.format(Locale.FRANCE, "%.1f", it)
                            } ?: "—",
                            caption = "sur ${note.outOf}",
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Ce que la note veut dire, la ou on la regarde. Sans ca,
                    // un chiffre seul laisse deviner ce qu'il compte — et on
                    // devine toujours quelque chose de plus complique que la
                    // verite.
                    Text(
                        text = "Ton ressenti, plus ce que tu as fait.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = "Comment c'est calculé ?",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showScoreHelp = true }
                            .padding(vertical = 8.dp),
                    )

                    if (showScoreHelp) {
                        ScoreHelpSheet(onDismiss = { showScoreHelp = false })
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "${yearSummary.filledDays} journée(s) notée(s) sur ${yearSummary.totalDays}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(12.dp))

                    DistributionBar(yearSummary)
                }
            }

            Spacer(Modifier.height(18.dp))

            Spacer(Modifier.height(16.dp))

            PartsCard(Stats.partAverages(yearDays))

            Spacer(Modifier.height(16.dp))

            InsightsCard(insights)

            Spacer(Modifier.height(16.dp))

            HabitsCard(yearDays)

            Spacer(Modifier.height(16.dp))

            WeightCard(weights, app.prefs.bodyMassIndex(weights.lastOrNull()?.weightKg))

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Mois par mois") {
                (1..12).forEach { monthValue ->
                    val month = YearMonth.of(year, monthValue)
                    val monthDays = yearDays.filter {
                        LocalDate.ofEpochDay(it.epochDay).monthValue == monthValue
                    }
                    val summary = Stats.summarize(
                        Dates.monthShort(month),
                        monthDays,
                        month.lengthOfMonth(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = Dates.monthShort(month),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(48.dp),
                        )
                        DistributionBar(
                            summary = summary,
                            modifier = Modifier.weight(1f),
                            height = 12,
                        )
                        Spacer(Modifier.width(10.dp))
                        NoteChip(summary.note)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Semaines marquantes") {
                if (bestWeek == null && hardestWeek == null) {
                    Text(
                        "Pas encore assez de jours notés cette année.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    bestWeek?.let { (weekStart, summary) ->
                        WeekLine("Ta meilleure semaine", weekStart, summary)
                    }
                    hardestWeek?.let { (weekStart, summary) ->
                        Spacer(Modifier.height(12.dp))
                        WeekLine("Ta semaine la plus dure", weekStart, summary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionCard(title = "Régularité") {
                StatLine("Jours notés en tout", allTimeSummary.filledDays.toString())
                StatLine("Série en cours", "$currentStreak jour(s)")
                StatLine("Plus longue série", "$longestStreak jour(s)")
                allTimeSummary.average?.let {
                    StatLine("Moyenne depuis le début", formatNote(allTimeSummary.note))
                }
            }

            Spacer(Modifier.height(32.dp))
            BottomBarSpace()
        }
    }
}

@Composable
private fun PartsCard(parts: List<PartSummary>) {
    SectionCard(title = "Tes moments de la journée") {
        if (parts.all { it.days == 0 }) {
            Text(
                "Note tes matins, après-midis, soirs et nuits dans une journée : " +
                    "tu verras ici quels moments sont les plus durs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        parts.forEach { part ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${part.part.emoji} ${part.part.label}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "${part.days} jour(s) noté(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AverageChip(part.average)
            }
        }
        val worst = parts.filter { it.days >= 3 && it.average != null }.minByOrNull { it.average!! }
        val best = parts.filter { it.days >= 3 && it.average != null }.maxByOrNull { it.average!! }
        if (worst != null && best != null && worst.part != best.part) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Tes ${best.part.label.lowercase()}s sont tes meilleurs moments, " +
                    "tes ${worst.part.label.lowercase()}s les plus durs.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun InsightsCard(insights: List<FactorInsight>) {
    SectionCard(title = "Ce qui va avec tes bonnes journées") {
        if (insights.isEmpty()) {
            Text(
                "Continue à remplir le sport, les repas, les sorties et les étiquettes : " +
                    "dès que tu auras assez de journées, tu verras ici ce qui revient " +
                    "dans tes bons jours.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        insights.take(6).forEach { insight ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        insight.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = String.format(Locale.FRANCE, "%+.1f", insight.delta),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (insight.delta >= 0) {
                            DayColor.GREEN.color
                        } else {
                            DayColor.RED.color
                        },
                    )
                }
                Text(
                    text = "${formatAverage(insight.withAverage)} sur ${insight.withDays} jour(s) " +
                        "· ${formatAverage(insight.withoutAverage)} sur ${insight.withoutDays} jour(s) sans",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "C'est une observation, pas une explication : ça montre ce qui accompagne " +
                "tes bonnes journées.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HabitsCard(yearDays: List<DayEntry>) {
    val movedDays = yearDays.count { (it.sportLevel ?: -1) >= SportLevel.LIGHT.key }
    val realSessions = yearDays.count { it.sportLevel == SportLevel.GOOD.key }
    val outDays = yearDays.count { it.wentOut == true }
    val goodFood = yearDays.count { it.foodLevel == FoodLevel.GOOD.key }
    val sportFilled = yearDays.count { it.sportLevel != null }

    SectionCard(title = "Bouger, manger, sortir") {
        StatLine("Jours où tu as bougé", "$movedDays jour(s)")
        StatLine("Vraies séances de sport", "$realSessions")
        StatLine("Jours où tu es sorti", "$outDays jour(s)")
        StatLine("Jours « bien mangé »", "$goodFood jour(s)")
        if (sportFilled == 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Remplis la partie « Ta journée en détail » pour voir ces chiffres bouger.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightCard(points: List<WeightPoint>, bmi: Double?) {
    SectionCard(title = "Poids & tour de taille") {
        if (points.isEmpty()) {
            Text(
                "Note ton poids quand tu veux dans une journée : la courbe apparaîtra ici.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }

        val first = points.first()
        val last = points.last()
        StatLine("Dernier poids", formatWeight(last.weightKg))
        if (points.size > 1) {
            StatLine(
                "Depuis le ${Dates.dayShort(LocalDate.ofEpochDay(first.epochDay))}",
                formatSignedKg(last.weightKg - first.weightKg),
            )
        }

        // Le tour de taille bouge lentement, donc son ecart se lit sur toute la
        // periode et pas d'un jour a l'autre. Il n'apparait que s'il a ete
        // mesure au moins une fois : une ligne « — » n'apprend rien.
        val waists = points.filter { it.waistCm != null }
        if (waists.isNotEmpty()) {
            StatLine("Dernier tour de taille", formatWaist(waists.last().waistCm))
            if (waists.size > 1) {
                val change = waists.last().waistCm!! - waists.first().waistCm!!
                StatLine(
                    "Depuis le ${Dates.dayShort(LocalDate.ofEpochDay(waists.first().epochDay))}",
                    formatSignedCm(change),
                )
            }
        }

        bmi?.let { StatLine("IMC", String.format(Locale.FRANCE, "%.1f", it)) }

        if (points.size >= 2) {
            Spacer(Modifier.height(12.dp))
            WeightSparkline(
                points = points.takeLast(120),
                lineColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
    }
}

@Composable
private fun WeightSparkline(
    points: List<WeightPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val minWeight = points.minOf { it.weightKg }
    val maxWeight = points.maxOf { it.weightKg }
    val span = (maxWeight - minWeight).takeIf { it > 0.5 } ?: 1.0
    val minDay = points.first().epochDay
    val dayRange = (points.last().epochDay - minDay).takeIf { it > 0 } ?: 1

    Canvas(modifier = modifier) {
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = ((point.epochDay - minDay).toFloat() / dayRange) * size.width
            val ratio = ((point.weightKg - minWeight) / span).toFloat()
            val y = size.height - ratio * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
        points.forEach { point ->
            val x = ((point.epochDay - minDay).toFloat() / dayRange) * size.width
            val ratio = ((point.weightKg - minWeight) / span).toFloat()
            val y = size.height - ratio * size.height
            drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
        }
    }
}

/**
 * D'ou sort la note.
 *
 * Un chiffre seul laisse deviner ce qu'il compte, et on devine toujours
 * quelque chose de plus complique que la verite. Deux choses valent d'etre
 * dites, et ce sont les deux moities de la note : le **ressenti** ne vient que
 * de la couleur — rien de ce qu'on fait ne doit pouvoir la corriger — et les
 * **actions** n'ajoutent que ce qui a ete fait, sans jamais rien retirer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreHelpSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("D'où vient cette note ?", style = MaterialTheme.typography.titleLarge)

            Text(
                text = "Elle a deux moitiés : comment tu t'es senti, et ce que tu as fait.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text("Le ressenti — 10 points", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "La couleur que tu donnes à tes journées, et elle seule. Rien de " +
                    "ce que tu fais ne vient la corriger.",
                style = MaterialTheme.typography.bodyMedium,
            )

            DayColor.entries.sortedByDescending { it.score }.forEach { color ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(color.color),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(color.label, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = String.format(
                            Locale.FRANCE,
                            "%.1f",
                            outOfTen(color.score.toDouble()),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("Ce que tu as fait — 10 points", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Chaque geste réussi ajoute des points. Un geste manqué n'en " +
                    "retire jamais : une journée où tu n'as rien pu faire garde son " +
                    "ressenti entier.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Deed.entries.forEach { deed ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = deed.badge?.emoji.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(deed.label, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = deed.card.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Un geste appartient à une carte. Masquer une carte retire ses " +
                    "gestes du calcul des deux côtés : tu ne perds rien à cacher les " +
                    "traitements quand tu n'en prends pas, ni l'hygiène si elle ne " +
                    "t'est pas utile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Les journées sans couleur ne comptent pas du tout : elles ne " +
                    "baissent pas ta note, elles n'y entrent simplement pas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WeekLine(label: String, weekStart: LocalDate, summary: PeriodSummary) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Semaine ${Stats.weekNumber(weekStart)} — du ${Dates.dayMedium(weekStart)} " +
                        "au ${Dates.dayMedium(weekStart.plusDays(6))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NoteChip(summary.note)
        }
        Spacer(Modifier.height(6.dp))
        DistributionBar(summary)
    }
}

/** Regroupe les jours d'une annee par semaine (du lundi au dimanche). */
private fun weeklySummaries(
    days: List<DayEntry>,
    hiddenCards: Set<DayCard>,
): List<Pair<LocalDate, PeriodSummary>> =
    days.groupBy { entry ->
        val date = LocalDate.ofEpochDay(entry.epochDay)
        date.minusDays((date.dayOfWeek.value - 1).toLong())
    }.map { (weekStart, entries) ->
        weekStart to Stats.summarize("Semaine", entries, 7, hiddenCards)
    }.sortedBy { it.first }
