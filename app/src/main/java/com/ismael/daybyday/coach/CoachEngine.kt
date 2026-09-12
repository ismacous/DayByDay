package com.ismael.daybyday.coach

/**
 * Ce que le coup de pouce a deja dit, et quand.
 *
 * Sert a deux choses : ne pas radoter (une situation revue trop vite attend
 * son tour) et ne pas envahir (au plus une apparition par jour, au plus une ou
 * deux notifications).
 */
interface CoachMemory {

    /** Jour (epochDay) ou cette situation a ete montree pour la derniere fois. */
    fun lastShown(slug: String): Long?

    /** Compteur de rotation des phrases pour cette situation. */
    fun variant(slug: String): Int

    /** Retient qu'on vient de montrer cette situation. */
    fun remember(slug: String, epochDay: Long)

    /** Nombre d'apparitions a l'ecran deja faites ce jour-la. */
    fun popupsShown(epochDay: Long): Int

    fun recordPopup(epochDay: Long)

    /** Nombre de notifications deja envoyees ce jour-la. */
    fun notificationsSent(epochDay: Long): Int

    fun recordNotification(epochDay: Long)
}

/** Memoire qui ne survit pas a la fermeture : pour les tests et les apercus. */
class TemporaryCoachMemory : CoachMemory {
    private val shown = mutableMapOf<String, Long>()
    private val variants = mutableMapOf<String, Int>()
    private val popups = mutableMapOf<Long, Int>()
    private val notifications = mutableMapOf<Long, Int>()

    override fun lastShown(slug: String): Long? = shown[slug]

    override fun variant(slug: String): Int = variants[slug] ?: 0

    override fun remember(slug: String, epochDay: Long) {
        if (shown[slug] != epochDay) variants[slug] = variant(slug) + 1
        shown[slug] = epochDay
    }

    override fun popupsShown(epochDay: Long): Int = popups[epochDay] ?: 0

    override fun recordPopup(epochDay: Long) {
        popups[epochDay] = popupsShown(epochDay) + 1
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
 * propositions, une situation deja vue attend son tour, et **une fois qu'on a
 * parle aujourd'hui, on se tait**. Une application qui interrompt deux fois
 * dans la meme journee n'est plus un ami qui passe, c'est une alarme.
 */
object CoachEngine {

    /** Apparitions a l'ecran autorisees dans une journee. */
    const val POPUPS_PER_DAY = 1

    /**
     * Le message a faire apparaitre sur [surface], ou `null` s'il n'y a rien a
     * dire. Ne modifie rien : c'est [markShown] qui enregistre l'apparition.
     */
    fun choose(
        snapshot: CoachSnapshot,
        memory: CoachMemory,
        surface: NudgeSurface,
        maxNotifications: Int = 1,
        candidates: List<NudgeCandidate> = CoachRules.candidates(snapshot),
    ): Nudge? {
        val today = snapshot.todayEpochDay

        val quotaReached = if (surface == NudgeSurface.NOTIFICATION) {
            memory.notificationsSent(today) >= maxNotifications
        } else {
            memory.popupsShown(today) >= POPUPS_PER_DAY
        }
        if (quotaReached) return null

        val best = candidates
            .filter { surface in it.rule.surfaces }
            .filter { isAllowedNow(it.rule, memory, today) }
            .maxWithOrNull(compareBy<NudgeCandidate> { it.score }.thenByDescending { it.rule.ordinal })
            ?: return null

        return CoachMessages.render(best, variantFor(memory, best.rule, today), snapshot.firstName)
    }

    /**
     * Le message d'une situation precise, declenchee par un geste plutot que
     * par un balayage — la journee qu'on vient de boucler, par exemple.
     *
     * Il ignore le quota du jour : il ne s'invite pas, il repond a quelque
     * chose qu'on vient de faire. Il reste soumis au delai d'attente, sinon
     * decocher puis recocher une carte le rejouerait en boucle.
     */
    fun forRule(snapshot: CoachSnapshot, memory: CoachMemory, rule: CoachRule): Nudge? {
        val today = snapshot.todayEpochDay
        val last = memory.lastShown(rule.slug)
        if (last != null && today - last < rule.cooldownDays) return null
        if (rule.card?.key in snapshot.hiddenCardKeys && rule.card != null) return null
        return CoachMessages.render(
            candidate = NudgeCandidate(rule),
            variant = variantFor(memory, rule, today),
            firstName = snapshot.firstName,
        )
    }

    /**
     * La formulation du jour. Le compteur n'avance qu'une fois par jour, mais
     * il avance au moment de l'affichage : tant que le message n'a pas encore
     * ete montre aujourd'hui, on vise deja la formulation suivante. Sans ca,
     * la bulle changerait de phrase juste apres etre apparue.
     */
    private fun variantFor(memory: CoachMemory, rule: CoachRule, today: Long): Int {
        val counter = memory.variant(rule.slug)
        return if (memory.lastShown(rule.slug) == today) counter else counter + 1
    }

    /** Le delai d'attente propre a la situation est-il passe ? */
    private fun isAllowedNow(rule: CoachRule, memory: CoachMemory, today: Long): Boolean {
        val last = memory.lastShown(rule.slug) ?: return true
        return today - last >= rule.cooldownDays
    }

    /** Enregistre qu'un message vient d'apparaitre. */
    fun markShown(memory: CoachMemory, nudge: Nudge, surface: NudgeSurface, epochDay: Long) {
        memory.remember(nudge.rule.slug, epochDay)
        when {
            surface == NudgeSurface.NOTIFICATION -> memory.recordNotification(epochDay)
            // La fete de la journee bouclee ne consomme pas le quota : elle
            // repond a un geste, elle ne s'invite pas.
            nudge.rule != CoachRule.DAY_COMPLETE -> memory.recordPopup(epochDay)
        }
    }

    /**
     * Un exemple de ce qui serait dit maintenant, sans rien consommer ni
     * retenir. Utilise par le bouton d'essai des reglages.
     */
    fun preview(snapshot: CoachSnapshot, surface: NudgeSurface = NudgeSurface.MONTH): Nudge? =
        choose(snapshot, TemporaryCoachMemory(), surface)
}
