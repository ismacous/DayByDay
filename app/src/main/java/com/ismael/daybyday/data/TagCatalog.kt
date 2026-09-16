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
        // Plus de « couché tard » : l'heure du coucher est deja saisie
        // juste au-dessus, et une etiquette qui repete une donnee laisse les
        // deux se contredire.

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

        // Ressentis. Ils ont quitte la carte Sante, ou ils n'avaient rien a
        // faire : pleurer n'est pas un symptome, et les ranger a cote du poids
        // revenait a dire le contraire. Ils ont maintenant leur carte, et de
        // quoi dire autre chose que le pire.
        //
        // L'ordre compte : la carte separe ce qui fait du bien de ce qui pese,
        // et elle le fait sur cette liste. Les douces d'abord, les lourdes
        // ensuite, et les deux faits a cocher tout a la fin.
        Builtin("joy", "😊", "Joie", TagCategory.EMOTION),
        Builtin("laugh", "😂", "Fou rire", TagCategory.EMOTION),
        Builtin("calm", "😌", "Calme", TagCategory.EMOTION),
        Builtin("excited", "🤩", "Excitation", TagCategory.EMOTION),
        Builtin("proud", "🏅", "Fierté", TagCategory.EMOTION),
        Builtin("grateful", "🙏", "Gratitude", TagCategory.EMOTION),
        Builtin("loved", "❤️", "Aimé", TagCategory.EMOTION),
        Builtin("motivated", "🔥", "Motivation", TagCategory.EMOTION),
        Builtin("relief", "🍃", "Soulagement", TagCategory.EMOTION),

        Builtin("sad", "😔", "Tristesse", TagCategory.EMOTION),
        Builtin("stress", "😬", "Stress", TagCategory.EMOTION),
        Builtin("anger", "😠", "Colère", TagCategory.EMOTION),
        Builtin("fear", "😨", "Peur", TagCategory.EMOTION),
        Builtin("lonely", "🫥", "Solitude", TagCategory.EMOTION),
        Builtin("guilt", "😞", "Culpabilité", TagCategory.EMOTION),
        Builtin("shame", "🙈", "Honte", TagCategory.EMOTION),
        Builtin("bored", "😐", "Ennui", TagCategory.EMOTION),
        Builtin("overwhelmed", "🌊", "Débordé", TagCategory.EMOTION),
        Builtin("empty", "🌫️", "Vide", TagCategory.EMOTION),

        // Ces deux-la ne sont pas des ressentis mais des **faits**, et elles
        // gardent leur forme : des lignes a cocher, et **sans emoji**. Un petit
        // visage qui pleure a cote de « J'ai pleuré » transforme un fait en
        // mise en scene. L'emoji reste vide ici, ce qui fait que `display` vaut
        // le nom seul partout ailleurs aussi — dans la recherche, par exemple.
        //
        // Les slugs ne changent pas : ce sont eux qui relient les journees deja
        // marquees. Seule leur famille change, et c'est sans risque.
        Builtin("cried", "", "J'ai pleuré", TagCategory.EMOTION),
        Builtin("anxiety", "", "Crise d'angoisse", TagCategory.EMOTION, listOf("Grosse angoisse")),

        // Sante, au sens du corps : ce qu'il a dit de la journee. C'est la
        // moitie qui manquait a la carte — elle ne portait qu'un poids, donc
        // elle ne servait qu'un jour sur dix.
        Builtin("headache", "🤕", "Mal de tête", TagCategory.HEALTH),
        Builtin("belly", "😖", "Mal au ventre", TagCategory.HEALTH),
        Builtin("pain", "🦴", "Douleurs", TagCategory.HEALTH),
        Builtin("sick", "🤒", "Malade", TagCategory.HEALTH),
        Builtin("body_tired", "🪫", "Corps épuisé", TagCategory.HEALTH),
        Builtin("body_good", "⚡", "En forme", TagCategory.HEALTH),

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

        // Ce qui reste sans identifiant stable apres ce passage n'a ete
        // reconnu par aucune entree du catalogue : c'est un reste des
        // migrations d'avant les slugs, que plus aucune carte n'affiche et donc
        // que l'on ne pourrait plus decocher. L'application ne permet pas de
        // creer ses propres etiquettes : il n'y a rien d'autre a perdre ici.
        dao.allTags().filter { it.slug == null }.forEach { orphan ->
            dao.deleteTagLinks(orphan.id)
            dao.deleteTag(orphan.id)
        }
    }
}
