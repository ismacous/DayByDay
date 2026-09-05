package com.ismael.daybyday.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.R
import com.ismael.daybyday.data.TextSpan
import com.ismael.daybyday.data.TextStyleKind

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
val HandFontFamily = FontFamily(Font(R.font.caveat_regular))
val SerifFontFamily = FontFamily(Font(R.font.lora_regular))
val ModernFontFamily = FontFamily(Font(R.font.poppins_regular))

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
class SpanTransformation(private val spans: List<TextSpan>) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (spans.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        return TransformedText(
            buildAnnotatedStringWithSpans(text.text, spans),
            OffsetMapping.Identity,
        )
    }
}

/** Le texte habille de ses intervalles, pour l'ecriture comme pour la relecture. */
fun buildAnnotatedStringWithSpans(text: String, spans: List<TextSpan>): AnnotatedString =
    AnnotatedString(
        text = text,
        spanStyles = spans
            .filter { it.start < text.length && it.end <= text.length && !it.isEmpty }
            .map { AnnotatedString.Range(it.style.toSpanStyle(), it.start, it.end) },
    )

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

    else -> when (this.family) {
        com.ismael.daybyday.data.StyleFamily.FONT -> SpanStyle(fontFamily = fontFamily())
        com.ismael.daybyday.data.StyleFamily.COLOR -> SpanStyle(color = Color(argb))
        com.ismael.daybyday.data.StyleFamily.HIGHLIGHT -> SpanStyle(
            // Le surlignage laisse voir le texte : la teinte est adoucie, et
            // le texte force en sombre pour rester lisible dessus.
            background = Color(argb).copy(alpha = 0.55f),
            color = Color(0xFF1B1B1B),
        )
        else -> SpanStyle()
    }
}
