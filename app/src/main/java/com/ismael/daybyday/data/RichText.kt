package com.ismael.daybyday.data

/**
 * La mise en forme du journal.
 *
 * Le texte reste du texte brut dans [DayEntry.note] : la recherche, l'export
 * annuel et les apercus du calendrier continuent de le lire sans rien savoir
 * de la mise en forme. Celle-ci vit a cote, sous forme d'intervalles ranges
 * dans une colonne separee. Melanger les deux — du HTML, du Markdown — aurait
 * pollue chaque endroit qui affiche simplement ce qui a ete ecrit.
 *
 * Un intervalle porte sur des positions de caracteres : [start] inclus,
 * [end] exclu, comme partout ailleurs en Kotlin.
 */
data class TextSpan(val start: Int, val end: Int, val style: TextStyleKind) {
    val isEmpty: Boolean get() = end <= start
}

/**
 * Les mises en forme disponibles. Le [code] est enregistre tel quel : le
 * changer casserait la mise en forme des journees deja ecrites.
 */
enum class TextStyleKind(val code: String, val label: String) {
    BOLD("b", "Gras"),
    ITALIC("i", "Italique"),
    UNDERLINE("u", "Souligné"),
    STRIKETHROUGH("s", "Barré"),
    HIGHLIGHT("h", "Surligné"),
    COLOR_RED("cr", "Rouge"),
    COLOR_ORANGE("co", "Orange"),
    COLOR_GREEN("cg", "Vert"),
    COLOR_BLUE("cb", "Bleu"),
    COLOR_VIOLET("cv", "Violet");

    /** Une couleur remplace la precedente : deux teintes ne se superposent pas. */
    val isColor: Boolean get() = code.startsWith("c")

    companion object {
        fun fromCode(code: String): TextStyleKind? = entries.firstOrNull { it.code == code }

        val colors: List<TextStyleKind> get() = entries.filter { it.isColor }

        val marks: List<TextStyleKind> get() = entries.filter { !it.isColor }
    }
}

object RichText {

    private const val SPAN_SEPARATOR = ";"
    private const val FIELD_SEPARATOR = ","

    /**
     * Les mises en forme actives sur [range], c'est-a-dire celles qui couvrent
     * la selection entiere. Une mise en forme qui n'en couvre qu'une partie
     * n'est pas "active" : reappuyer sur le bouton doit l'etendre, pas l'enlever.
     */
    fun stylesOn(spans: List<TextSpan>, range: IntRange): Set<TextStyleKind> {
        if (range.isEmpty()) {
            // Curseur seul : on regarde ce qui l'entoure, pour que le bouton
            // s'allume quand on pose le curseur au milieu d'un mot en gras.
            val at = range.first
            return spans.filter { at > it.start && at <= it.end }.map { it.style }.toSet()
        }
        return TextStyleKind.entries.filter { style ->
            (range.first until range.last).all { position ->
                spans.any { it.style == style && position >= it.start && position < it.end }
            }
        }.toSet()
    }

    /**
     * Applique ou retire [style] sur [start] jusqu'a [end]. Deja actif partout,
     * il est retire ; sinon il est etendu a toute la selection, ce qui rend le
     * bouton previsible quel que soit l'etat du texte dessous.
     */
    fun toggle(
        spans: List<TextSpan>,
        start: Int,
        end: Int,
        style: TextStyleKind,
    ): List<TextSpan> {
        if (end <= start) return spans
        val active = style in stylesOn(spans, start..end)
        return if (active) remove(spans, start, end, style) else add(spans, start, end, style)
    }

    private fun add(
        spans: List<TextSpan>,
        start: Int,
        end: Int,
        style: TextStyleKind,
    ): List<TextSpan> {
        // Une couleur chasse les autres : du texte n'a qu'une teinte a la fois.
        val cleaned = if (style.isColor) {
            spans.flatMap { span ->
                if (span.style.isColor && span.style != style) cut(span, start, end) else listOf(span)
            }
        } else {
            spans
        }
        return merge(cleaned + TextSpan(start, end, style))
    }

    private fun remove(
        spans: List<TextSpan>,
        start: Int,
        end: Int,
        style: TextStyleKind,
    ): List<TextSpan> = spans.flatMap { span ->
        if (span.style == style) cut(span, start, end) else listOf(span)
    }

    /** Ce qui reste de [span] une fois le morceau [start, end) retire. */
    private fun cut(span: TextSpan, start: Int, end: Int): List<TextSpan> {
        if (end <= span.start || start >= span.end) return listOf(span)
        return listOfNotNull(
            TextSpan(span.start, minOf(start, span.end), span.style).takeUnless { it.isEmpty },
            TextSpan(maxOf(end, span.start), span.end, span.style).takeUnless { it.isEmpty },
        )
    }

