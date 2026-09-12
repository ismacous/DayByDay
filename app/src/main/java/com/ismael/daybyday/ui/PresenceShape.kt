package com.ismael.daybyday.ui

import kotlin.math.sin

/**
 * Le rayon du disque, et il n'est **jamais** nul.
 *
 * A la toute premiere image, l'arrivee de la bulle vaut zero : le rayon tombait
 * a zero avec elle, et un degrade radial de rayon zero n'existe pas — Android
 * refuse d'en construire un (« radius must be > 0 ») et l'application se
 * fermait. Comme la fermeture arrivait avant que le quota du jour ne soit
 * ecrit, elle recommencait a chaque ouverture : deux secondes, puis plus rien.
 *
 * Le plancher n'est donc pas une precaution, c'est la correction — et il est
 * ici, dans une fonction sans Android, pour qu'un test le prouve pour toutes
 * les valeurs plutot qu'un oeil sur un ecran.
 */
internal fun presenceRadius(minDimension: Float, breath: Float, appear: Float): Float {
    val grow = 1f + 0.045f * sin(breath * 2f * Math.PI.toFloat())
    val full = minDimension * 0.31f * grow * appear.coerceIn(0f, 1f)
    return full.coerceAtLeast(MIN_RADIUS)
}

/** Meme regle pour le halo : une boite pas encore mesuree a une taille nulle. */
internal fun haloRadius(minDimension: Float): Float =
    (minDimension * 0.62f).coerceAtLeast(MIN_RADIUS)

/**
 * Un demi-point : invisible a l'oeil, suffisant pour que le degrade existe.
 */
private const val MIN_RADIUS = 0.5f
