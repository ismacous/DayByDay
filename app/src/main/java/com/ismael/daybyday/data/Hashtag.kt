package com.ismael.daybyday.data

/**
 * Les mots-cles du journal : `#mood`, `#sport`, `#maman`.
 *
 * Ils ne sont **pas** enregistres a cote du texte, contrairement a la mise en
 * forme : ils *sont* le texte. Un `#` tape dans une phrase reste un `#` dans
 * `note`, donc la recherche, l'export annuel et les apercus les voient sans
 * rien savoir de cette classe, et rien ne peut se desynchroniser. C'est aussi
 * ce qui les distingue des etiquettes : une etiquette est un catalogue fixe
 * qu'on coche, un mot-cle s'invente en ecrivant.
 *
 * Tout ici est du Kotlin pur, sans rien d'Android : les regles sont testees
 * dans `HashtagTest`.
 */
object Hashtag {

    /**
     * Le nombre de teintes.
     *
     * Six, comme les cartes de « Ma journee », et pour la meme raison : c'est
     * la repetition d'une petite palette qui fait un systeme. Trente couleurs
     * differentes ne se retiennent pas, six se reconnaissent.
     */
    const val TINTS = 6

    /**
     * Les intervalles des mots-cles dans un texte, dans l'ordre.
     *
     * Un mot-cle commence par un `#` qui n'est **pas colle a un mot** — sinon
     * `do#5` ou une adresse deviendraient des mots-cles — et il lui faut au
     * moins une lettre : `#3` est un numero, pas un mot-cle. Il court ensuite
     * tant qu'on trouve des lettres, des chiffres, un tiret ou un souligne.
     */
    fun rangesIn(text: String): List<IntRange> {
        val result = mutableListOf<IntRange>()
        var index = 0
        while (index < text.length) {
            if (text[index] != '#') {
                index += 1
                continue
            }
            val before = text.getOrNull(index - 1)
            if (before != null && isWordChar(before)) {
                index += 1
                continue
            }
            var end = index + 1
            while (end < text.length && isWordChar(text[end])) end += 1
            // Un `#` seul, ou suivi de chiffres seulement, n'est pas un mot-cle.
            val body = text.substring(index + 1, end)
            if (body.any { it.isLetter() }) {
                result += index until end
                index = end
            } else {
                index += 1
            }
        }
        return result
    }

    /** Les mots-cles d'un texte, tels qu'ecrits, sans doublon et dans l'ordre. */
    fun namesIn(text: String): List<String> {
        val seen = LinkedHashMap<String, String>()
        rangesIn(text).forEach { range ->
            val raw = text.substring(range.first + 1, range.last + 1)
            // Deux ecritures d'un meme mot-cle n'en font qu'un, et c'est la
            // premiere vue qui donne l'orthographe affichee : ecrire « #Mood »
            // puis « #mood » ne doit pas faire deux mots-cles.
            seen.putIfAbsent(key(raw), raw)
        }
        return seen.values.toList()
    }

    /**
     * La forme qui sert a comparer : sans majuscules ni accents.
     *
     * On reutilise le pliage de la recherche pour que `#ete` et `#été` soient
     * le meme mot-cle, exactement comme chercher « ete » trouve « été ».
     */
    fun key(name: String): String = DaySearch.fold(name.removePrefix("#"))

    /**
     * La teinte d'un mot-cle, entre 0 et [TINTS] exclu.
     *
     * Elle est **calculee a partir du nom**, jamais tiree au sort ni
     * enregistree : `#mood` a donc la meme couleur dans toutes les pages, dans
     * la recherche, et sur un telephone neuf apres une restauration. Le calcul
     * porte sur la forme pliee, donc `#Mood` et `#mood` se ressemblent aussi.
     */
    fun tint(name: String): Int {
        var hash = 0
        for (char in key(name)) {
            hash = hash * 31 + char.code
        }
        return ((hash % TINTS) + TINTS) % TINTS
    }

    /**
     * Les mots-cles d'un ensemble de journees, du plus employe au moins
     * employe, puis par ordre alphabetique.
     *
     * L'ordre compte : une liste alphabetique met en tete des mots-cles ecrits
     * une seule fois il y a deux ans, alors qu'on cherche presque toujours
     * parmi ceux dont on se sert.
     */
    fun counted(texts: List<String>): List<HashtagCount> {
        val labels = LinkedHashMap<String, String>()
        val counts = LinkedHashMap<String, Int>()
        texts.forEach { text ->
            namesIn(text).forEach { name ->
                val key = key(name)
                labels.putIfAbsent(key, name)
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
        return counts.map { (key, count) -> HashtagCount(key, labels.getValue(key), count) }
            .sortedWith(compareByDescending<HashtagCount> { it.count }.thenBy { it.key })
    }

    /** Vrai si [text] contient tous les mots-cles demandes. */
    fun containsAll(text: String, keys: Set<String>): Boolean {
        if (keys.isEmpty()) return true
        val present = namesIn(text).map { key(it) }.toSet()
        return present.containsAll(keys)
    }

    private fun isWordChar(char: Char): Boolean =
        char.isLetterOrDigit() || char == '_' || char == '-'
}

/** Un mot-cle, son orthographe affichee, et le nombre de journees ou il figure. */
data class HashtagCount(val key: String, val label: String, val count: Int)
