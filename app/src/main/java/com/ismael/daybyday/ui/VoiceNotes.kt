package com.ismael.daybyday.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.VoiceNote

/**
 * Les vocaux d'une page, en pastilles sous le titre.
 *
 * Pourquoi la, et pas dans le menu des outils : un enregistrement qu'on ne voit
 * qu'en ouvrant un panneau n'existe pas. Une page qui contient trois vocaux
 * doit le dire des qu'on l'ouvre — c'est la meme raison qui fait qu'une carte
 * repliee garde sa ligne de resume.
 *
 * La bande n'apparait pas quand il n'y a rien : une rangee vide en travers de
 * chaque page couterait de la place a toutes les journees pour renseigner sur
 * aucune.
 */
@Composable
fun VoiceNoteStrip(
    notes: List<VoiceNote>,
    /** Le chemin du vocal en train d'etre joue, s'il y en a un. */
    playing: String?,
    /** La duree ecoulee si l'on est en train d'enregistrer, null sinon. */
    recordingMs: Long?,
    ink: Color,
    onToggle: (VoiceNote) -> Unit,
    onDelete: (VoiceNote) -> Unit,
    onStopRecording: () -> Unit,
) {
    if (notes.isEmpty() && recordingMs == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (recordingMs != null) {
            RecordingPill(elapsedMs = recordingMs, onStop = onStopRecording)
        }
        notes.forEach { note ->
            VoicePill(
                note = note,
                playing = playing == note.relativePath,
                ink = ink,
                onToggle = { onToggle(note) },
                onDelete = { onDelete(note) },
            )
        }
    }
}

/**
 * Un vocal : on l'ecoute en appuyant dessus, on l'efface par sa croix.
 *
 * La croix est **toujours visible**, et c'est voulu : un vocal ne se relit pas
 * en diagonale comme une phrase, donc en effacer un par erreur coute cher. Une
 * croix qu'on voit se vise ; un appui long qu'on ne voit pas se declenche
 * quand on ne l'attendait pas.
 */
@Composable
private fun VoicePill(
    note: VoiceNote,
    playing: Boolean,
    ink: Color,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(ink.copy(alpha = if (playing) 0.16f else 0.08f))
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ink.copy(alpha = 0.12f))
                .clickable(onClickLabel = if (playing) "Arrêter" else "Écouter", onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (playing) {
                // Pas d'icone « arreter » dans le jeu d'icones de base : un
                // carre en dit autant, et c'est le signe universel.
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ink),
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = formatDuration(note.durationMs),
            style = MaterialTheme.typography.labelLarge,
            color = ink,
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = "Supprimer ce vocal", onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = null,
                tint = ink.copy(alpha = 0.55f),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

/**
 * L'enregistrement en cours.
 *
 * Un point rouge qui bat, et le temps qui defile : sans les deux, on ne sait
 * pas si le micro est vraiment ouvert. Le battement est lu dans un
 * `graphicsLayer` et non pendant la composition, comme partout ailleurs.
 */
@Composable
private fun RecordingPill(elapsedMs: Long, onStop: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "enregistrement")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "battement",
    )

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(RECORD_RED.copy(alpha = 0.14f))
            .clickable(onClickLabel = "Arrêter l'enregistrement", onClick = onStop)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer { alpha = pulse }
                .clip(CircleShape)
                .background(RECORD_RED),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatDuration(elapsedMs),
            style = MaterialTheme.typography.labelLarge,
            color = RECORD_RED,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(RECORD_RED),
        )
    }
}

/**
 * Le rouge de l'enregistrement, fixe.
 *
 * Ce n'est pas une couleur du theme : « ca enregistre » se dit en rouge
 * partout, et le prendre dans la palette de la page voudrait dire qu'il change
 * avec le papier — donc qu'il ne veut plus rien dire.
 */
private val RECORD_RED = Color(0xFFE1483F)
