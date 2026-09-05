package com.ismael.daybyday

import androidx.compose.ui.graphics.Color
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.ui.InkDark
import com.ismael.daybyday.ui.readableOnAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les couleurs des journees, et l'encre qu'on peut poser dessus.
 *
 * Deux regles seulement, mais toutes les deux ont ete apprises en les cassant :
 * une moyenne ne doit jamais avoir l'air meilleure que ce qu'elle vaut, et un
 * texte pose sur un degrade doit rester lisible d'un bout a l'autre.
 */
class DayColorTest {

    @Test
    fun `une note pleine donne exactement la couleur de la journee`() {
        DayColor.entries.forEach { day ->
            assertEquals(day.color, DayColor.fromAverage(day.score.toDouble()))
        }
    }

    @Test
    fun `une moyenne ne depasse jamais la couleur qu elle approche`() {
        // Le bug d'avant : melanger l'orange et le vert par la teinte fabriquait
        // un vert-jaune fluo, plus eclatant que le vert des bonnes journees. Une
        // semaine a 2,5 sur 3 avait alors l'air d'un sans-faute.
        val entreDeux = DayColor.fromAverage(2.5)
        val bonne = DayColor.GREEN.color

        assertTrue(
            "Une moyenne intermediaire ne doit pas etre plus saturee que le vert plein",
            saturation(entreDeux) <= saturation(bonne) + 0.001f,
        )
    }

    @Test
    fun `une moyenne reste dans la famille de la note la plus proche`() {
        // 2,2 est plus proche de 2 (mitigee) que de 3 : la couleur doit rester
        // orangee, donc plus de rouge que de bleu.
        val proche = DayColor.fromAverage(2.2)
        assertTrue(proche.red > proche.blue)
    }

    @Test
    fun `l encre d un degrade tient sur ses deux bouts`() {
        // Le vert part d'un vert moyen et finit clair : le blanc, lisible au
        // depart, disparait a l'arrivee. C'est l'encre sombre qui gagne.
        assertEquals(InkDark, readableOnAll(DayColor.GREEN.gradient))
        assertEquals(InkDark, readableOnAll(DayColor.ORANGE.gradient))

        // La journee tres noire est un indigo profond de bout en bout.
        assertEquals(Color.White, readableOnAll(DayColor.BLACK.gradient))
    }

    @Test
    fun `une liste vide ne fait pas tomber le calcul d encre`() {
        assertEquals(Color.White, readableOnAll(emptyList()))
    }

    /** Ecart entre le canal le plus fort et le plus faible : la vivacite percue. */
    private fun saturation(color: Color): Float {
        val high = maxOf(color.red, color.green, color.blue)
        val low = minOf(color.red, color.green, color.blue)
        return high - low
    }
}
