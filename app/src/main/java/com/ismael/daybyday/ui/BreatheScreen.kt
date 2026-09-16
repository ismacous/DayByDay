package com.ismael.daybyday.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.BreathPattern
import com.ismael.daybyday.data.BreathPhase
import com.ismael.daybyday.data.Breathing
import kotlin.math.cos
import kotlin.math.sin

/**
 * Respirer une minute.
 *
 * Le seul écran de l'application qui ne demande rien, ne note rien et
 * n'enregistre rien. Il n'y a pas de série à tenir, pas de médaille au bout,
 * pas de ligne dans le bilan : compter les respirations transformerait la
 * seule chose gratuite de l'application en une tâche de plus, et ce serait
 * exactement ce qu'il ne faut pas faire à quelqu'un qui l'ouvre un mauvais
 * jour.
 *
 * Ce qu'on voit : une bulle qui gonfle et se dégonfle au rythme du souffle, un
 * mot au milieu, un compte à rebours. Rien d'autre à l'écran une fois que ça
 * tourne — ni barre, ni bouton, ni chiffre qui monte.
 *
 * **Une seule horloge mène tout**, et elle n'est lue que dans le dessin. C'est
 * la règle de toutes les animations du projet : une valeur qui change à chaque
 * image et qu'on lit pendant la composition fait recomposer la page soixante
 * fois par seconde. Ici, le temps vit dans un `mutableFloatStateOf` lu
 * uniquement dans le `Canvas` ; seuls le mot et le compte à rebours — qui
 * changent une fois par seconde, pas soixante — passent par de l'état.
 */
