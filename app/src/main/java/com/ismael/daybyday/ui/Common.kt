package com.ismael.daybyday.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.PeriodSummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    private val MONTH_TITLE = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.FRANCE)
    private val DAY_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)
    private val DAY_MEDIUM = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)
    private val DAY_SHORT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE)
    private val MONTH_SHORT = DateTimeFormatter.ofPattern("LLL", Locale.FRANCE)

    fun monthTitle(month: YearMonth): String =
        month.format(MONTH_TITLE).replaceFirstChar { it.uppercase() }

    fun monthShort(month: YearMonth): String =
        month.format(MONTH_SHORT).replaceFirstChar { it.uppercase() }.removeSuffix(".")

    fun dayLong(date: LocalDate): String =
        date.format(DAY_LONG).replaceFirstChar { it.uppercase() }

    fun dayMedium(date: LocalDate): String =
        date.format(DAY_MEDIUM).replaceFirstChar { it.uppercase() }

    fun dayShort(date: LocalDate): String = date.format(DAY_SHORT)

    val weekDayInitials = listOf("L", "M", "M", "J", "V", "S", "D")
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun formatAverage(average: Double?): String =
    average?.let { String.format(Locale.FRANCE, "%.1f", it) + " / 3" } ?: "—"

fun formatWeight(weightKg: Double?): String =
    weightKg?.let { String.format(Locale.FRANCE, "%.1f kg", it) } ?: "—"

fun formatSignedKg(delta: Double): String =
    String.format(Locale.FRANCE, "%+.1f kg", delta)

/** Montant en centimes vers un texte en euros. */
fun formatMoney(cents: Long): String =
    String.format(Locale.FRANCE, "%,.2f €", cents / 100.0).replace('\u00A0', ' ')

fun formatSignedMoney(cents: Long): String {
    val sign = if (cents > 0) "+" else if (cents < 0) "−" else ""
    return sign + formatMoney(kotlin.math.abs(cents))
}

/** Noir ou blanc selon la luminosite du fond, pour rester lisible. */
fun readableOn(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color(0xFF101318) else Color.White
}

@Composable
fun AverageChip(average: Double?, modifier: Modifier = Modifier) {
    val base = average?.let { DayColor.fromAverage(it) }
    val fill: Brush = if (average != null) {
        Brush.linearGradient(DayColor.gradientForAverage(average))
    } else {
        SolidColor(MaterialTheme.colorScheme.surfaceVariant)
    }
    val textColor = if (base == null) MaterialTheme.colorScheme.onSurfaceVariant else readableOn(base)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(fill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = formatAverage(average),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

/** Petite pastille selectionnable, utilisee pour le sport, les repas, les tags. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // La pastille glisse d'une teinte a l'autre au lieu de sauter : c'est la
    // difference entre une interface qui repond et une qui clignote.
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(Motion.NORMAL),
        label = "fond",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.NORMAL),
        label = "texte",
    )
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/**
 * La carte de section, utilisee par tous les ecrans.
 *
 * Elle est blanche sur le creme du fond : c'est ce simple ecart de teinte qui
 * la fait exister, sans bordure ni ombre appuyee. Empiler des traits fatigue
 * l'oeil ; empiler des surfaces le laisse respirer.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun DistributionBar(summary: PeriodSummary, modifier: Modifier = Modifier, height: Int = 10) {
    val total = DayColor.entries.sumOf { summary.countOf(it) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        if (total == 0) return@Row
        DayColor.entries.forEach { color ->
            val count = summary.countOf(color)
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .fillMaxWidth()
                        .background(color.color),
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    summary: PeriodSummary,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${summary.filledDays} jour(s) noté(s) sur ${summary.totalDays}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AverageChip(summary.average)
        }
        Spacer(Modifier.height(12.dp))
        DistributionBar(summary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DayColor.entries.forEach { color ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color.color),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        summary.countOf(color).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Ouvre un ecran des reglages d'Android. Certains telephones n'ont pas l'ecran
 * precis demande : on retombe alors sur la fiche de l'application, qui existe
 * toujours et donne acces aux memes autorisations.
 */
fun openSystemScreen(context: Context, intent: Intent) {
    if (runCatching { context.startActivity(intent) }.isSuccess) return
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
