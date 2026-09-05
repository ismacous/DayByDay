package com.ismael.daybyday.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.ismael.daybyday.R

/**
 * Les deux voix de l'application.
 *
 * [Sans] dit les choses : dates, chiffres, boutons, textes courants. C'est une
 * geometrique moderne, nette, qui ne fatigue pas a petite taille.
 *
 * [Serif] les accentue : un seul mot d'un titre, une annee, une phrase qu'on
 * veut faire respirer. En italique, elle apporte le geste de la main la ou une
 * sans-serif reste neutre.
 *
 * Ce contraste — une moderne et une elegante — est ce qui donne son caractere
 * a l'application. Le regle : jamais les deux pour dire la meme chose, et
 * jamais la serif pour un paragraphe entier. Elle souligne, elle ne raconte
 * pas.
 *
 * [Hand] reste reservee au journal, ou l'utilisateur choisit lui-meme.
 *
 * Tous les fichiers sont dans l'APK, sous licence libre : rien n'est
 * telecharge, l'application n'a aucun acces reseau.
 */
val Sans = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_semibold, FontWeight.Bold),
)

val Serif = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.lora_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
)

val Hand = FontFamily(Font(R.font.caveat_regular))
