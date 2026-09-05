package com.ismael.daybyday.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.dayByDayApp

/**
 * Choisir ce que montre l'ecran d'une journee, et dans quel ordre.
 *
 * C'est un ecran entier et non une boite de dialogue : la liste fait dix
 * lignes, chacune avec un texte d'explication et trois gestes possibles.
 * Comprimee dans une fenetre, elle etait illisible et les boutons se
 * touchaient.
 *
 * Les fleches plutot qu'un glisser-deposer : le geste reussit du premier coup
 * et ne se bat pas avec le defilement de la liste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeCardsScreen(onBack: () -> Unit) {
    val prefs = LocalContext.current.dayByDayApp.prefs

    var order by remember { mutableStateOf(prefs.dayCardOrder) }
    var hidden by remember { mutableStateOf(prefs.hiddenDayCards) }

    fun apply(newOrder: List<DayCard>, newHidden: Set<DayCard>) {
        order = newOrder
        hidden = newHidden
        prefs.dayCardOrder = newOrder
        prefs.hiddenDayCards = newHidden
    }

    val shown = order.filterNot { it in hidden }
    val masked = order.filter { it in hidden }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organiser ma journée") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                Text(
                    text = "Les flèches déplacent une carte. La croix la retire de ta " +
                        "journée sans rien effacer : ce que tu y as noté reste, et la " +
                        "carte revient dès que tu la remets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            item { SectionLabel("DANS MA JOURNÉE") }

            items(shown, key = { it.key }) { card ->
                val index = order.indexOf(card)
                val position = shown.indexOf(card)
                CardRow(
                    card = card,
                    onUp = { apply(order.movedBefore(index, shown, position - 1), hidden) },
                    onDown = { apply(order.movedBefore(index, shown, position + 1), hidden) },
                    canMoveUp = position > 0,
                    canMoveDown = position < shown.lastIndex,
                    trailing = {
                        if (card.essential) {
                            Text(
                                "Toujours là",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                        } else {
                            IconButton(onClick = { apply(order, hidden + card) }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Retirer ${card.title}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

            if (masked.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("RETIRÉES")
                }
                items(masked, key = { "hidden-${it.key}" }) { card ->
                    CardRow(
                        card = card,
                        onUp = {},
                        onDown = {},
                        canMoveUp = false,
                        canMoveDown = false,
                        dimmed = true,
                        trailing = {
                            IconButton(onClick = { apply(order, hidden - card) }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Remettre ${card.title}",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun CardRow(
    card: DayCard,
    onUp: () -> Unit,
    onDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    trailing: @Composable () -> Unit,
    dimmed: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!dimmed) {
            Column {
                IconButton(
                    onClick = onUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Monter")
                }
                IconButton(
                    onClick = onDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descendre")
                }
            }
        } else {
            Spacer(Modifier.width(32.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (dimmed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = card.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        trailing()
    }
}

/**
 * Deplace la carte a [from] pour qu'elle prenne la place [targetVisible] parmi
 * les cartes affichees. Les cartes retirees gardent leur rang dans l'ordre
 * complet, donc on vise la position de la carte affichee voisine plutot qu'un
 * simple decalage d'un cran.
 */
private fun List<DayCard>.movedBefore(
    from: Int,
    visible: List<DayCard>,
    targetVisible: Int,
): List<DayCard> {
    if (from !in indices || targetVisible !in visible.indices) return this
    val neighbour = visible[targetVisible]
    val to = indexOf(neighbour)
    if (to == -1 || to == from) return this
    val copy = toMutableList()
    copy.add(to, copy.removeAt(from))
    return copy
}
