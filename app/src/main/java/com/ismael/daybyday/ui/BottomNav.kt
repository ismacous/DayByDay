package com.ismael.daybyday.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.ui.theme.Brand
import kotlin.math.PI
import kotlin.math.sin

/**
 * La barre de navigation, avec sa bille.
 *
 * Une bille de couleur se tient au-dessus de l'onglet choisi, dans une encoche
 * creusee dans le bord haut de la barre. Quand on change d'onglet, elle ne
 * glisse pas : elle **saute**, en suivant un arc. Un objet qui saute a un
 * poids ; un objet qui glisse n'est qu'une tache qui se deplace. Le creux la
 * suit, et la barre semble se deformer sous elle.
 *
 * Trois precautions, toutes apprises en les cassant ailleurs dans
 * l'application :
 *
 * 1. La forme de la barre change a chaque image pendant le saut. Elle est donc
 *    posee dans un `graphicsLayer` : le bloc est rejoue sans recomposer ni
 *    remesurer quoi que ce soit.
 * 2. La bille se deplace en `translation`, pas en changeant de position dans la
 *    mise en page.
 * 3. Les positions des onglets sont mesurees en coordonnees d'ecran, et la
 *    barre note les siennes : c'est la difference des deux qui donne la
 *    position dans la barre. Mesurer « par rapport au parent » se trompe des
 *    qu'une marge s'intercale.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slots = remember { mutableStateMapOf<String, Float>() }
    var barOrigin by remember { mutableFloatStateOf(0f) }

    // Le saut est une seule valeur, de 0 a 1 : la position horizontale
    // s'interpole entre le depart et l'arrivee, et la hauteur suit un demi-tour
    // de sinus — haute au milieu du trajet, nulle aux deux bouts. C'est ce qui
    // fait l'arc.
    val jump = remember { Animatable(1f) }
    var fromX by remember { mutableFloatStateOf(Float.NaN) }
    var toX by remember { mutableFloatStateOf(Float.NaN) }

    val target = currentRoute?.let { slots[it] }
    LaunchedEffect(target) {
        val destination = target ?: return@LaunchedEffect
        if (toX.isNaN()) {
            // Premiere mesure : la bille se pose, elle n'arrive pas du bord.
            fromX = destination
            toX = destination
            jump.snapTo(1f)
        } else {
            fromX = toX
            toX = destination
            jump.snapTo(0f)
            jump.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
    }

    val barColor = MaterialTheme.colorScheme.surface
    val ballColors = Brand.gradient

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = BALL_OVERHANG, bottom = 8.dp),
    ) {
        fun ballCenterX(): Float =
            if (toX.isNaN()) Float.NaN else fromX + (toX - fromX) * jump.value

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { barOrigin = it.positionInRoot().x }
                .graphicsLayer {
                    // La forme est relue ici, dans la couche : l'encoche peut
                    // donc se deplacer a chaque image sans qu'aucune mesure ne
                    // soit refaite.
                    val center = ballCenterX()
                    shape = NotchedBarShape(
                        notchCenterX = if (center.isNaN()) Float.NaN else center - barOrigin,
                        notchRadius = (BALL_RADIUS + NOTCH_MARGIN).toPx(),
                    )
                    clip = true
                    shadowElevation = 18.dp.toPx()
                    ambientShadowColor = Brand.Primary.copy(alpha = 0.35f)
                    spotShadowColor = Brand.Primary.copy(alpha = 0.45f)
                }
                .background(barColor),
        )

        // La bille, dessinee **avant** la rangee d'onglets : quand elle passe au
        // milieu, elle glisse derriere le bouton du jour au lieu de lui rentrer
        // dedans.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = BAR_HEIGHT - BALL_RADIUS)
                .size(BALL_RADIUS * 2)
                .graphicsLayer {
                    val center = ballCenterX()
                    if (center.isNaN()) {
                        alpha = 0f
                        return@graphicsLayer
                    }
                    alpha = 1f
                    translationX = center - barOrigin - BALL_RADIUS.toPx()
                    // L'arc : un demi-sinus, nul au depart et a l'arrivee.
                    translationY = -BALL_ARC.toPx() * sin(PI * jump.value).toFloat()
                }
                .clip(CircleShape)
                .background(Brush.linearGradient(ballColors)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val half = items.size / 2

            items.take(half).forEach { item ->
                NavTab(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onSelect(item.route) },
                    onCenter = { x -> slots[item.route] = x },
                    modifier = Modifier.weight(1f),
                )
            }

            TodayButton(onClick = onToday)

            items.drop(half).forEach { item ->
                NavTab(
                    item = item,
                    selected = currentRoute == item.route,
                    onClick = { onSelect(item.route) },
                    onCenter = { x -> slots[item.route] = x },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/** Hauteur de la barre elle-meme, sans ce qui depasse au-dessus. */
