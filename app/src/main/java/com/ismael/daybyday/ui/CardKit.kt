package com.ismael.daybyday.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.Prayer
import java.util.Locale

/**
 * De quoi sont faites les cartes de « Ma journée ».
 *
 * Le probleme que ce fichier resout : douze cartes blanches identiques, remplies
 * de petites pastilles toutes pareilles, ou rien ne disait de quoi on parlait ni
 * ce qui comptait. Une carte doit se reconnaitre avant d'etre lue.
 *
 * Trois regles, et tout le reste en decoule :
 *
 * 1. **Chaque carte a une couleur et un signe.** Pas une couleur decorative :
 *    la meme teinte tient l'icone de l'en-tete, le halo du coin, les pastilles
 *    choisies et les barres de la carte. On reconnait « Alimentation » a son
 *    ambre avant d'avoir lu le titre.
 * 2. **L'en-tete dit l'etat.** Sous le titre, une ligne de resume — « 9 h ·
 *    bien dormi », « 3 prieres sur 5 », « −14,90 € ». Une carte repliee doit
 *    encore renseigner, sinon la replier revient a effacer.
 * 3. **Une question, une reponse, une forme.** Un choix parmi trois est un
 *    selecteur segmente, pas trois pastilles ; huit verres d'eau sont huit
 *    verres, pas un compteur a fleches ; cinq prieres sont cinq perles. La
 *    forme doit ressembler a ce qu'elle mesure.
 */

/** L'identite visuelle d'une carte : son signe, sa teinte, et le halo de son coin. */
data class DayCardStyle(val emoji: String, val tint: Color, val glow: Color)

private val Indigo = Color(0xFF5B4DF0)
private val IndigoGlow = Color(0xFF3BA6FF)
private val Violet = Color(0xFF8B5CF6)
private val VioletGlow = Color(0xFFC4A0FF)
private val Mint = Color(0xFF10B981)
private val MintGlow = Color(0xFF5BE3B4)
private val Amber = Color(0xFFF59E0B)
private val AmberGlow = Color(0xFFFFD166)
private val Rose = Color(0xFFF2637F)
private val RoseGlow = Color(0xFFFFA9B8)
private val Sky = Color(0xFF2E9BF0)
private val SkyGlow = Color(0xFF8FD3FF)

/**
 * La teinte de chaque carte.
 *
 * Six teintes seulement, reprises dans un ordre tel que deux cartes voisines
 * n'ont jamais la meme. Douze couleurs differentes ne feraient pas douze
 * identites, seulement un nuancier : c'est la **repetition** d'une petite
 * palette qui donne a une application l'air d'avoir ete dessinee.
 */
fun cardStyle(card: DayCard): DayCardStyle = when (card) {
    DayCard.MOOD -> DayCardStyle("🎨", Indigo, IndigoGlow)
    DayCard.JOURNAL -> DayCardStyle("✍️", Indigo, IndigoGlow)
    DayCard.SLEEP -> DayCardStyle("🌙", Violet, VioletGlow)
    DayCard.ACTIVITY -> DayCardStyle("👟", Mint, MintGlow)
    DayCard.FOOD -> DayCardStyle("🍽️", Amber, AmberGlow)
    DayCard.HEALTH -> DayCardStyle("💊", Rose, RoseGlow)
    DayCard.SOCIAL -> DayCardStyle("👥", Sky, SkyGlow)
    DayCard.WORK -> DayCardStyle("💼", Indigo, IndigoGlow)
    DayCard.OUTSIDE -> DayCardStyle("🚪", Violet, VioletGlow)
    DayCard.MONEY -> DayCardStyle("💶", Mint, MintGlow)
    DayCard.PRAYER -> DayCardStyle("🕌", Amber, AmberGlow)
    DayCard.MEDIA -> DayCardStyle("📷", Rose, RoseGlow)
}

/**
 * Le halo d'une carte : deux taches de lumiere tres douces, une en bas a
 * droite dans la teinte de la carte, une plus petite en haut a droite dans sa
 * couleur compagne.
 *
 * Elles sont dessinees dans un `drawBehind`, donc **sous** le contenu et sans
 * rien recomposer. Elles ne bougent pas : un halo qui derive dans chacune des
 * douze cartes ferait douze animations pour un effet qu'on ne regarde pas.
 */
fun Modifier.cardGlow(style: DayCardStyle): Modifier = drawBehind {
    val big = Offset(size.width * 1.02f, size.height * 1.06f)
    val bigRadius = size.width * 0.62f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(style.tint.copy(alpha = 0.20f), Color.Transparent),
            center = big,
            radius = bigRadius,
        ),
        radius = bigRadius,
        center = big,
    )
    val small = Offset(size.width * 0.92f, -size.height * 0.10f)
    val smallRadius = size.width * 0.40f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(style.glow.copy(alpha = 0.22f), Color.Transparent),
            center = small,
            radius = smallRadius,
        ),
        radius = smallRadius,
        center = small,
    )
}

