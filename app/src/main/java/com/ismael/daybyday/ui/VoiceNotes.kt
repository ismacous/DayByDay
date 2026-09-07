package com.ismael.daybyday.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.Placement
import com.ismael.daybyday.data.VoiceNote
import com.ismael.daybyday.data.Waveform

/**
 * Les vocaux sur la page du journal.
 *
 * Ils sont **posés dans la page**, comme les photos, et pas rangés en bande au
 * dessus du texte. La raison est la même que pour les photos : sur un carnet,
 * on colle une chose à un endroit parce que c'est là qu'elle se rattache à ce
 * qu'on écrit. Une bande en haut de page range les vocaux ; les poser dans le
 * texte les raccroche à un moment.
 */

/**
 * Un vocal posé : la barre de lecture.
 *
 * Trois zones, une par geste, et c'est ce qui permet à un seul objet de tout
 * faire sans menu : le rond joue, le reste de la barre choisit le vocal (et
 * ouvre ses réglages), et un appui maintenu le déplace.
 */
@Composable
fun PlacedVoiceNote(
    note: VoiceNote,
    pageWidth: Float,
    playing: Boolean,
    /** Où en est la lecture, entre 0 et 1. Ignoré quand le vocal ne joue pas. */
    progress: () -> Float,
    ink: Color,
    accent: Color,
    selected: Boolean,
    snapToGrid: Boolean,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
    onMove: (VoiceNote) -> Unit,
) {
    if (!note.isPlaced) return
    val density = LocalDensity.current
    val width = Placement.voiceWidth(note.wide, pageWidth)

    // Relus a chaque evenement, jamais figes dans le detecteur : un
    // `pointerInput` n'installe son detecteur qu'une fois, et chaque evenement
    // n'apporte que le deplacement depuis le precedent. Fige, le vocal
    // repartirait de sa place d'origine a chaque image.
    val live = rememberUpdatedState(note)
    val liveSnap = rememberUpdatedState(snapToGrid)
    val livePageWidth = rememberUpdatedState(pageWidth)

    var centreX by remember(note.id) { mutableStateOf((note.placedX ?: 0f) + width / 2f) }
    var centreY by remember(note.id) {
        mutableStateOf((note.placedY ?: 0f) + Placement.VOICE_HEIGHT / 2f)
    }

    Row(
        modifier = Modifier
            .offset(x = (note.placedX ?: 0f).dp, y = (note.placedY ?: 0f).dp)
            .width(width.dp)
            .height(Placement.VOICE_HEIGHT.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ink.copy(alpha = 0.07f))
            .then(
                if (selected) {
                    Modifier.border(2.dp, accent, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClickLabel = "Réglages du vocal", onClick = onSelect)
            .pointerInput(note.id) {
                // Maintenir puis tirer : un simple glissement ferait defiler la
                // page, et c'est ce qu'on veut quand on lit.
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        centreX = (live.value.placedX ?: 0f) +
                            Placement.voiceWidth(live.value.wide, livePageWidth.value) / 2f
                        centreY = (live.value.placedY ?: 0f) + Placement.VOICE_HEIGHT / 2f
                    },
                ) { change, drag ->
                    change.consume()
                    centreX += with(density) { drag.x.toDp().value }
                    centreY += with(density) { drag.y.toDp().value }
                    onMove(
                        Placement.applyVoice(
                            note = live.value,
                            centreX = centreX,
                            centreY = centreY,
                            snapToGrid = liveSnap.value,
                            pageWidth = livePageWidth.value,
                        )
                    )
                }
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable(onClickLabel = if (playing) "Arrêter" else "Écouter", onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            if (playing) {
                // Le jeu d'icones de base n'a pas de « arreter » : un carre le
                // dit aussi bien, et c'est le signe que tout le monde connait.
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(readableOn(accent)),
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = readableOn(accent),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        VoiceWave(
            waveform = note.waveform,
            played = { if (playing) progress() else 0f },
            ink = ink,
            accent = accent,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 14.dp),
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = formatDuration(note.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = ink.copy(alpha = 0.75f),
        )
    }
}

/**
 * La silhouette du son.
 *
 * Elle est **dessinée**, pas composée en cinquante-six petites boîtes : une
 * barre par vue coûterait cinquante-six mesures à chaque image de la lecture,
 * pour des rectangles de deux points de large. Et l'avancée de la lecture est
 * lue **dans le dessin** — la règle habituelle : une valeur qui change à
 * chaque image ne se lit pas pendant la composition.
 */
@Composable
private fun VoiceWave(
    waveform: String,
    played: () -> Float,
    ink: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val heights = remember(waveform) { Waveform.decode(waveform) }
    val resting = ink.copy(alpha = 0.28f)

    Canvas(modifier = modifier) {
        if (heights.isEmpty() || size.width <= 0f) return@Canvas
        val step = size.width / heights.size
        val barWidth = (step * 0.55f).coerceAtLeast(1f)
        val radius = CornerRadius(barWidth / 2f)
        val edge = played() * heights.size

        heights.forEachIndexed { index, value ->
            val height = (size.height * value).coerceAtLeast(barWidth)
            drawRoundRect(
                // La partie déjà lue prend la couleur d'accent : on voit où on
                // en est sans chiffre qui défile.
                color = if (index < edge) accent else resting,
                topLeft = Offset(index * step + (step - barWidth) / 2f, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = radius,
            )
        }
    }
}

/**
 * L'enregistrement en cours, en bandeau au-dessus de la page.
 *
 * Il ne se pose pas dans la page : tant qu'on parle, le vocal n'existe pas
 * encore, il n'a ni durée ni silhouette. Le bandeau disparaît dès qu'on
 * s'arrête, et la barre apparaît alors dans la page.
 */
@Composable
fun RecordingBanner(elapsedMs: Long, onStop: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "enregistrement")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "battement",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(RECORD_RED.copy(alpha = 0.14f))
                .clickable(onClickLabel = "Arrêter l'enregistrement", onClick = onStop)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer { alpha = pulse }
                    .clip(CircleShape)
                    .background(RECORD_RED),
            )
            Text(
                text = formatDuration(elapsedMs),
                style = MaterialTheme.typography.labelLarge,
                color = RECORD_RED,
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RECORD_RED),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Appuie pour arrêter",
            style = MaterialTheme.typography.labelMedium,
            color = RECORD_RED.copy(alpha = 0.75f),
        )
    }
}

/**
 * Les réglages du vocal choisi, à la place de la barre de mise en forme : on ne
 * fait qu'une chose à la fois, donc une seule barre à la fois. Même règle que
 * pour les photos.
 */
@Composable
fun VoiceToolsBar(
    note: VoiceNote,
    onWide: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoftChip(
            label = "Barre entière",
            selected = note.wide,
            onClick = { onWide(true) },
        )
        SoftChip(
            label = "Rétrécie",
            selected = !note.wide,
            onClick = { onWide(false) },
        )
        Spacer(Modifier.weight(1f))
        SoftChip(
            label = "Supprimer",
            selected = false,
            accent = RECORD_RED,
            onClick = onDelete,
        )
        SoftChip(label = "Terminé", selected = true, onClick = onDone)
    }
}

/**
 * Le rouge de l'enregistrement, fixe.
 *
 * Ce n'est pas une couleur du thème : « ça enregistre » se dit en rouge
 * partout, et le prendre dans la palette de la page voudrait dire qu'il change
 * avec le papier — donc qu'il ne veut plus rien dire.
 */
private val RECORD_RED = Color(0xFFE1483F)
