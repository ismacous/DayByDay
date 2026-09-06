package com.ismael.daybyday.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenYear: (Int) -> Unit,
) {
    val app = LocalContext.current.dayByDayApp
    val repository = app.repository
    val today = LocalDate.now()

    val gridStart = remember(month) {
        val first = month.atDay(1)
        first.minusDays((first.dayOfWeek.value - 1).toLong())
    }
    val weekCount = remember(month) {
        val offset = month.atDay(1).dayOfWeek.value - 1
        ((offset + month.lengthOfMonth() + 6) / 7)
    }
    val gridEnd = remember(month) { gridStart.plusDays((weekCount * 7 - 1).toLong()) }

    val entries by remember(month) { repository.observeDaysBetween(gridStart, gridEnd) }
        .collectAsStateWithLifecycle(emptyMap())
    val mediaCounts by remember(month) { repository.observeMediaCounts(gridStart, gridEnd) }
        .collectAsStateWithLifecycle(emptyMap())
    val todayEntry by remember { repository.observeDay(today) }
        .collectAsStateWithLifecycle(null)

    val monthEntries = entries.values.filter {
        YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) == month
    }
    val monthSummary = Stats.summarize(Dates.monthTitle(month), monthEntries, month.lengthOfMonth())

    // Le titre se lit en deux voix : le bonjour en sans-serif, le prenom en
    // serif italique. C'est la signature typographique de l'application, et
    // c'est aussi ce qui rend l'accueil personnel plutot qu'administratif.
    val name = app.prefs.firstName.trim()
    val greeting = if (name.isEmpty()) "Mon" else "Salut"
    val accentWord = if (name.isEmpty()) "carnet" else name

    // Pas de barre d'application ici : on n'arrive pas dans un outil, on
    // ouvre son carnet. Le nom et la date tiennent lieu d'accueil.
    ScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            ScreenTitle(
                text = greeting,
                accent = accentWord,
                subtitle = Dates.dayLong(today),
                trailing = {
                    RoundIconButton(
                        icon = Icons.Default.Search,
                        label = "Rechercher",
                        onClick = onOpenSearch,
                    )
                },
            )

            Spacer(Modifier.height(20.dp))

            Appear(index = 0) {
            TodayCard(
                today = today,
                entry = todayEntry,
                onPickColor = { color ->
                    app.appScope.launch {
                        val base = repository.dayOnce(today) ?: DayEntry(epochDay = today.toEpochDay())
                        val next = if (base.colorKey == color.key) null else color.key
                        repository.saveDay(
                            base.copy(
                                colorKey = next,
                                // Choix explicite : il prime sur la moyenne des moments.
                                colorManual = next != null,
                            )
                        )
                    }
                },
                onOpenToday = { onDayClick(today) },
            )
            }

            Spacer(Modifier.height(18.dp))

            Appear(index = 1) {
            SoftCard(padding = 14.dp) {
            MonthHeader(
                month = month,
                onPrevious = { onMonthChange(month.minusMonths(1)) },
                onNext = { onMonthChange(month.plusMonths(1)) },
                onOpenYear = { onOpenYear(month.year) },
            )

            WeekDayHeader()

            repeat(weekCount) { weekIndex ->
                WeekRow(
                    weekStart = gridStart.plusDays((weekIndex * 7).toLong()),
                    month = month,
                    today = today,
                    entries = entries,
                    mediaCounts = mediaCounts,
                    onDayClick = onDayClick,
                )
            }
            }
            }

            Spacer(Modifier.height(18.dp))

            Appear(index = 2) {
                SummaryCard(title = "Bilan du mois", summary = monthSummary)
            }

            if (month != YearMonth.from(today)) {
                TextButton(
                    onClick = { onMonthChange(YearMonth.from(today)) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Revenir à aujourd'hui")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TodayCard(
    today: LocalDate,
    entry: DayEntry?,
    onPickColor: (DayColor) -> Unit,
    onOpenToday: () -> Unit,
) {
    // La carte du jour porte le degrade de la marque : c'est la premiere chose
    // qu'on voit, et la seule qu'on vient faire la plupart du temps. Tout le
    // reste de l'ecran est blanc pour qu'elle reste seule a briller.
    HeroCard(onClick = onOpenToday, onClickLabel = "Ouvrir ma journée") {
        Text(
            text = "AUJOURD'HUI",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = entry?.color?.label ?: "Comment s'est passée ta journée ?",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        val todayTitle = entry?.title.orEmpty()
        if (todayTitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = todayTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DayColor.entries.forEach { dayColor ->
                val selected = entry?.colorKey == dayColor.key
                // La pastille choisie grandit un peu et s'entoure : on voit son
                // choix d'un coup d'oeil, sans avoir a chercher une coche.
                val height by animateDpAsState(
                    targetValue = if (selected) 62.dp else 52.dp,
                    animationSpec = tween(Motion.NORMAL),
                    label = "hauteur",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(height)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(dayColor.gradient))
                        .border(
                            BorderStroke(
                                if (selected) 3.dp else 0.dp,
                                if (selected) Color.White else Color.Transparent,
                            ),
                            RoundedCornerShape(18.dp),
                        )
                        .clickable { onPickColor(dayColor) }
                        .testTag("today-${dayColor.name}"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Text("✓", color = readableOn(dayColor.color), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (entry == null) "Écrire dans mon journal →" else "Ouvrir ma journée →",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

/** Un bouton rond discret, pour les gestes qui accompagnent un titre. */
@Composable
fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
    }
}

/**
 * L'en-tete du mois. Le nom du mois est un bouton : il ouvre l'annee entiere.
 * C'est la qu'on regarde deja quand on veut prendre du recul, donc c'est la
 * que le geste doit se trouver — plutot que dans un onglet a part.
 */
@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenYear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            label = "Mois précédent",
            onClick = onPrevious,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClickLabel = "Voir l'année entière", onClick = onOpenYear)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = Dates.monthTitle(month).substringBefore(' '),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${month.year} · voir l'année",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        RoundIconButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            label = "Mois suivant",
            onClick = onNext,
        )
    }
}

@Composable
private fun WeekDayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    ) {
        Dates.weekDayInitials.forEach { initial ->
            Text(
                text = initial,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(modifier = Modifier.width(WEEK_COLUMN_WIDTH)) {
            Text(
                text = "sem.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val WEEK_COLUMN_WIDTH = 34.dp

@Composable
private fun WeekRow(
    weekStart: LocalDate,
    month: YearMonth,
    today: LocalDate,
    entries: Map<Long, DayEntry>,
    mediaCounts: Map<Long, Int>,
    onDayClick: (LocalDate) -> Unit,
) {
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val weekEntries = weekDays.mapNotNull { entries[it.toEpochDay()] }
    val weekSummary = Stats.summarize("Semaine", weekEntries, 7)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        weekDays.forEach { date ->
            DayCell(
                date = date,
                entry = entries[date.toEpochDay()],
                mediaCount = mediaCounts[date.toEpochDay()] ?: 0,
                inMonth = YearMonth.from(date) == month,
                isToday = date == today,
                isFuture = date.isAfter(today),
                onClick = { onDayClick(date) },
                modifier = Modifier.weight(1f),
            )
        }
        WeekScore(
            weekNumber = Stats.weekNumber(weekStart),
            average = weekSummary.average,
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    entry: DayEntry?,
    mediaCount: Int,
    inMonth: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayColor = entry?.color
    val alpha = when {
        !inMonth -> 0.30f
        isFuture -> 0.6f
        else -> 1f
    }
    val textColor = if (dayColor != null) {
        readableOn(dayColor.color)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(14.dp)

    // Une journee notee est une pastille pleine, en degrade et avec sa propre
    // ombre : elle avance vers le doigt. Une journee vide reste en retrait.
    // C'est ce contraste qui fait qu'on lit un mois d'un coup d'oeil.
    val fill: Brush = if (dayColor != null) {
        Brush.linearGradient(dayColor.gradient.map { it.copy(alpha = alpha) })
    } else {
        SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    }

    // Aujourd'hui porte un anneau qui bat lentement : c'est le seul repere
    // qu'on cherche vraiment dans une grille de trente-cinq cases.
    //
    // Deux precautions, et l'anneau saccadait sans elles.
    //
    // (1) L'animation n'existe que pour la case du jour. Elle etait creee dans
    //     les trente-cinq cases : trente-quatre horloges tournaient pour rien,
    //     et reveillaient la composition a chaque image.
    // (2) La valeur animee n'est **jamais lue pendant la composition**. Une
    //     epaisseur de bordure qui change, c'est une mesure refaite a chaque
    //     image — donc la case entiere recomposee et remesuree soixante ou cent
    //     vingt fois par seconde, pour un trait. Ici l'anneau est dessine a la
    //     main et la valeur n'est lue que dans le dessin : rien n'est recompose,
    //     rien n'est remesure, il ne reste qu'un trait a repeindre.
    val ring: State<Float>? = if (isToday) {
        val beat = rememberInfiniteTransition(label = "aujourdhui")
        beat.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                // Une rampe lineaire qui repart en arriere donne un battement
                // mecanique, en dents de scie. Adoucie aux deux bouts, elle
                // respire.
                animation = tween(1900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "anneau",
        )
    } else {
        null
    }
    val ringColor = MaterialTheme.colorScheme.primary
    val cornerRadius = 14.dp

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .testTag("day-${date.toEpochDay()}"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (dayColor != null && inMonth) {
                        Modifier.brandShadow(
                            elevation = 6.dp,
                            shape = shape,
                            color = dayColor.color,
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(shape)
                .background(fill)
                .then(
                    if (ring != null) {
                        Modifier.drawWithContent {
                            drawContent()
                            // La lecture de `ring` a lieu ici, dans le dessin.
                            // C'est tout l'interet : la case n'est pas
                            // recomposee, seulement repeinte.
                            val stroke = (1.5f + 1.5f * ring.value).dp.toPx()
                            val radius = cornerRadius.toPx() - stroke / 2f
                            drawRoundRect(
                                color = ringColor,
                                topLeft = Offset(stroke / 2f, stroke / 2f),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(radius, radius),
                                style = Stroke(width = stroke),
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor.copy(alpha = alpha),
            )

            val hasText = entry != null && (entry.title.isNotBlank() || entry.note.isNotBlank())
            val hasTracking = entry != null &&
                (entry.sportLevel != null || entry.foodLevel != null || entry.wentOut != null)
            if (hasText || hasTracking || mediaCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (hasText) {
                        Dot(textColor.copy(alpha = 0.85f * alpha), CircleShape)
                    }
                    if (mediaCount > 0) {
                        Dot(textColor.copy(alpha = 0.85f * alpha), RoundedCornerShape(1.dp))
                    }
                    if (hasTracking) {
                        Dot(textColor.copy(alpha = 0.5f * alpha), CircleShape)
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color, shape: androidx.compose.ui.graphics.Shape) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(shape)
            .background(color),
    )
}

@Composable
private fun WeekScore(weekNumber: Int, average: Double?) {
    val background = average?.let { DayColor.fromAverage(it) }
        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textColor = if (average == null) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } else {
        readableOn(background)
    }
    Box(
        modifier = Modifier
            .width(WEEK_COLUMN_WIDTH)
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = weekNumber.toString(), fontSize = 11.sp, color = textColor)
    }
}
