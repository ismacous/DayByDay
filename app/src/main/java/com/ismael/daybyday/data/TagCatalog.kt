package com.ismael.daybyday.data

/**
 * Liste d'etiquettes fournie avec l'application. Elle est fixe : elle evolue
 * avec les mises a jour plutot que d'etre geree a la main, pour que la
 * notation reste comparable d'un jour a l'autre.
 *
 * Chaque entree porte un identifiant stable ([slug]) : renommer une etiquette
 * dans une prochaine version ne casse pas les journees deja marquees.
 */
object TagCatalog {

    data class Builtin(
        val slug: String,
        val emoji: String,
        val name: String,
        val category: TagCategory,
        /** Anciens noms a reprendre, pour ne pas perdre les journees deja marquees. */
        val aliases: List<String> = emptyList(),
    )

    val tags: List<Builtin> = listOf(
        // Sommeil
        Builtin("sleep_good", "😴", "Bien dormi", TagCategory.SLEEP),
        Builtin("sleep_bad", "🥱", "Mal dormi", TagCategory.SLEEP),
        Builtin("sleep_late", "🌙", "Couché tard", TagCategory.SLEEP),

        // Social
        Builtin("girlfriend", "💬", "Ma copine", TagCategory.SOCIAL, listOf("Copine")),
        Builtin("friends", "👥", "Ami·es", TagCategory.SOCIAL),
        Builtin("family", "🏠", "Famille", TagCategory.SOCIAL),
        Builtin("alone", "🙈", "Personne aujourd'hui", TagCategory.SOCIAL),

        // Activite
        Builtin("walk", "🚶", "Marche", TagCategory.ACTIVITY),
        Builtin("outside", "🌳", "Dehors", TagCategory.ACTIVITY, listOf("Dehors / nature")),
        Builtin("chores", "🧹", "Ménage, rangement", TagCategory.ACTIVITY),
        Builtin("errands", "🛒", "Courses, sorties utiles", TagCategory.ACTIVITY),

        // Alimentation
        Builtin("home_cooking", "🍳", "Cuisine maison", TagCategory.FOOD),
        Builtin("fast_food", "🍟", "Fast-food", TagCategory.FOOD),
        Builtin("snacking", "🍫", "Grignotage", TagCategory.FOOD),
        Builtin("alcohol", "🍺", "Alcool", TagCategory.FOOD),

        // Travail & demarches
        Builtin("job_search", "💼", "Recherche d'emploi", TagCategory.WORK, listOf("Travail")),
        Builtin("admin", "📄", "Démarches, paperasse", TagCategory.WORK),
        Builtin("interview", "🤝", "Entretien", TagCategory.WORK),

        // Rien pour l'argent : le detail des mouvements du jour dit deja
        // « grosse depense » et « journee sans depense », et le dire deux fois
        // laissait les deux se contredire. Une etiquette n'a de sens que
        // lorsqu'aucune donnee ne porte deja l'information.

        // Ecrans
        Builtin("social_media", "📱", "Réseaux sociaux", TagCategory.SCREENS, listOf("Écrans +++")),
        Builtin("series", "📺", "Séries, films", TagCategory.SCREENS),
        Builtin("games", "🎮", "Jeux vidéo", TagCategory.SCREENS),

        // Sante
        Builtin("appointment", "🩺", "Rendez-vous médical", TagCategory.HEALTH),
        Builtin("anxiety", "🧠", "Grosse angoisse", TagCategory.HEALTH),
        Builtin("cried", "😢", "J'ai pleuré", TagCategory.HEALTH),
    )

    /**
     * Aligne la base sur la liste ci-dessus : ajoute les nouvelles etiquettes,
     * met a jour les libelles, et recupere celles portant un ancien nom sans
     * perdre les journees qui y sont rattachees.
     */
    suspend fun sync(dao: DayDao) {
        val existing = dao.allTags()

        // Une etiquette retiree du catalogue disparait aussi de la base, avec
        // les journees qui y renvoient : la garder afficherait une etiquette
        // que plus aucune carte ne montre, impossible a decocher.
        val known = tags.map { it.slug }.toSet()
        existing.filter { it.slug != null && it.slug !in known }.forEach { obsolete ->
            dao.deleteTagLinks(obsolete.id)
            dao.deleteTag(obsolete.id)
        }

        tags.forEachIndexed { index, builtin ->
            val match = existing.firstOrNull { it.slug == builtin.slug }
                ?: existing.firstOrNull { it.slug == null && it.name == builtin.name }
                ?: existing.firstOrNull { it.slug == null && it.name in builtin.aliases }

            val updated = (match ?: Tag(name = builtin.name)).copy(
                name = builtin.name,
                emoji = builtin.emoji,
                sortOrder = index,
                category = builtin.category.key,
                slug = builtin.slug,
            )
            dao.insertTag(updated)
        }
    }
}
