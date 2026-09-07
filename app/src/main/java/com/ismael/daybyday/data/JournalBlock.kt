package com.ismael.daybyday.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ce qu'un bloc de page peut etre.
 *
 * Le code est enregistre tel quel : le changer rendrait illisibles les pages
 * deja ecrites. En ajouter est sans risque.
 */
enum class BlockKind(val code: String) {
    /** Du texte : un ou plusieurs paragraphes ecrits d'affilee. */
    TEXT("t"),

    /** Une citation : son texte, son trait, son fond. */
    QUOTE("q"),

    /** Un trait de separation. Il n'a pas de texte. */
    RULE("r"),

    /** Un vocal. Le son vit dans [VoiceNote] ; le bloc dit seulement ou il est. */
    VOICE("v");

    companion object {
        fun fromCode(code: String): BlockKind = entries.firstOrNull { it.code == code } ?: TEXT
    }
}

/**
 * Un morceau de page.
 *
 * **Pourquoi des blocs.** La page etait un seul long champ de texte, et les
 * vocaux etaient ranges dessous. Consequence : rien ne pouvait se glisser
 * *entre* deux paragraphes, et il n'y avait rien a attraper pour deplacer quoi
 * que ce soit — un vocal ne bougeait pas parce qu'il n'existait aucun endroit
 * ou le mettre. Une page est maintenant une **suite de blocs** : on en attrape
 * un, on le monte, les autres s'ecartent.
 *
 * **Un bloc de texte n'est pas un paragraphe.** C'est le point qui rend la
 * chose vivable : appuyer sur Entree ne cree pas un bloc, il ecrit un retour a
 * la ligne comme partout ailleurs. Une page ordinaire n'a donc qu'un seul bloc
 * de texte, et le curseur, la selection, les mots-cles et les liens continuent
 * de travailler dans un seul champ. Un deuxieme bloc n'apparait que quand on
 * pose une citation, un trait ou un vocal **au milieu** du texte : le bloc se
 * coupe en deux, et l'element se met entre.
 *
 * **`note` reste la verite pour tout le reste.** La recherche, l'export de
 * l'annee, les apercus et le PDF lisent une page a plat, sans rien savoir des
 * blocs. Plutot que de les reecrire tous, [JournalBlocks.flatten] recompose ce
 * texte plat a chaque enregistrement. C'est une **projection**, pas une
 * deuxieme source : elle est ecrite a un seul endroit
 * ([DayRepository.saveJournal]) et jamais modifiee de son cote.
 */
@Entity(
    tableName = "journal_blocks",
    indices = [Index("epochDay")],
)
data class JournalBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    /** Le rang dans la page, a partir de 0. */
    val position: Int,
    val kindCode: String = BlockKind.TEXT.code,
    /** Le texte du bloc, brut. Vide pour un trait et pour un vocal. */
    val text: String = "",
    /** Sa mise en forme, en intervalles (voir [RichText]). */
    val spans: String = "",
    /** Bloc vocal : la ligne de `voice_notes` qu'il montre. */
    val voiceId: Long? = null,
    /** Citation : le code du style de trait ([StyleFamily.QUOTE_BAR]), vide si aucun. */
    val barCode: String = "",
    /** Citation : le code du fond ([StyleFamily.QUOTE_FILL]), vide si aucun. */
    val fillCode: String = "",
    /** Trait : lequel des trois. */
    val ruleCode: String = "",
) {
    val kind: BlockKind get() = BlockKind.fromCode(kindCode)

    /** Un bloc de texte qui ne contient rien : il ne merite pas d'etre garde. */
    val isEmptyText: Boolean get() = kind == BlockKind.TEXT && text.isEmpty()
}

/** Une page a plat : ce que voient la recherche, l'export et les apercus. */
data class FlatPage(val text: String, val spans: List<TextSpan>)

/**
 * Passer des blocs au texte plat, et inversement.
 *
 * Tout est ici, sans rien d'Android : c'est ce qui permet de le tester, et
 * c'est aussi ce qui permet a la migration de s'en servir pour decouper les
 * pages deja ecrites.
 */
object JournalBlocks {

