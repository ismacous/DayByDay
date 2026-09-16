package com.ismael.daybyday.data

/**
 * Les rythmes de respiration proposés.
 *
 * Trois, pas dix. Chacun répond à une situation différente, et si les trois ne
 * se distinguent pas d'un coup d'œil, c'est qu'il y en a un de trop : quelqu'un
 * qui ouvre cet écran au milieu d'une crise n'a pas la tête à comparer des
 * durées.
 *
 * Aucun n'est un traitement et aucun ne promet quoi que ce soit. Ce sont des
 * rythmes connus, rien de plus — c'est ce que disent les libellés.
 *
 * Les durées sont en secondes. `hold` ou `rest` à zéro veut dire que la phase
 * n'existe pas : le rythme passe directement à la suivante.
 */
enum class BreathPattern(
    val label: String,
    val summary: String,
    /** Ce à quoi il sert, en une phrase, sans rien promettre. */
    val purpose: String,
    val inhale: Int,
    val hold: Int,
    val exhale: Int,
    val rest: Int,
) {
    COHERENCE(
        label = "Cohérence",
        summary = "5 · 5",
        purpose = "Le rythme le plus simple. Si tu ne sais pas lequel prendre, prends celui-là.",
        inhale = 5, hold = 0, exhale = 5, rest = 0,
    ),
    CALM(
        label = "Apaiser",
        summary = "4 · 7 · 8",
        purpose = "L'expiration est longue. Plutôt le soir, ou quand ça monte.",
        inhale = 4, hold = 7, exhale = 8, rest = 0,
    ),
    SQUARE(
        label = "Carré",
        summary = "4 · 4 · 4 · 4",
        purpose = "Quatre temps égaux. Utile quand on a besoin de se raccrocher à quelque chose.",
        inhale = 4, hold = 4, exhale = 4, rest = 4,
    );

    val cycleSeconds: Int get() = inhale + hold + exhale + rest

    /** Les phases de ce rythme, celles qui durent plus de zéro. */
    val phases: List<BreathPhase>
        get() = listOfNotNull(
            BreathPhase.INHALE.takeIf { inhale > 0 },
            BreathPhase.HOLD.takeIf { hold > 0 },
            BreathPhase.EXHALE.takeIf { exhale > 0 },
            BreathPhase.REST.takeIf { rest > 0 },
        )

    fun secondsOf(phase: BreathPhase): Int = when (phase) {
        BreathPhase.INHALE -> inhale
        BreathPhase.HOLD -> hold
        BreathPhase.EXHALE -> exhale
        BreathPhase.REST -> rest
    }
}

/** Les quatre temps d'un cycle. */
enum class BreathPhase(val label: String) {
    INHALE("Inspire"),
    HOLD("Retiens"),
    EXHALE("Expire"),
    REST("Pose"),
}

/**
 * Où en est la respiration à un instant donné.
 *
 * Du Kotlin pur, sans rien d'Android : c'est ce qui permet de vérifier le
 * rythme sur machine plutôt qu'en regardant l'écran avec un chronomètre.
 *
 * @param phase le temps en cours.
 * @param remaining les secondes qu'il reste sur ce temps, arrondies vers le
 *   haut — on affiche « 1 » jusqu'à la toute fin de la dernière seconde, pas
 *   « 0 » pendant une seconde entière.
 * @param openness l'ouverture du souffle, de 0 (poumons vides) à 1 (pleins).
 *   C'est elle qui pilote la taille de la bulle, et elle seule : la bulle ne
 *   « sait » rien du rythme.
 */
data class BreathState(
    val phase: BreathPhase,
    val remaining: Int,
    val openness: Float,
) {
    /** Le souffle est-il en mouvement ? Les temps de pause ne bougent pas. */
    val moving: Boolean get() = phase == BreathPhase.INHALE || phase == BreathPhase.EXHALE
}

object Breathing {

    /**
     * L'état du souffle à [elapsed] secondes du départ.
     *
     * L'ouverture suit une courbe en S et non une droite : un souffle ne part
     * pas à pleine vitesse et ne s'arrête pas net. C'est la différence entre
     * une bulle qu'on suit sans y penser et une bulle qui tire.
     */
    fun stateAt(pattern: BreathPattern, elapsed: Double): BreathState {
        val cycle = pattern.cycleSeconds.toDouble()
        if (cycle <= 0.0) return BreathState(BreathPhase.INHALE, 0, 0f)

        var t = elapsed % cycle
        if (t < 0) t += cycle

        pattern.phases.forEach { phase ->
            val length = pattern.secondsOf(phase).toDouble()
            if (t < length) {
                val progress = (t / length).coerceIn(0.0, 1.0)
                val openness = when (phase) {
                    BreathPhase.INHALE -> ease(progress)
                    BreathPhase.HOLD -> 1.0
                    BreathPhase.EXHALE -> 1.0 - ease(progress)
                    BreathPhase.REST -> 0.0
                }
                return BreathState(
                    phase = phase,
                    remaining = Math.ceil(length - t).toInt().coerceAtLeast(1),
                    openness = openness.toFloat(),
                )
            }
            t -= length
        }
        // Inatteignable : la somme des phases vaut le cycle. On rend quand meme
        // quelque chose de valide plutot que de tomber.
        return BreathState(BreathPhase.REST, 1, 0f)
    }

    /** Une courbe en S douce : lent au depart, lent a l'arrivee. */
    private fun ease(t: Double): Double = 0.5 - 0.5 * Math.cos(Math.PI * t.coerceIn(0.0, 1.0))
}
