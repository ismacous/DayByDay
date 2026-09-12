package com.ismael.daybyday.coach

/**
 * Ce que le coup de pouce a deja dit, et quand. Sert a ne pas radoter : une
 * situation revue trop vite est mise de cote, et la formulation tourne a
 * chaque nouvelle fois.
 */
interface CoachMemory {

    /** Jour (epochDay) ou cette situation a ete montree pour la derniere fois. */
    fun lastShown(slug: String): Long?

    /** Compteur de rotation des phrases pour cette situation. */
    fun variant(slug: String): Int

    /** Retient qu'on vient de montrer cette situation. */
    fun remember(slug: String, epochDay: Long)

    /** Vrai si le message a ete ecarte a la main aujourd'hui. */
    fun isDismissed(surface: NudgeSurface, epochDay: Long): Boolean

    /** L'utilisateur a ferme la carte : on se tait sur cet ecran jusqu'a demain. */
    fun dismiss(surface: NudgeSurface, epochDay: Long)

    /** Nombre de notifications deja envoyees ce jour-la. */
    fun notificationsSent(epochDay: Long): Int

    fun recordNotification(epochDay: Long)
}

/** Memoire qui ne survit pas a la fermeture : pour les tests et les apercus. */
class TemporaryCoachMemory : CoachMemory {
    private val shown = mutableMapOf<String, Long>()
    private val variants = mutableMapOf<String, Int>()
    private val dismissed = mutableMapOf<NudgeSurface, Long>()
    private val notifications = mutableMapOf<Long, Int>()

    override fun lastShown(slug: String): Long? = shown[slug]

    override fun variant(slug: String): Int = variants[slug] ?: 0

    override fun remember(slug: String, epochDay: Long) {
        if (shown[slug] != epochDay) variants[slug] = variant(slug) + 1
        shown[slug] = epochDay
    }

    override fun isDismissed(surface: NudgeSurface, epochDay: Long): Boolean =
        dismissed[surface] == epochDay

    override fun dismiss(surface: NudgeSurface, epochDay: Long) {
        dismissed[surface] = epochDay
    }

    override fun notificationsSent(epochDay: Long): Int = notifications[epochDay] ?: 0

    override fun recordNotification(epochDay: Long) {
        notifications[epochDay] = notificationsSent(epochDay) + 1
    }
}

/**
 * Choisit quoi dire, ou, et quand se taire.
 *
 * Le tri est simple et volontairement previsible : le soutien passe avant les
 * propositions, et une situation deja vue recemment attend son tour.
 */
object CoachEngine {

    /**
     * Le message a afficher sur [surface], ou `null` s'il n'y a rien a dire.
     * Ne modifie rien : c'est [markShown] qui enregistre l'affichage.
     *
     * @param maxNotifications quota du jour, seulement pour les notifications.
     */
    fun choose(
        snapshot: CoachSnapshot,
        memory: CoachMemory,
        surface: NudgeSurface,
        maxNotifications: Int = 1,
        candidates: List<NudgeCandidate> = CoachRules.candidates(snapshot),
    ): Nudge? {
        val today = snapshot.todayEpochDay
        if (memory.isDismissed(surface, today)) return null
        if (surface == NudgeSurface.NOTIFICATION && memory.notificationsSent(today) >= maxNotifications) {
            return null
        }

        val usable = candidates
            .filter { surface in it.rule.surfaces }
            .filter { isAllowedNow(it.rule, memory, surface, today) }
            .sortedWith(compareByDescending<NudgeCandidate> { it.score }.thenBy { it.rule.ordinal })

        // Une notification ne sert a rien si elle ne dit rien : on garde le
        // silence plutot que d'envoyer la phrase de politesse.
        val best = usable.firstOrNull {
            surface != NudgeSurface.NOTIFICATION || it.rule != CoachRule.HELLO
        } ?: return null

        return CoachMessages.render(best, variantFor(memory, best.rule, today), snapshot.firstName)
    }

    /**
     * La formulation du jour. Le compteur n'avance qu'une fois par jour, mais
     * il avance au moment de l'affichage : tant que le message n'a pas encore
     * ete montre aujourd'hui, on vise deja la formulation suivante. Sans ca,
     * la carte changerait de phrase juste apres etre apparue.
     */
    private fun variantFor(memory: CoachMemory, rule: CoachRule, today: Long): Int {
        val counter = memory.variant(rule.slug)
        return if (memory.lastShown(rule.slug) == today) counter else counter + 1
    }

    /**
     * Le delai d'attente est respecte, avec une nuance : dans l'application,
     * un message deja affiche aujourd'hui reste affichable aujourd'hui, sinon
     * la carte changerait de texte a chaque retour sur l'ecran.
     */
    private fun isAllowedNow(
        rule: CoachRule,
        memory: CoachMemory,
        surface: NudgeSurface,
        today: Long,
    ): Boolean {
        val last = memory.lastShown(rule.slug) ?: return true
        if (last == today) return surface != NudgeSurface.NOTIFICATION
        return today - last >= rule.cooldownDays
    }

    /** Enregistre qu'un message vient d'etre montre. */
    fun markShown(memory: CoachMemory, nudge: Nudge, surface: NudgeSurface, epochDay: Long) {
        memory.remember(nudge.rule.slug, epochDay)
        if (surface == NudgeSurface.NOTIFICATION) memory.recordNotification(epochDay)
    }

    /**
     * Un exemple de ce qui serait dit maintenant, sans rien consommer ni
     * retenir. Utilise par le bouton d'essai des reglages.
     */
    fun preview(snapshot: CoachSnapshot, surface: NudgeSurface = NudgeSurface.HOME): Nudge? =
        choose(snapshot, TemporaryCoachMemory(), surface, maxNotifications = 1)
}
