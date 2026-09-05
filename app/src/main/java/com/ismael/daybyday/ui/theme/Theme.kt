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
 * l'application et doivent rester les seules taches vives **du contenu**. La
 * couleur de l'application se choisit donc contre elles : un indigo vif qui
 * file vers le ciel, une menthe pour ce qui va bien, un rose pour ce qui
 * s'anime. Aucune ne ressemble a un etat d'humeur, et ensemble elles donnent
 * le ton — jeune, franc, de bonne humeur.
 *
 * Le fond n'est ni blanc ni creme mais un blanc a peine lavande, choisi pour
 * que l'indigo et la menthe y claquent. C'est le seul role du fond : faire
 * exister ce qu'on pose dessus.
 *
 * Rien n'est plat : les surfaces importantes portent un degrade, les cartes
 * une ombre teintee de la couleur de l'application, et le fond un halo de
 * couleurs qui derive lentement. Une application qu'on ouvre chaque soir doit
 * donner envie d'etre ouverte.
 */
private val Indigo = Color(0xFF5B4DF0)
private val Sky = Color(0xFF3BA6FF)
private val Mint = Color(0xFF17C99A)
private val Blush = Color(0xFFFF7BA9)

private val Cloud = Color(0xFFF5F5FE)
private val Night = Color(0xFF16142B)

/**
 * Les degrades de la marque. Un degrade dit quelque chose qu'une couleur plate
 * ne dit pas : qu'il y a du mouvement dessous.
 */
object Brand {
    val Primary = Indigo
    val PrimaryEnd = Sky
    val Accent = Mint
    val Playful = Blush

    /** Le degrade principal : celui du bouton du jour et des cartes fortes. */
    val gradient = listOf(Indigo, Sky)

    /** Un degrade plus doux, pour les surfaces qui ne doivent pas crier. */
    val softGradient = listOf(Color(0xFF7A6BFF), Color(0xFF5FBEFF))

    /** Le vert de ce qui va bien : progression, reussite, bonne nouvelle. */
    val accentGradient = listOf(Mint, Color(0xFF5BE3C0))
}

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E1FF),
    onPrimaryContainer = Color(0xFF231A6B),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F7EC),
    onSecondaryContainer = Color(0xFF06463A),
    tertiary = Blush,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0EA),
    onTertiaryContainer = Color(0xFF5C0F2C),
    background = Cloud,
    onBackground = Night,
    surface = Color.White,
    onSurface = Night,
    surfaceVariant = Color(0xFFEFEFFA),
    onSurfaceVariant = Color(0xFF5C5A75),
    outline = Color(0xFFC9C7DE),
    outlineVariant = Color(0xFFE6E5F3),
    error = Color(0xFFE0384A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9E93FF),
    onPrimary = Color(0xFF1B1157),
    primaryContainer = Color(0xFF352B8C),
    onPrimaryContainer = Color(0xFFE4E1FF),
    secondary = Color(0xFF45E3B8),
    onSecondary = Color(0xFF00382C),
    secondaryContainer = Color(0xFF075243),
    onSecondaryContainer = Color(0xFFB9F5E4),
    tertiary = Color(0xFFFFA6C4),
    onTertiary = Color(0xFF5C0F2C),
    tertiaryContainer = Color(0xFF7C2848),
    onTertiaryContainer = Color(0xFFFFE0EA),
    background = Color(0xFF0F0E1C),
    onBackground = Color(0xFFEAE8FA),
    surface = Color(0xFF1A1930),
    onSurface = Color(0xFFEAE8FA),
    surfaceVariant = Color(0xFF262541),
    onSurfaceVariant = Color(0xFFB9B6D4),
    outline = Color(0xFF4A4870),
    outlineVariant = Color(0xFF32304F),
    error = Color(0xFFFF9AA4),
    onError = Color(0xFF5C0511),
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
