package com.ismael.daybyday.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.Hashtag
import com.ismael.daybyday.data.PageLink
import com.ismael.daybyday.data.RichText
import com.ismael.daybyday.data.StyleFamily
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind
import com.ismael.daybyday.ui.theme.Hand
import com.ismael.daybyday.ui.theme.Sans
import com.ismael.daybyday.ui.theme.Serif

/**
 * Les polices proposees dans le journal.
 *
 * Les trois premieres sont des fichiers livres dans l'application ; les deux
 * dernieres sont celles d'Android. Dans tous les cas rien n'est telecharge :
 * l'application n'a pas la permission Internet, et n'en aura jamais besoin
 * pour ecrire.
 *
 * Une seule graisse est embarquee par police : Android fabrique le gras et
 * l'italique a partir d'elle, ce qui evite de tripler le poids de l'APK.
 */
val HandFontFamily = Hand
val SerifFontFamily = Serif
val ModernFontFamily = Sans

/** La police d'un style, ou null pour les styles qui n'en changent pas. */
fun TextStyleKind.fontFamily(): FontFamily? = when (this) {
    TextStyleKind.FONT_HAND -> HandFontFamily
    TextStyleKind.FONT_SERIF -> SerifFontFamily
    TextStyleKind.FONT_MODERN -> ModernFontFamily
    TextStyleKind.FONT_SANS -> FontFamily.SansSerif
    TextStyleKind.FONT_MONO -> FontFamily.Monospace
    else -> null
}

/**
 * Pose la mise en forme sur le texte affiche.
 *
 * Elle n'ajoute ni ne retire un seul caractere, donc la correspondance de
 * positions est l'identite : le curseur, la selection et le clavier
 * travaillent sur le texte brut, sans decalage possible. C'est ce qui evite
 * le defaut classique des editeurs riches.
 */
/**
 * Combien de lignes du lignage un titre occupe.
 *
 * C'est ce qui garde la page reguliere : un titre est plus haut qu'une ligne
 * ordinaire, alors plutot que de laisser le texte suivant glisser hors du
 * lignage, on lui donne un nombre **entier** de lignes. Le rythme reprend
 * exactement au paragraphe d'apres.
 */
fun TextStyleKind.lineSpan(): Int = when (this) {
    TextStyleKind.TITLE_1, TextStyleKind.TITLE_2 -> 2
    else -> 1
}

class SpanTransformation(
    private val spans: List<TextSpan>,
    /**
     * La selection a peindre nous-memes. Ouvrir un panneau retire le focus du
     * champ — c'est ce qui ferme le clavier — et un champ sans focus ne peint
     * plus sa selection. Sans ce rappel, on choisirait une couleur sans voir
     * sur quoi elle va s'appliquer.
     */
    private val selection: TextRange? = null,
    private val selectionTint: Color = Color.Unspecified,
    /**
     * La hauteur d'une ligne du lignage. Fournie, les titres se calent sur un
     * nombre entier de lignes ; absente, le texte se met en forme sans se
     * soucier d'un quelconque rythme (l'apercu du calendrier, par exemple).
     */
    private val rhythm: TextUnit? = null,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val held = selection
            ?.takeIf { selectionTint != Color.Unspecified }
            ?.let { range ->
                val from = minOf(range.start, range.end).coerceIn(0, text.length)
                val to = maxOf(range.start, range.end).coerceIn(0, text.length)
                if (to > from) {
                    AnnotatedString.Range(SpanStyle(background = selectionTint), from, to)
                } else {
                    null
                }
            }

        // On ne peut plus sortir tot quand il n'y a aucun intervalle : les
        // mots-cles, eux, sont relus dans le texte a chaque fois.


        val decorated = buildAnnotatedStringWithSpans(text.text, spans, rhythm)
        val result = if (held == null) {
            decorated
        } else {
            // La selection passe en dernier : elle doit se voir par-dessus un
            // surlignage deja pose.
            AnnotatedString(
                text = decorated.text,
                spanStyles = decorated.spanStyles + held,
                paragraphStyles = decorated.paragraphStyles,
            )
        }
        return TransformedText(result, OffsetMapping.Identity)
    }
}

/** Le texte habille de ses intervalles, pour l'ecriture comme pour la relecture. */
fun buildAnnotatedStringWithSpans(
    text: String,
    spans: List<TextSpan>,
    rhythm: TextUnit? = null,
    /**
     * Colorer les mots-cles, ou seulement les mettre en valeur.
     *
     * Sur la page du journal, c'est la **pastille** dessinee derriere qui porte
     * la couleur : le mot lui-meme garde l'encre du papier, et reste donc
     * lisible sur l'ardoise comme sur l'ivoire. Ailleurs — l'apercu d'une
     * carte, une ligne de resultat — il n'y a pas de pastille, alors la couleur
     * passe dans le texte.
     */
    hashtagColors: Boolean = false,
): AnnotatedString = AnnotatedString(
    text = text,
    spanStyles = spans
        .filter { it.start < text.length && it.end <= text.length && !it.isEmpty }
        .map { AnnotatedString.Range(it.style.toSpanStyle(), it.start, it.end) } +
        hashtagSpans(text, hashtagColors) + linkSpans(text),
    paragraphStyles = if (rhythm == null) emptyList() else headingParagraphs(text, spans, rhythm),
)

