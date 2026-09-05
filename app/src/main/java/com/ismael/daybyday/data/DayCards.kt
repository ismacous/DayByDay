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
    val emoji: String,
    /** Une carte essentielle ne peut pas etre masquee : elle est le coeur de la journee. */
    val essential: Boolean = false,
    val description: String = "",
) {
    MOOD(
        key = "humeur",
        title = "Comment tu te sens",
        emoji = "🎨",
        essential = true,
        description = "La couleur de la journée et de ses quatre moments.",
    ),
    JOURNAL(
        key = "journal",
        title = "Ton journal",
        emoji = "📓",
        description = "Un titre et le texte libre de la journée.",
    ),
    SLEEP(
        key = "sommeil",
        title = "Sommeil",
        emoji = "😴",
        description = "Heure de coucher et de lever, durée de la nuit.",
    ),
    ACTIVITY(
        key = "activite",
        title = "Activité physique",
        emoji = "🏃",
        description = "Pas, séance de sport, poids du jour.",
    ),
    FOOD(
        key = "alimentation",
        title = "Alimentation",
        emoji = "🍽️",
        description = "Comment tu as mangé, ce que tu as mangé, l'eau bue.",
    ),
    HEALTH(
        key = "sante",
        title = "Santé & traitements",
        emoji = "💊",
        description = "Cocher les médicaments pris, matin, midi, soir, nuit.",
    ),
    OUTSIDE(
        key = "dehors",
        title = "Dehors & écrans",
        emoji = "🚪",
        description = "Être sorti ou non, temps passé sur le téléphone.",
    ),
    TAGS(
        key = "etiquettes",
        title = "Étiquettes",
        emoji = "🏷️",
        description = "Les repères rapides comparés à tes bonnes journées.",
    ),
    MONEY(
        key = "argent",
        title = "Argent du jour",
        emoji = "💶",
        description = "Les rentrées et dépenses de cette journée.",
    ),
    MEDIA(
        key = "medias",
        title = "Photos & vidéos",
        emoji = "📷",
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
