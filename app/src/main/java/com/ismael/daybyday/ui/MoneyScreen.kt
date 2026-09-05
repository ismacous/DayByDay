package com.ismael.daybyday.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import com.ismael.daybyday.ui.theme.Brand
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.MoneyCategory
import com.ismael.daybyday.data.MoneyEntry
import com.ismael.daybyday.data.Stats
import com.ismael.daybyday.dayByDayApp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(onDayClick: (LocalDate) -> Unit) {
    val app = LocalContext.current.dayByDayApp
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    var monthIndex by rememberSaveable {
        mutableIntStateOf(YearMonth.now().year * 12 + YearMonth.now().monthValue - 1)
    }
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)

    var editing by remember { mutableStateOf<MoneyEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    /** Suppression immediate, annulable tant que le message reste affiche. */
    fun removeWithUndo(entry: MoneyEntry) {
        scope.launch {
            repository.deleteMoney(entry)
            val result = snackbar.showSnackbar(
                message = "« ${entry.displayLabel} » supprimé.",
                actionLabel = "Annuler",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                repository.saveMoney(entry.copy(id = 0))
            }
        }
    }

    val balance by remember { repository.observeMoneyBalance() }
        .collectAsStateWithLifecycle(0L)
    val monthEntries by remember(month) {
        repository.observeMoneyBetween(month.atDay(1), month.atEndOfMonth())
    }.collectAsStateWithLifecycle(emptyList())

    val summary = Stats.summarizeMoney(monthEntries)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        ScreenBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(14.dp))
                ScreenTitle(text = "Mon", accent = "argent")
                Spacer(Modifier.height(20.dp))
            }

            item {
                // Le point chaud de l'ecran : ce qu'il reste, en grand, sur la
                // carte en degrade. Un solde negatif passe au corail — c'est la
                // seule information qui doit sauter aux yeux d'ici.
                Appear(index = 0) {
                    HeroCard(
                        colors = if (balance < 0) {
                            DayColor.RED.gradient
                        } else {
                            Brand.gradient
                        },
                    ) {
                        Text(
                            "CE QU'IL TE RESTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = formatMoney(balance),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f))
                                .clickable(onClickLabel = "Corriger mon solde") {
                                    adjusting = true
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "Corriger mon solde",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthIndex -= 1 }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Mois précédent",
                        )
                    }
                    Text(
                        text = Dates.monthTitle(month),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { monthIndex += 1 }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Mois suivant",
                        )
                    }
                }
            }

            item {
                SectionCard {
                    MoneyLine("Rentrées", summary.incomeCents, DayColor.GREEN.color)
                    MoneyLine("Dépenses", -summary.spentCents, DayColor.RED.color)
                    Spacer(Modifier.height(6.dp))
                    MoneyLine(
                        label = "Différence du mois",
                        cents = summary.netCents,
                        color = if (summary.netCents < 0) DayColor.RED.color else DayColor.GREEN.color,
                        strong = true,
                    )
                    // Une correction de solde n'est ni un gain ni une depense :
                    // la compter avec le reste faisait passer un recalage de
                    // 14,90 pour de l'argent gagne.
                    if (summary.hasAdjustments) {
                        Spacer(Modifier.height(6.dp))
                        MoneyLine(
                            "Corrections du solde",
                            summary.adjustmentCents,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Les corrections remettent le total juste : elles ne " +
                                "comptent pas dans la différence du mois.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { creating = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add-money"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Ajouter un mouvement")
                }
                Spacer(Modifier.height(16.dp))
            }

            if (monthEntries.isEmpty()) {
                item {
                    Text(
                        "Aucun mouvement ce mois-ci.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Text(
                        "Appuie sur une ligne pour la corriger, glisse-la sur le " +
                            "côté pour la supprimer.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(monthEntries, key = { it.id }) { entry ->
                    SwipeToDelete(entry = entry, onDelete = { removeWithUndo(entry) }) {
                        MoneyRow(
                            entry = entry,
                            onClick = { editing = entry },
                            onOpenDay = { onDayClick(LocalDate.ofEpochDay(entry.epochDay)) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
        }
    }

    if (creating) {
        MoneyEntryDialog(
            initial = null,
            defaultDate = if (month == YearMonth.from(today)) today else month.atDay(1),
            onDismiss = { creating = false },
            onSave = { entry ->
                creating = false
                scope.launch { repository.saveMoney(entry) }
            },
            onDelete = null,
        )
    }

    editing?.let { current ->
        MoneyEntryDialog(
            initial = current,
            defaultDate = LocalDate.ofEpochDay(current.epochDay),
            onDismiss = { editing = null },
            onSave = { entry ->
                editing = null
                scope.launch { repository.saveMoney(entry) }
            },
            onDelete = {
                editing = null
                scope.launch { repository.deleteMoney(current) }
            },
        )
    }

    if (adjusting) {
        AdjustBalanceDialog(
            currentCents = balance,
            onDismiss = { adjusting = false },
            onConfirm = { targetCents ->
                adjusting = false
                val difference = targetCents - balance
                if (difference != 0L) {
                    scope.launch {
                        repository.saveMoney(
                            MoneyEntry(
                                epochDay = today.toEpochDay(),
                                amountCents = difference,
                                label = MoneyCategory.ADJUSTMENT_LABEL,
                                categoryKey = MoneyCategory.ADJUSTMENT.key,
                            )
                        )
                        snackbar.showSnackbar(
                            "Solde corrigé de ${formatSignedMoney(difference)}."
                        )
                    }
                }
            },
        )
    }
}

/**
 * Glisser une ligne vers la gauche ou la droite la supprime. Une erreur de
 * saisie se repare ainsi en un geste, et le message qui suit permet de revenir
 * en arriere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(
    entry: MoneyEntry,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val swipedAway = value != SwipeToDismissBoxValue.Settled
            if (swipedAway) onDelete()
            swipedAway
        }
    )

    // La ligne supprimee disparait de la liste : l'etat du geste doit repartir
    // de zero si la meme ligne revient apres une annulation.
    LaunchedEffect(entry.id) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) state.reset()
    }

    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DayColor.RED.color.copy(alpha = 0.18f))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                },
            ) {
                Text(
                    "Supprimer",
                    style = MaterialTheme.typography.labelLarge,
                    color = DayColor.RED.color,
                )
            }
        },
        content = { content() },
    )
}

@Composable
private fun MoneyLine(label: String, cents: Long, color: androidx.compose.ui.graphics.Color, strong: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = formatSignedMoney(cents),
            style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

@Composable
private fun MoneyRow(entry: MoneyEntry, onClick: () -> Unit, onOpenDay: () -> Unit) {
    SectionCard(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (entry.isIncome) {
                            DayColor.GREEN.color.copy(alpha = 0.2f)
                        } else {
                            DayColor.RED.color.copy(alpha = 0.2f)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(entry.category?.emoji ?: if (entry.isIncome) "➕" else "➖")
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayLabel,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = Dates.dayMedium(LocalDate.ofEpochDay(entry.epochDay)) +
                        (entry.category?.let { " · ${it.label}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onOpenDay),
                )
            }
            Text(
                text = formatSignedMoney(entry.amountCents),
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.isIncome) DayColor.GREEN.color else DayColor.RED.color,
            )
        }
    }
}

/**
 * Correction du solde. Le dialogue montre le mouvement qui va etre cree avant
 * de l'enregistrer : sans cela, corriger le total juste apres avoir saisi une
 * depense la comptait une seconde fois, a l'envers.
 */
@Composable
private fun AdjustBalanceDialog(
    currentCents: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var text by remember {
        mutableStateOf(String.format(java.util.Locale.FRANCE, "%.2f", currentCents / 100.0))
    }
    val target = text.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() }
    val difference = target?.minus(currentCents)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corriger mon solde") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "À n'utiliser que si le compte ne tombe pas juste. Tes dépenses " +
                        "et tes rentrées sont déjà retirées ou ajoutées : si tu viens " +
                        "d'en saisir une, il n'y a rien à corriger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "L'application compte ${formatMoney(currentCents)}.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(10)
                    },
                    label = { Text("Ce que tu as vraiment") },
                    suffix = { Text("€") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().imePadding(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when {
                        difference == null -> "Entre un montant pour voir la correction."
                        difference == 0L -> "Le compte tombe déjà juste : rien à corriger."
                        else -> "Une correction de ${formatSignedMoney(difference)} sera " +
                            "ajoutée aujourd'hui. Elle n'est comptée ni dans tes " +
                            "rentrées ni dans tes dépenses."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (difference == null || difference == 0L) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = difference != null && difference != 0L,
                onClick = { target?.let(onConfirm) },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