    /** Recolle les intervalles d'un meme style qui se touchent ou se chevauchent. */
    fun merge(spans: List<TextSpan>): List<TextSpan> {
        val result = mutableListOf<TextSpan>()
        spans.filterNot { it.isEmpty }
            .groupBy { it.style }
            .forEach { (style, group) ->
                var current: TextSpan? = null
                group.sortedBy { it.start }.forEach { span ->
                    val open = current
                    current = if (open == null || span.start > open.end) {
                        open?.let { result += it }
                        span
                    } else {
                        open.copy(end = maxOf(open.end, span.end))
                    }
                }
                current?.let { result += it }
            }
        return result.sortedWith(compareBy({ it.start }, { it.style.ordinal }))
    }

    /** La zone reellement remplacee entre deux versions d'un texte. */
    data class Edit(val start: Int, val oldEnd: Int, val newEnd: Int) {
        val delta: Int get() = newEnd - oldEnd
        val inserted: IntRange get() = start until newEnd
    }

    /**
     * Situe la modification a partir du seul texte avant et apres.
     *
     * On ne sait pas ce que l'utilisateur a fait : le prefixe et le suffixe
     * communs suffisent a cerner ce qui a change, et c'est tout ce dont la
     * mise en forme a besoin.
     */
    fun diff(before: String, after: String): Edit {
        var prefix = 0
        val shortest = minOf(before.length, after.length)
        while (prefix < shortest && before[prefix] == after[prefix]) prefix++

        var suffix = 0
        while (
            suffix < shortest - prefix &&
            before[before.length - 1 - suffix] == after[after.length - 1 - suffix]
        ) {
            suffix++
        }

        return Edit(
            start = prefix,
            oldEnd = before.length - suffix,
            newEnd = after.length - suffix,
        )
    }

    /**
     * Rattrape les positions apres une modification du texte : ce qui est
     * avant ne bouge pas, ce qui est apres se decale, et ce qui habillait le
     * texte remplace disparait avec lui.
     */
    fun adjust(spans: List<TextSpan>, before: String, after: String): List<TextSpan> {
        if (spans.isEmpty() || before == after) return spans
        val edit = diff(before, after)

        fun move(offset: Int): Int = when {
            offset <= edit.start -> offset
            offset >= edit.oldEnd -> offset + edit.delta
            // Le caractere etait dans la zone remplacee : il n'existe plus.
            else -> edit.start
        }

        return merge(
            spans.map { TextSpan(move(it.start), move(it.end), it.style) }
                .map { it.copy(end = minOf(it.end, after.length)) }
                .filterNot { it.isEmpty }
        )
    }

    /** Pose [styles] sur [start] jusqu'a [end], sans rien retirer d'existant. */
    fun applyAll(
        spans: List<TextSpan>,
        start: Int,
        end: Int,
        styles: Set<TextStyleKind>,
    ): List<TextSpan> {
        if (end <= start || styles.isEmpty()) return spans
        return styles.fold(spans) { current, style -> add(current, start, end, style) }
    }

    /** Format compact : "debut,fin,code;debut,fin,code". */
    fun encode(spans: List<TextSpan>): String = merge(spans).joinToString(SPAN_SEPARATOR) {
        "${it.start}$FIELD_SEPARATOR${it.end}$FIELD_SEPARATOR${it.style.code}"
    }

    /**
     * Relit le format compact. Tout ce qui n'a pas de sens est ignore : une
     * mise en forme abimee ne doit jamais empecher de relire ce qui a ete
     * ecrit. [textLength] borne les intervalles au texte reellement present.
     */
    fun decode(encoded: String?, textLength: Int): List<TextSpan> {
        if (encoded.isNullOrBlank()) return emptyList()
        val spans = encoded.split(SPAN_SEPARATOR).mapNotNull { part ->
            val fields = part.split(FIELD_SEPARATOR)
            if (fields.size != 3) return@mapNotNull null
            val start = fields[0].toIntOrNull() ?: return@mapNotNull null
            val end = fields[1].toIntOrNull() ?: return@mapNotNull null
            val style = TextStyleKind.fromCode(fields[2]) ?: return@mapNotNull null
            TextSpan(
                start = start.coerceIn(0, textLength),
                end = end.coerceIn(0, textLength),
                style = style,
            ).takeUnless { it.isEmpty }
        }
        return merge(spans)
    }
}
