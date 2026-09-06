package com.ismael.daybyday.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.dayByDayApp

/**
 * Choisir ce que montre l'ecran d'une journee, et dans quel ordre.
 *
 * L'ecran precedent etait une liste de lignes grises avec deux fleches et une
 * croix : il ne ressemblait a rien de ce qu'il reglait. Or c'est **exactement**
 * ce qu'il regle qu'il faut montrer — on ne choisit pas une ligne de texte, on
 * choisit une carte. Chaque rangee est donc la carte elle-meme en petit, avec
 * sa teinte, son signe et son halo.
 *
 * Trois decisions :
 *
 * 1. **On deplace en tirant, pas avec des fleches.** L'ancienne version s'en
 *    privait pour que le geste ne se batte pas avec le defilement : il ne s'en
 *    bat plus, parce qu'il faut **maintenir le doigt** avant de tirer. Et
 *    tirer une carte a sa place dit ce qu'on fait, la ou deux fleches
 *    demandent de compter les crans.
 * 2. **Rien ne disparait.** Une carte retiree descend dans « Rangées », qui est
 *    la meme liste : elle **glisse** jusque-la sous les yeux, et remonte
 *    pareil. Rien ne se supprime, et le mouvement le prouve.
 * 3. **Les hauteurs sont fixes.** C'est ce qui permet au deplacement de se
 *    calculer sans mesurer quoi que ce soit : un cran vaut une hauteur de
 *    rangee plus l'ecart. Une liste a hauteurs variables demanderait de lire
 *    la position de chaque element a chaque image.
 */
@Composable
fun OrganizeCardsScreen(onBack: () -> Unit) {
    val prefs = LocalContext.current.dayByDayApp.prefs

    var order by remember { mutableStateOf(prefs.dayCardOrder) }
    var hidden by remember { mutableStateOf(prefs.hiddenDayCards) }

    fun apply(newOrder: List<DayCard>, newHidden: Set<DayCard>) {
        order = newOrder
        hidden = newHidden
        prefs.dayCardOrder = newOrder
        prefs.hiddenDayCards = newHidden
    }

    val shown = order.filterNot { it in hidden }
    val masked = order.filter { it in hidden }

    // La carte qu'on tient, et de combien on l'a tiree depuis sa place.
    var dragged by remember { mutableStateOf<DayCard?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val step = remember(density) { with(density) { (ROW_HEIGHT + ROW_GAP).toPx() } }

    /**
     * Echange la carte tiree avec sa voisine, une fois par cran franchi.
     *
     * Rend `false` quand il n'y a plus de voisine — c'est ce qui arrete la
     * boucle de l'appelant. Sans ce retour, tirer au-dela de la premiere ou de
     * la derniere carte bouclerait a l'infini : la condition resterait vraie
     * alors que rien ne bougerait plus.
     */
    fun shift(card: DayCard, direction: Int): Boolean {
        val visible = order.filterNot { it in hidden }
        val at = visible.indexOf(card)
        val target = at + direction
        if (at < 0 || target !in visible.indices) return false
        val from = order.indexOf(card)
        val to = order.indexOf(visible[target])
        val next = order.toMutableList()
        next.add(to, next.removeAt(from))
        apply(next, hidden)
        dragOffset -= direction * step
        return true
    }

    val rows = buildList {
        shown.forEach { add(OrganizeRow.Card(it, put = false)) }
        add(OrganizeRow.Divider)
        masked.forEach { add(OrganizeRow.Card(it, put = true)) }
    }

    ScreenBackground(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ROW_GAP),
        ) {
            item(key = "titre") {
                Column {
                    Spacer(Modifier.height(10.dp))
                    ScreenTitle(
                        text = "Organiser",
                        accent = "ma journée",
                        trailing = {
                            RoundIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                label = "Retour",
                                onClick = onBack,
                            )
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Maintiens une carte pour la déplacer. La croix la range " +
                            "plus bas sans rien effacer : ce que tu y as noté reste, et " +
                            "elle revient entière dès que tu la remets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    OrganizeLabel("Dans ma journée", shown.size)
                }
            }

            items(
                items = rows,
                key = { row ->
                    when (row) {
                        is OrganizeRow.Card -> row.card.key
                        OrganizeRow.Divider -> "separateur"
                    }
                },
            ) { row ->
                when (row) {
                    OrganizeRow.Divider -> {
                        Column(modifier = Modifier.animateItem()) {
                            Spacer(Modifier.height(14.dp))
                            OrganizeLabel("Rangées", masked.size)
                        }
                    }
                    is OrganizeRow.Card -> {
                        val card = row.card
                        val holding = dragged == card
                        val lift by animateFloatAsState(
                            targetValue = if (holding) 1f else 0f,
                            animationSpec = tween(Motion.QUICK),
                            label = "prise",
                        )
                        OrganizeCardRow(
                            card = card,
                            put = row.put,
                            onToggle = {
                                apply(order, if (row.put) hidden - card else hidden + card)
                            },
                            modifier = Modifier
                                // La carte tenue ne suit pas l'animation de
                                // placement : elle est deja menee par le doigt.
                                .then(if (holding) Modifier.zIndex(1f) else Modifier.animateItem())
                                .graphicsLayer {
                                    val held = lift
                                    if (holding) translationY = dragOffset
                                    val grow = 1f + 0.04f * held
                                    scaleX = grow
                                    scaleY = grow
                                    shadowElevation = 22.dp.toPx() * held
                                    shape = RoundedCornerShape(20.dp)
                                    clip = false
                                }
                                .then(
                                    if (row.put || card.essential) {
                                        Modifier
                                    } else {
                                        Modifier.pointerInput(card) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    dragged = card
                                                    dragOffset = 0f
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragOffset += amount.y
                                                    // Un cran franchi, un
                                                    // echange : c'est la
                                                    // hauteur fixe des rangees
                                                    // qui rend ce calcul juste.
                                                    while (dragOffset > step / 2f &&
                                                        shift(card, 1)
                                                    ) {
                                                        // Rien de plus : le
                                                        // decalage est retire
                                                        // par `shift`.
                                                    }
                                                    while (dragOffset < -step / 2f &&
                                                        shift(card, -1)
                                                    ) {
                                                        // Idem dans l'autre sens.
                                                    }
                                                },
                                                onDragEnd = {
                                                    dragged = null
                                                    dragOffset = 0f
                                                },
                                                onDragCancel = {
                                                    dragged = null
                                                    dragOffset = 0f
                                                },
                                            )
                                        }
                                    }
                                ),
                        )
                    }
                }
            }

            item(key = "bas") { Spacer(Modifier.height(48.dp)) }
        }
    }
}

