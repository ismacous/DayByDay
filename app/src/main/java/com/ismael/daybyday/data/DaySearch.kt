package com.ismael.daybyday.data

import java.text.Normalizer

/**
 * Ce qu'on cherche : un mot, et/ou un ressenti.
 *
 * La recherche par texte seule ne repond pas aux questions qu'on se pose
 * vraiment. « Qu'est-ce que j'ai fait pendant mes bonnes journees de ce
 * printemps ? », « c'etait quand, la derniere fois que je suis sorti un jour
 * noir ? » — ces questions-la croisent la couleur, ce qu'on a fait et les
 * etiquettes, pas des mots-cles.
 */
data class SearchFilter(
    val text: String = "",
    /** Les couleurs retenues. Plusieurs couleurs se lisent « ou ». */
    val colors: Set<DayColor> = emptySet(),
    val moved: Boolean = false,
    val wentOut: Boolean = false,
    val ateWell: Boolean = false,
    val withPhoto: Boolean = false,
    val withText: Boolean = false,
    /**
     * Les etiquettes retenues. Plusieurs etiquettes se lisent « et » : chacune
     * ajoutee **reduit** le resultat. C'est le sens qu'on attend quand on
     * cherche une journee precise ; « ou » donnerait une liste qui s'allonge a
     * chaque appui, ce qui n'aide personne.
     */
    val tagIds: Set<Long> = emptySet(),
) {
    val isEmpty: Boolean
        get() = text.isBlank() && colors.isEmpty() && !moved && !wentOut &&
            !ateWell && !withPhoto && !withText && tagIds.isEmpty()

    /** Nombre de criteres actifs, pour le dire a l'ecran. */
    val activeCount: Int
        get() = colors.size + tagIds.size +
            listOf(moved, wentOut, ateWell, withPhoto, withText).count { it } +
            (if (text.isBlank()) 0 else 1)
}

object DaySearch {

    /**
     * Applique le filtre. Tout se fait en memoire, et c'est volontaire : meme
     * apres dix ans, ca fait quelques milliers de lignes, et une requete SQL
     * capable de croiser huit criteres facultatifs serait illisible pour un
     * gain nul.
     */
    fun matching(
        days: List<DayEntry>,
        filter: SearchFilter,
        tagsByDay: Map<Long, Set<Long>>,
        mediaCounts: Map<Long, Int>,
    ): List<DayEntry> {
        if (filter.isEmpty) return emptyList()
        val needle = fold(filter.text.trim())

        return days.filter { entry ->
            if (needle.isNotEmpty() &&
                !fold(entry.title).contains(needle) &&
                !fold(entry.note).contains(needle)
            ) {
                return@filter false
            }
            if (filter.colors.isNotEmpty() && entry.color !in filter.colors) return@filter false
            if (filter.moved && (entry.sportLevel ?: 0) < SportLevel.LIGHT.key) return@filter false
            if (filter.wentOut && entry.wentOut != true) return@filter false
            if (filter.ateWell && entry.foodLevel != FoodLevel.GOOD.key) return@filter false
            if (filter.withPhoto && (mediaCounts[entry.epochDay] ?: 0) == 0) return@filter false
            if (filter.withText && entry.title.isBlank() && entry.note.isBlank()) return@filter false
            if (filter.tagIds.isNotEmpty()) {
                val onDay = tagsByDay[entry.epochDay].orEmpty()
                if (!onDay.containsAll(filter.tagIds)) return@filter false
            }
            true
        }.sortedByDescending { it.epochDay }
    }

    /**
     * Met un texte a plat pour la comparaison : sans majuscules et **sans
     * accents**.
     *
     * Sans ca, chercher « ete » ne trouve pas « été », et personne ne pense a
     * poser les accents dans une barre de recherche.
     *
     * Le pliage se fait **lettre par lettre**, et c'est important : normaliser
     * la chaine entiere puis jeter les accents la raccourcit, donc la position
     * du mot trouve ne correspond plus a l'original — l'extrait affiche autour
     * du mot serait decale d'autant de lettres accentuees qu'il y a avant lui.
     * Ici chaque lettre en donne exactement une, et les deux chaines restent
     * alignees.
     */
    fun fold(text: String): String {
        val builder = StringBuilder(text.length)
        for (char in text) {
            val lower = char.lowercaseChar()
            builder.append(Normalizer.normalize(lower.toString(), Normalizer.Form.NFD).first())
        }
        return builder.toString()
    }
}
