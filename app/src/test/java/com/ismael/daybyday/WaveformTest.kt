package com.ismael.daybyday

import com.ismael.daybyday.data.Waveform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La forme d'onde des vocaux.
 *
 * Ce qu'il faut protéger : qu'elle ait toujours le bon nombre de barres, que
 * la plus forte soit toujours au maximum (un vocal chuchoté doit se voir), et
 * qu'un vocal enregistré avant cette version rende quand même une silhouette.
 */
class WaveformTest {

    @Test
    fun `le nombre de barres ne depend pas du nombre de mesures`() {
        listOf(1, 5, 200, 5000).forEach { count ->
            // Jamais zéro : un silence complet ne rend rien du tout, et c'est
            // voulu — c'est le test d'à côté qui le vérifie.
            val samples = List(count) { it % 1000 + 1 }
            assertEquals("$count mesures", Waveform.BARS, Waveform.encode(samples).length)
        }
    }

    @Test
    fun `la mesure la plus forte donne la barre la plus haute`() {
        val encoded = Waveform.encode(List(100) { 1000 } + listOf(32000))
        assertTrue("la barre maximale devrait exister", encoded.contains('9'))
    }

    @Test
    fun `un vocal chuchote se voit autant qu'un vocal crie`() {
        // Les deux ont le même rythme, à un facteur près : la silhouette doit
        // être la même. On ne dessine pas un volume, on dessine un rythme.
        val doux = Waveform.encode(List(60) { if (it % 10 < 5) 40 else 400 })
        val fort = Waveform.encode(List(60) { if (it % 10 < 5) 400 else 4000 })
        assertEquals(doux, fort)
    }

    @Test
    fun `le silence complet ne rend rien`() {
        assertEquals("", Waveform.encode(List(50) { 0 }))
        assertEquals("", Waveform.encode(emptyList()))
    }

    @Test
    fun `les hauteurs restent dans l'intervalle dessinable`() {
        val heights = Waveform.decode(Waveform.encode(List(200) { it * 37 % 9000 }))
        assertEquals(Waveform.BARS, heights.size)
        heights.forEach { assertTrue("hauteur hors bornes : $it", it in 0f..1f) }
    }

    @Test
    fun `un vocal sans mesure garde une silhouette`() {
        // Les vocaux enregistrés avant que la forme d'onde existe : une barre
        // de lecture sans barres ne ressemblerait à rien.
        val heights = Waveform.decode("")
        assertEquals(Waveform.BARS, heights.size)
        assertTrue(heights.all { it > 0f })
        assertTrue("elle doit ondoyer, pas être plate", heights.toSet().size > 1)
    }
}
