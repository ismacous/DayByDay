package com.ismael.daybyday

import com.ismael.daybyday.data.MediaItem
import com.ismael.daybyday.data.MediaShape
import com.ismael.daybyday.data.Placement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le placement libre des photos sur la page du journal.
 *
 * Une photo posee doit rester exactement ou on l'a mise, et redevenir
 * saisissable meme quand on l'a poussee vers un bord : c'est tout ce que ces
 * regles protegent.
 */
class PlacementTest {

    private val page = 360f

    private fun photo(
        x: Float = 100f,
        y: Float = 100f,
        width: Float = 120f,
        height: Float = 90f,
        rotation: Float = 0f,
        shape: MediaShape = MediaShape.RECTANGLE,
    ) = MediaItem(
        id = 1,
        epochDay = 20000,
        relativePath = "photo.jpg",
        kindKey = 0,
        placedX = x,
        placedY = y,
        placedWidth = width,
        placedHeight = height,
        placedRotation = rotation,
        shapeKey = shape.key,
    )

    // --- La grille --------------------------------------------------------

    @Test
    fun `l aimant colle la position sur la grille`() {
        val pas = Placement.GRID

        assertEquals(pas, Placement.snap(pas * 0.6f), 0.01f)
        assertEquals(0f, Placement.snap(pas * 0.4f), 0.01f)
        assertEquals(pas * 3f, Placement.snap(pas * 3.1f), 0.01f)
    }

    @Test
    fun `sans aimant la position est gardee telle quelle`() {
        val posee = Placement.apply(
            item = photo(),
            centreX = 197.4f,
            centreY = 96.9f,
            width = 120f,
            rotation = 0f,
            snapToGrid = false,
            pageWidth = page,
        )

        assertEquals(137.4f, posee.placedX!!, 0.01f)
        assertEquals(51.9f, posee.placedY!!, 0.01f)
    }

    @Test
    fun `avec aimant la position tombe sur la grille`() {
        val posee = Placement.apply(
            item = photo(),
            centreX = 197.4f,
            centreY = 96.9f,
            width = 120f,
            rotation = 0f,
            snapToGrid = true,
            pageWidth = page,
        )

        assertEquals(0f, posee.placedX!! % Placement.GRID, 0.01f)
        assertEquals(0f, posee.placedY!! % Placement.GRID, 0.01f)
    }

    @Test
    fun `un petit deplacement finit par sortir de la case de depart`() {
        // Le piege : aimanter le resultat puis repartir de lui ferait
        // retomber chaque petit pas sur le meme point de grille, et la photo
        // semblerait collee. Le centre brut, lui, avance vraiment.
        val depart = photo(x = 0f, y = 0f)
        var centreX = depart.placedWidth / 2f
        var posee = depart

        repeat(6) {
            centreX += Placement.GRID / 3f
            posee = Placement.apply(
                item = depart,
                centreX = centreX,
                centreY = depart.displayHeight / 2f,
                width = depart.placedWidth,
                rotation = 0f,
                snapToGrid = true,
                pageWidth = page,
            )
        }

        assertTrue(posee.placedX!! > 0f)
    }

    // --- Ne jamais perdre une photo ---------------------------------------

    @Test
    fun `une photo poussee au dela du bord reste rattrapable`() {
        val posee = Placement.apply(
            item = photo(),
            centreX = 5000f,
            centreY = 5000f,
            width = 120f,
            rotation = 0f,
            snapToGrid = false,
            pageWidth = page,
        )

        // Il en reste toujours un morceau dans la page, sinon plus moyen de la
        // reprendre au doigt.
        assertTrue(posee.placedX!! < page)
        assertTrue(posee.placedX!! + posee.placedWidth > 0f)
    }

    @Test
    fun `une photo poussee vers le haut ne disparait pas non plus`() {
        val posee = Placement.apply(
            item = photo(),
            centreX = -5000f,
            centreY = -5000f,
            width = 120f,
            rotation = 0f,
            snapToGrid = false,
            pageWidth = page,
        )

        assertTrue(posee.placedX!! + posee.placedWidth > 0f)
        assertTrue(posee.placedY!! + posee.displayHeight > 0f)
    }

    @Test
    fun `une photo ne devient jamais plus petite qu une vignette`() {
        val minuscule = Placement.apply(
            item = photo(),
            centreX = 160f,
            centreY = 145f,
            width = 2f,
            rotation = 0f,
            snapToGrid = false,
            pageWidth = page,
        )

        assertEquals(Placement.MIN_SIZE, minuscule.placedWidth, 0.01f)
    }

