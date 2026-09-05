package com.ismael.daybyday.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaLayer
import com.ismael.daybyday.data.MediaShape
import com.ismael.daybyday.data.Placement
import java.io.File

/**
 * Les photos posees librement sur la page du journal.
 *
 * Une photo posee ne bouge plus : ecrire un paragraphe de plus au-dessus ne la
 * pousse pas vers le bas. C'est l'inverse d'un traitement de texte, et c'est
 * ce qui permet de composer une page — une image de fond avec le texte
 * par-dessus, une photo de travers dans un coin — au lieu de subir une
 * insertion qui reorganise tout.
 */

/** La forme dans laquelle l'image vient se ranger. */
fun MediaShape.toComposeShape(): Shape = when (this) {
    MediaShape.CIRCLE -> CircleShape
    MediaShape.SQUARE -> RoundedCornerShape(6.dp)
    MediaShape.RECTANGLE -> RoundedCornerShape(10.dp)
    // L'autocollant n'a pas de cadre : c'est tout l'interet.
    MediaShape.FREE -> RectangleShape
}

/**
 * Une photo a sa place sur la page. L'image remplit la forme en se recadrant
 * au centre : c'est le masque, sans avoir a poser une forme puis une image
 * dedans en deux temps.
 */
@Composable
fun PlacedPhoto(
    item: MediaItem,
    file: File,
    onSelect: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (!item.isPlaced) return
    val interaction = remember(item.id) { MutableInteractionSource() }

    Box(
        modifier = modifier
            .offset(x = (item.placedX ?: 0f).dp, y = (item.placedY ?: 0f).dp)
            .size(width = item.placedWidth.dp, height = item.displayHeight.dp)
            // La rotation vient apres la taille : elle tourne autour du centre
            // sans changer la place que la photo occupe dans la mise en page.
            .rotate(item.placedRotation)
            .clip(item.shape.toComposeShape())
            .then(
                if (onSelect == null) {
                    Modifier
                } else {
                    // Un simple clic, pas un detecteur de gestes : le clic
                    // laisse le doigt qui glisse faire defiler la page.
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClickLabel = "Modifier cette photo",
                        onClick = onSelect,
                    )
                }
            ),
    ) {
        if (item.shape == MediaShape.FREE) {
            // Un PNG detoure garde sa silhouette : on ne recadre pas, on ne
            // rogne pas, et la transparence reste. C'est ce qui en fait un
            // autocollant plutot qu'une photo dans un cadre.
            StickerImage(
                file = file,
                kind = item.kind,
                outline = if (item.stickerOutline) STICKER_OUTLINE else 0.dp,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            MediaImage(
                file = file,
                kind = item.kind,
                modifier = Modifier.fillMaxSize(),
                maxSize = 1280,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** L'epaisseur du contour blanc d'un autocollant. */
private val STICKER_OUTLINE = 5.dp

/**
 * Le cadre de manipulation de la photo choisie.
 *
 * Il est dessine par-dessus tout le reste, a la place exacte de la photo, meme
 * quand celle-ci est au fond sous le texte : c'est la seule facon d'attraper
 * une image que le champ de texte recouvre.
 *
 * Un doigt deplace, deux doigts redimensionnent et font tourner. Dans les deux
 * cas le geste est consomme ici, donc la page ne defile pas sous les doigts.
 */
@Composable
fun PhotoHandle(
    item: MediaItem,
    pageWidth: Float,
    snapToGrid: Boolean,
    onChange: (MediaItem) -> Unit,
) {
    if (!item.isPlaced) return
    val density = LocalDensity.current
    val accent = MaterialTheme.colorScheme.primary

    // Le detecteur de gestes n'est installe qu'une fois par photo : il garderait
    // sinon la photo telle qu'elle etait au premier appui. Or chaque evenement
    // n'apporte que le deplacement depuis le precedent, pas depuis le debut du
    // geste — l'image repartait donc de son point de depart a chaque image de
    // l'animation, et semblait revenir en place toute seule. Ces trois valeurs
    // sont donc relues a chaque fois, au lieu d'etre figees dans le detecteur.
    val live = rememberUpdatedState(item)
    val liveSnap = rememberUpdatedState(snapToGrid)
    val livePageWidth = rememberUpdatedState(pageWidth)

    // Le centre, la largeur et l'angle **bruts**, tels que les doigts les
    // laissent. La grille ne s'applique qu'a l'affichage : si on repartait de
    // la position aimantee a chaque evenement, chaque petit deplacement
    // retomberait sur le meme point de grille et la photo semblerait collee.
    var centreX by remember(item.id) {
        mutableStateOf((item.placedX ?: 0f) + item.placedWidth / 2f)
    }
    var centreY by remember(item.id) {
        mutableStateOf((item.placedY ?: 0f) + item.displayHeight / 2f)
    }
    var rawWidth by remember(item.id) { mutableStateOf(item.placedWidth) }
    var rawRotation by remember(item.id) { mutableStateOf(item.placedRotation) }

    Box(
        modifier = Modifier
            .offset(x = (item.placedX ?: 0f).dp, y = (item.placedY ?: 0f).dp)
            .size(width = item.placedWidth.dp, height = item.displayHeight.dp)
            .rotate(item.placedRotation)
            .border(2.dp, accent, item.shape.toComposeShape())
            .pointerInput(item.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    centreX += with(density) { pan.x.toDp().value }
                    centreY += with(density) { pan.y.toDp().value }
                    rawWidth *= zoom
                    rawRotation += rotation
                    onChange(
                        Placement.apply(
                            item = live.value,
                            centreX = centreX,
                            centreY = centreY,
                            width = rawWidth,
                            rotation = rawRotation,
                            snapToGrid = liveSnap.value,
                            pageWidth = livePageWidth.value,
                        )
                    )
                }
            },
    ) {
        // Quatre coins pleins : on voit tout de suite quelle photo repond aux
        // doigts, y compris quand elle est cachee sous le texte.
        listOf(
            Alignment.TopStart,
            Alignment.TopEnd,
            Alignment.BottomStart,
            Alignment.BottomEnd,
        ).forEach { corner ->
            Box(
                modifier = Modifier
                    .align(corner)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    }
}

/**
 * La grille d'aimantation, visible seulement pendant qu'on manipule une photo.
 * Laissee en permanence, elle transformerait une page d'ecriture en papier
 * millimetre.
 */
@Composable
fun PhotoGrid(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    Canvas(modifier = modifier) {
        val step = Placement.GRID.dp.toPx()
        if (step <= 0f) return@Canvas
        var x = step
        while (x < size.width) {
            drawLine(tint, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = step
        while (y < size.height) {
            drawLine(tint, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}

/**
 * Les reglages de la photo choisie, a la place de la barre de mise en forme :
 * on ne fait qu'une chose a la fois, donc une seule barre a la fois.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhotoToolsBar(
    item: MediaItem,
    snapToGrid: Boolean,
    onShape: (MediaShape) -> Unit,
    onOutline: (Boolean) -> Unit,
    onLayer: (MediaLayer) -> Unit,
    onSnap: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaShape.entries.forEach { shape ->
                    PhotoChip(
                        label = shape.label,
                        selected = item.shape == shape,
                        onClick = { onShape(shape) },
                    )
                }
                if (item.shape == MediaShape.FREE) {
                    PhotoChip(
                        label = if (item.stickerOutline) "Contour blanc" else "Sans contour",
                        selected = item.stickerOutline,
                        onClick = { onOutline(!item.stickerOutline) },
                    )
                }
                PhotoChip(
                    label = if (snapToGrid) "Aimant activé" else "Aimant coupé",
                    selected = snapToGrid,
                    onClick = { onSnap(!snapToGrid) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // "Au fond" et "au milieu" sont toutes deux sous le texte :
                // elles ne different que l'une par rapport a l'autre. Le dire
                // vaut mieux que de laisser deviner.
                Text(
                    text = "Profondeur — le texte s'écrit entre « au milieu » et « devant »",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MediaLayer.entries.forEach { layer ->
                        PhotoChip(
                            label = layer.label,
                            selected = item.layer == layer,
                            onClick = { onLayer(layer) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PhotoChip(
                    label = "Retirer de la page",
                    selected = false,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                )
                PhotoChip(label = "Terminé", selected = true, onClick = onDone)
            }
        }
    }
}

@Composable
private fun PhotoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.primary else tint,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
