package com.ismael.daybyday.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.ui.theme.Brand
import com.ismael.daybyday.ui.theme.Serif
import com.ismael.daybyday.ui.theme.accentSerif
import kotlinx.coroutines.delay

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
 * Le titre d'un onglet, qui **survit** au changement de page.
 *
 * Avant, chaque ecran dessinait le sien : passer du bilan a l'argent detruisait
 * « Mon bilan » pour reconstruire « Mon argent », et tout l'en-tete clignotait
 * alors que les deux titres sont au meme endroit et commencent par le meme mot.
 *
 * Ici le titre vit **au-dessus** de la navigation : il ne disparait jamais. Les
 * deux mots sont animes separement, donc « Mon » ne bouge pas du tout entre le
 * bilan et l'argent — seul le second mot glisse et se remplace. C'est ce que le
 * regard attend : ce qui ne change pas ne doit pas bouger.
 */
@Composable
fun MorphingTitle(
    text: String,
    accent: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Les deux mots s'alignent sur leur **ligne d'ecriture**, pas sur
            // le bas de leur boite. Ce sont deux polices differentes : la
            // serif descend plus bas que la sans-serif, donc aligner les bas
            // faisait flotter « journée » sous « Ma ». Le titre en un seul
            // texte ([ScreenTitle]) n'avait pas ce defaut, parce que c'est le
            // paragraphe qui s'en occupait.
            Row(verticalAlignment = Alignment.Bottom) {
                MorphingWord(word = text, modifier = Modifier.alignByBaseline()) { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(9.dp))
                MorphingWord(word = accent, modifier = Modifier.alignByBaseline()) { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displaySmall.accentSerif(),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                }
            }
            MorphingWord(word = subtitle.orEmpty()) { value ->
                if (value.isNotEmpty()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Un mot qui se remplace en glissant vers le haut, le suivant arrivant par le
 * bas. Un mot inchange n'est pas anime du tout : `AnimatedContent` ne rejoue
 * rien quand la cible est la meme.
 */
@Composable
private fun MorphingWord(
    word: String,
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit,
) {
    AnimatedContent(
        targetState = word,
        modifier = modifier,
        transitionSpec = {
            (
                fadeIn(tween(Motion.NORMAL)) +
                    slideInVertically(tween(Motion.NORMAL)) { it / 2 }
                ) togetherWith (
                fadeOut(tween(Motion.QUICK)) +
                    slideOutVertically(tween(Motion.NORMAL)) { -it / 2 }
                )
        },
        label = "mot",
    ) { value ->
        content(value)
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
            .scale(scale)
            // Une ombre legere mais teintee : la carte flotte au-dessus du
            // fond au lieu d'y etre collee.
            .brandShadow(elevation = 10.dp, shape = MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = color,
        tonalElevation = 0.dp,
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
 * Le fond de l'application.
 *
 * Pas un aplat : trois halos de couleur, tres doux, qui derivent lentement les
 * uns par rapport aux autres. On ne les regarde pas — on les sent. C'est la
 * difference entre une page qui attend et une page qui vit.
 *
 * Chaque halo vit dans sa propre couche et n'est peint qu'une fois ; la derive
 * ne fait que la deplacer. Voir [Halo] : les avoir dessines ensemble a chaque
 * image rendait toute l'application saccadee.
 *
 * Les halos restent pales : le contenu doit rester la chose la plus lisible de
 * l'ecran, et les quatre couleurs des journees les seules taches franches.
 */
/**
 * Vrai quand on est deja a l'interieur d'un fond. Sert a ce que le fond global
 * de l'application, pose une fois derriere la navigation, ne soit pas repeint
 * une deuxieme fois par chaque ecran : les ecrans gardent leur appel, il ne
 * fait plus rien qu'une boite. Deux fonds superposes, ce serait deux fois le
 * meme travail a chaque image, et des halos deux fois plus fonces.
 */
private val LocalInsideBackground = compositionLocalOf { false }

@Composable
fun ScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    if (LocalInsideBackground.current) {
        Box(modifier = modifier, content = content)
        return
    }

    val base = MaterialTheme.colorScheme.background
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Sur fond clair, il faut de la matiere pour que ca se voie : a 20 % de
    // transparence les halos etaient invisibles. Ils sont maintenant francs en
    // haut de l'ecran, la ou il n'y a pas encore de carte, et s'eteignent vers
    // le bas pour ne jamais gener la lecture.
    val strength = if (dark) 0.55f else 0.75f

    val drift = rememberInfiniteTransition(label = "halos")
    // Volontairement une State et pas un `by` : la valeur ne doit **jamais**
    // etre lue pendant la composition. Elle n'est lue que dans les blocs
    // `graphicsLayer` des halos, ou elle ne coute qu'un deplacement d'image
    // deja peinte.
    val phase = drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "derive",
    )

    // Le voile du bas ne bouge pas : il est calcule une fois pour toutes.
    val veil = remember(base) {
        Brush.verticalGradient(0.18f to Color.Transparent, 0.62f to base)
    }

    BoxWithConstraints(
        modifier = modifier
            .background(base)
            // Les halos debordent volontiers de l'ecran : sans ca, ils
            // agrandiraient la zone a repeindre.
            .clipToBounds(),
    ) {
        val w = maxWidth
        val h = maxHeight

        Halo(
            color = Brand.Primary.copy(alpha = strength),
            diameter = w * 1.9f,
            centerX = w * 0.05f,
            centerY = -h * 0.02f,
            driftX = w * 0.18f,
            driftY = 0.dp,
            phase = phase,
        )
        Halo(
            color = Brand.Accent.copy(alpha = strength * 0.75f),
            diameter = w * 1.6f,
            centerX = w * 1.05f,
            centerY = h * 0.10f,
            driftX = -w * 0.18f,
            driftY = h * 0.06f,
            phase = phase,
        )
        Halo(
            color = Brand.Playful.copy(alpha = strength * 0.7f),
            diameter = w * 1.3f,
            centerX = w * 0.30f,
            centerY = h * 0.30f,
            driftX = w * 0.30f,
            driftY = 0.dp,
            phase = phase,
        )

        // Le bas de l'ecran revient au calme : les couleurs restent en haut,
        // la ou le regard arrive, et laissent les cartes tranquilles.
        Box(modifier = Modifier.matchParentSize().background(veil))

        CompositionLocalProvider(LocalInsideBackground provides true) {
            content()
        }
    }
}

/**
 * Une tache de couleur du fond, et la raison pour laquelle elle est un
 * composant a part.
 *
 * Les trois halos etaient dessines ensemble dans un `Canvas` plein ecran, avec
 * leur position calculee a partir de la valeur animee. Consequence : quatre
 * degrades plein ecran recalcules **a chaque image**, en permanence, sur tous
 * les ecrans de l'application. Rien ne saccadait a cause de l'element qu'on
 * regardait — c'est le fond qui mangeait le temps de tout le monde.
 *
 * Ici chaque halo est peint une fois dans sa propre couche, et l'animation ne
 * fait que la **deplacer** : le telephone sait faire ca sans rien repeindre.
 */
@Composable
private fun Halo(
    color: Color,
    diameter: Dp,
    centerX: Dp,
    centerY: Dp,
    driftX: Dp,
    driftY: Dp,
    phase: androidx.compose.runtime.State<Float>,
) {
    val brush = remember(color) { Brush.radialGradient(listOf(color, Color.Transparent)) }
    val density = LocalDensity.current
    val driftXPx = remember(driftX, density) { with(density) { driftX.toPx() } }
    val driftYPx = remember(driftY, density) { with(density) { driftY.toPx() } }

    Box(
        modifier = Modifier
            .offset(x = centerX - diameter / 2, y = centerY - diameter / 2)
            .size(diameter)
            .graphicsLayer {
                // La lecture a lieu ici, dans le bloc de la couche : ni
                // recomposition, ni remesure, ni repeinture — juste un
                // glissement.
                translationX = driftXPx * phase.value
                translationY = driftYPx * phase.value
            }
            .background(brush),
    )
}

/**
 * L'ombre de la marque : teintee de la couleur de l'application plutot que
 * grise. Une ombre grise pose un objet sur une feuille ; une ombre coloree le
 * fait flotter dans la lumiere de la page.
 */
fun Modifier.brandShadow(
    elevation: Dp = 16.dp,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color = Brand.Primary,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = color.copy(alpha = 0.35f),
    spotColor = color.copy(alpha = 0.45f),
)

/**
 * La carte forte : celle qu'on voit en premier sur un ecran. Elle porte le
 * degrade de la marque, une ombre de sa propre couleur, et un halo clair en
 * haut a droite qui lui donne du volume.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    colors: List<Color> = Brand.gradient,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = tween(Motion.QUICK),
        label = "pression",
    )
    val shape = MaterialTheme.shapes.extraLarge

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .brandShadow(shape = shape, color = colors.first())
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClickLabel = onClickLabel,
                        onClick = onClick,
                    )
                }
            ),
    ) {
        // Le reflet : un halo clair en haut a droite. Sans lui, un degrade
        // reste une bande de couleur ; avec lui, la carte a un volume.
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.85f, -size.height * 0.15f),
                    radius = size.width * 0.75f,
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.85f, -size.height * 0.15f),
            )
        }
        Column(modifier = Modifier.padding(22.dp), content = content)
    }
}

/**
 * Une entree en scene. Les cartes ne sont pas la d'un coup : elles montent et
 * apparaissent, decalees les unes des autres. C'est ce qui donne l'impression
 * que l'ecran se compose devant soi au lieu d'etre affiche.
 */
@Composable
fun Appear(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "entree",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 40f
            },
    ) {
        content()
    }
}

/**
 * Le grand anneau de progression : le point chaud d'un ecran de bilan.
 *
 * Un chiffre seul ne dit rien ; le meme chiffre au centre d'un anneau qui se
 * remplit se lit d'un coup d'oeil, et l'animation de remplissage donne envie de
 * regarder. C'est la piece qui manque a la plupart des ecrans de statistiques.
 */
@Composable
fun ScoreRing(
    progress: Float,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
    track: Color = Color.White.copy(alpha = 0.22f),
    colors: List<Color> = listOf(Color.White, Color.White.copy(alpha = 0.75f)),
    valueColor: Color = Color.White,
) {
    // L'anneau se remplit a l'ouverture de l'ecran : c'est ce mouvement, pas le
    // chiffre, qui accroche le regard.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(progress) { shown = true }
    val filled by animateFloatAsState(
        targetValue = if (shown) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "remplissage",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = this.size.minDimension * 0.09f
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                this.size.width - stroke,
                this.size.height - stroke,
            )
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (filled > 0f) {
                drawArc(
                    brush = Brush.linearGradient(colors),
                    startAngle = -90f,
                    sweepAngle = 360f * filled,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = valueColor,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = valueColor.copy(alpha = 0.8f),
            )
        }
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
