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
    const val GRID = 14f

    /** Une photo ne peut pas devenir plus petite qu'une vignette. */
    const val MIN_SIZE = 56f

    /** Les angles remarquables sur lesquels la rotation vient se caler. */
    const val ANGLE_STEP = 15f

    /** Ecart en degres en deca duquel la rotation se cale sur l'angle rond. */
    const val ANGLE_TOLERANCE = 4f

    /** Largeur d'une photo qu'on vient de poser, en part de la largeur de page. */
    const val DEFAULT_WIDTH_RATIO = 0.55f

    /**
     * Le lignage de la page se mesure en pas de grille : deux pas entre deux
     * lignes d'ecriture, un pas de marge en haut. C'est ce qui fait tomber
     * chaque ligne d'ecriture pile sur une ligne de la grille — sinon les deux
     * quadrillages se croisent de travers des qu'on ouvre la grille.
     */
    const val LINE_STEPS = 2
    const val TOP_STEPS = 1

    /** L'ecart entre deux lignes d'ecriture, en points. */
    val lineSpacing: Float get() = GRID * LINE_STEPS

    /** La marge en haut du texte, en points. */
    val topMargin: Float get() = GRID * TOP_STEPS

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
     * La photo telle qu'elle doit apparaitre pendant qu'on la manipule.
     *
     * Les doigts donnent un centre, une largeur et un angle **bruts**, gardes
     * tels quels d'un evenement a l'autre ; la grille ne s'applique qu'au
     * resultat affiche. C'est essentiel : aimanter la position a chaque
     * evenement et repartir de la position aimantee ferait disparaitre tous
     * les petits deplacements, puisque chacun retomberait sur le meme point de
     * grille. La photo semblerait collee, ou revenir en place toute seule.
     *
     * Elle peut deborder de la page — c'est utile pour une image de fond a
     * cheval sur le bord — mais jamais au point de sortir entierement, sinon
     * on ne peut plus la rattraper.
     */
    fun apply(
        item: MediaItem,
        centreX: Float,
        centreY: Float,
        width: Float,
        rotation: Float,
        snapToGrid: Boolean,
        pageWidth: Float,
    ): MediaItem {
        val ratio = if (item.placedWidth > 0f) item.placedHeight / item.placedWidth else 1f
        val maxWidth = maxOf(MIN_SIZE, pageWidth * 2f)
        val wanted = width.coerceIn(MIN_SIZE, maxWidth)
        val finalWidth = (if (snapToGrid) snap(wanted) else wanted).coerceAtLeast(MIN_SIZE)
        val finalHeight = finalWidth * ratio
        val shown = if (item.shape == MediaShape.RECTANGLE) finalHeight else finalWidth

        val rawX = centreX - finalWidth / 2f
        val rawY = centreY - shown / 2f
        val placedX = if (snapToGrid) snap(rawX) else rawX
        val placedY = if (snapToGrid) snap(rawY) else rawY
        val margin = 0.6f

        return item.copy(
            placedX = placedX.coerceIn(
                -finalWidth * margin,
                maxOf(-finalWidth * margin, pageWidth - finalWidth * (1f - margin)),
            ),
            placedY = placedY.coerceAtLeast(-shown * margin),
            placedWidth = finalWidth,
            placedHeight = finalHeight,
            placedRotation = if (snapToGrid) snapAngle(rotation) else normaliseAngle(rotation),
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
