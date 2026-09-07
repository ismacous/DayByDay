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
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.Note
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

/**
 * La moyenne d'une couleur (0 a 3) devient une note sur dix.
 *
 * Les quatre couleurs valent 0, 1, 2 et 3 — c'est ce qui est **enregistre**, et
 * ca ne bouge pas. Mais « 1,7 sur 3 » ne veut rien dire pour personne : sur
 * trois, on ne sait pas si c'est bien ou mal sans y reflechir. Sur dix, si.
 * La conversion se fait donc a l'affichage, et seulement la.
 */
fun outOfTen(average: Double): Double = average / DayColor.MAX_SCORE * 10.0

/**
 * Le **ressenti** seul, sur dix.
 *
 * Sert la ou l'on compare des couleurs entre elles — « les jours ou tu as
 * bouge » contre les autres. Y melanger les gestes n'aurait aucun sens : on
 * comparerait une chose a elle-meme.
 */
fun formatAverage(average: Double?): String =
    average?.let { String.format(Locale.FRANCE, "%.1f", outOfTen(it)) + " / 10" } ?: "—"

/** La note complete : le ressenti plus les gestes. */
fun formatNote(note: Note): String {
    val total = note.total ?: return "—"
    return String.format(Locale.FRANCE, "%.1f", total) + " / " + note.outOf
}



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

/** L'encre sombre de l'application : jamais un noir pur, qui vibre sur couleur. */
val InkDark = Color(0xFF101318)

/** Noir ou blanc selon la luminosite du fond, pour rester lisible. */
fun readableOn(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) InkDark else Color.White
}

/**
 * L'encre d'un **degrade**, et non d'une couleur.
 *
 * Choisir d'apres la seule couleur de depart se paie tout de suite : un vert
 * moyen appelle du blanc, mais le meme degrade finit dans un vert clair ou le
 * blanc disparait. On essaie donc les deux encres sur **toutes** les couleurs du
 * degrade et on garde celle dont le pire contraste est le meilleur. Le calcul
 * est celui du contraste reel (WCAG), pas une moyenne des canaux : c'est le vert
 * qui trompe le plus l'oeil, et c'est justement la couleur des bonnes journees.
 */
fun readableOnAll(colors: List<Color>): Color {
    if (colors.isEmpty()) return Color.White
    val dark = colors.minOf { contrastRatio(InkDark, it) }
    val light = colors.minOf { contrastRatio(Color.White, it) }
    return if (dark >= light) InkDark else Color.White
}

private fun contrastRatio(a: Color, b: Color): Float {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val high = kotlin.math.max(la, lb)
    val low = kotlin.math.min(la, lb)
    return (high + 0.05f) / (low + 0.05f)
}

private fun relativeLuminance(color: Color): Float {
    fun channel(value: Float): Float =
        if (value <= 0.03928f) value / 12.92f
        else Math.pow((value + 0.055f).toDouble() / 1.055, 2.4).toFloat()
    return 0.2126f * channel(color.red) +
        0.7152f * channel(color.green) +
        0.0722f * channel(color.blue)
}

/**
 * La note d'une periode, sur la couleur du ressenti.
 *
 * Le **texte** est la note complete, gestes compris ; la **teinte** ne vient
 * que du ressenti. C'est voulu : la couleur repond a « comment ca s'est
 * passe ? », et une semaine noire ou l'on s'est beaucoup bouge ne doit pas
 * s'afficher en vert.
 */
@Composable
fun NoteChip(note: Note, modifier: Modifier = Modifier) {
    val base = note.moodAverage?.let { DayColor.fromAverage(it) }
    val fill = base ?: MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (base == null) MaterialTheme.colorScheme.onSurfaceVariant else readableOn(base)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(fill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = formatNote(note),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

/** Le ressenti seul : les moments d'une journee n'ont pas de gestes. */
@Composable
fun AverageChip(average: Double?, modifier: Modifier = Modifier) {
    // Pas de degrade ici : sur une pastille de deux centimetres, un degrade ne
    // se lit pas comme une matiere mais comme une autre couleur — le coin clair
    // d'un vert moyen passait pour un vert eclatant, et la semaine avait l'air
    // bien meilleure qu'elle ne l'etait. Les degrades restent aux grandes
    // surfaces ; ici, un aplat qui dit juste la bonne teinte.
    val base = average?.let { DayColor.fromAverage(it) }
    val fill = base ?: MaterialTheme.colorScheme.surfaceVariant
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
    /**
     * Le rang de la carte dans l'ecran. Quand il est donne, la carte arrive en
     * glissant, un peu apres la precedente : c'est ce decalage qui fait qu'un
     * ecran s'ouvre au lieu d'apparaitre d'un bloc. `null` pour les cartes
     * d'une liste, ou l'effet se repeterait a chaque defilement.
     */
    index: Int? = null,
    content: @Composable () -> Unit,
) {
    val card = @Composable {
        SoftCard(modifier = modifier) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
            }
            content()
        }
    }
    if (index == null) card() else Appear(index = index) { card() }
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
            NoteChip(summary.note)
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
