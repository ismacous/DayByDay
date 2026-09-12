package com.ismael.daybyday.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ismael.daybyday.R
import com.ismael.daybyday.coach.Nudge
import com.ismael.daybyday.coach.NudgeTone
import com.ismael.daybyday.ui.theme.Brand
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Le moment ou quelqu'un te parle.
 *
 * Ce n'est pas une carte posee dans une page. Une carte reste la, personne ne
 * la ferme, et au bout de deux jours on ne la lit plus — c'est du decor. Ici
 * c'est une **apparition** : elle arrive par-dessus ce que tu regardes, elle
 * dit une chose, tu reponds, elle s'en va. Une fois par jour au maximum.
 *
 * Trois couches, et chacune fait une seule chose :
 *
 * 1. **Le voile**, a peine pose : la page doit rester visible dessous, sinon
 *    ce n'est plus quelqu'un qui passe la tete, c'est une interruption.
 * 2. **La bulle**, qui monte et depasse legerement sa taille avant de se
 *    poser. C'est ce depassement qui fait la difference entre apparaitre et
 *    surgir.
 * 3. **La presence** : un disque de lumiere qui respire, aux couleurs du ton
 *    du message. Dessine, pas importe — il doit vieillir avec l'application.
 *
 * Memes regles d'animation que la medaille : une seule horloge, lue uniquement
 * dans des `graphicsLayer` et des `draw*`, donc rien n'est recompose pendant
 * qu'elle joue.
 */
@Composable
fun CoachPopup(nudge: Nudge, onDismiss: () -> Unit) {
    val palette = tonePalette(nudge.tone)

    // L'horloge de la matiere (la respiration du disque) et deux ressorts :
    // l'arrivee de la bulle, et le voile.
    val breath = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }

    val confetti by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.celebration_confetti)
    )
    val confettiTime = remember { Animatable(0f) }

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(nudge) {
        // Le son et la secousse partent **avec** l'image : c'est ce qui fait
        // qu'on percoit un seul evenement et non trois.
        if (nudge.isCheer) CoachSound.playCheer(context) else CoachSound.play(context)
        haptics.performHapticFeedback(
            if (nudge.isCheer) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
        )
        launch { fade.animateTo(1f, tween(180)) }
        if (nudge.isCheer) {
            launch { confettiTime.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing)) }
        }
        launch {
            pop.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow),
            )
        }
        // La respiration tourne tant que la bulle est la. Elle ne s'arrete
        // jamais d'elle-meme : c'est la fermeture qui emporte tout.
        breath.animateTo(1f, tween(BREATH_MS, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Fermer",
                onClick = onDismiss,
            )
            .background(Color.Black.copy(alpha = 0.42f))
            .testTag("coach-popup"),
        contentAlignment = Alignment.Center,
    ) {
        if (nudge.isCheer) {
            LottieAnimation(
                composition = confetti,
                // Progression en lambda : lue au dessin, elle ne recompose rien.
                progress = { confettiTime.value },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .padding(horizontal = 26.dp)
                .graphicsLayer {
                    val grow = pop.value
                    scaleX = 0.88f + 0.12f * grow
                    scaleY = 0.88f + 0.12f * grow
                    translationY = (1f - grow) * 70f
                    alpha = grow.coerceIn(0f, 1f)
                }
                // Le clic sur la bulle ne doit pas la fermer : seule la croix,
                // le bouton et le voile le font. Sinon on la perd en voulant
                // lire une phrase longue.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 28.dp),
            ) {
                Presence(tone = nudge.tone, palette = palette, breath = breath, pop = pop)

                Spacer(Modifier.height(20.dp))

                Text(
                    text = nudge.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(22.dp))

                Answers(tone = nudge.tone, palette = palette, onDismiss = onDismiss)
            }
        }
    }
}

/**
 * Les reponses.
 *
 * Une proposition se repond, elle ne se ferme pas : deux boutons, et le
 * « pas aujourd'hui » est aussi legitime que l'autre — c'est ce qui evite que
 * la bulle ressemble a une consigne. Tout le reste n'a qu'un bouton : il n'y a
 * rien a decider, on a juste ete prevenu.
 *
 * Les deux boutons font la meme chose (fermer). C'est voulu : le coup de pouce
 * n'est pas un gestionnaire de taches, il n'a rien a enregistrer de ta reponse.
 * Ce qui change, c'est ce qu'on a le droit de repondre.
 */