@Composable
fun BreatheScreen(onBack: () -> Unit) {
    var pattern by remember { mutableStateOf(BreathPattern.COHERENCE) }
    var running by remember { mutableStateOf(false) }

    // Le temps écoulé, en secondes. Lu dans le dessin, jamais dans la
    // composition : c'est ce qui fait la différence entre une bulle fluide et
    // une page qui se reconstruit soixante fois par seconde.
    val elapsed = remember { mutableFloatStateOf(0f) }

    // Le mot et le compte à rebours, eux, sont de l'état : ils changent une
    // fois par seconde. Les faire vivre dans le dessin obligerait à dessiner le
    // texte à la main pour rien.
    var phase by remember { mutableStateOf(BreathPhase.INHALE) }
    var remaining by remember { mutableStateOf(pattern.inhale) }

    val current by rememberUpdatedState(pattern)

    // L'écran reste allumé pendant qu'on respire : on ne le touche pas, et un
    // écran qui s'éteint au milieu d'une expiration casse tout.
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        elapsed.floatValue = 0f
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) {
                    elapsed.floatValue += (now - previous) / 1_000_000_000f
                }
                previous = now
            }
            val state = Breathing.stateAt(current, elapsed.floatValue.toDouble())
            if (state.phase != phase) phase = state.phase
            if (state.remaining != remaining) remaining = state.remaining
        }
    }

    // Repartir de zéro quand on change de rythme : reprendre au milieu d'un
    // cycle donnerait une première respiration tronquée.
    LaunchedEffect(pattern) {
        elapsed.floatValue = 0f
        phase = pattern.phases.first()
        remaining = pattern.secondsOf(phase)
    }

    ScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Fermer") }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                BreathOrb(
                    elapsed = elapsed,
                    pattern = current,
                    running = running,
                    modifier = Modifier.fillMaxSize(),
                )

                // Le mot au milieu de la bulle. Il se remplace en fondu : un
                // mot qui saute d'un coup rompt exactement ce qu'on essaie
                // d'installer.
                AnimatedContent(
                    targetState = if (running) phase.label else "Respirer",
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(tween(600)),
                            initialContentExit = fadeOut(tween(400)),
                            sizeTransform = SizeTransform(clip = false),
                        )
                    },
                    label = "temps",
                ) { word ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (running) {
                            Text(
                                text = remaining.toString(),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Pendant la respiration, l'écran se vide : il ne reste que la
            // bulle et le mot. Tout le reste est une invitation à regarder
            // autre chose que son souffle.
            if (!running) {
                Text(
                    text = "Cale-toi sur la bulle. Elle grossit quand tu inspires, " +
                        "elle se resserre quand tu expires.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BreathPattern.entries.forEach { option ->
                        PatternChip(
                            pattern = option,
                            selected = option == pattern,
                            onClick = { pattern = option },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = pattern.purpose,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
            }

            TextButton(onClick = { running = !running }) {
                Text(if (running) "Arrêter" else "Commencer")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PatternChip(
    pattern: BreathPattern,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) tint.copy(alpha = 0.16f) else tint.copy(alpha = 0.05f))
            .clickable(onClickLabel = pattern.label, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = pattern.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            text = pattern.summary,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * La bulle.
 *
 * Trois formes molles qui se chevauchent, chacune dans sa couleur et sur son
 * propre tempo, plus un halo derrière et une lumière en haut à gauche. Ce n'est
 * pas un cercle : un cercle parfait qui grossit et rétrécit ressemble à un
 * chargement, et on attend qu'un chargement se termine. Un contour qui ondule
 * doucement, lui, n'attend rien — c'est ce qui donne envie de le suivre.
 *
 * L'ondulation vient de trois sinusoïdes de fréquences premières entre elles
 * (2, 3 et 5 lobes) : leurs sommes ne se répètent jamais à l'identique, donc
 * l'œil ne trouve pas de boucle. Elle **diminue quand le souffle est retenu** :
 * sur un temps de pause, la bulle se calme aussi.
 *
 * Trois règles du projet s'appliquent ici, et elles ne sont pas décoratives :
 *
 * 1. Le temps est lu **dans le dessin**, jamais pendant la composition.
 * 2. Tout rayon a un **plancher**. Un dégradé radial de rayon nul fait tomber
 *    l'application — c'est arrivé sur la bulle du coup de pouce, et une bulle
 *    qui part de zéro est exactement le cas qui le déclenche.
 * 3. La lumière vient d'en haut à gauche, comme partout ailleurs.
 */
@Composable
private fun BreathOrb(
    elapsed: State<Float>,
    pattern: BreathPattern,
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val hues = listOf(
        Color(0xFF5B4DF0),
        Color(0xFF3BA6FF),
        Color(0xFF17C99A),
    )

    Canvas(modifier = modifier) {
        val time = elapsed.value
        val state = Breathing.stateAt(pattern, time.toDouble())
        // Au repos, la bulle respire quand meme, tres lentement : un ecran
        // fige avant d'avoir appuye ne donne pas envie d'appuyer.
        val openness = if (running) {
            state.openness
        } else {
            (0.5f + 0.5f * sin(time * 0.6f)) * 0.55f + 0.20f
        }

        val half = size.minDimension / 2f
        // Le plancher, et il compte : sans lui, poumons vides, tous les rayons
        // tombent a zero.
        val base = (half * (0.40f + 0.44f * openness)).coerceAtLeast(1f)

        // L'ondulation se calme quand le souffle est retenu.
        val wobble = if (running && !state.moving) 0.012f else 0.030f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Le halo : il deborde largement, tres pale. C'est lui qui fait que la
        // bulle a l'air posee dans quelque chose plutot que collee sur le fond.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(hues[0].copy(alpha = 0.16f), Color.Transparent),
                center = center,
                radius = (base * 1.95f).coerceAtLeast(1f),
            ),
            radius = (base * 1.95f).coerceAtLeast(1f),
            center = center,
        )

        hues.forEachIndexed { index, hue ->
            val phase = time * (0.22f + index * 0.07f) + index * 2.1f
            // Les trois formes sont legerement decalees les unes des autres :
            // superposees pile, elles ne feraient qu'une seule couleur terne.
            val drift = base * 0.055f
            val blobCenter = Offset(
                center.x + cos(phase * 0.8f + index) * drift,
                center.y + sin(phase * 0.6f + index) * drift,
            )
            val radius = base * (1f - index * 0.045f)
            drawPath(
                path = blobPath(blobCenter, radius, phase, wobble),
                brush = Brush.radialGradient(
                    colors = listOf(
                        hue.copy(alpha = 0.50f),
                        hue.copy(alpha = 0.26f),
                        hue.copy(alpha = 0.04f),
                    ),
                    center = Offset(
                        blobCenter.x - radius * 0.28f,
                        blobCenter.y - radius * 0.30f,
                    ),
                    radius = (radius * 1.5f).coerceAtLeast(1f),
                ),
            )
        }

        // La lumiere, en haut a gauche comme partout ailleurs dans
        // l'application. C'est elle qui donne du volume : sans elle, la bulle
        // est un aplat colore.
        val gleam = (base * 0.62f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                center = Offset(center.x - base * 0.34f, center.y - base * 0.38f),
                radius = gleam,
            ),
            radius = gleam,
            center = Offset(center.x - base * 0.34f, center.y - base * 0.38f),
        )

        // Un creux au milieu, de la couleur de la page : le mot et le compte a
        // rebours se posent dessus et restent lisibles quelle que soit la
        // couleur qui passe derriere.
        val well = (base * 0.52f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(surface.copy(alpha = 0.82f), surface.copy(alpha = 0f)),
                center = center,
                radius = well,
            ),
            radius = well,
            center = center,
        )
    }
}

/**
 * Le contour ondulant d'une forme molle.
 *
 * Cent soixante points suffisent : au-dela, on paie des calculs pour des
 * differences d'un demi-pixel. Les trois frequences (2, 3, 5) sont premieres
 * entre elles a dessein — leur somme ne repasse jamais par la meme forme, donc
 * l'oeil ne trouve pas de boucle a suivre.
 */
private fun blobPath(center: Offset, radius: Float, phase: Float, wobble: Float): Path {
    val path = Path()
    val steps = 160
    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
        val ripple = 1f +
            wobble * sin(3f * angle + phase * 1.7f) +
            wobble * 0.7f * sin(5f * angle - phase * 1.1f) +
            wobble * 1.2f * sin(2f * angle + phase * 0.7f)
        val r = (radius * ripple).coerceAtLeast(1f)
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
