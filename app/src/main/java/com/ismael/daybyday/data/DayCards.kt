package com.ismael.daybyday.data

/**
 * Les cartes de l'ecran "Ma journee". Chacune s'affiche, se replie, se masque
 * et se deplace independamment des autres.
 *
 * Le [key] est un identifiant stable, comme le slug des etiquettes : c'est lui
 * qui est enregistre dans les preferences. Le renommer casserait la disposition
 * choisie par l'utilisateur ; changer [title] ou [emoji] est sans risque.
 *
 * L'ordre de declaration est la disposition par defaut, et sert aussi de place
 * d'arrivee aux cartes ajoutees par une future mise a jour : une carte inconnue
 * des preferences vient se ranger la, visible, plutot que de disparaitre.
 */
enum class DayCard(
    val key: String,
    val title: String,
    /** Une carte essentielle ne peut pas etre masquee : elle est le coeur de la journee. */
    val essential: Boolean = false,
    val description: String = "",
    /**
     * Famille d'etiquettes montree dans cette carte. Les reperes rapides ne
     * vivent plus dans une liste a part : chacun s'affiche la ou il a du sens,
     * sous la question qu'il precise.
     */
    val tagCategory: TagCategory? = null,
    /**
     * Cette carte propose d'y attacher une photo ou une video. Toutes ne le
     * font pas : un bouton sur chacune encombrerait l'ecran pour rien.
     */
    val canHoldMedia: Boolean = false,
) {
    MOOD(
        key = "humeur",
        title = "Comment tu te sens",
        essential = true,
        description = "La couleur de la journée et de ses quatre moments.",
    ),
    JOURNAL(
        key = "journal",
        title = "Ton journal",
        description = "Un titre et le texte libre de la journée.",
        canHoldMedia = true,
    ),
    SLEEP(
        key = "sommeil",
        title = "Sommeil",
        description = "Heure de coucher et de lever, durée de la nuit.",
        tagCategory = TagCategory.SLEEP,
    ),
    ACTIVITY(
        key = "activite",
        title = "Activité physique",
        description = "Pas, séance de sport, poids du jour.",
        tagCategory = TagCategory.ACTIVITY,
        canHoldMedia = true,
    ),
    FOOD(
        key = "alimentation",
        title = "Alimentation",
        description = "Comment tu as mangé, ce que tu as mangé, l'eau bue.",
        tagCategory = TagCategory.FOOD,
        canHoldMedia = true,
    ),
    HEALTH(
        key = "sante",
        title = "Santé & traitements",
        description = "Cocher les médicaments pris, matin, midi, soir, nuit.",
        tagCategory = TagCategory.HEALTH,
        canHoldMedia = true,
    ),
    SOCIAL(
        key = "social",
        title = "Qui tu as vu",
        description = "Les gens qui ont compté dans la journée.",
        tagCategory = TagCategory.SOCIAL,
        canHoldMedia = true,
    ),
    WORK(
        key = "travail",
        title = "Travail & démarches",
        description = "Recherche d'emploi, paperasse, entretiens.",
        tagCategory = TagCategory.WORK,
        canHoldMedia = true,
    ),
    OUTSIDE(
        key = "dehors",
        title = "Dehors & écrans",
        description = "Être sorti ou non, temps passé sur le téléphone.",
        tagCategory = TagCategory.SCREENS,
    ),
    MONEY(
        key = "argent",
        title = "Argent du jour",
        description = "Les rentrées et dépenses de cette journée.",
        tagCategory = TagCategory.MONEY,
        canHoldMedia = true,
    ),
    MEDIA(
        key = "medias",
        title = "Photos & vidéos",
        description = "Les souvenirs attachés à la journée.",
    );

    companion object {
        fun fromKey(key: String?): DayCard? = entries.firstOrNull { it.key == key }

        /**
         * Remet une disposition enregistree en ordre de marche : les cartes
         * connues d'abord, dans l'ordre choisi, puis celles que l'utilisateur
         * n'a jamais vues (ajoutees par une mise a jour) a leur place d'origine.
         * Les cles inconnues, restes d'une version plus recente, sont ignorees.
         */
        fun order(savedKeys: List<String>): List<DayCard> {
            val chosen = savedKeys.mapNotNull { fromKey(it) }.distinct()
            val missing = entries.filter { it !in chosen }
            if (missing.isEmpty()) return chosen

            val result = chosen.toMutableList()
            missing.sortedBy { it.ordinal }.forEach { card ->
                // La nouvelle carte se glisse juste apres la derniere de celles
                // qui la precedent dans la disposition d'origine. Viser plutot
                // la premiere qui la suit ferait remonter la carte neuve tout en
                // haut des qu'une carte de fin a ete deplacee la : l'ordre
                // choisi doit rester intact.
                val after = result.indexOfLast { it.ordinal < card.ordinal }
                result.add(after + 1, card)
            }
            return result
        }
    }
}