@Composable
private fun Answers(tone: NudgeTone, palette: TonePalette, onDismiss: () -> Unit) {
    when (tone) {
        NudgeTone.NUDGE -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) {
                Text("Pas aujourd'hui", style = MaterialTheme.typography.labelLarge)
            }
            GradientAnswer(label = "Je vais essayer", colors = palette.gradient, onClick = onDismiss)
        }

        NudgeTone.CHEER -> GradientAnswer(
            label = "Merci !",
            colors = palette.gradient,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )

        else -> GradientAnswer(
            label = "Merci",
            colors = palette.gradient,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Un bouton plein, au degrade du ton : la seule tache vive de la bulle. */
@Composable
private fun GradientAnswer(
    label: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(colors), MaterialTheme.shapes.large)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = readableOnAll(colors),
            )
        }
    }
}

/**
 * La presence : un disque de lumiere qui respire, avec le signe du ton au
 * centre.
 *
 * Pourquoi pas simplement un emoji pose sur la carte : un emoji seul est un
 * decor, il ne dit pas qu'il y a quelqu'un. Un halo qui bouge lentement, si —
 * c'est le meme principe qu'un point qui clignote pendant qu'on ecrit.
 *
 * Tout est dessine dans un `drawBehind` et lu depuis une seule horloge : les
 * soixante images de la respiration ne recomposent rien.
 */
@Composable
private fun Presence(
    tone: NudgeTone,
    palette: TonePalette,
    breath: Animatable<Float, *>,
    pop: Animatable<Float, *>,
) {
    Box(
        modifier = Modifier
            .size(74.dp)
            .drawBehind {
                val phase = breath.value * 2f * Math.PI.toFloat()
                val centre = Offset(size.width / 2f, size.height / 2f)

                // Le halo, large et tres transparent : c'est lui qui donne
                // l'impression que quelque chose est allume derriere.
                val halo = haloRadius(size.minDimension)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.glow.copy(alpha = 0.42f), Color.Transparent),
                        center = centre,
                        radius = halo,
                    ),
                    radius = halo,
                    center = centre,
                )

                // Le disque, qui respire de quelques pour cent seulement. Au-dela
                // ca devient un battement de coeur, et ce n'est pas le sujet.
                val radius = presenceRadius(size.minDimension, breath.value, pop.value)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = palette.gradient,
                        start = Offset(centre.x - radius, centre.y - radius),
                        end = Offset(centre.x + radius, centre.y + radius),
                    ),
                    radius = radius,
                    center = centre,
                )

                // Un reflet decentre qui tourne tres lentement : la meme
                // lumiere en haut a gauche que partout ailleurs dans l'app.
                val driftX = cos(phase) * radius * 0.16f
                val driftY = sin(phase) * radius * 0.16f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                        center = Offset(centre.x - radius * 0.34f + driftX, centre.y - radius * 0.36f + driftY),
                        radius = radius * 0.72f,
                    ),
                    radius = radius * 0.72f,
                    center = Offset(centre.x - radius * 0.34f + driftX, centre.y - radius * 0.36f + driftY),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tone.emoji,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.graphicsLayer { alpha = pop.value },
        )
    }
}

/** Les couleurs d'un ton. */
internal data class TonePalette(val gradient: List<Color>, val glow: Color)

/**
 * Six tons, trois familles de couleur seulement.
 *
 * Le soutien prend la menthe — la couleur de ce qui va bien dans
 * l'application, pas un rouge d'alerte : un message de soutien qui arrive en
 * rouge fait peur avant d'etre lu. Les propositions prennent l'indigo de
 * l'application, la fete le rose.
 */
@Composable
private fun tonePalette(tone: NudgeTone): TonePalette = when (tone) {
    NudgeTone.CARE -> TonePalette(Brand.accentGradient, Brand.Accent)
    NudgeTone.WARM -> TonePalette(Brand.accentGradient, Brand.Accent)
    NudgeTone.PROUD -> TonePalette(Brand.gradient, Brand.Primary)
    NudgeTone.NUDGE -> TonePalette(Brand.gradient, Brand.Primary)
    NudgeTone.SOFT -> TonePalette(Brand.softGradient, Brand.PrimaryEnd)
    NudgeTone.CHEER -> TonePalette(listOf(Brand.Playful, Brand.Primary), Brand.Playful)
}

/** Un tour de respiration complet. Lent : on doit le sentir, pas le voir. */
private const val BREATH_MS = 5200

private const val CONFETTI_MS = 2400
