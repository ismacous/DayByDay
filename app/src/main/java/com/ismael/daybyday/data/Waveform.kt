package com.ismael.daybyday.data

/**
 * La forme d'onde d'un vocal : la silhouette du son, en une poignee de barres.
 *
 * Elle est **mesuree pendant l'enregistrement** et rangee avec le vocal, pas
 * recalculee a l'affichage. Relire un fichier audio pour dessiner une liste de
 * barres demanderait de le decoder entierement — plusieurs secondes pour un
 * vocal de dix minutes, a chaque fois qu'on ouvre la page.
 *
 * L'encodage est volontairement bete : un chiffre par barre, de `0` a `9`.
 * Cinquante-six caracteres pour un vocal, lisibles a l'oeil dans la base et
 * dans une sauvegarde, et rien a faire evoluer si un jour on veut plus de
 * barres — une ancienne chaine plus courte reste valable.
 *
 * Tout est du Kotlin pur, sans rien d'Android : les regles sont testees dans
 * `WaveformTest`.
 */
object Waveform {

    /** Le nombre de barres d'un vocal, quelle que soit sa duree. */
    const val BARS = 56

    /**
     * La hauteur minimale d'une barre, en part de la hauteur totale.
     *
     * Un silence ne doit pas donner une barre invisible : une forme d'onde
     * trouee au milieu ressemble a un dessin abime, pas a un silence.
     */
    const val FLOOR = 0.12f

    /**
     * Reduit les mesures brutes a [bars] barres, entre `0` et `9`.
     *
     * Deux choses se passent ici. Le **sous-echantillonnage** d'abord : on
     * garde le plus fort de chaque tranche, et pas la moyenne — c'est ce que
     * l'oreille retient d'un passage, et une moyenne aplatit tout jusqu'a
     * donner une ligne droite. La **normalisation** ensuite : la barre la plus
     * haute vaut toujours 9, donc un vocal chuchote se voit autant qu'un vocal
     * crie. On ne mesure pas le volume absolu, on dessine un rythme.
     */
    fun encode(samples: List<Int>, bars: Int = BARS): String {
        val useful = samples.filter { it >= 0 }
        if (useful.isEmpty()) return ""
        val peak = useful.max().takeIf { it > 0 } ?: return ""

        val builder = StringBuilder(bars)
        for (index in 0 until bars) {
            val from = (index.toLong() * useful.size / bars).toInt()
            val to = ((index + 1).toLong() * useful.size / bars).toInt().coerceAtLeast(from + 1)
            val slice = useful.subList(from.coerceAtMost(useful.lastIndex), to.coerceAtMost(useful.size))
            val loudest = slice.maxOrNull() ?: 0
            builder.append(('0' + (loudest * 9 / peak).coerceIn(0, 9)))
        }
        return builder.toString()
    }

    /**
     * Les hauteurs a dessiner, entre 0 et 1.
     *
     * Une chaine vide — les vocaux enregistres avant que la forme d'onde
     * existe — rend une silhouette **neutre** plutot qu'une liste vide : une
     * barre de lecture sans barres ne ressemble a rien, et un ancien vocal
     * reste un vocal.
     */
    fun decode(text: String, bars: Int = BARS): List<Float> {
        if (text.isEmpty()) return neutral(bars)
        return (0 until bars).map { index ->
            val at = index * text.length / bars
            val digit = text.getOrNull(at)?.takeIf { it.isDigit() }?.let { it - '0' } ?: 0
            FLOOR + (1f - FLOOR) * (digit / 9f)
        }
    }

    /**
     * La silhouette d'un vocal dont on n'a pas mesure le son.
     *
     * Une ondulation douce et regulière : elle dit « il y a du son ici » sans
     * pretendre decrire ce qu'on va entendre. Une rangee de barres toutes
     * egales aurait l'air d'un bogue.
     */
    private fun neutral(bars: Int): List<Float> = (0 until bars).map { index ->
        val phase = index * 0.7f
        val wave = (kotlin.math.sin(phase.toDouble()) * 0.5 + 0.5).toFloat()
        FLOOR + (0.55f - FLOOR) * wave + 0.12f
    }
}