/**
 * Le titre d'une section a l'interieur d'une carte : la question posee.
 *
 * Il porte la teinte de la carte, en petit et espace. C'est lui qui remplace
 * les rangees de pastilles sans introduction — on ne pose pas une reponse sans
 * avoir pose la question.
 */
@Composable
fun CardSection(
    title: String,
    tint: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(Locale.FRANCE),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** Une des reponses d'un [SegmentedChoice]. */
data class Segment(val emoji: String, val label: String)

/**
 * Un choix parmi trois, en un seul objet.
 *
 * Trois pastilles rondes posees cote a cote ne disent pas qu'il faut en
 * choisir **une**. Un selecteur segmente le dit par sa forme : un seul cadre,
 * trois places, et une seule allumee. Toucher celle qui est deja choisie
 * l'eteint — comme partout ailleurs dans l'application.
 */
@Composable
fun SegmentedChoice(
    options: List<Segment>,
    selectedIndex: Int?,
    tint: Color,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    TileRow(
        options = options,
        tint = tint,
        isSelected = { it == selectedIndex },
        onTap = { onSelect(if (it == selectedIndex) null else it) },
        modifier = modifier,
    )
}

/**
 * Plusieurs reponses possibles, dans la meme forme qu'un choix unique.
 *
 * Utile quand les reponses vont ensemble et se comparent — « ta copine, tes
 * amis, ta famille » — la ou une rangee de pastilles rondes les disperserait.
 */
@Composable
fun MultiTiles(
    options: List<Segment>,
    selected: Set<Int>,
    tint: Color,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TileRow(
        options = options,
        tint = tint,
        isSelected = { it in selected },
        onTap = onToggle,
        modifier = modifier,
    )
}

/** Le cadre commun : un seul objet, N places, celles qui sont allumees. */
@Composable
private fun TileRow(
    options: List<Segment>,
    tint: Color,
    isSelected: (Int) -> Boolean,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.09f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = isSelected(index)
            val background by animateColorAsState(
                targetValue = if (selected) tint else Color.Transparent,
                animationSpec = tween(Motion.NORMAL),
                label = "fond",
            )
            val ink by animateColorAsState(
                targetValue = if (selected) {
                    readableOn(tint)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(Motion.NORMAL),
                label = "encre",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(background)
                    .clickable(onClickLabel = option.label) { onTap(index) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(option.emoji, fontSize = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ink,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp,
                )
            }
        }
    }
}

/**
 * Une ligne a cocher, pleine largeur.
 *
 * Pour ce qui se fait ou ne se fait pas dans la journee — une demarche, un
 * rendez-vous. Une liste de cases cochees se lit comme une liste de choses
 * faites ; les memes items en pastilles rondes se liraient comme des
 * etiquettes, c'est-a-dire comme un classement.
 */
@Composable
fun CheckRow(
    label: String,
    checked: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (checked) tint.copy(alpha = 0.14f) else tint.copy(alpha = 0.05f),
        animationSpec = tween(Motion.NORMAL),
        label = "fond",
    )
    val boxColor by animateColorAsState(
        targetValue = if (checked) tint else Color.Transparent,
        animationSpec = tween(Motion.NORMAL),
        label = "case",
    )
    val markScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = Motion.softSpring(),
        label = "coche",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(23.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(boxColor)
                .border(2.dp, if (checked) Color.Transparent else tint.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = readableOn(tint),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = markScale
                        scaleY = markScale
                        alpha = markScale
                    },
            )
        }
        Spacer(Modifier.width(13.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Une pastille de fait : « Marche », « Fast-food », « Ma copine ».
 *
 * Ce sont des faits qu'on coche, pas des choix exclusifs — d'ou la forme
 * ronde, differente du selecteur segmente. Elle prend la teinte de sa carte
 * plutot que le violet de l'application : c'est ce qui fait qu'une rangee de
 * pastilles appartient a sa carte au lieu de flotter.
 */
@Composable
fun CardChip(
    label: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(Motion.QUICK),
        label = "pression",
    )
    val background by animateColorAsState(
        targetValue = if (selected) tint else tint.copy(alpha = 0.10f),
        animationSpec = tween(Motion.NORMAL),
        label = "fond",
    )
    val ink by animateColorAsState(
        targetValue = if (selected) readableOn(tint) else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(Motion.NORMAL),
        label = "encre",
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = label,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = ink,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * Une mesure relevee par le telephone : les pas, le temps d'ecran.
 *
 * Un chiffre seul ne dit pas s'il est grand. La barre en dessous le situe par
 * rapport a un repere ordinaire — six mille pas, six heures d'ecran — sans
 * jamais parler d'objectif ni de reussite : l'application constate.
 */
@Composable
fun MetricRow(
    emoji: String,
    label: String,
    value: String?,
    caption: String,
    progress: Float?,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tint.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(emoji, fontSize = 16.sp)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = value ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else tint,
            )
        }
        if (progress != null) {
            Spacer(Modifier.height(11.dp))
            TrackBar(progress = progress, tint = tint)
        }
    }
}