/** Une rangee de l'ecran : une carte, ou le trait qui separe les deux sections. */
private sealed interface OrganizeRow {
    data class Card(val card: DayCard, val put: Boolean) : OrganizeRow
    data object Divider : OrganizeRow
}

@Composable
private fun OrganizeLabel(text: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Une carte en petit : sa teinte, son signe, son halo, son titre.
 *
 * C'est la meme identite que sur l'ecran d'une journee — meme couleur, meme
 * emoji, meme halo dans le coin. On reconnait ce qu'on deplace sans lire.
 * Rangee, elle perd ses couleurs et devient grise : elle est la, mais elle
 * n'est plus dans la journee.
 */
@Composable
private fun OrganizeCardRow(
    card: DayCard,
    put: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = cardStyle(card)
    val tint = if (put) MaterialTheme.colorScheme.onSurfaceVariant else style.tint

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (put) Modifier else Modifier.cardGlow(style))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = if (put) 0.08f else 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = style.emoji,
                fontSize = 19.sp,
                modifier = Modifier.graphicsLayer { alpha = if (put) 0.45f else 1f },
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (put) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = card.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))

        when {
            card.essential -> RowAction(
                icon = Icons.Default.Lock,
                label = "Toujours là",
                tint = tint.copy(alpha = 0.5f),
                onClick = null,
            )
            put -> RowAction(
                icon = Icons.Default.Add,
                label = "Remettre ${card.title}",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onToggle,
            )
            else -> {
                RowAction(
                    icon = Icons.Default.Clear,
                    label = "Ranger ${card.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggle,
                )
                Spacer(Modifier.width(4.dp))
                // La poignee ne fait rien elle-meme : c'est un panneau. Elle dit
                // que la carte se prend, ce qu'un maintien du doigt ne peut pas
                // annoncer tout seul.
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RowAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = label, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * La hauteur d'une rangee et l'ecart entre deux.
 *
 * Fixes, et c'est ce qui rend le deplacement simple : un cran vaut la somme des
 * deux, donc on sait qu'il faut echanger deux cartes sans avoir a mesurer quoi
 * que ce soit pendant le geste.
 */
private val ROW_HEIGHT = 76.dp

private val ROW_GAP = 10.dp
