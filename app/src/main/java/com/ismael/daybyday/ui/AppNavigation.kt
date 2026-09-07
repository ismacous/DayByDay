package com.ismael.daybyday.ui

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import com.ismael.daybyday.dayByDayApp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.time.LocalDate
import java.time.YearMonth

private fun YearMonth.toIndex(): Int = year * 12 + (monthValue - 1)

private fun indexToMonth(index: Int): YearMonth = YearMonth.of(index / 12, index % 12 + 1)

/**
 * Les destinations de la barre du bas.
 *
 * "Année" n'y est plus : ce n'etait pas une destination mais un niveau de
 * zoom du calendrier, et elle occupait une place au meme titre que l'argent
 * ou les reglages. On y va maintenant en appuyant sur le nom du mois, la ou
 * l'on regarde deja. Quatre onglets et le bouton du milieu : la barre respire,
 * et chaque onglet peut afficher son nom.
 */
private val tabs = listOf(
    NavItem("calendar", "Mois", Icons.Default.DateRange),
    NavItem("stats", "Bilan", Icons.Default.Star),
    NavItem("money", "Argent", Icons.Default.ShoppingCart),
    NavItem("settings", "Réglages", Icons.Default.Settings),
)

@Composable
fun AppNavigation(
    /** Destination demandee par une notification, ou `null`. */
    pendingDestination: String? = null,
    onDestinationConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    // La notification du lundi ouvre le bilan de la semaine. On consomme la
    // demande tout de suite : sans ca, chaque recomposition rouvrirait l'ecran
    // et on ne pourrait plus en sortir.
    LaunchedEffect(pendingDestination) {
        when (pendingDestination) {
            MainActivity.OPEN_WEEK -> navController.navigate("week")
        }
        if (pendingDestination != null) onDestinationConsumed()
    }

    var monthIndex by rememberSaveable { mutableIntStateOf(YearMonth.now().toIndex()) }
    var yearShown by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // L'annee reste une page a part entiere, mais elle s'ouvre par-dessus le
    // calendrier au lieu d'occuper un onglet.
    val showTabs = tabs.any { it.route == currentRoute }

    // Le fond anime passe **derriere tout** : derriere l'en-tete, derriere les
    // ecrans, derriere la barre du bas. Quand chaque ecran portait le sien, la
    // place prise par l'en-tete et par la barre restait en dehors, et laissait
    // deux bandes plates et opaques en haut et en bas de l'ecran — d'autant plus
    // larges que la barre avait grandi.
    ScreenBackground(modifier = Modifier.fillMaxSize()) {

        // L'en-tete se retire quand on descend et revient quand on remonte.
        // Il est branche sur le defilement par `nestedScroll` plutot que sur
        // chaque ecran : les listes et les colonnes qui defilent annoncent
        // toutes leur mouvement de cette facon, donc aucun ecran n'a rien a
        // declarer. Et la valeur n'est lue que dans la couche graphique de
        // l'en-tete : rien n'est remesure pendant qu'on fait defiler.
        val density = LocalDensity.current
        val headerHeightPx = with(density) { TAB_HEADER_HEIGHT.toPx() }
        var headerOffset by remember { mutableFloatStateOf(0f) }
        val hideOnScroll = remember(headerHeightPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    headerOffset = (headerOffset + available.y).coerceIn(-headerHeightPx, 0f)
                    // On ne consomme rien : l'ecran defile normalement, on ne
                    // fait qu'ecouter.
                    return Offset.Zero
                }
            }
        }
        // Changer d'onglet remet l'en-tete en place : on arrive en haut d'une
        // page, pas au milieu de la precedente.
        LaunchedEffect(currentRoute) { headerOffset = 0f }

        // Le contenu n'est **pas** repousse au-dessus de la barre du bas : il
        // passe dessous. La barre est une pastille qui flotte, avec du vide de
        // chaque cote et au-dessus ; lui reserver un bandeau plein revenait a
        // poser un rectangle opaque en travers de la page, et c'est exactement
        // ce qu'on voyait. Ce sont les ecrans qui reservent la place, en bas de
        // leur contenu, par un [BottomBarSpace] : ainsi la derniere carte se
        // lit entierement, mais tout le reste continue de defiler derriere la
        // barre et derriere la bille.
        //
        // C'est aussi pourquoi il n'y a plus de `Scaffold` ici : son role etait
        // justement de retirer au contenu la place de la barre.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Box(modifier = Modifier.fillMaxSize().nestedScroll(hideOnScroll)) {
                // Les ecrans ne se remplacent plus d'un coup : celui qui arrive
                // monte en apparaissant, celui qui part s'efface. Un basculement
                // brut donne l'impression de changer d'application ; un fondu
                // glisse donne celle de tourner une page.
                NavHost(
                    navController = navController,
                    startDestination = "calendar",
                    enterTransition = {
                        fadeIn(tween(Motion.NORMAL)) +
                            slideInVertically(tween(Motion.NORMAL)) { it / 14 }
                    },
                    exitTransition = { fadeOut(tween(Motion.QUICK)) },
                    popEnterTransition = { fadeIn(tween(Motion.NORMAL)) },
                    popExitTransition = {
                        fadeOut(tween(Motion.QUICK)) +
                            slideOutVertically(tween(Motion.NORMAL)) { it / 14 }
                    },
                ) {

                    composable("calendar") {
                        CalendarScreen(
                            month = indexToMonth(monthIndex),
                            onMonthChange = { monthIndex = it.toIndex() },
                            onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                            onOpenYear = { year ->
                                yearShown = year
                                navController.navigate("year")
                            },
                        )
                    }

                    composable("year") {
                        YearScreen(
                            year = yearShown,
                            onYearChange = { yearShown = it },
                            onMonthClick = { month ->
                                monthIndex = month.toIndex()
                                navController.popBackStack()
                            },
                            onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable("stats") {
                        StatsScreen(onOpenWeek = { navController.navigate("week") })
                    }

                    composable("money") {
                        MoneyScreen(
                            onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                        )
                    }

                    composable("settings") {
                        SettingsScreen(onOpenWeek = { navController.navigate("week") })
                    }

                    composable("week") {
                        WeekReviewScreen(
                            onBack = { navController.popBackStack() },
                            onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                        )
                    }

                    composable(
                        route = "day/{epochDay}",
                        arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
                    ) { entry ->
                        val epochDay = entry.arguments?.getLong("epochDay") ?: LocalDate.now().toEpochDay()
                        DayScreen(
                            initialDate = LocalDate.ofEpochDay(epochDay),
                            onBack = { navController.popBackStack() },
                            onOrganizeCards = { navController.navigate("organize-cards") },
                            onOpenJournal = { day ->
                                navController.navigate("journal/${day.toEpochDay()}")
                            },
                        )
                    }

                    composable(
                        route = "journal/{epochDay}",
                        arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
                    ) { entry ->
                        val day = entry.arguments?.getLong("epochDay") ?: LocalDate.now().toEpochDay()
                        JournalScreen(
                            date = LocalDate.ofEpochDay(day),
                            onBack = { navController.popBackStack() },
                            // Un lien empile une page de plus : le retour
                            // ramene a celle d'ou l'on vient, comme on
                            // l'attend. Sans ca, sauter de page en page
                            // finirait toujours par sortir du journal.
                            onOpenHashtag = { name ->
                                // Toucher un mot-cle repond a « ou est-ce que
                                // je l'ai mis ? » : la recherche s'ouvre deja
                                // filtree dessus.
                                navController.navigate("search?tag=" + Uri.encode(name))
                            },
                            onOpenDay = { target ->
                                navController.navigate("journal/${target.toEpochDay()}")
                            },
                        )
                    }

                    composable("organize-cards") {
                        OrganizeCardsScreen(onBack = { navController.popBackStack() })
                    }

                    composable(
                        route = "search?tag={tag}",
                        arguments = listOf(
                            navArgument("tag") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        ),
                    ) { entry ->
                        SearchScreen(
                            onBack = { navController.popBackStack() },
                            onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                            startHashtag = entry.arguments?.getString("tag"),
                        )
                    }
                }
            }

            if (showTabs) {
                TabHeader(
                    route = currentRoute,
                    firstName = LocalContext.current.dayByDayApp.prefs.firstName.trim(),
                    onOpenSearch = { navController.navigate("search") },
                    modifier = Modifier
                        .height(TAB_HEADER_HEIGHT)
                        .graphicsLayer {
                            translationY = headerOffset
                            // Il s'efface en partant : un titre a moitie sorti
                            // de l'ecran se lit mal et attire l'oeil pour rien.
                            alpha = 1f + headerOffset / headerHeightPx
                        }
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                )
            }
        }

        // La barre du bas, posee **par-dessus** le contenu et non a cote : elle
        // flotte, et la page continue derriere elle.
        if (showTabs) {
            FloatingNavBar(
                items = tabs,
                currentRoute = currentRoute,
                onSelect = { route -> navController.switchTab(route) },
                onToday = { navController.navigate("day/${LocalDate.now().toEpochDay()}") },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * La hauteur reservee a l'en-tete des onglets.
 *
 * Elle est **la meme pour les quatre**, alors que seul le calendrier porte une
 * date sous son titre. C'est volontaire : une hauteur qui change d'un onglet a
 * l'autre ferait sauter le contenu au moment ou l'on change de page, et c'est
 * exactement ce qu'on cherche a eviter ici.
 */
val TAB_HEADER_HEIGHT = 86.dp

/**
 * La place que la barre du bas occupe, reservee **par le contenu** et non par
 * la mise en page.
 *
 * La barre flotte au-dessus des ecrans : rien ne lui est retire, tout passe
 * dessous. Sans ce vide en bas de chaque page, la derniere carte finirait
 * cachee sous la barre. C'est la meme idee que le [TAB_HEADER_HEIGHT] en haut,
 * a l'envers.
 */
@Composable
fun BottomBarSpace() {
    Spacer(
        modifier = Modifier
            .navigationBarsPadding()
            .height(BOTTOM_BAR_SPACE),
    )
}

private val BOTTOM_BAR_SPACE = 104.dp

/**
 * L'en-tete des quatre onglets. Il ne change que de mots.
 *
 * Le bouton de recherche n'appartient qu'au calendrier : c'est la qu'on se dit
 * « c'etait quand, deja ». Le mettre partout ferait une barre d'outils, et une
 * barre d'outils n'accueille personne.
 */
@Composable
private fun TabHeader(
    route: String?,
    firstName: String,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val (text, accent) = when (route) {
        "stats" -> "Mon" to "bilan"
        "money" -> "Mon" to "argent"
        "settings" -> "Mes" to "réglages"
        else -> if (firstName.isEmpty()) "Mon" to "carnet" else "Salut" to firstName
    }

    MorphingTitle(
        text = text,
        accent = accent,
        modifier = modifier,
        subtitle = if (route == "calendar") Dates.dayLong(today) else null,
        trailing = if (route == "calendar") {
            {
                RoundIconButton(
                    icon = Icons.Default.Search,
                    label = "Rechercher",
                    onClick = onOpenSearch,
                )
            }
        } else {
            null
        },
    )
}

/** Change d'onglet sans empiler les destinations les unes sur les autres. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
