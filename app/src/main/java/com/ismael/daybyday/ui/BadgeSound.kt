package com.ismael.daybyday.ui

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import com.ismael.daybyday.R

/**
 * Le petit carillon d'une medaille.
 *
 * C'est un fichier **fabrique**, pas un son telecharge : un arpege majeur —
 * do, mi, sol, do — ou chaque note porte son octave et sa quinte en harmonique,
 * avec une attaque douce et une longue decroissance. Un son de cloche, pas un
 * bip. Un bip previent d'un probleme ; une cloche marque un moment.
 *
 * Deux regles, et la seconde compte plus que la premiere :
 *
 * - **Il suit le telephone.** En mode silencieux ou vibreur, rien ne sort. Une
 *   application personnelle qui sonne pendant une reunion parce qu'on a bu son
 *   huitieme verre d'eau, c'est une application qu'on desinstalle.
 * - **Il ne bloque jamais.** Tout est enveloppe : un son qui ne part pas ne
 *   doit pas empecher la medaille d'apparaitre.
 */
object BadgeSound {

    fun play(context: Context) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        runCatching {
            MediaPlayer.create(context.applicationContext, R.raw.badge_chime)?.apply {
                // Volontairement en retrait : la medaille se regarde, elle ne
                // s'annonce pas.
                setVolume(VOLUME, VOLUME)
                setOnCompletionListener { player ->
                    runCatching { player.release() }
                }
                start()
            }
        }
    }

    private const val VOLUME = 0.55f
}
