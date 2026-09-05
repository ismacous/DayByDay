package com.ismael.daybyday.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.ui.theme.Serif

/**
 * Les briques visuelles communes a toute l'application.
 *
 * Trois idees, et tout le reste en decoule :
 *
 * 1. **Une carte blanche sur un fond creme.** Pas de bordure, pas d'ombre
 *    lourde : c'est le simple ecart de teinte qui fait exister la carte. Une
 *    interface qui empile les traits fatigue ; une qui empile les surfaces
 *    respire.
 * 2. **Une typographie a deux voix.** Le sans-serif dit, la serif italique
 *    accentue. Jamais les deux pour la meme chose.
 * 3. **Le mouvement repond au doigt.** Ce qui se touche s'enfonce legerement,
 *    ce qui se choisit change de couleur en glissant. Des animations courtes,
 *    toujours les memes durees, pour que l'application ait un rythme et pas
 *    une collection d'effets.
 */

/** Les durees. Une seule serie, partout : c'est ce qui fait un rythme. */
object Motion {
    /** Reponse immediate a un doigt : enfoncement, apparition d'un etat. */
    const val QUICK = 140

    /** Changement d'etat visible : couleur d'un onglet, ouverture d'un panneau. */
    const val NORMAL = 260

    /** Un ressort doux, sans rebond exagere, pour ce qui bouge en taille. */
    fun <T> softSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

/**
 * Un titre d'ecran a deux voix : le debut en sans-serif, l'accent en serif
 * italique. C'est la signature typographique de l'application.
 *
 * Par exemple « Septembre » puis « 2026 », ou « Ma » puis « journée ».
 */
@Composable
fun ScreenTitle(
    text: String,
    accent: String? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append(text)
                    if (accent != null) {
                        append(" ")
                        withStyle(
                            SpanStyle(
                                fontFamily = Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Normal,
                            )
                        ) {
                            append(accent)
                        }
                    }
                },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Une carte de contenu. Blanche sur le creme du fond, coins genereux, et — si
 * on lui donne un [onClick] — qui s'enfonce sous le doigt.
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    padding: Dp = 18.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.98f else 1f,
        animationSpec = tween(Motion.QUICK),
        label = "pression",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = MaterialTheme.shapes.large,
        color = color,
        tonalElevation = 0.dp,
        shadowElevation = if (color == MaterialTheme.colorScheme.surface) 1.dp else 0.dp,
        onClick = onClick ?: {},
        enabled = onClick != null,
        interactionSource = interaction,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

/**
 * Le titre d'une section a l'interieur d'une carte : petit, espace, discret.
 * Il nomme sans crier, parce que c'est le contenu qui doit se voir.
 */
@Composable
fun SectionLabelText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Une pastille de choix. Elle change de couleur en glissant plutot qu'en
 * sautant : c'est ce qui fait la difference entre une interface qui repond et
 * une interface qui clignote.
 */
@Composable
fun SoftChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    leading: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(Motion.QUICK),
        label = "pression",
    )
    val background by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(Motion.NORMAL),
        label = "fond",
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            readableOn(accent)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(Motion.NORMAL),
        label = "texte",
    )

    Surface(
        modifier = modifier.scale(scale),
        shape = CircleShape,
        color = background,
        onClick = onClick,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            leading?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

/**
 * Le fond de l'application : le creme du carnet, avec un halo de la couleur
 * principale en haut. Sans lui, un fond uni sur toute la hauteur donne
 * l'impression d'une feuille de calcul.
 */
@Composable
fun ScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent,
                        )
                    )
                ),
        )
        content()
    }
}

/** Une pastille ronde de couleur, pour une journee ou une legende. */
@Composable
fun ColorDot(color: Color, size: Dp = 10.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

/** Un separateur qui ne coupe pas : plus clair au bord qu'au milieu. */
@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
