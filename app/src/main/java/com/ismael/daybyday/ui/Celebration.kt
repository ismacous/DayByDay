package com.ismael.daybyday.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ismael.daybyday.R
import com.ismael.daybyday.data.Badge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Le moment ou quelque chose est fait.
 *
 * C'est, avec le bonjour du demarrage, le seul endroit de l'application ou il
 * se passe quelque chose pour le plaisir. Une application qu'on ouvre chaque
 * soir a besoin d'au moins un moment qui recompense l'ouverture.
 *
 * Trois couches, et chacune fait une seule chose :
 *
 * 1. **Le fond** : les confettis, un fichier Lottie embarque dans l'APK.
 * 2. **La medaille** : dessinee a la main, pas importee. Elle doit vieillir
 *    avec l'application — reprendre ses couleurs, ses rayons, sa lumiere — ce
 *    qu'un badge tout fait telecharge quelque part ne fait jamais.
 * 3. **Le texte** : ce qui a ete fait, en deux lignes.
 *
 * Quatre regles, valables pour toute animation de ce genre :
 *
 * - **Elle se declenche au passage**, jamais a l'affichage. Rouvrir la journee
 *   demain ne doit pas la rejouer.
 * - **Elle dure moins de deux secondes et demie**, entree et sortie comprises.
 * - **Un appui n'importe ou la coupe.**
 * - **Tout est lu dans le dessin.** Une seule horloge mene la medaille, son
 *   reflet et son onde ; elle n'est lue que dans des `graphicsLayer` et des
 *   `draw*`. Rien n'est recompose pendant qu'elle joue.
 */
@Composable
fun Celebration(badge: Badge, onDone: () -> Unit) {
    val palette = badgePalette(badge)

    // Une seule horloge pour tout l'ecran, plus deux ressorts : l'arrivee et
    // la sortie. Les ressorts font le geste, l'horloge fait la matiere.
    val time = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }

    val confetti by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.celebration_confetti)
    )
    val confettiTime = remember { Animatable(0f) }

    LaunchedEffect(badge) {
        launch { fade.animateTo(1f, tween(160)) }
        launch { confettiTime.animateTo(1f, tween(CONFETTI_MS, easing = LinearEasing)) }
        launch {
            // Peu amorti : la medaille depasse sa taille et revient. C'est ce
            // depassement qui fait la difference entre apparaitre et surgir.
            pop.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.44f, stiffness = Spring.StiffnessLow),
            )
        }
        time.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
        launch { pop.animateTo(0.86f, tween(240)) }
        fade.animateTo(0f, tween(260))
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Fermer",
                onClick = onDone,
            )
            // Un voile a peine pose : la page doit rester visible dessous, sinon
            // ce n'est plus une recompense, c'est une interruption.
            .background(Color.Black.copy(alpha = 0.34f)),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = confetti,
            // La progression est passee en **lambda** : elle est lue au dessin
            // et non pendant la composition, donc les soixante-quinze images ne
            // provoquent aucune recomposition.
            progress = { confettiTime.value },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    val grow = pop.value
                    scaleX = grow
                    scaleY = grow
                    // Elle arrive de travers et se redresse : une medaille
                    // posee bien droite d'emblee n'a pas ete gagnee.
                    rotationZ = (1f - grow.coerceIn(0f, 1f)) * -22f
                    alpha = grow.coerceIn(0f, 1f)
                },
        ) {
            Medal(palette = palette, emoji = badge.emoji, time = time)
            Spacer(Modifier.height(22.dp))
            Text(
                text = badge.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = badge.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.86f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * La medaille.
 *
 * Elle est dessinee, et c'est le point : un badge de jeu video pris tel quel
 * ressemble a un badge de jeu video. Celle-ci est faite des couleurs de
 * l'application, et sa lumiere suit la meme regle que partout ailleurs — une
 * source en haut a gauche, une ombre teintee, jamais de gris.
 *
 * Quatre couches donnent la profondeur, de l'exterieur vers l'interieur :
 * l'ombre portee, la couronne en degrade balaye qui **tourne**, le disque
 * central avec sa lumiere decentree, et le reflet qui traverse.
 */
@Composable
private fun Medal(
    palette: List<Color>,
    emoji: String,
    time: Animatable<Float, *>,
) {
    val ring = remember(palette) {
        // Un degrade balaye, avec la premiere couleur repetee a la fin : sans
        // ca, la couronne aurait une couture visible a midi.
        Brush.sweepGradient(
            palette + palette.reversed().drop(1) + palette.first()
        )
    }

    Box(
        modifier = Modifier.size(148.dp),
        contentAlignment = Alignment.Center,
    ) {
        // La couronne, qui tourne lentement sur elle-meme.
        Box(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer { rotationZ = time.value * 90f }
                .drawBehind {
                    drawCircle(brush = ring)
                },
        )
        // Le disque central : un degrade radial dont le centre est decale en
        // haut a gauche, la ou est la lumiere de toute l'application.
        Box(
            modifier = Modifier
                .size(114.dp)
                .drawBehind {
                    val light = Offset(size.width * 0.32f, size.height * 0.26f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.first().copy(alpha = 0.95f),
                                palette.last(),
                            ),
                            center = light,
                            radius = size.minDimension * 0.85f,
                        )
                    )
                    // Le liere clair du bord haut : c'est lui qui donne
                    // l'epaisseur, comme sur une vraie piece.
                    drawCircle(
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.Transparent),
                        ),
                        radius = size.minDimension / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                    )
                }
                .drawWithContent {
                    drawContent()
                    // Le reflet qui traverse une fois, en biais.
                    val pass = (time.value * 2.2f - 0.25f).coerceIn(0f, 1f)
                    if (pass > 0f && pass < 1f) {
                        val x = size.width * (pass * 2.4f - 0.7f)
                        drawCircle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.30f),
                                    Color.Transparent,
                                ),
                                startX = x - size.width * 0.28f,
                                endX = x + size.width * 0.28f,
                            ),
                            radius = size.minDimension / 2f,
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = 48.sp,
                modifier = Modifier.graphicsLayer {
                    // Un souffle lent : la medaille respire au lieu de se figer.
                    val breathe = 1f + 0.035f * sin(time.value * 3f * PI.toFloat())
                    scaleX = breathe
                    scaleY = breathe
                },
            )
        }
    }
}

/**
 * Les couleurs d'un badge.
 *
 * Elles reprennent les teintes des cartes : le badge des prieres est de
 * l'ambre de la carte des prieres, celui des pas du vert de l'activite. On
 * reconnait d'ou vient la recompense avant d'avoir lu son titre.
 */
private fun badgePalette(badge: Badge): List<Color> = when (badge) {
    Badge.PRAYERS -> listOf(Color(0xFFFFD166), Color(0xFFF59E0B))
    Badge.STEPS, Badge.WORKOUT -> listOf(Color(0xFF5BE3B4), Color(0xFF10B981))
    Badge.OUTSIDE -> listOf(Color(0xFFC4A0FF), Color(0xFF8B5CF6))
    Badge.WATER -> listOf(Color(0xFF8FD3FF), Color(0xFF2E9BF0))
    Badge.APPLICATION, Badge.WEEK_APPLICATIONS ->
        listOf(Color(0xFF9B90FF), Color(0xFF5B4DF0))
    Badge.JOURNAL -> listOf(Color(0xFFFFA9B8), Color(0xFFF2637F))
}

/** Duree du fichier de confettis : soixante-quinze images a trente par seconde. */
private const val CONFETTI_MS = 2500

/** Le temps que la medaille reste a l'ecran une fois arrivee. */
private const val HOLD_MS = 1900
