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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
 * Les reglages d'une photo posee sur la page.
 *
 * C'etait un tas de pastilles : quatre formes, un contour, un aimant et trois
 * profondeurs, tous de la meme taille et de la meme couleur, sur trois rangees
 * qui se ressemblaient. Rien ne disait quelles pastilles allaient ensemble, ni
 * lesquelles s'excluaient — et le blanc et le violet du theme tombaient sur un
 * papier ivoire comme un morceau d'une autre application.
 *
 * Trois regles le remettent d'aplomb, et ce sont celles des cartes de « Ma
 * journee » :
 * 1. **Une question, une reponse, une forme.** Quatre formes qui s'excluent
 *    sont un choix segmente, pas quatre pastilles ; un oui-ou-non est un
 *    interrupteur, pas une pastille qui s'allume.
 * 2. **Chaque question porte son intitule.** « Profondeur » etait le seul a en
 *    avoir un, et c'est pour ca qu'il etait le seul comprehensible.
 * 3. **Les couleurs viennent du papier**, comme la barre d'outils du journal :
 *    sur l'ardoise, du blanc sur du sombre disparaissait.
 */
@Composable
fun PhotoToolsBar(
    item: MediaItem,
    snapToGrid: Boolean,
    paper: Color,
    ink: Color,
    onShape: (MediaShape) -> Unit,
    onOutline: (Boolean) -> Unit,
    onLayer: (MediaLayer) -> Unit,
    onSnap: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val surface = JournalPaper.shade(paper, 0.06f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PhotoSection(title = "Forme", ink = ink) {
            PhotoSegmented(
                labels = MediaShape.entries.map { it.label },
                selected = MediaShape.entries.indexOf(item.shape),
                paper = paper,
                ink = ink,
                onSelect = { onShape(MediaShape.entries[it]) },
            )
        }

        if (item.shape == MediaShape.FREE) {
            // Un contour blanc n'a de sens que sur un detourage : le proposer
            // sur un rectangle, c'est proposer de border un bord.
            PhotoSwitch(
                label = "Contour blanc",
                hint = "Detoure l'autocollant comme un sticker",
                checked = item.stickerOutline,
                ink = ink,
                onChange = onOutline,
            )
        }

        PhotoSection(
            title = "Profondeur",
            hint = "Le texte s'ecrit entre « au milieu » et « devant »",
            ink = ink,
        ) {
            PhotoSegmented(
                labels = MediaLayer.entries.map { it.label },
                selected = MediaLayer.entries.indexOf(item.layer),
                paper = paper,
                ink = ink,
                onSelect = { onLayer(MediaLayer.entries[it]) },
            )
        }

        PhotoSwitch(
            label = "Aimant",
            hint = "La photo se cale sur la grille de la page",
            checked = snapToGrid,
            ink = ink,
            onChange = onSnap,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Retirer de la page",
                style = MaterialTheme.typography.labelLarge,
                color = PHOTO_DELETE_RED,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClickLabel = "Retirer cette photo", onClick = onDelete)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Terminé",
                style = MaterialTheme.typography.labelLarge,
                color = readableOn(JournalPaper.shade(paper, 0.82f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(JournalPaper.shade(paper, 0.82f))
                    .clickable(onClickLabel = "Terminé", onClick = onDone)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            )
        }
    }
}

/** Un intitule, sa precision au besoin, et la reponse dessous. */
@Composable
private fun PhotoSection(
    title: String,
    ink: Color,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = ink.copy(alpha = 0.7f),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.45f),
            )
        }
        content()
    }
}

/**
 * Un choix parmi plusieurs, dans une seule piste.
 *
 * La piste dit « une seule de ces reponses » avant meme qu'on lise les
 * intitules — ce que des pastilles separees ne disent jamais.
 */
@Composable
private fun PhotoSegmented(
    labels: List<String>,
    selected: Int,
    paper: Color,
    ink: Color,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JournalPaper.shade(paper, 0.10f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val chosen = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (chosen) paper else Color.Transparent)
                    .clickable(onClickLabel = label) { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = if (chosen) 1f else 0.6f),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Un oui-ou-non : un interrupteur, jamais une pastille qui s'allume. */
@Composable
private fun PhotoSwitch(
    label: String,
    hint: String,
    checked: Boolean,
    ink: Color,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = label) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = ink,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.45f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                uncheckedThumbColor = ink.copy(alpha = 0.5f),
                uncheckedTrackColor = ink.copy(alpha = 0.10f),
                uncheckedBorderColor = ink.copy(alpha = 0.20f),
            ),
        )
    }
}

/** Le rouge de « ca s'efface », le meme que partout ailleurs. */
private val PHOTO_DELETE_RED = Color(0xFFCF4238)
