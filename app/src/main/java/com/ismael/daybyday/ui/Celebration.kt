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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ismael.daybyday.R
import com.ismael.daybyday.data.Badge
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
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
    // Le tour sur elle-meme, une seule fois, pendant l'arrivee.
    val spin = remember { Animatable(0f) }

    val confetti by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.celebration_confetti)
    )
    val confettiTime = remember { Animatable(0f) }

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(badge) {
        // Le son et la secousse partent **avec** l'image, pas apres : c'est ce
        // qui fait qu'on percoit un seul evenement et non trois.
        BadgeSound.play(context)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
        launch { spin.animateTo(1f, tween(720, easing = FastOutSlowInEasing)) }
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
                    // Pas d'inclinaison ici : la piece fait deja un tour sur
                    // elle-meme. Deux rotations en meme temps ne se lisent
                    // plus comme un geste, mais comme un desordre.
                    alpha = grow.coerceIn(0f, 1f)
                },
        ) {
            Medal(badge = badge, palette = palette, time = time, spin = spin)
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
 * Elle est **dessinee**, pas importee, et c'est tout l'enjeu : un badge de jeu
 * pris tel quel ressemble a un badge de jeu, pas a cette application. Celle-ci
 * est faite des couleurs des cartes, et sa lumiere suit la meme regle que
 * partout ailleurs — une source en haut a gauche, jamais de gris.
 *
 * Six couches, de l'arriere vers l'avant, et chacune fait une seule chose :
 *
 * 1. **Le ruban**, deux pans avec leur encoche, poses derriere l'hexagone.
 * 2. **La couronne facettee.** C'est elle qui donne le relief : chacune des six
 *    faces est eclairee selon son orientation par rapport a la lumiere, comme
 *    une vraie piece biseautee. Un simple contour de couleur serait plat.
 * 3. **La plaque interieure**, plus sombre, avec sa lumiere decentree.
 * 4. **Le vernis** : la moitie haute de la plaque, un peu plus claire.
 * 5. **L'emoji** au centre, a la place de l'etoile.
 * 6. **Le reflet** qui traverse une fois.
 *
 * Et un tour sur elle-meme a l'arrivee. Ce n'est pas decoratif : une piece qui
 * tourne montre qu'elle a une face et une epaisseur, ce qu'une image posee a
 * plat ne montre jamais.
 */
@Composable
private fun Medal(
    badge: Badge,
    palette: List<Color>,
    time: Animatable<Float, *>,
    spin: Animatable<Float, *>,
) {
    val bright = palette.first()
    val deep = palette.last()
    val density = LocalDensity.current

    Box(
        modifier = Modifier.size(width = MEDAL_WIDTH, height = MEDAL_HEIGHT),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Le tour sur elle-meme. `cameraDistance` eloigne l'oeil :
                    // sans ca, la perspective est si forte a mi-tour que la
                    // medaille se deforme au lieu de tourner.
                    rotationY = spin.value * 360f
                    cameraDistance = 14f * density.density
                }
                .drawBehind {
                    val side = size.width
                    val centerX = size.width / 2f
                    val centerY = side / 2f
                    val radius = side / 2f

                    drawRibbon(centerX = centerX, top = centerY, radius = radius, deep = deep)

                    val outer = hexagon(centerX, centerY, radius)
                    val inner = hexagon(centerX, centerY, radius * 0.74f)

                    drawFacets(outer = outer, inner = inner, bright = bright, deep = deep)

                    // La plaque : plus sombre que la couronne, sinon le
                    // contenu se noie dedans.
                    val plate = Path().apply {
                        moveTo(inner[0].x, inner[0].y)
                        inner.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(
                        path = plate,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                deep.darken(0.18f),
                                deep.darken(0.42f),
                            ),
                            center = Offset(centerX - radius * 0.22f, centerY - radius * 0.28f),
                            radius = radius * 1.5f,
                        ),
                    )
                    // Le vernis : la moitie haute de la plaque, a peine plus
                    // claire. C'est ce qui fait la difference entre une surface
                    // peinte et une surface vitrifiee.
                    clipPath(plate) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                            ),
                            topLeft = Offset(0f, centerY - radius),
                            size = androidx.compose.ui.geometry.Size(size.width, radius),
                        )
                        // Le reflet qui traverse, une seule fois.
                        val pass = (time.value * 2.4f - 0.2f).coerceIn(0f, 1f)
                        if (pass > 0f && pass < 1f) {
                            val x = size.width * (pass * 2.2f - 0.6f)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.34f),
                                        Color.Transparent,
                                    ),
                                    startX = x - side * 0.26f,
                                    endX = x + side * 0.26f,
                                ),
                                topLeft = Offset(0f, centerY - radius),
                                size = androidx.compose.ui.geometry.Size(size.width, side),
                            )
                        }
                    }
                },
        )
        Box(
            modifier = Modifier
                .size(MEDAL_WIDTH)
                .graphicsLayer {
                    // L'emoji ne tourne pas avec la piece : il resterait a
                    // l'envers la moitie du tour. Il grandit a l'arrivee, puis
                    // respire.
                    val arrival = spin.value.coerceIn(0f, 1f)
                    val breathe = 1f + 0.04f * sin(time.value * 3f * PI.toFloat())
                    scaleX = arrival * breathe
                    scaleY = arrival * breathe
                },
            contentAlignment = Alignment.Center,
        ) {
            // Le signe du badge est anime, comme les visages de l'humeur :
            // il boucle pendant que la medaille est a l'ecran. C'est le seul
            // endroit ou une boucle a du sens — la medaille ne dure que deux
            // secondes, et c'est une fete.
            val face by rememberLottieComposition(
                LottieCompositionSpec.RawRes(badgeAnimation(badge))
            )
            LottieAnimation(
                composition = face,
                progress = { (time.value * BADGE_LOOPS) % 1f },
                modifier = Modifier.size(58.dp),
            )
        }
    }
}

