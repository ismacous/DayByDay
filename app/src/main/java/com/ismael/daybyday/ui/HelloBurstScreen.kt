package com.ismael.daybyday.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.DayColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Le bonjour du demarrage.
 *
 * Au depart, quatre pastilles empilees, a peine decalees, comme un jeu de
 * cartes pose sur la table. Puis elles **eclatent** : chacune part a sa place,
 * en tournant, portee par un ressort volontairement peu amorti — elle depasse
 * un peu sa cible avant de s'y poser. C'est ce depassement qui fait la
 * difference entre un mouvement mecanique et un mouvement vivant.
 *
 * Trois regles se cachent la-dedans, et elles valent pour toute animation
 * d'accueil :
 *
 * - **Elle dure moins de deux secondes.** Une animation de demarrage se voit
 *   plusieurs fois par jour : la troisieme fois, elle n'emerveille plus, elle
 *   retarde.
 * - **Elle s'interrompt.** Un appui n'importe ou passe directement a
 *   l'application. Une animation qu'on ne peut pas couper est une porte fermee.
 * - **Elle ne joue qu'une fois par ouverture de l'application**, pas a chaque
 *   retour a l'ecran d'accueil.
 *
 * Les mots et les couleurs sont ceux de l'application — les quatre teintes des
 * journees — plutot qu'un decor importe. Le demarrage doit ressembler a ce
 * qu'on ouvre.
 */
@Composable
fun HelloBurstScreen(firstName: String, onDone: () -> Unit) {
    val words = remember(firstName) {
        val name = firstName.trim()
        listOf(
            BurstWord("Bonjour", DayColor.GREEN),
            BurstWord(if (name.isEmpty()) "toi" else name, DayColor.ORANGE),
            BurstWord("on", DayColor.RED),
            BurstWord("y va !", DayColor.BLACK),
        )
    }

    // Une seule valeur par pastille : de 0 (empilee) a 1 (a sa place). Elle
    // n'est lue que dans la couche graphique, donc l'ecran entier ne se
    // recompose pas soixante fois par seconde pour quatre pastilles.
    val spreads = remember { List(4) { Animatable(0f) } }
    val exit = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(260)
        spreads.forEachIndexed { index, spread ->
            launch {
                delay(index * 70L)
                spread.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        // Peu amorti : la pastille depasse sa place et revient.
                        dampingRatio = 0.52f,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
        }
        delay(1250)
        exit.animateTo(0f, tween(220))
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Passer",
                onClick = onDone,
            ),
    ) {
        ScreenBackground(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = exit.value },
                contentAlignment = Alignment.Center,
            ) {
                words.forEachIndexed { index, word ->
                    val place = PLACES[index]
                    val spread = spreads[index]
                    Text(
                        text = word.text,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = readableOn(word.color.color),
                        modifier = Modifier
                            .graphicsLayer {
                                val progress = spread.value
                                translationX = place.x.dp.toPx() * progress
                                translationY = place.y.dp.toPx() * progress
                                rotationZ = place.rotation * progress
                                // Empilees, les pastilles sont un peu plus
                                // petites : l'ecartement les fait aussi grandir.
                                val grow = 0.86f + 0.14f * progress
                                scaleX = grow
                                scaleY = grow
                            }
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Brush.linearGradient(word.color.gradient))
                            .padding(horizontal = 26.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

private data class BurstWord(val text: String, val color: DayColor)

/**
 * Ou chaque pastille va se poser, en points depuis le centre, et de combien de
 * degres elle penche. Les valeurs sont irregulieres exprès : quatre pastilles
 * posees a intervalles egaux font un tableau, pas un geste.
 */
private data class BurstPlace(val x: Float, val y: Float, val rotation: Float)

private val PLACES = listOf(
    BurstPlace(x = -34f, y = -108f, rotation = -7f),
    BurstPlace(x = 46f, y = -36f, rotation = 5f),
    BurstPlace(x = -62f, y = 38f, rotation = -4f),
    BurstPlace(x = 30f, y = 110f, rotation = 8f),
)