private val BAR_HEIGHT = 62.dp

/**
 * Le rayon de la bille.
 *
 * Elle etait a vingt et un points, et c'etait trop : une bille plus large qu'un
 * pouce ne designe plus un onglet, elle occupe la barre. Un reperage doit se
 * voir sans se substituer a ce qu'il repere. Maintenant que la page defile
 * **derriere** la barre, elle passe devant du texte : une raison de plus pour
 * qu'elle reste petite.
 */
private val BALL_RADIUS = 11.dp

/** Ce que la bille laisse depasser au-dessus du bord de la barre. */
private val BALL_OVERHANG = 12.dp

/** Hauteur du sommet de l'arc pendant le saut. */
private val BALL_ARC = 20.dp

/** L'air entre la bille et le bord de l'encoche. */
private val NOTCH_MARGIN = 4.dp

/**
 * La barre : un rectangle a bouts ronds, moins un disque mordu dans son bord
 * haut. Le disque est retire par difference de chemins — c'est ce qui donne le
 * creux, et non un simple cercle pose par-dessus, qui laisserait un bord.
 */
private class NotchedBarShape(
    private val notchCenterX: Float,
    private val notchRadius: Float,
) : androidx.compose.ui.graphics.Shape {

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val bar = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        size.height / 2f,
                        size.height / 2f,
                    ),
                )
            )
        }
        if (notchCenterX.isNaN()) {
            return androidx.compose.ui.graphics.Outline.Generic(bar)
        }

        val notch = Path().apply {
            addOval(
                Rect(
                    left = notchCenterX - notchRadius,
                    top = -notchRadius,
                    right = notchCenterX + notchRadius,
                    bottom = notchRadius,
                )
            )
        }
        val result = Path().apply { op(bar, notch, PathOperation.Difference) }
        return androidx.compose.ui.graphics.Outline.Generic(result)
    }
}

/**
 * Un onglet : son dessin et son nom, l'un au-dessus de l'autre.
 *
 * Le dessin de l'onglet choisi s'efface : c'est la bille qui le remplace
 * au-dessus. Le nom, lui, reste et prend du poids — sans quoi on ne saurait
 * plus ce que la bille designe.
 */
@Composable
private fun NavTab(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    onCenter: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.NORMAL),
        label = "contenu",
    )
    val iconAlpha = animateFloatAsState(
        targetValue = if (selected) 0f else 1f,
        animationSpec = tween(Motion.NORMAL),
        label = "dessin",
    )
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = Motion.softSpring(),
        label = "pression",
    )

    Column(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                onCenter(coordinates.positionInRoot().x + coordinates.size.width / 2f)
            }
            // Un repere pour les tests : viser le **nom** de l'onglet marchait
            // tant qu'aucune page ne contenait le meme mot, ce qui n'est vrai
            // que par chance.
            .testTag("tab-${item.route}")
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = item.label,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    alpha = iconAlpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
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

    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(54.dp)
            .scale(scale)
            .brandShadow(elevation = 14.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = Brand.gradient,
                    start = Offset(0f, 0f),
                    end = Offset(140f, 140f),
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = "Ma journée d'aujourd'hui",
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Un crayon, et plus un « + ».
        //
        // Le plus disait « ajouter quelque chose », alors que ce bouton
        // n'ajoute rien : il ouvre la journee d'aujourd'hui, qui existe deja.
        // Un crayon dit ce qu'on va y faire — l'ecrire.
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Ma journée d'aujourd'hui",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}
