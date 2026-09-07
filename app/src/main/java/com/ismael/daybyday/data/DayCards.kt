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
     * Famille d'etiquettes dont cette carte est responsable.
     *
     * Elle ne dit plus **comment** les reperes s'affichent — chaque carte leur
     * donne maintenant la forme de sa question : un selecteur pour le sommeil,
     * des cases a cocher pour les demarches, des pastilles pour le reste. Elle
     * dit seulement qu'aucune famille ne se retrouve sans carte, donc sans
     * moyen d'etre decochee. C'est ce qu'un test verifie.
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
        description = "Pas, sorties, séance de sport, ce que tu as fait.",
        canHoldMedia = true,
        tagCategory = TagCategory.ACTIVITY,
    ),
    FOOD(
        key = "alimentation",
        title = "Alimentation",
        description = "Comment tu as mangé, ce que tu as mangé, l'eau bue.",
        canHoldMedia = true,
        tagCategory = TagCategory.FOOD,
    ),
    HEALTH(
        key = "sante",
        title = "Santé",
        description = "Ton poids, et ce que ton corps a dit de la journée.",
        canHoldMedia = true,
        tagCategory = TagCategory.HEALTH,
    ),
    // Les traitements ont quitte la carte Sante : ils ne concernent pas toutes
    // les periodes de la vie. En carte a part, ils se masquent d'un geste dans
    // « Organiser ma journee » quand il n'y a rien a prendre, et reviennent
    // entiers le jour ou il y a de nouveau quelque chose.
    TREATMENT(
        key = "traitements",
        title = "Traitements",
        description = "Les médicaments à cocher, et les rendez-vous médicaux.",
        canHoldMedia = true,
        tagCategory = TagCategory.MEDICAL,
    ),
    SOCIAL(
        key = "social",
        title = "Qui tu as vu",
        description = "Les gens qui ont compté dans la journée.",
        canHoldMedia = true,
        tagCategory = TagCategory.SOCIAL,
    ),
    WORK(
        key = "travail",
        title = "Travail & recherche",
        description = "Candidatures envoyées, entretiens, démarches, freelance.",
        canHoldMedia = true,
        tagCategory = TagCategory.WORK,
    ),
    // La cle reste « dehors » : c'est elle qui est enregistree dans la
    // disposition choisie. Le titre, lui, a change — sortir ou non est parti
    // dans l'activite physique, ou il a un sens, et il ne reste ici que les
    // ecrans.
    OUTSIDE(
        key = "dehors",
        title = "Écrans",
        description = "Temps passé sur le téléphone, et sur quoi.",
        tagCategory = TagCategory.SCREENS,
    ),
    MONEY(
        key = "argent",
        title = "Argent du jour",
        description = "Les rentrées et dépenses de cette journée.",
        canHoldMedia = true,
    ),
    // Comme les traitements : une carte a part, qu'on masque quand elle ne
    // concerne pas la periode qu'on traverse, et qu'on remet entiere le jour ou
    // elle redevient utile. Se laver n'est un sujet que pour certains, et
    // certains jours — mais ces jours-la, c'en est un vrai.
    HYGIENE(
        key = "hygiene",
        title = "Hygiène",
        description = "La douche, et les trois brossages de dents.",
    ),
    PRAYER(
        key = "priere",
        title = "Prières",
        description = "Cocher les cinq prières de la journée.",
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
