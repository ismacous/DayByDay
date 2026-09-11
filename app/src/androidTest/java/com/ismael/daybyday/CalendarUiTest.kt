package com.ismael.daybyday

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismael.daybyday.data.Prefs
import com.ismael.daybyday.ui.Dates
import com.ismael.daybyday.ui.MainActivity
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class CalendarUiTest {

    companion object {
        /**
         * Deux ecrans passent **devant** le calendrier au lancement, et les deux
         * doivent etre ecartes avant que la regle ne demarre l'activite.
         *
         * 1. L'ecran de reprise de sauvegarde, qui s'affiche quand la base est
         *    vide — c'est-a-dire toujours, sur un emulateur neuf.
         * 2. L'animation d'accueil (« Salut Ismael »). Elle ne se joue qu'une
         *    fois par lancement du processus, ce qui la rend facile a oublier :
         *    sur un telephone on la voit une seconde et on passe. Dans les
         *    tests, elle recouvrait tout, et les six tests d'interface
         *    echouaient ensemble sur « aucun noeud ne correspond » — ce qui
         *    ressemble a six bugs alors qu'il n'y en a qu'un, et pas dans
         *    l'application.
         */
        @JvmStatic
        @BeforeClass
        fun ecarterLesEcransDAccueil() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            Prefs(context).firstRunRestoreChecked = true
            (context.applicationContext as DayByDayApp).helloPlayed = true
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun leCalendrierDuMoisEnCoursEstAffiche() {
        // La carte du jour est en haut, toujours visible.
        composeRule.onNodeWithText("Aujourd'hui", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        // L'en-tete du calendrier ne porte que le nom du mois : l'annee est sur
        // la ligne du dessous, celle qui ouvre l'annee entiere.
        composeRule.onNodeWithText(Dates.monthTitle(YearMonth.now()).substringBefore(' '))
            .assertExists()
        // Le bilan est en bas de la page : il existe sans forcement etre visible.
        composeRule.onNodeWithText("Bilan du mois").assertExists()
    }

    @Test
    fun colorierUnJourEtEcrireUnTitreEstConserve() {
        val today = LocalDate.now()
        val dayTag = "day-${today.toEpochDay()}"
        val title = "Test ${System.currentTimeMillis()}"

        // Le calendrier est sous la carte du jour : on l'amene a l'ecran avant
        // d'appuyer, sinon le doigt tombe a cote.
        composeRule.onNodeWithTag(dayTag).performScrollTo().performClick()
        composeRule.onNodeWithText("Comment tu te sens").assertExists()

        composeRule.onNodeWithTag("color-GREEN").performScrollTo().performClick()

        // Le journal s'ecrit sur son propre ecran, ouvert depuis son apercu.
        composeRule.onNodeWithTag("journal-preview").performScrollTo().performClick()
        composeRule.onNodeWithTag("day-title-field").performTextInput(title)
        composeRule.onNodeWithContentDescription("Retour").performClick()

        composeRule.onNodeWithContentDescription("Retour").performClick()
        composeRule.onNodeWithTag(dayTag).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Bonne journée").assertExists()
    }

    @Test
    fun colorierAujourdHuiDepuisLAccueil() {
        // Une couleur differente de l'autre test pour rester independant de
        // l'ordre d'execution (un clic sur la couleur deja choisie l'enleve).
        composeRule.onNodeWithTag("today-ORANGE").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Journée mitigée").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun lesOngletsDuBasFonctionnent() {
        // Les onglets se visent par leur repere et non par leur nom : « Argent »
        // ou « Bilan » peuvent aussi etre ecrits dans la page, et la recherche
        // echoue alors sur « deux noeuds au lieu d'un » — une panne qui
        // n'arrive que le jour ou l'on ajoute un titre quelque part.
        //
        // Et on verifie l'arrivee sur un contenu de la page, pas sur son titre :
        // les titres sont animes mot par mot, « Mon bilan » est fait de deux
        // noeuds et ne se cherche pas comme une phrase.
        composeRule.onNodeWithTag("tab-stats").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Mois par mois").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("tab-money").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Corriger mon solde").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("tab-settings").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Rappel quotidien").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("tab-calendar").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Bilan du mois").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * L'annee n'est plus un onglet : ce n'etait pas une destination mais un
     * niveau de zoom du calendrier. On y va en appuyant sur le nom du mois, la
     * ou l'on regarde deja — et c'est **cette porte** qu'on verifie ici.
     *
     * On ne l'ouvre pas. Une version de ce test le faisait, et elle **tuait
     * l'emulateur** : l'annee dessine douze mini-mois, soit environ cinq cents
     * cases, ce qu'une carte graphique logicielle sur deux coeurs ne tient pas.
     * L'appareil disparaissait (« device 'emulator-5554' not found »), et avec
     * lui les vingt et un tests suivants — dont ceux des migrations, les seuls
     * qui protegent vraiment des donnees. Un test qui emporte toute la seance
     * coute plus cher que ce qu'il rapporte. Sur le telephone, l'annee s'ouvre
     * sans broncher : c'est l'emulateur qui ne suit pas, pas l'application.
     */
    @Test
    fun leMoisPorteLaPorteVersLAnnee() {
        composeRule.onNodeWithText("voir l'année", substring = true).assertExists()
    }

    @Test
    fun laRechercheTrouveUneJourneeEcrite() {
        val today = LocalDate.now()
        val marker = "Ruisseau${System.currentTimeMillis() % 100000}"

        composeRule.onNodeWithTag("day-${today.toEpochDay()}").performScrollTo().performClick()
        composeRule.onNodeWithTag("journal-preview").performScrollTo().performClick()
        composeRule.onNodeWithTag("day-note-field").performTextInput(marker)
        composeRule.onNodeWithContentDescription("Retour").performClick()
        composeRule.onNodeWithContentDescription("Retour").performClick()

        composeRule.onNodeWithContentDescription("Rechercher").performClick()
        composeRule.onNodeWithTag("search-field").performTextInput(marker)

        // Le compteur de resultats n'apparait que si la journee est retrouvee
        // en base (le champ de recherche contient deja le texte tape).
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("1 journée(s) trouvée(s)")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun noterUnMomentDeLaJourneeColoreLaJournee() {
        // Un jour que les autres tests ne touchent pas, pour que la couleur du
        // jour soit encore en mode automatique.
        val day = LocalDate.now().minusDays(3)

        composeRule.onNodeWithTag("day-${day.toEpochDay()}").performScrollTo().performClick()
        composeRule.onNodeWithText("Moment par moment").assertExists()

        // Un matin vert et une nuit noire donnent une journée orange en moyenne.
        composeRule.onNodeWithTag("part-MORNING-GREEN").performScrollTo().performClick()
        composeRule.onNodeWithTag("part-NIGHT-BLACK").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Calculée à partir de tes moments.")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
