package com.ismael.daybyday.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showTabs) {
                FloatingNavBar(
                    items = tabs,
                    currentRoute = currentRoute,
                    onSelect = { route -> navController.switchTab(route) },
                    onToday = {
                        navController.navigate("day/${LocalDate.now().toEpochDay()}")
                    },
                )
            }
        },
    ) { scaffoldPadding ->
        // Le rembourrage du bas ne sert que sous la barre d'onglets. Les ecrans
        // qui s'ouvrent par-dessus gerent leurs propres marges : l'appliquer
        // ici aussi laissait une bande vide sous eux, et une deuxieme quand le
        // clavier s'ouvrait.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (showTabs) scaffoldPadding.calculateBottomPadding() else 0.dp
                ),
        ) {
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
                        onOpenSearch = { navController.navigate("search") },
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
                        onBack = { navController.popBackStack() },
                        onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
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
                    )
                }

                composable("organize-cards") {
                    OrganizeCardsScreen(onBack = { navController.popBackStack() })
                }

                composable("search") {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onDayClick = { date -> navController.navigate("day/${date.toEpochDay()}") },
                    )
                }
            }
        }
    }
}

/** Change d'onglet sans empiler les destinations les unes sur les autres. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
