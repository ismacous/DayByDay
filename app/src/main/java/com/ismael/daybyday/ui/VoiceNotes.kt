package com.ismael.daybyday.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.R
import com.ismael.daybyday.data.VoiceNote
import com.ismael.daybyday.data.Waveform

/**
 * Un vocal, dans le fil de la page.
 *
 * Il est **pose entre deux paragraphes**, pas range en bas ni colle de travers
 * comme une photo : un enregistrement appartient a un moment du texte. On le
 * deplace en maintenant le doigt dessus puis en tirant, comme un bloc — c'est
 * le meme geste partout dans la page.
 *
 * Ses couleurs viennent du **papier** : le rond de lecture est une teinte
 * sombre de la page, opaque, et la barre une teinte a peine marquee. Une
 * pastille violette du theme sur un papier ivoire ressemble a un bouton pose
 * par une autre application.
 *
 * Il n'y a **pas de menu** : tout ce qu'on peut faire est sur la barre
 * elle-meme — ecouter, changer sa largeur, l'effacer — et le reste est un
 * geste. Un menu pour trois boutons est un menu de trop.
 */
@Composable
fun VoiceNoteRow(
    note: VoiceNote,
    playing: Boolean,
    /** Ou en est la lecture, entre 0 et 1. Ignore quand le vocal ne joue pas. */
    progress: () -> Float,
    paper: Color,
    /** La hauteur d'une ligne du lignage : la barre en occupe deux, pile. */
    lineHeight: Dp,
    /**
     * Le vocal est **choisi** : ses reglages apparaissent.
     *
     * Au repos la barre ne montre que ce qu'on regarde — le bouton, la
     * silhouette, la duree. Changer la largeur ou effacer sont des gestes
     * rares : les laisser en permanence encombrait une barre qu'on ouvre
     * surtout pour ecouter.
     */
    selected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onToggleWidth: () -> Unit,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val ink = JournalPaper.ink(paper)
    val surface = JournalPaper.shade(paper, 0.07f)
    val button = JournalPaper.shade(paper, 0.78f)

    VoiceShell(
        paper = paper,
        lineHeight = lineHeight,
        wide = note.wide,
        // Choisi, le fond se marque un peu : on voit lequel des trois vocaux
        // repond aux boutons qui viennent d'apparaitre.
        surface = JournalPaper.shade(paper, if (selected) 0.13f else 0.07f),
        modifier = modifier.then(dragModifier),
    ) {
        Box(
            modifier = Modifier
                .size(PLAY_SIZE)
                .clip(CircleShape)
                .background(button)
                .clickable(onClickLabel = if (playing) "Arrêter" else "Écouter", onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            if (playing) {
                // Le jeu d'icones de base n'a pas de « arreter » : un carre le
                // dit aussi bien, et c'est le signe que tout le monde connait.
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
                .fillMaxHeight()
                .clickable(
                    onClickLabel = if (selected) "Refermer" else "Régler ce vocal",
                    onClick = onSelect,
                )
                .padding(vertical = 12.dp),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = formatDuration(note.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = ink.copy(alpha = 0.75f),
        )

        if (selected) {
            // La poignee de largeur. Deux tailles et pas une largeur libre :
            // une barre de lecture n'a pas de proportions a respecter comme une
            // photo, et la tirer au doigt donnerait surtout des largeurs
            // bancales.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClickLabel = if (note.wide) "Rétrécir le vocal" else "Élargir le vocal",
                        onClick = onToggleWidth,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_width),
                    contentDescription = null,
                    tint = ink.copy(alpha = 0.55f),
                    modifier = Modifier.size(15.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
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
        } else {
            Spacer(Modifier.width(4.dp))
        }
    }
}

/**
 * L'enregistrement en cours, **a la place ou le vocal se posera**.
 *
 * C'etait la demande, et elle vaut mieux que ce qu'il y avait : un petit
 * temoin rouge en haut de l'ecran laissait deviner qu'il se passait quelque
 * chose, puis le vocal apparaissait ailleurs, en haut du texte, et il fallait
 * le descendre a la main. Ici la barre est **deja** la, au bon endroit, a la
 * bonne taille ; le bouton rouge occupe la place du bouton d'ecoute, et on
 * appuie au meme endroit pour arreter. Rien ne bouge quand l'enregistrement
 * s'arrete : la barre rouge devient la barre grise, sur place.
 */
@Composable
fun RecordingRow(
    elapsedMs: Long,
    /** La silhouette du son mesuree jusqu'ici : elle pousse pendant qu'on parle. */
    waveform: String,
    paper: Color,
    lineHeight: Dp,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = JournalPaper.ink(paper)
    val transition = rememberInfiniteTransition(label = "enregistrement")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "battement",
    )

    VoiceShell(
        paper = paper,
        lineHeight = lineHeight,
        wide = true,
        surface = RECORD_RED.copy(alpha = 0.10f),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(PLAY_SIZE)
                .clip(CircleShape)
                .background(RECORD_RED)
                .clickable(onClickLabel = "Arrêter l'enregistrement", onClick = onStop),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    // Le battement se lit dans `graphicsLayer` : lu pendant la
                    // composition, il ferait recomposer la page a chaque image.
                    .graphicsLayer { alpha = pulse }
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White),
            )
        }

        Spacer(Modifier.width(10.dp))

        VoiceWave(
            waveform = waveform,
            played = { 0f },
            ink = RECORD_RED,
            accent = RECORD_RED,
            modifier = Modifier
                .weight(1f)
                .height(26.dp),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = formatDuration(elapsedMs),
            style = MaterialTheme.typography.labelMedium,
            color = RECORD_RED,
        )

        Spacer(Modifier.width(10.dp))
        Text(
            text = "Appuie pour arrêter",
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = 0.5f),
        )
        Spacer(Modifier.width(6.dp))
    }
}