    /**
     * Recompose le texte plat d'une page.
     *
     * Les blocs sont colles par un retour a la ligne — sauf apres un trait,
     * qui apporte deja le sien. Un vocal n'ecrit rien : ce n'est pas du texte,
     * et le faire apparaitre dans la recherche n'aurait aucun sens.
     */
    fun flatten(blocks: List<JournalBlock>): FlatPage {
        val out = StringBuilder()
        val spans = mutableListOf<TextSpan>()
        // Un drapeau, et pas « est-ce que ca finit par un retour a la ligne ? ».
        // Depuis qu'un paragraphe vide est un bloc a part entiere, deux blocs
        // vides d'affilee doivent donner deux lignes vides — en regardant le
        // dernier caractere, elles se seraient fondues en une seule.
        var pending = false

        fun separate() {
            if (pending) out.append('\n')
            pending = false
        }

        blocks.forEach { block ->
            when (block.kind) {
                BlockKind.VOICE -> Unit

                BlockKind.RULE -> {
                    separate()
                    val at = out.length
                    // Le trait vit sur une ligne vide qui existe vraiment : le
                    // style est pose sur le saut de ligne lui-meme, donc la
                    // ligne garde sa place dans le rythme du lignage. Ce saut
                    // termine deja sa ligne : le bloc suivant n'en ajoute pas.
                    out.append('\n')
                    pending = false
                    TextStyleKind.fromCode(block.ruleCode)
                        ?.takeIf { it.isRule }
                        ?.let { spans += TextSpan(at, at + 1, it) }
                }

                BlockKind.TEXT, BlockKind.QUOTE -> {
                    separate()
                    val at = out.length
                    out.append(block.text)
                    val to = out.length
                    RichText.decode(block.spans, block.text.length).forEach {
                        spans += TextSpan(it.start + at, it.end + at, it.style)
                    }
                    if (block.kind == BlockKind.QUOTE && to > at) {
                        spans += TextSpan(at, to, TextStyleKind.QUOTE)
                        styleOf(block.barCode, StyleFamily.QUOTE_BAR)
                            ?.let { spans += TextSpan(at, to, it) }
                        styleOf(block.fillCode, StyleFamily.QUOTE_FILL)
                            ?.let { spans += TextSpan(at, to, it) }
                    }
                    pending = true
                }
            }
        }

        return FlatPage(out.toString(), RichText.merge(spans))
    }

    /**
     * Decoupe un texte plat en blocs, **un par paragraphe**.
     *
     * C'est l'operation que fait la migration sur les pages deja ecrites : une
     * citation ecrite avant les blocs doit devenir un bloc, sinon on se
     * retrouverait avec des citations qu'on peut deplacer et d'autres non — une
     * regle a moitie appliquee se voit plus qu'une regle absente.
     *
     * Un paragraphe par bloc, et pas un bloc pour tout le texte suivi : c'est
     * ce qui permet de **remonter un paragraphe** sans toucher aux autres. La
     * premiere version les regroupait pour n'avoir qu'un seul champ de texte
     * par page ; le resultat etait une page ou l'on pouvait deplacer les vocaux
     * et les citations, mais pas ce qu'on avait ecrit.
     *
     * Une ligne vide reste un bloc : c'est une ligne blanche voulue, et la
     * jeter recollerait deux paragraphes que l'on avait separes.
     */
    fun split(text: String, spans: List<TextSpan>, epochDay: Long): List<JournalBlock> {
        val blocks = mutableListOf<JournalBlock>()

        lines(text).forEach { line ->
            val from = line.first
            val to = line.second
            val ruleHere = spans.firstOrNull {
                it.style.isRule && from == to && it.start == to
            }
            val quoteHere = spans.firstOrNull {
                it.style == TextStyleKind.QUOTE && it.start < maxOf(to, from + 1) && from < it.end
            }

            val inner = {
                spans.mapNotNull { span ->
                    when {
                        span.style == TextStyleKind.QUOTE -> null
                        span.style.family == StyleFamily.QUOTE_BAR -> null
                        span.style.family == StyleFamily.QUOTE_FILL -> null
                        span.style.isRule -> null
                        else -> {
                            val s = maxOf(span.start, from)
                            val e = minOf(span.end, to)
                            if (e > s) TextSpan(s - from, e - from, span.style) else null
                        }
                    }
                }
            }

            blocks += when {
                ruleHere != null -> JournalBlock(
                    epochDay = epochDay,
                    position = blocks.size,
                    kindCode = BlockKind.RULE.code,
                    ruleCode = ruleHere.style.code,
                )

                quoteHere != null && to > from -> JournalBlock(
                    epochDay = epochDay,
                    position = blocks.size,
                    kindCode = BlockKind.QUOTE.code,
                    text = text.substring(from, to),
                    spans = RichText.encode(inner()),
                    barCode = spans.firstOrNull {
                        it.style.family == StyleFamily.QUOTE_BAR && it.start < to && from < it.end
                    }?.style?.code.orEmpty(),
                    fillCode = spans.firstOrNull {
                        it.style.family == StyleFamily.QUOTE_FILL && it.start < to && from < it.end
                    }?.style?.code.orEmpty(),
                )

                else -> JournalBlock(
                    epochDay = epochDay,
                    position = blocks.size,
                    kindCode = BlockKind.TEXT.code,
                    text = text.substring(from, to),
                    spans = RichText.encode(inner()),
                )
            }
        }

        return blocks.ifEmpty { listOf(JournalBlock(epochDay = epochDay, position = 0)) }
    }

