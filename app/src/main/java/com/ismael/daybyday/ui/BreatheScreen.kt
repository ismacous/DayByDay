package com.ismael.daybyday.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.BREATH_ADVICE
import com.ismael.daybyday.data.BreathPattern
import com.ismael.daybyday.data.BreathPhase
import com.ismael.daybyday.data.Breathing
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Respirer une minute.
 *
 * Le seul écran de l'application qui ne demande rien, ne note rien et
 * n'enregistre rien. Pas de série à tenir, pas de médaille au bout, pas de
 * ligne dans le bilan : compter les respirations transformerait la seule chose
 * gratuite de l'application en une tâche de plus, et ce serait exactement ce
 * qu'il ne faut pas faire à quelqu'un qui l'ouvre un mauvais jour.
 *
 * Trois choses décidées après le premier essai, et elles tiennent l'écran :
 *
 * 1. **Rien ne bouge quand on choisit un rythme.** La description tenait sur
 *    une ligne ou sur trois selon l'exercice, et toute la page — la bulle
 *    comprise — montait et descendait à chaque changement. Le texte a
 *    maintenant une hauteur fixe : il change, la page ne bouge pas. Sur un
 *    écran qui sert à se poser, c'est la première chose à régler.
 * 2. **On part depuis la bulle.** Le bouton était tout en bas, à l'autre bout
 *    de l'écran ; le regard devait faire l'aller-retour juste avant de
 *    commencer à se calmer. Le mot est dans la bulle, on appuie dessus, elle
 *    répond.
 * 3. **La bulle vit même à l'arrêt.** Immobile, elle avait l'air d'une image.
 *    Au repos elle tourne et se déforme sur elle-même sans grossir : elle
 *    respire quand on respire, elle bouge quand on la regarde.
 *
 * **Une seule horloge mène tout**, et elle n'est lue que dans le dessin. C'est
 * la règle de toutes les animations du projet : une valeur qui change à chaque
 * image et qu'on lit pendant la composition fait recomposer la page soixante
 * fois par seconde. Seuls le mot et le compte à rebours — qui changent une fois
 * par seconde, pas soixante — passent par de l'état.
 */
