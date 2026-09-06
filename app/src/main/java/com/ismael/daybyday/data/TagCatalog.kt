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
        // Sommeil. « Bien » et « mal » s'excluent : la carte les montre en
        // selecteur, pas en pastilles.
        Builtin("sleep_good", "😴", "Bien dormi", TagCategory.SLEEP),
        Builtin("sleep_bad", "🥱", "Mal dormi", TagCategory.SLEEP),
        Builtin("sleep_late", "🌙", "Couché tard", TagCategory.SLEEP),

        // Social. Pas de « personne aujourd'hui » : ne rien cocher le dit
        // deja, et une case pour dire qu'il ne s'est rien passe est une case
        // de trop.
        Builtin(
            "girlfriend", "💬", "Ma moitié", TagCategory.SOCIAL,
            listOf("Copine", "Ma copine"),
        ),
        Builtin("friends", "👥", "Ami·es", TagCategory.SOCIAL),
        Builtin("family", "🏠", "Famille", TagCategory.SOCIAL),

        // Activite. « Dehors » n'y est plus : sortir ou rester chez soi est
        // devenu une question a part entiere dans la carte, avec deux
        // reponses, au lieu d'une pastille perdue entre le menage et les
        // courses. Les deux autres sont raccourcies a un seul mot : trois
        // pastilles d'un mot forment une rangee, trois pastilles a rallonge
        // font un paragraphe.
        Builtin("walk", "🚶", "Marche", TagCategory.ACTIVITY),
        Builtin("chores", "🧹", "Ménage", TagCategory.ACTIVITY, listOf("Ménage, rangement")),
        Builtin("errands", "🛒", "Courses", TagCategory.ACTIVITY, listOf("Courses, sorties utiles")),

        // Alimentation. Une seule reste, et elle ne s'affiche plus en
        // pastille : « compliquée / correcte / bien mangé » disent deja
        // comment la journee s'est passee a table, et le grignotage merite sa
        // propre place avec de quoi ecrire ce que c'etait.
        Builtin("snacking", "🍫", "Grignotage", TagCategory.FOOD),

        // Travail. « Recherche d'emploi » n'est plus une case a cocher mais un
        // nombre de candidatures : chercher du travail est une quantite, pas
        // un oui-ou-non.
        Builtin("interview", "🤝", "Entretien", TagCategory.WORK),
        Builtin("admin", "📄", "Démarches, paperasse", TagCategory.WORK),
        Builtin("freelance", "🎨", "Boulot freelance", TagCategory.WORK),
        Builtin("temp_work", "📦", "Intérim, manutention", TagCategory.WORK),

        // Ecrans
        Builtin("social_media", "📱", "Réseaux sociaux", TagCategory.SCREENS, listOf("Écrans +++")),
        Builtin("series", "📺", "Séries, films", TagCategory.SCREENS),
        Builtin("games", "🎮", "Jeux vidéo", TagCategory.SCREENS),

        // Sante. « Grosse angoisse » est partie : elle n'a jamais servi.
        // « J'ai pleuré » reste, mais la carte l'affiche en ligne a cocher et
        // non en pastille — ce n'est pas une etiquette qu'on colle a sa
        // journee, c'est quelque chose qui est arrive.
        Builtin("cried", "😢", "J'ai pleuré", TagCategory.HEALTH),

        // Traitements, qui ont maintenant leur propre carte.
        Builtin("appointment", "🩺", "Rendez-vous médical", TagCategory.MEDICAL),
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
