package com.ismael.daybyday.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.VoiceNote
import com.ismael.daybyday.data.Waveform

/**
 * Un vocal, dans le fil de la page.
 *
 * Il n'est **pas** posé librement comme une photo : un enregistrement n'est pas
 * une image qu'on colle de travers dans un coin, c'est un morceau de la
 * journée. Il se range donc sous le texte, dans l'ordre où il a été dit, et
 * garde la largeur de la page.
 *
 * Ses couleurs viennent du **papier**, pas du thème : le rond de lecture est
 * une teinte sombre de la page, opaque, et la barre une teinte à peine
 * marquée. Une pastille violette du thème sur un papier ivoire ressemble à un
 * bouton posé par une autre application.
 */
@Composable
fun VoiceNoteRow(
    note: VoiceNote,
    playing: Boolean,
    /** Où en est la lecture, entre 0 et 1. Ignoré quand le vocal ne joue pas. */
    progress: () -> Float,
    paper: Color,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    val ink = JournalPaper.ink(paper)
    val surface = JournalPaper.shade(paper, 0.07f)
    val button = JournalPaper.shade(paper, 0.78f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(button)
                .clickable(onClickLabel = if (playing) "Arrêter" else "Écouter", onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            if (playing) {
                // Le jeu d'icônes de base n'a pas de « arrêter » : un carré le
                // dit aussi bien, et c'est le signe que tout le monde connaît.
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(readableOn(button)),
                )
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = readableOn(button),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        VoiceWave(
            waveform = note.waveform,
            played = { if (playing) progress() else 0f },
            ink = ink,
            accent = button,
            modifier = Modifier
                .weight(1f)
                .height(26.dp),
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = formatDuration(note.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = ink.copy(alpha = 0.75f),
        )

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = "Supprimer ce vocal", onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = null,
                tint = ink.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp),
            )
        }
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
 * Le rouge de l'enregistrement, fixe.
 *
 * Ce n'est pas une couleur du thème : « ça enregistre » se dit en rouge
 * partout, et le prendre dans la palette de la page voudrait dire qu'il change
 * avec le papier — donc qu'il ne veut plus rien dire.
 */
private val RECORD_RED = Color(0xFFE1483F)
