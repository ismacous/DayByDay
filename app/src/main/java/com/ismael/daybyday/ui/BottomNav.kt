package com.ismael.daybyday.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.ui.theme.Brand
import kotlinx.coroutines.launch

/**
 * La barre de navigation.
 *
 * Elle flotte au-dessus du contenu au lieu d'etre collee au bord : une barre
 * posee sur le fond fait partie de la page, une barre collee fait partie du
 * telephone. Et au milieu, un bouton en relief qui va droit a la journee du
 * jour — c'est ce qu'on vient faire ici neuf fois sur dix, ca ne doit pas
 * demander de chercher.
 *
 * Le choix d'un onglet ne se joue pas en deux pastilles qui se croisent, l'une
 * qui s'eteint et l'autre qui s'allume : **une seule** pastille glisse de l'un
 * a l'autre. C'est toute la difference — un objet qui se deplace se suit du
 * regard, deux objets qui clignotent se subissent. L'icone choisie se souleve
 * d'un cheveu au passage, et son nom prend du poids.
 *
 * Les quatre onglets occupent des largeurs **egales** et fixes. Ce n'est pas un
 * detail de mise en page : c'est ce qui permet a la pastille de n'avoir qu'un
 * seul mouvement a jouer, un glissement. Des largeurs qui changent avec le
 * texte l'obligeraient a se redimensionner en meme temps, et la mesure serait
 * refaite a chaque image.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // La position et la largeur de chaque onglet, mesurees par la mise en page.
    // Elles sont notees **en coordonnees d'ecran**, et la barre note aussi les
    // siennes : c'est la soustraction des deux qui donne la position dans la
    // barre. Mesurer « par rapport au parent » aurait ete plus court, mais le
    // parent d'un onglet n'est pas la surface qu'on peint — une marge entre les
    // deux, et la pastille se retrouve decalee sans qu'on comprenne pourquoi.
    val slots = remember { mutableStateMapOf<String, ClosedFloatingPointRange<Float>>() }
    var barOrigin by remember { mutableFloatStateOf(0f) }
    val indicatorX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer

    val target = currentRoute?.let { slots[it] }
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        val x = target.start
        val width = target.endInclusive - target.start
        if (indicatorWidth.value == 0f) {
            // Premiere mesure : la pastille se pose, elle ne glisse pas depuis
            // le bord gauche de l'ecran.
            indicatorX.snapTo(x)
            indicatorWidth.snapTo(width)
        } else {
            launch {
                indicatorX.animateTo(
                    x,
                    spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                )
            }
            launch {
                indicatorWidth.animateTo(width, spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .onGloballyPositioned { barOrigin = it.positionInRoot().x }
                // La pastille est **dessinee**, pas posee : la valeur animee
                // n'est donc lue que dans le dessin. Rien n'est recompose, rien
                // n'est remesure, il ne reste qu'une forme a repeindre — et
                // c'est ce qui fait la difference entre un glissement fluide et
                // un glissement qui accroche.
                .drawBehind {
                    val width = indicatorWidth.value
                    if (width <= 0f) return@drawBehind
                    val height = 48.dp.toPx()
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = Offset(
                            indicatorX.value - barOrigin,
                            (size.height - height) / 2f,
                        ),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(height / 2f, height / 2f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        onBounds = { start, end -> slots[item.route] = start..end },
                        modifier = Modifier.weight(1f),
                    )
                }

                TodayButton(onClick = onToday)

                items.drop(half).forEach { item ->
                    NavTab(
                        item = item,
                        selected = currentRoute == item.route,
                        onClick = { onSelect(item.route) },
                        onBounds = { start, end -> slots[item.route] = start..end },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/**
 * Un onglet : son dessin et son nom, l'un au-dessus de l'autre.
 *
 * Le nom est toujours la. Le faire apparaitre seulement quand l'onglet est
 * choisi obligeait la barre a changer de largeurs pendant l'animation, et donc
 * la pastille a courir apres une cible qui bouge. Quatre noms visibles en
 * permanence, c'est aussi quatre destinations qu'on n'a pas a deviner.
 */
@Composable
private fun NavTab(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    onBounds: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.NORMAL),
        label = "contenu",
    )
    // Le dessin se souleve quand on le choisit, et s'enfonce sous le doigt. Les
    // deux valeurs ne servent qu'a la couche graphique : elles ne coutent aucun
    // recalcul de mise en page.
    val lift = animateFloatAsState(
        targetValue = if (selected) -3f else 0f,
        animationSpec = Motion.softSpring(),
        label = "elevation",
    )
    val scale = animateFloatAsState(
        targetValue = when {
            pressed -> 0.88f
            selected -> 1.08f
            else -> 1f
        },
        animationSpec = Motion.softSpring(),
        label = "taille",
    )

    Column(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val start = coordinates.positionInRoot().x
                onBounds(start, start + coordinates.size.width)
            }
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
                    translationY = lift.value.dp.toPx()
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

    // Pas d'animation de fond ici. Un anneau qui pulse ressemblait a une
    // notification, un degrade qui tourne attirait l'oeil en permanence : ce
    // bouton n'a rien a annoncer, il doit juste etre le plus evident de la
    // barre. Un degrade fixe, en diagonale, y suffit — c'est le degrade de
    // l'application, et il reste le seul de la barre.
    val sweep = Brush.linearGradient(
        colors = Brand.gradient,
        start = Offset(0f, 0f),
        end = Offset(140f, 140f),
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(56.dp)
            .scale(scale)
            .brandShadow(elevation = 18.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(sweep)
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
