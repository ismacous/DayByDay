package com.ismael.daybyday.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DoseTaken
import com.ismael.daybyday.data.DoseTime
import com.ismael.daybyday.data.Treatment
import java.util.Locale

/**
 * L'enveloppe d'une carte de l'ecran d'une journee : son titre, et le geste
 * qui la replie. Le contenu n'est pas compose quand la carte est repliee, donc
 * une carte fermee ne coute rien.
 */
@Composable
fun DayCardShell(
    card: DayCard,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    /**
     * Le degrade de la carte, quand elle doit se detacher des autres. Une
     * seule carte par ecran le porte : celle de l'humeur, qui est la raison
     * d'etre de la page. Les autres restent blanches autour d'elle — c'est ce
     * contraste qui donne une hierarchie, pas la taille des titres.
     */
    accent: List<Color>? = null,
    /**
     * L'encre a utiliser, quand elle est imposee. Sert au degrade de
     * l'application, qui se porte en blanc partout ailleurs : le calcul
     * automatique choisirait du sombre, et la carte ne ressemblerait plus a
     * celle qu'on a touchee pour arriver ici.
     */
    accentInk: Color? = null,
    content: @Composable () -> Unit,
) {
    // L'encre se choisit sur **tout** le degrade, pas sur sa premiere couleur :
    // le vert des bonnes journees part d'un vert moyen et finit clair, et une
    // encre choisie sur le depart s'efface a l'arrivee.
    val onAccent = accent?.let { accentInk ?: readableOnAll(it) }
    val title = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleCollapse),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                card.title,
                style = MaterialTheme.typography.titleMedium,
                color = onAccent ?: MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (collapsed) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.Default.KeyboardArrowUp
                },
                contentDescription = if (collapsed) "Déplier" else "Replier",
                tint = onAccent ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // L'encre est annoncee une fois pour toute la carte : sans ca, seul le titre
    // etait lisible et le reste du contenu gardait les couleurs du theme, gris
    // sur vert ou bleu nuit sur indigo.
    CompositionLocalProvider(LocalCardInk provides onAccent) {
        if (accent != null) {
            HeroCard(colors = accent) {
                title()
                if (!collapsed) {
                    Spacer(Modifier.height(14.dp))
                    content()
                }
            }
        } else {
            SoftCard {
                title()
                if (!collapsed) {
                    Spacer(Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}

/**
 * Le sommeil de la nuit qui a mene a cette journee. Rien n'est impose : sans
 * montre ni saisie, la carte reste une seule ligne discrete.
 */
@Composable
fun SleepCardBody(
    startMinutes: Int?,
    endMinutes: Int?,
    fromDevice: Boolean,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TimeButton(
            label = "Couché à",
            minutes = startMinutes,
            onClick = onPickStart,
            modifier = Modifier.weight(1f),
        )
        TimeButton(
            label = "Levé à",
            minutes = endMinutes,
            onClick = onPickEnd,
            modifier = Modifier.weight(1f),
        )
    }

    val duration = durationMinutes(startMinutes, endMinutes)
    Spacer(Modifier.height(12.dp))

    if (duration == null) {
        Text(
            text = "Appuie sur une heure pour noter ta nuit. Si ta montre ou ton " +
                "téléphone l'enregistre dans Health Connect, elle se remplit toute seule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = sleepColor(duration),
                )
                Text(
                    text = if (fromDevice) {
                        "Lu sur ton téléphone. Touche une heure pour corriger."
                    } else {
                        sleepComment(duration)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClear) { Text("Effacer") }
        }
    }
}

@Composable
private fun TimeButton(
    label: String,
    minutes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = minutes?.let { formatClock(it) } ?: "—:—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Les verres d'eau de la journee, comptes un par un. */
@Composable
fun WaterRow(glasses: Int?, onChange: (Int?) -> Unit) {
    val count = glasses ?: 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("💧 Eau", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (glasses == null) {
                    "Pas encore compté."
                } else {
                    "$count verre(s) · environ ${formatLitres(count)}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onChange((count - 1).takeIf { it > 0 }) },
            enabled = count > 0,
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = { onChange((count + 1).coerceAtMost(30)) }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/**
 * Les traitements du jour. Chaque prise attendue est une case a cocher : la
 * ligne n'existe en base que si la prise a eu lieu, donc "pas encore pris" ne
 * consomme rien.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TreatmentsCardBody(
    treatments: List<Treatment>,
    taken: List<DoseTaken>,
    onToggle: (Treatment, DoseTime, Boolean) -> Unit,
    onEdit: (Treatment) -> Unit,
    onAdd: () -> Unit,
) {
    val active = treatments.filter { it.active }

    if (active.isEmpty()) {
        Text(
            text = "Aucun traitement pour l'instant. Ajoute ce que tu prends et tu " +
                "pourras cocher chaque prise, matin, midi, soir ou nuit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val done = taken.map { it.treatmentId to it.timeKey }.toSet()
        active.forEachIndexed { index, treatment ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(treatment.name, style = MaterialTheme.typography.bodyLarge)
                    if (treatment.dose.isNotBlank()) {
                        Text(
                            treatment.dose,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = { onEdit(treatment) }) { Text("Modifier") }
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                treatment.times.forEach { time ->
                    val checked = (treatment.id to time.key) in done
                    DoseChip(
                        time = time,
                        checked = checked,
                        onClick = { onToggle(treatment, time, !checked) },
                    )
                }
            }
        }

        val expected = active.sumOf { it.times.size }
        val doneCount = taken.count { dose -> active.any { it.id == dose.treatmentId } }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                expected == 0 -> "Aucune prise prévue."
                doneCount >= expected -> "Tout est pris pour aujourd'hui."
                else -> "$doneCount prise(s) sur $expected."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (doneCount >= expected && expected > 0) {
                DayColor.GREEN.color
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }

    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Ajouter un traitement")
    }
}

@Composable
private fun DoseChip(time: DoseTime, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (checked) {
                    DayColor.GREEN.color.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = DayColor.GREEN.color,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        } else {
            Text(time.emoji)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = time.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Creation ou modification d'un traitement. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TreatmentDialog(
    initial: Treatment?,
    onDismiss: () -> Unit,
    onSave: (Treatment) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var dose by remember { mutableStateOf(initial?.dose.orEmpty()) }
    var mask by remember { mutableIntStateOf(initial?.timesMask ?: DoseTime.MORNING.bit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau traitement" else "Modifier le traitement") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it.take(40) },
                    label = { Text("Dose (optionnel)") },
                    placeholder = { Text("1 comprimé") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text("Quand le prends-tu ?", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DoseTime.entries.forEach { time ->
                        ChoiceChip(
                            label = "${time.emoji} ${time.label}",
                            selected = mask and time.bit != 0,
                            onClick = { mask = mask xor time.bit },
                        )
                    }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) {
                        Text(
                            "Supprimer ce traitement",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && mask != 0,
                onClick = {
                    onSave(
                        (initial ?: Treatment(name = "")).copy(
                            name = name.trim(),
                            dose = dose.trim(),
                            timesMask = mask,
                        )
                    )
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Selecteur d'heure qui rend des minutes depuis minuit. */
fun showClock(context: Context, initialMinutes: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        true,
    ).show()
}

/** Duree d'une nuit, en tenant compte du passage de minuit. */
fun durationMinutes(startMinutes: Int?, endMinutes: Int?): Int? {
    val start = startMinutes ?: return null
    val end = endMinutes ?: return null
    val length = if (end >= start) end - start else end + DayEntry.MINUTES_PER_DAY - start
    return if (length in 1 until DayEntry.MINUTES_PER_DAY) length else null
}

fun formatClock(minutes: Int): String =
    String.format(Locale.FRANCE, "%02d:%02d", (minutes / 60) % 24, minutes % 60)

fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "${hours} h" else "${hours} h ${String.format(Locale.FRANCE, "%02d", rest)}"
}

private fun formatLitres(glasses: Int): String =
    String.format(Locale.FRANCE, "%.2f L", glasses * 0.25)

/**
 * Reperes de duree, volontairement larges : la carte informe, elle ne juge pas
 * et ne remplace pas un avis medical.
 */
private fun sleepComment(minutes: Int): String = when {
    minutes < 5 * 60 -> "Nuit très courte."
    minutes < 7 * 60 -> "Nuit courte."
    minutes <= 9 * 60 -> "Nuit dans la moyenne."
    else -> "Nuit longue."
}

@Composable
private fun sleepColor(minutes: Int): androidx.compose.ui.graphics.Color = when {
    minutes < 5 * 60 -> DayColor.RED.color
    minutes < 7 * 60 -> DayColor.ORANGE.color
    minutes <= 9 * 60 -> DayColor.GREEN.color
    else -> MaterialTheme.colorScheme.onSurface
}
