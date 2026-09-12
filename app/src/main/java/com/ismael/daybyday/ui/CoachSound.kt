package com.ismael.daybyday.ui

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import com.ismael.daybyday.R

/**
 * La voix du coup de pouce.
 *
 * Deux sons **fabriques**, pas telecharges, sur le modele du carillon des
 * medailles : des notes de cloche avec leurs harmoniques, une attaque douce et
 * une longue decroissance.
 *
 * - [play] : deux notes qui montent d'une quinte, discretes. Ce n'est pas une
 *   alerte, c'est quelqu'un qui se manifeste.
 * - [playCheer] : l'arpege complet, pour la journee bouclee.
 *
 * Memes deux regles que pour les medailles, et la seconde compte plus : il
 * **suit le telephone** (rien ne sort en silencieux ni en vibreur) et il ne
 * **bloque jamais** — un son qui ne part pas ne doit pas empecher la bulle
 * d'apparaitre.
 */
object CoachSound {

    fun play(context: Context) = sound(context, R.raw.coach_voice, VOLUME)

    fun playCheer(context: Context) = sound(context, R.raw.coach_cheer, CHEER_VOLUME)

    private fun sound(context: Context, resource: Int, volume: Float) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        runCatching {
            MediaPlayer.create(context.applicationContext, resource)?.apply {
                setVolume(volume, volume)
                setOnCompletionListener { player -> runCatching { player.release() } }
                start()
            }
        }
    }

    /** Tres en retrait : le message se lit, il ne s'annonce pas. */
    private const val VOLUME = 0.38f

    private const val CHEER_VOLUME = 0.55f
}
