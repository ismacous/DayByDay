package com.ismael.daybyday.data

import java.time.LocalDate

/**
 * Les liens d'une page vers une autre : `@07/09/2026`.
 *
 * Meme principe que les mots-cles, et pour les memes raisons : un lien **est**
 * du texte. On ne le remplace pas a l'affichage par « mardi 7 septembre » —
 * c'est ce qui permet a la transformation visuelle de garder
 * `OffsetMapping.Identity`, donc au curseur de ne jamais se decaler d'un
 * caractere. Un lien substitue serait plus joli une seconde, et faux tout le
 * reste du temps.
 *
 * La forme est donc choisie pour se lire telle quelle : `@07/09/2026` est une
 * date que n'importe qui reconnait, et qui ne demande a personne de deviner un
 * identifiant.
 */
object PageLink {

    /** La forme ecrite d'un lien vers [date]. */
    fun format(date: LocalDate): String =
        "@%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)

    /**
     * Les liens d'un texte, avec la journee visee.
     *
     * Un `@` colle a un mot n'ouvre pas un lien — sinon une adresse de courriel
     * en deviendrait un — et la date doit exister vraiment : `@31/02/2026` reste
     * du texte ordinaire, pas un lien mort.
     */
    fun linksIn(text: String): List<PageReference> {
        val result = mutableListOf<PageReference>()
        var index = 0
        while (index < text.length) {
            if (text[index] != '@') {
                index += 1
                continue
            }
            val before = text.getOrNull(index - 1)
            if (before != null && (before.isLetterOrDigit() || before == '_' || before == '.')) {
                index += 1
                continue
            }
            val date = parseAt(text, index)
            if (date == null) {
                index += 1
                continue
            }
            val end = index + LENGTH
            result += PageReference(index until end, date)
            index = end
        }
        return result
    }

    /** La journee visee par le lien qui commence a [at], ou null si ce n'en est pas un. */
    private fun parseAt(text: String, at: Int): LocalDate? {
        val end = at + LENGTH
        if (end > text.length) return null
        // Rien ne doit continuer la date, sinon `@07/09/20261` passerait pour
        // un lien vers 2026 suivi d'un « 1 » perdu.
        val after = text.getOrNull(end)
        if (after != null && after.isLetterOrDigit()) return null

        val body = text.substring(at + 1, end)
        if (body[2] != '/' || body[5] != '/') return null
        val digits = body.filterIndexed { i, _ -> i != 2 && i != 5 }
        if (digits.length != 8 || digits.any { !it.isDigit() }) return null

        val day = body.substring(0, 2).toInt()
        val month = body.substring(3, 5).toInt()
        val year = body.substring(6, 10).toInt()
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    /** Le lien qui contient la position [offset], s'il y en a un. */
    fun at(text: String, offset: Int): PageReference? =
        linksIn(text).firstOrNull { offset >= it.range.first && offset <= it.range.last }

    /** `@` + `dd/mm/yyyy`, soit onze caracteres, toujours. */
    private const val LENGTH = 11
}

/** Un lien dans un texte : l'endroit ou il est ecrit, et la journee qu'il vise. */
data class PageReference(val range: IntRange, val date: LocalDate)
