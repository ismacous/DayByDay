package com.ismael.daybyday.data

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Le placement libre des photos sur la page du journal.
 *
 * Une photo posee reste ou elle est : ecrire un paragraphe de plus ne la
 * deplace pas. C'est le contraire d'un traitement de texte, et c'est voulu —
 * on colle une photo sur une page de carnet, elle ne "flotte" pas dans le
 * texte.
 *
 * Tout est en points (dp) depuis le coin haut gauche de la page, et tout est
 * ici : ce fichier ne connait ni Android ni Compose, donc chaque regle se
 * teste directement.
 */
object Placement {

    /** Le pas de la grille d'aimantation, en points. */
    const val GRID = 12f

    /** Une photo ne peut pas devenir plus petite qu'une vignette. */
    const val MIN_SIZE = 56f

    /** Les angles remarquables sur lesquels la rotation vient se caler. */
    const val ANGLE_STEP = 15f

    /** Ecart en degres en deca duquel la rotation se cale sur l'angle rond. */
    const val ANGLE_TOLERANCE = 4f

    /** Largeur d'une photo qu'on vient de poser, en part de la largeur de page. */
    const val DEFAULT_WIDTH_RATIO = 0.55f

    fun snap(value: Float, step: Float = GRID): Float =
        if (step <= 0f) value else (value / step).roundToInt() * step

    /**
     * Cale la rotation sur le multiple de 15 le plus proche quand on en est
     * tout pres. Sans ca, une photo "droite" ne l'est jamais vraiment : le
     * doigt laisse toujours un ou deux degres.
     */
    fun snapAngle(degrees: Float): Float {
        val normalised = normaliseAngle(degrees)
        val nearest = (normalised / ANGLE_STEP).roundToInt() * ANGLE_STEP
        return if (abs(normalised - nearest) <= ANGLE_TOLERANCE) normaliseAngle(nearest) else normalised
    }

    fun normaliseAngle(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    /**
     * Deplace une photo. Elle peut deborder de la page — c'est utile pour une
     * image de fond a cheval sur le bord — mais jamais au point de sortir
     * entierement, sinon on ne peut plus la rattraper.
     */
    fun move(
        item: MediaItem,
        x: Float,
        y: Float,
        snapToGrid: Boolean,
        pageWidth: Float,
    ): MediaItem {
        val width = item.placedWidth
        val height = item.displayHeight
        val placedX = if (snapToGrid) snap(x) else x
        val placedY = if (snapToGrid) snap(y) else y
        val margin = 0.6f
        return item.copy(
            placedX = placedX.coerceIn(-width * margin, pageWidth - width * (1f - margin)),
            placedY = placedY.coerceAtLeast(-height * margin),
        )
    }

    /**
     * Redimensionne et fait tourner, en gardant les proportions de l'image.
     * Le centre ne bouge pas : agrandir une photo ne doit pas la faire fuir
     * vers le bas a droite.
     */
    fun resize(
        item: MediaItem,
        width: Float,
        rotation: Float,
        snapToGrid: Boolean,
        pageWidth: Float,
    ): MediaItem {
        val ratio = if (item.placedWidth > 0f) item.placedHeight / item.placedWidth else 1f
        val maxWidth = maxOf(MIN_SIZE, pageWidth * 2f)
        val target = (if (snapToGrid) snap(width) else width).coerceIn(MIN_SIZE, maxWidth)

        val centreX = (item.placedX ?: 0f) + item.placedWidth / 2f
        val centreY = (item.placedY ?: 0f) + item.displayHeight / 2f
        val newHeight = target * ratio
        val displayed = if (item.shape == MediaShape.RECTANGLE) newHeight else target

        return item.copy(
            placedWidth = target,
            placedHeight = newHeight,
            placedRotation = if (snapToGrid) snapAngle(rotation) else normaliseAngle(rotation),
            placedX = centreX - target / 2f,
            placedY = centreY - displayed / 2f,
        )
    }

    /**
     * Donne une place aux photos qui n'en ont pas : celles ajoutees avant que
     * le placement libre existe, et celle qu'on vient de choisir dans la
     * galerie. Elles se rangent en deux colonnes sous le debut du texte,
     * decalees les unes des autres pour rester saisissables.
     */
    fun autoPlace(
        item: MediaItem,
        index: Int,
        pageWidth: Float,
        topY: Float,
        aspectRatio: Float = 1f,
    ): MediaItem {
        val width = snap(pageWidth * DEFAULT_WIDTH_RATIO)
        val column = index % 2
        val row = index / 2
        val safeRatio = if (aspectRatio > 0f) aspectRatio else 1f
        val height = width * safeRatio
        return item.copy(
            placedX = snap(if (column == 0) GRID * 2f else pageWidth - width - GRID * 2f),
            placedY = snap(topY + row * (height + GRID * 3f)),
            placedWidth = width,
            placedHeight = height,
        )
    }

    /** Le bas de la photo la plus basse : la page doit descendre au moins jusque la. */
    fun lowestEdge(items: List<MediaItem>): Float =
        items.filter { it.isPlaced }
            .maxOfOrNull { (it.placedY ?: 0f) + it.displayHeight }
            ?: 0f
}