/**
 * Le cadre commun a la barre d'ecoute et a celle d'enregistrement.
 *
 * Les deux **doivent** avoir exactement la meme forme : c'est ce qui fait que
 * la fin d'un enregistrement ne deplace rien a l'ecran. Le partager evite
 * qu'elles divergent a la premiere retouche.
 *
 * La hauteur vaut deux lignes du lignage, pile : la page garde son rythme, et
 * le texte qui suit retombe sur ses lignes.
 */
@Composable
private fun VoiceShell(
    paper: Color,
    lineHeight: Dp,
    wide: Boolean,
    modifier: Modifier = Modifier,
    surface: Color = JournalPaper.shade(paper, 0.07f),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(lineHeight * VOICE_LINES),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(if (wide) 1f else NARROW_FRACTION)
                .fillMaxHeight()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(surface)
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * La silhouette du son.
 *
 * Elle est **dessinee**, pas composee en cinquante-six petites boites : une
 * barre par vue couterait cinquante-six mesures a chaque image de la lecture,
 * pour des rectangles de deux points de large. Et l'avancee de la lecture est
 * lue **dans le dessin** — la regle habituelle : une valeur qui change a
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
                // La partie deja lue prend la couleur d'accent : on voit ou on
                // en est sans chiffre qui defile.
                color = if (index < edge) accent else resting,
                topLeft = Offset(index * step + (step - barWidth) / 2f, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = radius,
            )
        }
    }
}

/** Deux lignes du lignage : la barre garde le rythme de la page. */
const val VOICE_LINES = 2

private val PLAY_SIZE = 38.dp

/** La largeur d'un vocal retreci, en part de la page. */
private const val NARROW_FRACTION = 0.62f

/**
 * Le rouge de l'enregistrement, fixe.
 *
 * Ce n'est pas une couleur du theme : « ca enregistre » se dit en rouge
 * partout, et le prendre dans la palette de la page voudrait dire qu'il change
 * avec le papier — donc qu'il ne veut plus rien dire.
 */
private val RECORD_RED = Color(0xFFE1483F)
