package com.ismael.daybyday

import com.ismael.daybyday.data.BreathPattern
import com.ismael.daybyday.data.BreathPhase
import com.ismael.daybyday.data.Breathing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le rythme de la respiration.
 *
 * Il se verifie ici plutot qu'en regardant l'ecran avec un chronometre : ce
 * sont des secondes, elles doivent tomber juste, et un cycle qui derive d'un
 * dixieme se voit au bout de trois minutes.
 */
class BreathingTest {

    @Test
    fun `chaque temps dure ce qu'il annonce`() {
        val p = BreathPattern.CALM // 4 · 7 · 8
        assertEquals(BreathPhase.INHALE, Breathing.stateAt(p, 0.0).phase)
        assertEquals(BreathPhase.INHALE, Breathing.stateAt(p, 3.9).phase)
        assertEquals(BreathPhase.HOLD, Breathing.stateAt(p, 4.1).phase)
        assertEquals(BreathPhase.HOLD, Breathing.stateAt(p, 10.9).phase)
        assertEquals(BreathPhase.EXHALE, Breathing.stateAt(p, 11.1).phase)
        assertEquals(BreathPhase.EXHALE, Breathing.stateAt(p, 18.9).phase)
        // Et le cycle recommence.
        assertEquals(BreathPhase.INHALE, Breathing.stateAt(p, 19.1).phase)
    }

    @Test
    fun `le cycle ne derive pas`() {
        // Trois minutes de coherence : on doit retomber exactement au meme
        // endroit qu'au depart, sinon la bulle se decale de l'affichage.
        val p = BreathPattern.COHERENCE
        val start = Breathing.stateAt(p, 0.0)
        val later = Breathing.stateAt(p, 180.0)

        assertEquals(start.phase, later.phase)
        assertEquals(start.openness, later.openness, 0.0001f)
    }

    @Test
    fun `les poumons sont vides au depart et pleins a la bascule`() {
        val p = BreathPattern.COHERENCE
        assertEquals(0f, Breathing.stateAt(p, 0.0).openness, 0.001f)
        assertEquals(1f, Breathing.stateAt(p, 5.0).openness, 0.001f)
        assertEquals(0f, Breathing.stateAt(p, 10.0).openness, 0.001f)
    }

    @Test
    fun `l ouverture ne sort jamais de ses bornes`() {
        // Le plancher du rayon de la bulle depend de ca : une ouverture
        // negative donnerait un degrade de rayon nul, qui fait tomber Android.
        BreathPattern.entries.forEach { pattern ->
            var t = 0.0
            while (t < pattern.cycleSeconds * 2.0) {
                val open = Breathing.stateAt(pattern, t).openness
                assertTrue("$pattern à $t : $open", open in 0f..1f)
                t += 0.05
            }
        }
    }

    @Test
    fun `le compte a rebours ne montre jamais zero`() {
        // « 0 » affiche pendant une seconde entiere donne l'impression que
        // c'est fini alors qu'il reste une seconde a tenir.
        BreathPattern.entries.forEach { pattern ->
            var t = 0.0
            while (t < pattern.cycleSeconds * 2.0) {
                val state = Breathing.stateAt(pattern, t)
                assertTrue("$pattern à $t", state.remaining >= 1)
                assertTrue("$pattern à $t", state.remaining <= pattern.secondsOf(state.phase))
                t += 0.05
            }
        }
    }

    @Test
    fun `un temps a zero n'existe pas`() {
        // La coherence n'a ni pause ni retenue : elle ne doit jamais les
        // afficher, sinon le mot « Retiens » passerait une image a l'ecran.
        val phases = BreathPattern.COHERENCE.phases
        assertEquals(listOf(BreathPhase.INHALE, BreathPhase.EXHALE), phases)
        assertEquals(4, BreathPattern.SQUARE.phases.size)
    }

    @Test
    fun `un temps negatif ne fait pas tomber le calcul`() {
        // Le temps ecoule part de zero, mais rien n'oblige le reste du code a
        // le garantir pour toujours.
        val state = Breathing.stateAt(BreathPattern.SQUARE, -3.0)
        assertTrue(state.openness in 0f..1f)
        assertTrue(state.remaining >= 1)
    }
}
