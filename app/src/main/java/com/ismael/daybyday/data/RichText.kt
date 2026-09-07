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

/** A quoi sert une mise en forme, et comment elle se combine aux autres. */
enum class StyleFamily {
    /** Gras, italique… Elles se cumulent librement. */
    MARK,

    /** Couleur du texte : une seule a la fois. */
    COLOR,

    /** Couleur de surlignage : une seule a la fois. */
    HIGHLIGHT,

    /** Titre : un seul niveau a la fois, et il prend la ligne entiere. */
    HEADING,

    /** Police de caracteres : une seule a la fois. */
    FONT,

    /**
     * Un bloc : une citation. Comme un titre, il prend la ligne entiere, mais
     * il ne change ni la taille ni la graisse — il change le **cadre**.
     */
    BLOCK,
}

/**
 * Les mises en forme disponibles. Le [code] est enregistre tel quel : le
 * changer casserait la mise en forme des journees deja ecrites. En ajouter
 * est sans risque.
 */
enum class TextStyleKind(
    val code: String,
    val label: String,
    val family: StyleFamily,
    /** Teinte affichee sur le bouton, pour les couleurs et les surlignages. */
    val argb: Long = 0L,
) {
    BOLD("b", "Gras", StyleFamily.MARK),
    ITALIC("i", "Italique", StyleFamily.MARK),
    UNDERLINE("u", "Souligné", StyleFamily.MARK),
    STRIKETHROUGH("s", "Barré", StyleFamily.MARK),

    TITLE_1("t1", "Titre 1", StyleFamily.HEADING),
    TITLE_2("t2", "Titre 2", StyleFamily.HEADING),
    TITLE_3("t3", "Titre 3", StyleFamily.HEADING),

    // Teintes moyennes : lisibles sur fond clair comme sur fond sombre.
    COLOR_RED("cr", "Rouge", StyleFamily.COLOR, 0xFFE1483F),
    COLOR_ORANGE("co", "Orange", StyleFamily.COLOR, 0xFFEF8A2B),
    COLOR_AMBER("ca", "Ambre", StyleFamily.COLOR, 0xFFD4A017),
    COLOR_GREEN("cg", "Vert", StyleFamily.COLOR, 0xFF3FBF6A),
    COLOR_TEAL("ct", "Turquoise", StyleFamily.COLOR, 0xFF1FA6A6),
    COLOR_BLUE("cb", "Bleu", StyleFamily.COLOR, 0xFF4A9BE8),
    COLOR_INDIGO("cn", "Indigo", StyleFamily.COLOR, 0xFF5C6BC0),
    COLOR_VIOLET("cv", "Violet", StyleFamily.COLOR, 0xFF9B6BD6),
    COLOR_PINK("cp", "Rose", StyleFamily.COLOR, 0xFFE0559B),
    COLOR_BROWN("cw", "Brun", StyleFamily.COLOR, 0xFF8D6E63),
    COLOR_GREY("cy", "Gris", StyleFamily.COLOR, 0xFF8A9199),

    // "h" garde son code d'origine : le jaune existait deja.
    HIGHLIGHT("h", "Surligné jaune", StyleFamily.HIGHLIGHT, 0xFFFFD54F),
    HIGHLIGHT_GREEN("hg", "Surligné vert", StyleFamily.HIGHLIGHT, 0xFF9BE8A8),
    HIGHLIGHT_BLUE("hb", "Surligné bleu", StyleFamily.HIGHLIGHT, 0xFF9BD1F5),
    HIGHLIGHT_PINK("hp", "Surligné rose", StyleFamily.HIGHLIGHT, 0xFFF7A8CE),
    HIGHLIGHT_ORANGE("ho", "Surligné orange", StyleFamily.HIGHLIGHT, 0xFFFFC08A),
    HIGHLIGHT_VIOLET("hv", "Surligné violet", StyleFamily.HIGHLIGHT, 0xFFCDB4F0),
    HIGHLIGHT_GREY("hy", "Surligné gris", StyleFamily.HIGHLIGHT, 0xFFD2D7DC),

    // Trois polices sont livrees dans l'application (Caveat, Lora, Poppins,
    // libres de droits), les deux autres viennent d'Android. Les fichiers sont
    // dans l'APK : rien n'est telecharge, ni a l'installation ni a l'usage.
    FONT_HAND("fh", "Manuscrite", StyleFamily.FONT),
    FONT_SERIF("fs", "Serif", StyleFamily.FONT),
    FONT_MODERN("fo", "Moderne", StyleFamily.FONT),
    FONT_SANS("fn", "Sans serif", StyleFamily.FONT),
    FONT_MONO("fm", "Machine à écrire", StyleFamily.FONT),

    /**
     * La citation : un trait coloré le long du paragraphe, et le texte
     * décalé pour lui laisser la place. Rien d'autre — pas de guillemets
     * ajoutés, pas de taille changée : ce qui est cité reste ce qui a été
     * écrit, c'est la marge qui dit qu'on cite.
     */
    QUOTE("q", "Citation", StyleFamily.BLOCK);

    /**
     * Une citation, comme un titre, habille la ligne entiere : ni l'une ni
     * l'autre ne se posent sur trois mots au milieu d'une phrase.
     */
    val takesWholeLine: Boolean
        get() = family == StyleFamily.HEADING || family == StyleFamily.BLOCK

    companion object {
        fun fromCode(code: String): TextStyleKind? = entries.firstOrNull { it.code == code }

        fun of(family: StyleFamily): List<TextStyleKind> = entries.filter { it.family == family }

        val marks: List<TextStyleKind> get() = of(StyleFamily.MARK)

        val headings: List<TextStyleKind> get() = of(StyleFamily.HEADING)

        val colors: List<TextStyleKind> get() = of(StyleFamily.COLOR)

        val highlights: List<TextStyleKind> get() = of(StyleFamily.HIGHLIGHT)

        val fonts: List<TextStyleKind> get() = of(StyleFamily.FONT)

        val blocks: List<TextStyleKind> get() = of(StyleFamily.BLOCK)
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
    fun stylesOn(spans: List<TextSpan>, start: Int, end: Int): Set<TextStyleKind> {
        if (end <= start) {
            // Curseur seul : on regarde ce qui l'entoure, pour que le bouton
            // s'allume quand on pose le curseur dans un mot en gras, ou juste
            // a sa fin — c'est ce qui permet de continuer a taper en gras.
            return spans.filter { start > it.start && start <= it.end }.map { it.style }.toSet()
        }
        return TextStyleKind.entries.filter { style ->
            (start until end).all { position ->
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
        val active = style in stylesOn(spans, start, end)
        return if (active) remove(spans, start, end, style) else add(spans, start, end, style)
    }

    private fun add(
        spans: List<TextSpan>,
        start: Int,
        end: Int,
        style: TextStyleKind,
    ): List<TextSpan> {
        // Couleur, surlignage et titre s'excluent au sein de leur famille :
        // du texte n'a qu'une teinte, et une ligne qu'un niveau de titre.
        val exclusive = style.family != StyleFamily.MARK
        val cleaned = if (exclusive) {
            spans.flatMap { span ->
                if (span.style.family == style.family && span.style != style) {
                    cut(span, start, end)
                } else {
                    listOf(span)
                }
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

        // Au point exact d'une insertion, un debut et une fin ne se comportent
        // pas pareil : le texte tape juste avant un passage en gras le pousse
        // vers la droite, celui tape juste apres ne doit pas devenir gras.
        fun moveStart(offset: Int): Int = when {
            offset < edit.start -> offset
            offset >= edit.oldEnd -> offset + edit.delta
            // Le caractere etait dans la zone remplacee : il n'existe plus.
            else -> edit.start
        }

        fun moveEnd(offset: Int): Int = when {
            offset <= edit.start -> offset
            offset >= edit.oldEnd -> offset + edit.delta
            else -> edit.start
        }

        return merge(
            spans.map { TextSpan(moveStart(it.start), moveEnd(it.end), it.style) }
                .map { it.copy(end = minOf(it.end, after.length)) }
                .filterNot { it.isEmpty }
        )
    }

    /**
     * Retire toute la famille [family] sur [start] jusqu'a [end].
     *
     * Different d'un [toggle] : celui-ci ne retire que ce qui couvre la
     * selection entiere. Pour revenir au texte normal, il faut nettoyer aussi
     * ce qui n'en habille qu'un bout.
     */
    fun clearFamily(
        spans: List<TextSpan>,
        start: Int,
        end: Int,
        family: StyleFamily,
    ): List<TextSpan> {
        if (end <= start) return spans
        return merge(
            spans.flatMap { span ->
                if (span.style.family == family) cut(span, start, end) else listOf(span)
            }
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

    /**
     * Les bornes de la ligne qui contient [offset], ou de toutes les lignes
     * touchees par [start] jusqu'a [end]. Un titre habille la ligne entiere :
     * l'appliquer a trois mots au milieu d'un paragraphe n'aurait pas de sens.
     */
    fun lineRange(text: String, start: Int, end: Int = start): IntRange {
        val from = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0))
            .let { if (it == -1 || start == 0) 0 else it + 1 }
        val next = text.indexOf('\n', end.coerceAtMost(text.length))
        val to = if (next == -1) text.length else next
        return from until maxOf(to, from)
    }

    /**
     * Les bornes du mot qui touche [caret], ou null si le curseur est sur un
     * espace ou une ponctuation.
     *
     * C'est ce que fait le double appui. Compose ne le declenche pas ici, on
     * le refait donc a la main — mais a partir du curseur, pas du point
     * touche : le premier appui a deja pose le curseur au bon endroit, et
     * partir de lui evite d'avoir a convertir des coordonnees d'ecran en
     * position dans un texte qui defile.
     *
     * Un mot est une suite de lettres ou de chiffres. L'apostrophe n'en fait
     * pas partie : dans "l'ami", on veut selectionner "ami".
     */
    fun wordAt(text: String, caret: Int): IntRange? {
        fun isWord(c: Char) = c.isLetterOrDigit() || c == '_'
        val at = caret.coerceIn(0, text.length)
        val after = at < text.length && isWord(text[at])
        val before = at > 0 && isWord(text[at - 1])
        if (!after && !before) return null

        var from = at
        while (from > 0 && isWord(text[from - 1])) from--
        var to = at
        while (to < text.length && isWord(text[to])) to++
        return if (to > from) from until to else null
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