/**
 * Les six sommets d'un hexagone pointe en haut et en bas.
 *
 * Pointe en haut et en bas plutot que a plat : c'est la forme d'un ecusson, et
 * elle tient debout. Un hexagone a plat ressemble a une alveole.
 */
private fun hexagon(centerX: Float, centerY: Float, radius: Float): List<Offset> =
    (0 until 6).map { index ->
        val angle = (-90f + index * 60f) * PI.toFloat() / 180f
        Offset(centerX + radius * cos(angle), centerY + radius * sin(angle))
    }

/**
 * La couronne, face par face.
 *
 * Chaque face du biseau est eclairee selon son orientation : celles qui
 * regardent vers le haut a gauche prennent la lumiere, celles qui regardent en
 * bas a droite tombent dans l'ombre. C'est ce calcul — et pas un contour de
 * couleur — qui fait qu'une piece a du relief.
 */
private fun DrawScope.drawFacets(
    outer: List<Offset>,
    inner: List<Offset>,
    bright: Color,
    deep: Color,
) {
    // La lumiere vient d'en haut a gauche, comme partout dans l'application.
    val light = Offset(-0.6f, -0.8f)
    repeat(6) { index ->
        val next = (index + 1) % 6
        val face = Path().apply {
            moveTo(outer[index].x, outer[index].y)
            lineTo(outer[next].x, outer[next].y)
            lineTo(inner[next].x, inner[next].y)
            lineTo(inner[index].x, inner[index].y)
            close()
        }
        // La normale de la face, prise au milieu de son arete exterieure.
        val mid = Offset(
            (outer[index].x + outer[next].x) / 2f,
            (outer[index].y + outer[next].y) / 2f,
        )
        val center = Offset(
            outer.sumOf { it.x.toDouble() }.toFloat() / 6f,
            outer.sumOf { it.y.toDouble() }.toFloat() / 6f,
        )
        val normal = Offset(mid.x - center.x, mid.y - center.y)
        val length = kotlin.math.hypot(normal.x, normal.y).coerceAtLeast(0.001f)
        val lit = ((normal.x / length) * light.x + (normal.y / length) * light.y)
            .coerceIn(-1f, 1f)
        drawPath(
            path = face,
            color = if (lit > 0f) {
                bright.lighten(lit * 0.55f)
            } else {
                bright.darken(-lit * 0.4f)
            },
        )
    }
    val outline = Path().apply {
        moveTo(outer[0].x, outer[0].y)
        outer.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(
        path = outline,
        color = deep.darken(0.35f).copy(alpha = 0.55f),
        style = Stroke(width = 2f),
    )
}

/**
 * Le ruban : deux pans avec leur encoche, derriere la piece.
 *
 * Il ne sert a rien et c'est exactement pour ca qu'il compte — c'est lui qui
 * fait qu'on lit « medaille » et pas « pastille ».
 */
private fun DrawScope.drawRibbon(centerX: Float, top: Float, radius: Float, deep: Color) {
    val width = radius * 0.42f
    val start = top + radius * 0.25f
    val end = top + radius * 1.55f
    val notch = radius * 0.22f
    listOf(-1f, 1f).forEach { side ->
        val near = centerX + side * radius * 0.06f
        val far = centerX + side * (radius * 0.06f + width)
        val tail = Path().apply {
            moveTo(near, start)
            lineTo(far, start)
            lineTo(far, end)
            lineTo((near + far) / 2f, end - notch)
            lineTo(near, end)
            close()
        }
        drawPath(
            path = tail,
            brush = Brush.verticalGradient(
                colors = if (side < 0f) {
                    listOf(deep.darken(0.1f), deep.darken(0.35f))
                } else {
                    listOf(deep.darken(0.28f), deep.darken(0.5f))
                },
                startY = start,
                endY = end,
            ),
        )
    }
}

private fun Color.lighten(amount: Float): Color = Color(
    red = red + (1f - red) * amount,
    green = green + (1f - green) * amount,
    blue = blue + (1f - blue) * amount,
    alpha = alpha,
)

private fun Color.darken(amount: Float): Color = Color(
    red = red * (1f - amount),
    green = green * (1f - amount),
    blue = blue * (1f - amount),
    alpha = alpha,
)

private val MEDAL_WIDTH = 150.dp

/** De la place sous la piece pour le ruban. */
private val MEDAL_HEIGHT = 208.dp

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

/**
 * Le signe anime de chaque badge.
 *
 * Ce sont les memes emoji animes de Google que les visages de l'humeur, du
 * vecteur pur sans image ni adresse dedans.
 */
private fun badgeAnimation(badge: Badge): Int = when (badge) {
    Badge.PRAYERS -> R.raw.badge_prayers
    Badge.STEPS -> R.raw.badge_steps
    Badge.WORKOUT -> R.raw.badge_workout
    Badge.OUTSIDE -> R.raw.badge_outside
    Badge.WATER -> R.raw.badge_water
    Badge.APPLICATION -> R.raw.badge_application
    Badge.WEEK_APPLICATIONS -> R.raw.badge_week
    Badge.JOURNAL -> R.raw.badge_journal
}

/** Combien de fois le signe rejoue pendant que la medaille est la. */
private const val BADGE_LOOPS = 2f

/** Duree du fichier de confettis : soixante-quinze images a trente par seconde. */
private const val CONFETTI_MS = 2500

/** Le temps que la medaille reste a l'ecran une fois arrivee. */
private const val HOLD_MS = 1900
