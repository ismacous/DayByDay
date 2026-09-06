package com.ismael.daybyday.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DaySearch
import com.ismael.daybyday.data.SearchFilter
import com.ismael.daybyday.dayByDayApp
import java.time.LocalDate

/**
 * La recherche.
 *
 * Elle ne cherche plus seulement des mots. Les questions qu'on se pose
 * vraiment croisent le ressenti et ce qu'on a fait — « mes bonnes journees ou
 * je suis sorti », « les journees noires du mois dernier », « les jours avec
 * des photos ou j'ai bien mangé ». Toutes les donnees etaient deja la ; il ne
 * manquait qu'un moyen de les croiser.
 *
 * Le champ de texte reste en haut, parce que c'est encore le plus rapide quand
 * on se souvient d'un mot. Les criteres sont juste dessous, et rien ne
 * s'affiche tant qu'on n'a rien demande.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val repository = LocalContext.current.dayByDayApp.repository

    var text by rememberSaveable { mutableStateOf("") }
    var colors by remember { mutableStateOf(emptySet<DayColor>()) }
    var moved by rememberSaveable { mutableStateOf(false) }
    var wentOut by rememberSaveable { mutableStateOf(false) }
    var ateWell by rememberSaveable { mutableStateOf(false) }
    var withPhoto by rememberSaveable { mutableStateOf(false) }
    var withText by rememberSaveable { mutableStateOf(false) }
    var tagIds by remember { mutableStateOf(emptySet<Long>()) }
    var showTags by rememberSaveable { mutableStateOf(false) }

    val allDays by remember { repository.observeAllDays() }
        .collectAsStateWithLifecycle(emptyList())
    val allTags by remember { repository.observeTags() }
        .collectAsStateWithLifecycle(emptyList())
    val dayTags by remember { repository.observeAllDayTags() }
        .collectAsStateWithLifecycle(emptyList())
    val mediaCounts by remember { repository.observeAllMediaCounts() }
        .collectAsStateWithLifecycle(emptyMap())

    val filter = SearchFilter(
        text = text,
        colors = colors,
        moved = moved,
        wentOut = wentOut,
        ateWell = ateWell,
        withPhoto = withPhoto,
        withText = withText,
        tagIds = tagIds,
    )

    val tagsByDay = remember(dayTags) {
        dayTags.groupBy({ it.epochDay }, { it.tagId }).mapValues { it.value.toSet() }
    }
    val results = remember(allDays, filter, tagsByDay, mediaCounts) {
        DaySearch.matching(allDays, filter, tagsByDay, mediaCounts)
    }

    ScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))

            ScreenTitle(
                text = "Ma",
                accent = "recherche",
                trailing = {
                    RoundIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "Retour",
                        onClick = onBack,
                    )
                },
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Un mot, un prénom, un lieu…") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search-field"),
            )

            Spacer(Modifier.height(12.dp))

            // Les quatre couleurs, en premier : c'est le critere le plus
            // specifique a cette application, et celui qu'on vient croiser avec
            // tout le reste.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DayColor.entries.forEach { dayColor ->
                    ColorFilterTile(
                        dayColor = dayColor,
                        selected = dayColor in colors,
                        onClick = {
                            colors = if (dayColor in colors) colors - dayColor else colors + dayColor
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // `onClick` est nomme, et il faut qu'il le reste : le dernier
                // parametre de SoftChip est le dessin optionnel, donc des
                // accolades a la fin s'y rattacheraient au lieu du clic.
                SoftChip("🏃 Bougé", moved, onClick = { moved = !moved })
                SoftChip("🚪 Sorti", wentOut, onClick = { wentOut = !wentOut })
                SoftChip("🥗 Bien mangé", ateWell, onClick = { ateWell = !ateWell })
                SoftChip("📷 Avec photo", withPhoto, onClick = { withPhoto = !withPhoto })
                SoftChip("✍️ Écrit", withText, onClick = { withText = !withText })
                if (allTags.isNotEmpty()) {
                    SoftChip(
                        label = if (tagIds.isEmpty()) {
                            "Étiquettes"
                        } else {
                            "Étiquettes · ${tagIds.size}"
                        },
                        selected = showTags || tagIds.isNotEmpty(),
                        onClick = { showTags = !showTags },
                    )
                }
            }

            AnimatedVisibility(visible = showTags) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    // Plusieurs etiquettes se lisent « et » : chacune ajoutee
                    // reduit le resultat. C'est ce qu'on attend quand on cherche
                    // une journee precise.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        allTags.forEach { tag ->
                            SoftChip(
                                label = tag.display,
                                selected = tag.id in tagIds,
                                onClick = {
                                    tagIds = if (tag.id in tagIds) tagIds - tag.id else tagIds + tag.id
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        filter.isEmpty -> "Cherche un mot, ou choisis un ressenti."
                        results.isEmpty() -> "Aucune journée ne correspond."
                        results.size == 1 -> "1 journée"
                        else -> "${results.size} journées"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (!filter.isEmpty) {
                    TextButton(onClick = {
                        text = ""
                        colors = emptySet()
                        moved = false
                        wentOut = false
                        ateWell = false
                        withPhoto = false
                        withText = false
                        tagIds = emptySet()
                    }) {
                        Text("Tout effacer")
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn {
                items(results, key = { it.epochDay }) { entry ->
                    ResultRow(
                        entry = entry,
                        query = text.trim(),
                        photos = mediaCounts[entry.epochDay] ?: 0,
                        onClick = { onDayClick(LocalDate.ofEpochDay(entry.epochDay)) },
                    )
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

/** Une des quatre couleurs, en filtre : la pastille s'allume quand elle compte. */
@Composable
private fun ColorFilterTile(
    dayColor: DayColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(dayColor.gradient)
                } else {
                    Brush.linearGradient(
                        dayColor.gradient.map { it.copy(alpha = 0.22f) }
                    )
                }
            )
            .clickable(onClickLabel = dayColor.label, onClick = onClick)
            .testTag("filter-${dayColor.name}"),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelLarge,
                color = readableOn(dayColor.color),
            )
        }
    }
}

@Composable
private fun ResultRow(entry: DayEntry, query: String, photos: Int, onClick: () -> Unit) {
    val date = LocalDate.ofEpochDay(entry.epochDay)
    SoftCard(
        modifier = Modifier.padding(vertical = 4.dp),
        onClick = onClick,
        onClickLabel = "Ouvrir cette journée",
        padding = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(
                        entry.color?.color
                            ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Dates.dayMedium(date), style = MaterialTheme.typography.labelLarge)
                    if (photos > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "📷 $photos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (entry.title.isNotBlank()) {
                    Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                }
                val snippet = snippetAround(entry.note, query)
                if (snippet.isNotBlank()) {
                    Text(
                        snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Extrait le morceau de note autour du mot recherche. */
private fun snippetAround(note: String, query: String, radius: Int = 60): String {
    if (note.isBlank()) return ""
    if (query.isBlank()) return note.take(radius * 2).replace('\n', ' ')
    // On cherche sur le texte mis a plat, mais on decoupe l'original : c'est ce
    // qui permet de trouver « été » en tapant « ete » sans rendre l'extrait
    // illisible.
    val index = DaySearch.fold(note).indexOf(DaySearch.fold(query))
    if (index < 0) return note.take(radius * 2).replace('\n', ' ')
    val start = (index - radius).coerceAtLeast(0)
    val end = (index + query.length + radius).coerceAtMost(note.length)
    if (start >= end) return note.take(radius * 2).replace('\n', ' ')
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < note.length) "…" else ""
    return prefix + note.substring(start, end).replace('\n', ' ') + suffix
}