    // --- Redimensionner ---------------------------------------------------

    @Test
    fun `agrandir garde les proportions`() {
        val plusGrand = Placement.apply(
            item = photo(width = 120f, height = 90f),
            centreX = 160f,
            centreY = 145f,
            width = 240f,
            rotation = 0f,
            snapToGrid = false,
            pageWidth = page,
        )

        assertEquals(240f, plusGrand.placedWidth, 0.01f)
        assertEquals(180f, plusGrand.placedHeight, 0.01f)
    }

    @Test
    fun `agrandir ne deplace pas le centre`() {
        val depart = photo(x = 100f, y = 100f, width = 120f, height = 90f)
        val centreX = depart.placedX!! + depart.placedWidth / 2f
        val centreY = depart.placedY!! + depart.displayHeight / 2f

        val plusGrand = Placement.apply(
            item = depart,
            centreX = centreX,
            centreY = centreY,
            width = 240f,
            rotation = 0f,
            snapToGrid = false,
            pageWidth = page,
        )

        assertEquals(centreX, plusGrand.placedX!! + plusGrand.placedWidth / 2f, 0.01f)
        assertEquals(centreY, plusGrand.placedY!! + plusGrand.displayHeight / 2f, 0.01f)
    }

    @Test
    fun `un cercle reste aussi haut que large`() {
        val rond = photo(width = 120f, height = 90f, shape = MediaShape.CIRCLE)

        assertEquals(rond.placedWidth, rond.displayHeight, 0.01f)
    }

    // --- Tourner ----------------------------------------------------------

    @Test
    fun `une photo presque droite se remet droite`() {
        assertEquals(0f, Placement.snapAngle(2f), 0.01f)
        assertEquals(15f, Placement.snapAngle(16f), 0.01f)
        assertEquals(0f, Placement.snapAngle(359f), 0.01f)
    }

    @Test
    fun `une inclinaison voulue est respectee`() {
        // Sept degres, ce n'est pas une main qui tremble : c'est une photo
        // posee de travers expres.
        assertEquals(7f, Placement.snapAngle(7f), 0.01f)
    }

    @Test
    fun `l angle reste entre zero et un tour`() {
        assertEquals(350f, Placement.normaliseAngle(-10f), 0.01f)
        assertEquals(10f, Placement.normaliseAngle(370f), 0.01f)
    }

    // --- Ranger celles qui n'ont pas de place -----------------------------

    @Test
    fun `une photo sans place en recoit une, a ses proportions`() {
        val nowhere = photo(width = 0f, height = 0f).copy(placedX = null, placedY = null)

        val placed = Placement.autoPlace(nowhere, index = 0, pageWidth = page, topY = 40f, aspectRatio = 1.5f)

        assertTrue(placed.isPlaced)
        assertEquals(1.5f, placed.placedHeight / placed.placedWidth, 0.01f)
        assertTrue(placed.placedX!! >= 0f)
        assertTrue(placed.placedX!! + placed.placedWidth <= page)
    }

    @Test
    fun `deux photos rangees d affilee ne se superposent pas`() {
        val nowhere = photo().copy(placedX = null, placedY = null)

        val first = Placement.autoPlace(nowhere, index = 0, pageWidth = page, topY = 40f)
        val second = Placement.autoPlace(nowhere, index = 1, pageWidth = page, topY = 40f)

        assertTrue(first.placedX != second.placedX || first.placedY != second.placedY)
    }

    @Test
    fun `le bas de la page suit la photo la plus basse`() {
        val high = photo(y = 100f, height = 90f)
        val low = photo(y = 800f, height = 200f).copy(id = 2)

        assertEquals(1000f, Placement.lowestEdge(listOf(high, low)), 0.01f)
    }

    @Test
    fun `les lignes d ecriture tombent sur la grille des photos`() {
        // Les deux quadrillages doivent se superposer, sinon la page a l'air
        // de trembler des qu'on ouvre la grille. L'ecart entre deux lignes
        // vaut un nombre entier de pas de grille, et la marge du haut aussi.
        assertEquals(0f, Placement.lineSpacing % Placement.GRID, 0.01f)
        assertEquals(0f, Placement.topMargin % Placement.GRID, 0.01f)
        assertTrue(Placement.lineSpacing > Placement.GRID)
    }

    @Test
    fun `une page sans photo posee n a pas de bas impose`() {
        val nowhere = photo().copy(placedX = null, placedY = null)

        assertEquals(0f, Placement.lowestEdge(listOf(nowhere)), 0.01f)
    }
}
