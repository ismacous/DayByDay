package com.ismael.daybyday.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * L'identite de DayByDay.
 *
 * Le point de depart n'est pas une couleur de marque : ce sont les quatre
 * couleurs des journees — vert, orange, rouge, noir. Elles portent le sens de
 * l'application et doivent rester les seules taches vives de l'ecran. La
 * couleur de l'application se choisit donc **contre** elles : une prune
 * profonde, qui ne ressemble a aucune des quatre, ne se confond avec aucun
 * etat d'humeur, et reste chaleureuse.
 *
 * Le fond n'est pas blanc mais creme : c'est un carnet, pas un tableau de
 * bord. Le blanc est reserve aux cartes, qui se detachent ainsi du papier
 * sans avoir besoin d'un trait autour.
 */
private val Plum = Color(0xFF7A3E5D)
private val PlumLight = Color(0xFFE9D4DF)
private val Apricot = Color(0xFFD98F63)

private val Paper = Color(0xFFFBF6F1)
private val Ink = Color(0xFF241F26)

private val LightColors = lightColorScheme(
    primary = Plum,
    onPrimary = Color.White,
    primaryContainer = PlumLight,
    onPrimaryContainer = Color(0xFF33132A),
    secondary = Apricot,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E2D4),
    onSecondaryContainer = Color(0xFF41210F),
    tertiary = Color(0xFF4C6B5A),
    background = Paper,
    onBackground = Ink,
    // Les cartes sont blanches sur le creme du fond : c'est ce leger ecart qui
    // les fait exister, sans bordure ni ombre appuyee.
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2EAE3),
    onSurfaceVariant = Color(0xFF5C5259),
    outline = Color(0xFFCFC3BB),
    outlineVariant = Color(0xFFE6DCD4),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE7B4CE),
    onPrimary = Color(0xFF43172F),
    primaryContainer = Color(0xFF5C2A45),
    onPrimaryContainer = Color(0xFFFFD8E7),
    secondary = Color(0xFFEFB48C),
    onSecondary = Color(0xFF4A2410),
    secondaryContainer = Color(0xFF63381E),
    onSecondaryContainer = Color(0xFFFFDCC7),
    tertiary = Color(0xFFA9CDB6),
    background = Color(0xFF16131A),
    onBackground = Color(0xFFEDE4EA),
    surface = Color(0xFF201C25),
    onSurface = Color(0xFFEDE4EA),
    surfaceVariant = Color(0xFF2B2531),
    onSurfaceVariant = Color(0xFFC9BDC6),
    outline = Color(0xFF574C58),
    outlineVariant = Color(0xFF3A3341),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/**
 * Les formes. Rien d'anguleux : des rayons genereux, qui vont avec des cartes
 * qu'on a envie de toucher. La grande taille sert aux cartes de contenu, la
 * petite aux pastilles et aux champs.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * La typographie.
 *
 * Deux hauteurs de voix seulement : les titres, en sans-serif dense, et le
 * texte courant, plus aere. La serif italique n'est pas ici — elle ne
 * s'utilise jamais toute seule, mais toujours comme accent dans un titre, via
 * [accentSerif].
 */
private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
    ),
)

/**
 * L'accent : la serif italique, a la meme taille que le titre qu'elle
 * accompagne. C'est le seul endroit ou les deux polices se cotoient, et c'est
 * ce qui donne son ton a l'application — moderne, mais ecrit a la main.
 */
fun TextStyle.accentSerif(): TextStyle = copy(
    fontFamily = Serif,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
)

@Composable
fun DayByDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