/**
 * Les mots-cles, mis en valeur.
 *
 * Ils ne viennent pas des intervalles enregistres : ils sont **relus dans le
 * texte** a chaque affichage. C'est ce qui fait qu'effacer le `#` d'un mot le
 * rend ordinaire aussitot, sans rien a nettoyer nulle part.
 */
private fun hashtagSpans(
    text: String,
    colored: Boolean,
): List<AnnotatedString.Range<SpanStyle>> = Hashtag.rangesIn(text).map { range ->
    val style = if (colored) {
        SpanStyle(
            color = hashtagTint(text.substring(range.first + 1, range.last + 1)),
            fontWeight = FontWeight.Medium,
        )
    } else {
        SpanStyle(fontWeight = FontWeight.Medium)
    }
    AnnotatedString.Range(style, range.first, range.last + 1)
}

/**
 * Les liens vers une autre journee, soulignes et colores.
 *
 * Une seule couleur pour tous, et pas la teinte de l'application : ce bleu est
 * celui de la palette du texte, choisi pour rester lisible aussi bien sur
 * l'ivoire que sur l'ardoise. Un lien doit se reconnaitre comme un lien
 * partout, pas prendre la couleur de la page.
 */
private fun linkSpans(text: String): List<AnnotatedString.Range<SpanStyle>> =
    PageLink.linksIn(text).map { link ->
        AnnotatedString.Range(
            SpanStyle(
                color = Color(TextStyleKind.COLOR_BLUE.argb),
                textDecoration = TextDecoration.Underline,
            ),
            link.range.first,
            link.range.last + 1,
        )
    }

/**
 * Les titres, ramenes a des lignes entieres et a une hauteur multiple du
 * lignage.
 *
 * Deux precautions : un titre pose sur trois mots au milieu d'une ligne est
 * etendu a la ligne entiere, et deux titres qui se chevaucheraient ne donnent
 * qu'un seul paragraphe. Compose refuse categoriquement des paragraphes qui se
 * recouvrent, et rien n'empeche l'utilisateur d'en poser deux au meme endroit.
 */
private fun headingParagraphs(
    text: String,
    spans: List<TextSpan>,
    rhythm: TextUnit,
): List<AnnotatedString.Range<ParagraphStyle>> {
    if (text.isEmpty()) return emptyList()

    val wanted = spans
        .filter { it.style.takesWholeLine && !it.isEmpty }
        .mapNotNull { span ->
            val from = span.start.coerceIn(0, text.length - 1)
            val line = RichText.lineRange(text, from, (span.end - 1).coerceIn(from, text.length - 1))
            var end = (line.last + 1).coerceAtMost(text.length)
            // Le retour a la ligne appartient au paragraphe du titre, sinon
            // Compose ouvre un paragraphe vide juste apres.
            if (text.getOrNull(end) == '\n') end += 1
            if (end > line.first) Triple(line.first, end, span.style) else null
        }
        .sortedBy { it.first }

    val result = mutableListOf<AnnotatedString.Range<ParagraphStyle>>()
    var covered = 0
    wanted.forEach { (from, to, style) ->
        if (from < covered) return@forEach
        result += AnnotatedString.Range(
            ParagraphStyle(
                lineHeight = rhythm * style.lineSpan(),
                // La citation se decale pour laisser passer son trait. Le
                // decalage porte sur **toutes** les lignes, sinon seule la
                // premiere s'ecarterait et le trait traverserait le texte.
                textIndent = if (style == TextStyleKind.QUOTE) {
                    TextIndent(firstLine = QUOTE_INDENT, restLine = QUOTE_INDENT)
                } else {
                    TextIndent.None
                },
            ),
            from,
            to,
        )
        covered = to
    }
    return result
}

/**
 * Un titre se rend par une taille et une graisse, pas par un style de
 * paragraphe : c'est ce qui permet de rester sur une transformation purement
 * decorative, et donc de ne jamais decaler le curseur.
 */
fun TextStyleKind.toSpanStyle(): SpanStyle = when (this) {
    TextStyleKind.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    TextStyleKind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    TextStyleKind.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    TextStyleKind.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)

    TextStyleKind.TITLE_1 -> SpanStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold)
    TextStyleKind.TITLE_2 -> SpanStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold)
    TextStyleKind.TITLE_3 -> SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

    // L'italique, et rien de plus : la couleur du trait vient de la couleur
    // posee sur le texte, s'il y en a une, et l'encre de la page sinon. Une
    // citation grisee d'office serait illisible sur l'ardoise.
    TextStyleKind.QUOTE -> SpanStyle(fontStyle = FontStyle.Italic)

    else -> when (this.family) {
        StyleFamily.FONT -> SpanStyle(fontFamily = fontFamily())
        StyleFamily.COLOR -> SpanStyle(color = Color(argb))
        StyleFamily.HIGHLIGHT -> SpanStyle(
            // Le surlignage laisse voir le texte : la teinte est adoucie, et
            // le texte force en sombre pour rester lisible dessus.
            background = Color(argb).copy(alpha = 0.55f),
            color = Color(0xFF1B1B1B),
        )
        else -> SpanStyle()
    }
}

/**
 * La place laissee au trait d'une citation.
 *
 * En `sp` et non en points : c'est un decalage de **texte**, il doit grandir
 * avec les caracteres quand on agrandit la police dans Android, sinon le trait
 * finirait par mordre sur les lettres.
 */
val QUOTE_INDENT = 20.sp