/** La barre d'une mesure : un rail clair, une part remplie. */
@Composable
fun TrackBar(progress: Float, tint: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val filled by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "remplissage",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (filled <= 0f) return@Canvas
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(tint, tint.copy(alpha = 0.72f))),
                size = androidx.compose.ui.geometry.Size(size.width * filled, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
            )
        }
    }
}

/**
 * La nuit, dessinee.
 *
 * Un axe qui va de dix-huit heures a dix-huit heures : une nuit ordinaire y
 * tombe d'un seul tenant, au milieu, au lieu d'etre coupee par minuit comme
 * elle le serait sur un axe qui commence a zero heure. Deux reperes discrets
 * marquent minuit et six heures.
 */
@Composable
fun SleepBar(startMinutes: Int, endMinutes: Int, tint: Color, modifier: Modifier = Modifier) {
    val marks = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp),
    ) {
        val radius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        drawRoundRect(color = tint.copy(alpha = 0.14f), cornerRadius = radius)

        fun place(minutes: Int): Float = ((minutes - AXIS_START + 1440) % 1440) / 1440f

        val from = place(startMinutes)
        val to = place(endMinutes)
        // Une nuit qui repasserait par dix-huit heures dure plus de vingt-quatre
        // heures : elle n'existe pas, on la laisse alors vide plutot que de
        // dessiner une barre fausse.
        if (to > from) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(tint, tint.copy(alpha = 0.65f)),
                    startX = size.width * from,
                    endX = size.width * to,
                ),
                topLeft = Offset(size.width * from, 0f),
                size = androidx.compose.ui.geometry.Size(size.width * (to - from), size.height),
                cornerRadius = radius,
            )
        }

        listOf(0, 6 * 60, 12 * 60).forEach { hour ->
            val x = size.width * place(hour)
            drawLine(
                color = marks,
                start = Offset(x, size.height * 0.62f),
                end = Offset(x, size.height),
                strokeWidth = 1.5f,
            )
        }
    }
}

private const val AXIS_START = 18 * 60

/**
 * Les verres d'eau, en verres.
 *
 * Un compteur a deux fleches demande de lire un chiffre ; huit verres se
 * comptent d'un coup d'oeil, et se remplissent d'un seul doigt — toucher le
 * cinquieme verre en remplit cinq. Toucher le dernier verre plein vide tout.
 */
@Composable
fun WaterGlasses(count: Int?, tint: Color, onChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val filled = count ?: 0
    val shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 13.dp, bottomEnd = 13.dp)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(GLASSES) { index ->
            val on = index < filled
            val background by animateColorAsState(
                targetValue = if (on) tint else tint.copy(alpha = 0.12f),
                animationSpec = tween(Motion.NORMAL),
                label = "verre",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(shape)
                    .background(background)
                    .border(1.dp, tint.copy(alpha = 0.28f), shape)
                    .clickable(onClickLabel = "${index + 1} verre(s)") {
                        onChange(if (filled == index + 1) null else index + 1)
                    },
            )
        }
    }
}

private const val GLASSES = 8

/**
 * Les cinq prieres, en perles.
 *
 * Cinq grandes lignes a cocher occupaient la moitie de l'ecran pour cinq
 * oui-ou-non. Une rangee de perles dit la meme chose en une ligne, dans
 * l'ordre du jour, et se lit sans lire : on voit tout de suite ou on en est.
 *
 * Rien n'est vert ni rouge. L'application constate — elle ne felicite pas et
 * ne reproche pas.
 */
@Composable
fun PrayerBeads(
    mask: Int?,
    tint: Color,
    onToggle: (Prayer, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = mask ?: 0
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Prayer.entries.forEach { prayer ->
            val checked = done and prayer.bit != 0
            val background by animateColorAsState(
                targetValue = if (checked) tint else tint.copy(alpha = 0.10f),
                animationSpec = tween(Motion.NORMAL),
                label = "perle",
            )
            val markScale by animateFloatAsState(
                targetValue = if (checked) 1f else 0f,
                animationSpec = Motion.softSpring(),
                label = "coche",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClickLabel = prayer.label) { onToggle(prayer, !checked) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(background)
                        .border(1.5.dp, tint.copy(alpha = if (checked) 0f else 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = readableOn(tint),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                // Lue dans la couche : la coche rebondit sans
                                // rien faire remesurer.
                                scaleX = markScale
                                scaleY = markScale
                                alpha = markScale
                            },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = prayer.label,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (checked) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
