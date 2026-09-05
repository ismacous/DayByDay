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
        assertEquals(12f, Placement.snap(14f), 0.01f)
        assertEquals(24f, Placement.snap(19f), 0.01f)
        assertEquals(0f, Placement.snap(5f), 0.01f)
    }

    @Test
    fun `sans aimant la position est gardee telle quelle`() {
        val moved = Placement.move(photo(), x = 137.4f, y = 51.9f, snapToGrid = false, pageWidth = page)

        assertEquals(137.4f, moved.placedX!!, 0.01f)
        assertEquals(51.9f, moved.placedY!!, 0.01f)
    }

    @Test
    fun `avec aimant la position tombe sur la grille`() {
        val moved = Placement.move(photo(), x = 137.4f, y = 51.9f, snapToGrid = true, pageWidth = page)

        assertEquals(0f, moved.placedX!! % Placement.GRID, 0.01f)
        assertEquals(0f, moved.placedY!! % Placement.GRID, 0.01f)
    }

    // --- Ne jamais perdre une photo ---------------------------------------

    @Test
    fun `une photo poussee au dela du bord reste rattrapable`() {
        val moved = Placement.move(photo(), x = 5000f, y = 5000f, snapToGrid = false, pageWidth = page)

        // Il en reste toujours un morceau dans la page, sinon plus moyen de la
        // reprendre au doigt.
        assertTrue(moved.placedX!! < page)
        assertTrue(moved.placedX!! + moved.placedWidth > 0f)
    }

    @Test
    fun `une photo poussee vers le haut ne disparait pas non plus`() {
        val moved = Placement.move(photo(), x = -5000f, y = -5000f, snapToGrid = false, pageWidth = page)

        assertTrue(moved.placedX!! + moved.placedWidth > 0f)
        assertTrue(moved.placedY!! + moved.displayHeight > 0f)
    }

    @Test
    fun `une photo ne devient jamais plus petite qu une vignette`() {
        val small = Placement.resize(photo(), width = 2f, rotation = 0f, snapToGrid = false, pageWidth = page)

        assertEquals(Placement.MIN_SIZE, small.placedWidth, 0.01f)
    }

    // --- Redimensionner ---------------------------------------------------

    @Test
    fun `agrandir garde les proportions`() {
        val bigger = Placement.resize(photo(width = 120f, height = 90f), width = 240f, rotation = 0f, snapToGrid = false, pageWidth = page)

        assertEquals(240f, bigger.placedWidth, 0.01f)
        assertEquals(180f, bigger.placedHeight, 0.01f)
    }

    @Test
    fun `agrandir ne deplace pas le centre`() {
        val start = photo(x = 100f, y = 100f, width = 120f, height = 90f)
        val centreX = start.placedX!! + start.placedWidth / 2f
        val centreY = start.placedY!! + start.displayHeight / 2f

        val bigger = Placement.resize(start, width = 240f, rotation = 0f, snapToGrid = false, pageWidth = page)

        assertEquals(centreX, bigger.placedX!! + bigger.placedWidth / 2f, 0.01f)
        assertEquals(centreY, bigger.placedY!! + bigger.displayHeight / 2f, 0.01f)
    }

    @Test
    fun `un cercle reste aussi haut que large`() {
        val round = photo(width = 120f, height = 90f, shape = MediaShape.CIRCLE)

        assertEquals(round.placedWidth, round.displayHeight, 0.01f)
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
    fun `une page sans photo posee n a pas de bas impose`() {
        val nowhere = photo().copy(placedX = null, placedY = null)

        assertEquals(0f, Placement.lowestEdge(listOf(nowhere)), 0.01f)
    }
}
