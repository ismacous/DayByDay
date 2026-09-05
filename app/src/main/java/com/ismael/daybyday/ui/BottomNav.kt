package com.ismael.daybyday.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.ui.theme.Brand

/**
 * La barre de navigation.
 *
 * Elle flotte au-dessus du contenu au lieu d'etre collee au bord : une barre
 * posee sur le fond fait partie de la page, une barre collee fait partie du
 * telephone. Et au milieu, un bouton en relief qui va droit a la journee du
 * jour — c'est ce qu'on vient faire ici neuf fois sur dix, ca ne doit pas
 * demander de chercher.
 *
 * L'onglet choisi ne se contente pas de changer de teinte : son nom apparait a
 * cote de son dessin, en s'ouvrant sur le cote. Les autres restent muets. On
 * voit donc ou l'on est sans avoir a lire cinq mots.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            // La barre du telephone passe par-dessus la notre : sans cette
            // marge, on n'atteint que le haut des boutons. C'est le genre de
            // detail qui rend une belle barre inutilisable.
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .brandShadow(elevation = 18.dp, shape = CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val half = items.size / 2
            items.take(half).forEach { item ->
                NavPill(item = item, selected = currentRoute == item.route) {
                    onSelect(item.route)
                }
            }

            TodayButton(onClick = onToday)

            items.drop(half).forEach { item ->
                NavPill(item = item, selected = currentRoute == item.route) {
                    onSelect(item.route)
                }
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun NavPill(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(Motion.QUICK),
        label = "pression",
    )
    val background by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(Motion.NORMAL),
        label = "fond",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.NORMAL),
        label = "contenu",
    )

    Surface(
        modifier = Modifier.scale(scale),
        shape = CircleShape,
        color = background,
        onClick = onClick,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = content,
                modifier = Modifier.size(21.dp),
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(Motion.NORMAL)) + expandHorizontally(tween(Motion.NORMAL)),
                exit = fadeOut(tween(Motion.QUICK)) + shrinkHorizontally(tween(Motion.QUICK)),
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp),
                )
            }
        }
    }
}

/** Le bouton du milieu : la journee du jour, toujours a un doigt. */
@Composable
private fun TodayButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = Motion.softSpring(),
        label = "pression",
    )

    // La respiration : un anneau qui s'ecarte du bouton et s'efface, en boucle.
    // La version precedente ne faisait varier que l'ombre — invisible. Un halo
    // qui grandit, lui, se voit du coin de l'oeil sans jamais clignoter.
    val breath = rememberInfiniteTransition(label = "souffle")
    val pulse by breath.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "onde",
    )

    Box(
        modifier = Modifier.size(78.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = size.minDimension / 2f
            val ring = radius * (0.68f + 0.32f * pulse)
            drawCircle(
                color = Brand.Primary.copy(alpha = 0.30f * (1f - pulse)),
                radius = ring,
            )
        }

    Box(
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .brandShadow(elevation = 16.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Brush.linearGradient(Brand.gradient))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = "Ma journée d'aujourd'hui",
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Ma journée d'aujourd'hui",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
    }
}