    /**
     * Les lignes d'un texte, en couples (debut, fin) sans le retour a la ligne.
     *
     * Un texte vide rend **une** ligne vide, pas zero : une page vide a bien un
     * paragraphe ou poser le curseur.
     */
    private fun lines(text: String): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var start = 0
        text.forEachIndexed { index, c ->
            if (c == '\n') {
                out += start to index
                start = index + 1
            }
        }
        out += start to text.length
        return out
    }

    private fun styleOf(code: String, family: StyleFamily): TextStyleKind? =
        TextStyleKind.fromCode(code)?.takeIf { it.family == family }

    /**
     * Accorde les blocs et les vocaux reellement enregistres.
     *
     * Les deux peuvent diverger, et c'est **normal** : l'ecran garde ses blocs
     * en memoire et ne les ecrit qu'en partant, alors qu'un vocal est ecrit des
     * qu'on arrete d'enregistrer — pour ne pas risquer de perdre le son. Si
     * l'application est tuee entre les deux, le vocal existe sans son bloc.
     *
     * Plutot que d'ecrire les blocs a chaque vocal (donc a chaque fois qu'on
     * parle), on repare a l'ouverture : un vocal sans bloc se range a la fin,
     * un bloc qui montre un vocal disparu s'en va. La page se repare toute
     * seule au lieu de payer une ecriture complete a chaque enregistrement.
     */
    fun reconcile(
        blocks: List<JournalBlock>,
        voiceIds: List<Long>,
        epochDay: Long,
    ): List<JournalBlock> {
        val known = voiceIds.toSet()
        val kept = blocks.filter { it.kind != BlockKind.VOICE || it.voiceId in known }
        val shown = kept.mapNotNull { it.voiceId }.toSet()
        val missing = voiceIds.filterNot { it in shown }.map { id ->
            JournalBlock(
                epochDay = epochDay,
                position = 0,
                kindCode = BlockKind.VOICE.code,
                voiceId = id,
            )
        }
        val all = kept + missing
        return if (all.isEmpty()) {
            listOf(JournalBlock(epochDay = epochDay, position = 0))
        } else {
            all.mapIndexed { index, block -> block.copy(position = index) }
        }
    }

    /**
     * Remet les rangs a plat apres un deplacement ou une suppression.
     *
     * Un bloc de texte vide n'est **pas** jete : depuis qu'Entree cree un bloc,
     * un bloc vide est une ligne blanche que l'on a voulue, et la faire
     * disparaitre recollerait deux paragraphes que l'on venait de separer. Une
     * page qui n'aurait plus rien garde quand meme un bloc : il faut bien un
     * endroit ou ecrire.
     */
    fun tidy(blocks: List<JournalBlock>, epochDay: Long): List<JournalBlock> {
        val list = blocks.ifEmpty { listOf(JournalBlock(epochDay = epochDay, position = 0)) }
        return list.mapIndexed { index, block -> block.copy(position = index, epochDay = epochDay) }
    }
}