@Composable
fun BreatheScreen(onBack: () -> Unit) {
    var pattern by remember { mutableStateOf(BreathPattern.COHERENCE) }
    var running by remember { mutableStateOf(false) }

    val elapsed = remember { mutableFloatStateOf(0f) }
    // La reponse de la bulle au doigt : elle se resserre, puis rebondit. Lue
    // dans le dessin, comme le reste.
    val press = remember { Animatable(0f) }

    var phase by remember { mutableStateOf(BreathPhase.INHALE) }
    var remaining by remember { mutableStateOf(pattern.inhale) }

    val current by rememberUpdatedState(pattern)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // L'ecran reste allume pendant qu'on respire : on ne le touche pas, et un
    // ecran qui s'eteint au milieu d'une expiration casse tout.
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
                if (previous != 0L) elapsed.floatValue += (now - previous) / 1_000_000_000f
                previous = now
            }
            val state = Breathing.stateAt(current, elapsed.floatValue.toDouble())
            if (state.phase != phase) phase = state.phase
            if (state.remaining != remaining) remaining = state.remaining
        }
    }

    // Repartir de zero quand on change de rythme : reprendre au milieu d'un
    // cycle donnerait une premiere respiration tronquee.
    LaunchedEffect(pattern) {
        elapsed.floatValue = 0f
        phase = pattern.phases.first()
        remaining = pattern.secondsOf(phase)
    }

    fun toggle() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        running = !running
        scope.launch {
            press.snapTo(0f)
            press.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        }
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
                    .fillMaxWidth(0.88f)
                    .aspectRatio(1f)
                    // Pas d'ondulation ni de halo au doigt : une bulle qui se
                    // teinte en gris quand on la touche casse net ce qu'elle
                    // est en train d'installer. Elle repond par son mouvement.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = if (running) "Arrêter" else "Commencer",
                    ) { toggle() },
                contentAlignment = Alignment.Center,
            ) {
                BreathOrb(
                    elapsed = elapsed,
                    press = press.asState(),
                    pattern = current,
                    running = running,
                    modifier = Modifier.fillMaxSize(),
                )

                AnimatedContent(
                    targetState = if (running) phase.label else "Commencer",
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
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (running) {
                            Text(
                                text = remaining.toString(),
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Une hauteur **fixe**, et c'est tout l'objet de cette boite : la
            // description tient sur deux lignes ou sur quatre selon le rythme,
            // et sans hauteur imposee, choisir un exercice faisait monter et
            // descendre la bulle elle-meme.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ADVICE_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = if (running) pattern.hintOf(phase) else pattern.purpose,
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(tween(450)),
                            initialContentExit = fadeOut(tween(300)),
                            sizeTransform = SizeTransform(clip = false),
                        )
                    },
                    label = "conseil",
                ) { text ->
                    Text(
                        text = text,
                        style = if (running) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Les rythmes gardent leur place pendant l'exercice : ils
            // s'effacent, ils ne disparaissent pas. Une rangee qui se retire
            // ferait remonter tout ce qui est au-dessus, bulle comprise.
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.graphicsLayer { alpha = if (running) 0f else 1f },
            ) {
                BreathPattern.entries.forEach { option ->
                    PatternChip(
                        pattern = option,
                        selected = option == pattern,
                        enabled = !running,
                        onClick = { pattern = option },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!running) {
                    Text(
                        text = BREATH_ADVICE,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Deux lignes de titre, trois de texte : la plus longue des descriptions. */
private val ADVICE_HEIGHT = 92.dp

@Composable
private fun PatternChip(
    pattern: BreathPattern,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) tint.copy(alpha = 0.16f) else tint.copy(alpha = 0.05f))
            .clickable(enabled = enabled, onClickLabel = pattern.label, onClick = onClick)
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
 * Trois formes molles qui se chevauchent, chacune dans sa couleur, sur son
 * tempo et **tournant à sa vitesse**. Ce n'est pas un cercle, et c'est le
 * point : un cercle parfait qui grossit et rétrécit ressemble à un chargement,
 * et on attend qu'un chargement se termine. Un contour qui ondule n'attend
 * rien.
 *
 * **Au repos, elle tourne sans grossir.** C'était le défaut de la première
 * version : à l'arrêt elle ne bougeait presque pas, donc elle avait l'air
 * d'une image posée là, et rien ne donnait envie d'appuyer dessus. Elle garde
 * maintenant sa taille à trois pour cent près et passe son mouvement dans la
 * rotation et la déformation — elle vit sans rien annoncer. Dès que
 * l'exercice part, l'inverse : la déformation se calme et c'est la taille qui
 * parle, parce que c'est elle qu'on doit suivre.
 *
 * L'ondulation vient de trois sinusoïdes de fréquences premières entre elles
 * (2, 3 et 5 lobes) : leur somme ne repasse jamais par la même forme, donc
 * l'œil ne trouve pas de boucle à suivre.
 *
 * Trois règles du projet s'appliquent, et elles ne sont pas décoratives :
 * le temps est lu **dans le dessin** ; tout rayon a un **plancher** (un dégradé
 * radial de rayon nul fait tomber l'application, et une bulle qui part de zéro
 * est exactement le cas qui le déclenche) ; la lumière vient d'en haut à
 * gauche, comme partout ailleurs.
 */
@Composable
private fun BreathOrb(
    elapsed: State<Float>,
    press: State<Float>,
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

        // Au repos la taille ne raconte rien : elle bouge de trois pour cent,
        // juste assez pour ne pas etre figee. C'est la rotation qui porte la
        // vie. Pendant l'exercice, c'est l'inverse.
        val openness = if (running) state.openness else 0.58f + 0.03f * sin(time * 0.5f)

        val half = size.minDimension / 2f
        val span = if (running) 0.44f else 0.10f
        val floor = if (running) 0.40f else 0.60f
        // Le plancher, et il compte : sans lui, poumons vides, tous les rayons
        // tombent a zero.
        val base = (half * (floor + span * openness)).coerceAtLeast(1f)

        // La reponse au doigt : un resserrement franc, puis un rebond.
        val k = press.value
        val squeeze = when {
            k <= 0f || k >= 1f -> 0f
            k < 0.22f -> -(k / 0.22f) * 0.09f
            else -> 0.09f * sin(Math.PI.toFloat() * (k - 0.22f) / 0.78f)
        }
        val radius0 = (base * (1f + squeeze)).coerceAtLeast(1f)

        // L'ondulation se calme quand le souffle est retenu, et elle est plus
        // ample au repos : a l'arret, c'est elle qui fait tout le mouvement.
        val wobble = when {
            !running -> 0.055f
            state.moving -> 0.030f
            else -> 0.012f
        }
        val center = Offset(size.width / 2f, size.height / 2f)

        // Le halo : il deborde largement, tres pale. C'est lui qui fait que la
        // bulle a l'air posee dans quelque chose plutot que collee sur le fond.
        val halo = (radius0 * 1.95f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(hues[0].copy(alpha = 0.16f), Color.Transparent),
                center = center,
                radius = halo,
            ),
            radius = halo,
            center = center,
        )

        // L'onde qui part du doigt quand on appuie : elle dit « c'est parti »
        // sans un mot, et elle a disparu avant qu'on ait fini d'inspirer.
        if (k > 0f && k < 1f) {
            val ring = (radius0 * (0.92f + 0.9f * k)).coerceAtLeast(1f)
            drawCircle(
                color = hues[1].copy(alpha = 0.28f * (1f - k)),
                radius = ring,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f),
            )
        }

        hues.forEachIndexed { index, hue ->
            // Chacune tourne a sa vitesse : c'est ce glissement des unes sur
            // les autres qui fait qu'on croit voir une matiere, et pas trois
            // taches superposees.
            val spin = time * (0.13f + index * 0.05f) + index * 2.1f
            val phase = time * (0.22f + index * 0.07f) + index * 1.3f
            val drift = radius0 * (if (running) 0.055f else 0.085f)
            val blobCenter = Offset(
                center.x + cos(phase * 0.8f + index) * drift,
                center.y + sin(phase * 0.6f + index) * drift,
            )
            val radius = radius0 * (1f - index * 0.045f)
            val path = blobPath(blobCenter, radius, phase, wobble, spin)
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(
                        hue.copy(alpha = 0.52f),
                        hue.copy(alpha = 0.27f),
                        hue.copy(alpha = 0.04f),
                    ),
                    center = Offset(
                        blobCenter.x - radius * 0.28f,
                        blobCenter.y - radius * 0.30f,
                    ),
                    radius = (radius * 1.5f).coerceAtLeast(1f),
                ),
            )
            // Un trait de lumiere sur le bord de la premiere forme seulement :
            // c'est ce qui la fait passer de tache coloree a bulle de verre.
            if (index == 0) {
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.22f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f),
                )
            }
        }

        // La lumiere, en haut a gauche comme partout ailleurs. C'est elle qui
        // donne du volume : sans elle, la bulle est un aplat colore.
        val gleam = (radius0 * 0.58f).coerceAtLeast(1f)
        val gleamAt = Offset(center.x - radius0 * 0.34f, center.y - radius0 * 0.38f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                center = gleamAt,
                radius = gleam,
            ),
            radius = gleam,
            center = gleamAt,
        )

        // Un creux au milieu, tres doux, de la couleur de la page : le mot et
        // le compte a rebours se posent dessus et restent lisibles quelle que
        // soit la couleur qui passe derriere. Il etait deux fois plus marque, et
        // la bulle avait un trou blanc au centre.
        val well = (radius0 * 0.44f).coerceAtLeast(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(surface.copy(alpha = 0.55f), surface.copy(alpha = 0f)),
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
 * differences d'un demi-pixel. [spin] fait tourner le motif sur lui-meme —
 * c'est lui qui fait vivre la bulle a l'arret, sans qu'elle change de taille.
 */
private fun blobPath(
    center: Offset,
    radius: Float,
    phase: Float,
    wobble: Float,
    spin: Float,
): Path {
    val path = Path()
    val steps = 160
    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * (2f * Math.PI.toFloat())
        val turned = angle + spin
        val ripple = 1f +
            wobble * sin(3f * turned + phase * 1.7f) +
            wobble * 0.7f * sin(5f * turned - phase * 1.1f) +
            wobble * 1.2f * sin(2f * turned + phase * 0.7f)
        val r = (radius * ripple).coerceAtLeast(1f)
        val x = center.x + cos(angle) * r
        val y = center.y + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
