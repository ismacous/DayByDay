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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Le moment ou quelque chose est fini.
 *
 * Ce n'est pas une phrase de plus dans une carte : c'est le seul endroit de
 * l'application, avec le bonjour du demarrage, ou il se passe **quelque chose**.
 * Une application qu'on ouvre chaque soir a besoin d'au moins un moment qui
 * recompense l'ouverture.
 *
 * Quatre regles, et elles valent pour toute animation de ce genre :
 *
 * - **Elle ne se declenche qu'au passage.** Cocher la cinquieme priere la
 *   lance ; rouvrir la journee le lendemain ne la relance pas. Une celebration
 *   qui rejoue a chaque affichage devient une porte a pousser.
 * - **Elle dure moins de deux secondes** et se coupe d'un appui n'importe ou.
 * - **Elle ne bloque rien.** Pendant qu'elle joue, la page dessous continue
 *   d'exister ; a la fin elle disparait sans rien demander.
 * - **Tout est lu dans le dessin.** Une seule valeur animee mene les quarante
 *   confettis, le disque et le texte, et elle n'est lue que dans des
 *   `graphicsLayer` et des `drawBehind` : rien n'est recompose ni remesure
 *   pendant les deux secondes.
 */
@Composable
fun Celebration(
    title: String,
    subtitle: String,
    emoji: String,
    colors: List<Color>,
    onDone: () -> Unit,
) {
    // Les confettis sont tires une fois pour toutes : leur trajectoire est une
    // fonction du temps, pas une suite de positions recalculees. C'est ce qui
    // permet de tout dessiner en une passe.
    val flakes = remember {
        val random = Random(System.currentTimeMillis())
        List(44) {
            Flake(
                angle = (random.nextFloat() * 2f - 1f) * 0.85f - PI.toFloat() / 2f,
                speed = 0.55f + random.nextFloat() * 0.75f,
                spin = (random.nextFloat() * 2f - 1f) * 9f,
                size = 5f + random.nextFloat() * 7f,
                color = colors[random.nextInt(colors.size)],
                delay = random.nextFloat() * 0.18f,
                square = random.nextBoolean(),
            )
        }
    }

    // Une seule horloge, de 0 a 1, pour tout l'ecran.
    val time = remember { Animatable(0f) }
    // Le disque arrive au ressort, en depassant sa taille : c'est ce
    // depassement qui fait la difference entre apparaitre et surgir.
    val pop = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(180)) }
        launch {
            pop.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow),
            )
        }
        time.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        delay(120)
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
            // Un voile a peine pose : on doit voir que la page est toujours la.
            .background(Color.Black.copy(alpha = 0.28f))
            .drawBehind {
                val t = time.value
                val origin = Offset(size.width / 2f, size.height * 0.42f)
                flakes.forEach { flake ->
                    val local = ((t - flake.delay) / (1f - flake.delay)).coerceIn(0f, 1f)
                    if (local <= 0f) return@forEach
                    // Une parabole : la vitesse initiale pousse, la gravite
                    // rattrape. Deux lignes, et le confetti a un poids.
                    val reach = size.height * 0.85f * flake.speed
                    val x = origin.x + cos(flake.angle) * reach * local
                    val y = origin.y + sin(flake.angle) * reach * local +
                        GRAVITY * size.height * local * local
                    val alpha = (1f - local * local).coerceIn(0f, 1f)
                    val side = flake.size * (1f + local * 0.3f)
                    if (flake.square) {
                        // Un rectangle qui tourne : sa largeur se pince quand il
                        // se met de profil, ce qui suffit a faire croire a une
                        // rotation dans l'espace.
                        val turn = kotlin.math.abs(cos(flake.spin * local * PI.toFloat()))
                        drawRect(
                            color = flake.color.copy(alpha = alpha),
                            topLeft = Offset(x - side * turn / 2f, y - side / 2f),
                            size = Size(side * turn.coerceAtLeast(0.15f), side),
                        )
                    } else {
                        drawCircle(
                            color = flake.color.copy(alpha = alpha),
                            radius = side / 2.4f,
                            center = Offset(x, y),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    val grow = pop.value
                    scaleX = grow
                    scaleY = grow
                    // Un souffle lent apres l'arrivee : le disque respire au
                    // lieu de se figer.
                    val breathe = 1f + 0.03f * sin(time.value * 2f * PI.toFloat())
                    scaleX *= breathe
                    scaleY *= breathe
                    alpha = grow.coerceIn(0f, 1f)
                },
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors))
                    .drawBehind {
                        // Une onde qui s'echappe du disque, une seule fois.
                        val wave = (time.value * 2.4f).coerceIn(0f, 1f)
                        if (wave < 1f) {
                            drawCircle(
                                color = colors.first().copy(alpha = 0.35f * (1f - wave)),
                                radius = size.minDimension / 2f * (1f + wave * 1.1f),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 52.sp)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class Flake(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val delay: Float,
    val square: Boolean,
)

private const val DURATION_MS = 1500

/** De combien la gravite ramene les confettis, en part de la hauteur d'ecran. */
private const val GRAVITY = 1.15f
