package com.ismael.daybyday.ui

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.health.HealthSync
import com.ismael.daybyday.ui.theme.DayByDayTheme

class MainActivity : FragmentActivity() {

    /**
     * La destination demandee par une notification, ou `null`.
     *
     * C'est un `mutableStateOf` et pas une simple lecture de l'intention : quand
     * l'application tourne deja, Android ne recree pas l'activite mais appelle
     * [onNewIntent]. Sans etat observable, la notification du lundi n'ouvrirait
     * le bilan qu'une fois sur deux — celle ou l'application etait fermee.
     */
    private var pendingDestination by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = intent.getStringExtra(EXTRA_OPEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applySecureFlag()
        pendingDestination = intent?.getStringExtra(EXTRA_OPEN)

        setContent {
            DayByDayTheme {
                val app = dayByDayApp
                // null tant qu'on ne sait pas encore si la base est vide.
                var offerRestore by remember { mutableStateOf<Boolean?>(null) }
                var welcomeDone by remember { mutableStateOf(app.prefs.onboardingDone) }
                var helloDone by remember { mutableStateOf(app.helloPlayed) }
                LaunchedEffect(Unit) {
                    val empty = app.repository.allDays().isEmpty()
                    // Une installation qui a deja des journees n'est pas une
                    // nouvelle installation : elle vient d'avant l'ecran
                    // d'accueil, et on ne demande pas son prenom a quelqu'un
                    // qui note ses journees depuis un an.
                    if (!empty) {
                        app.prefs.adoptExistingInstall()
                        welcomeDone = true
                    }
                    offerRestore = !app.prefs.firstRunRestoreChecked && empty
                    // Met a jour les pas et le temps d'ecran des jours deja notes.
                    runCatching {
                        HealthSync.syncRecentDays(this@MainActivity, app.repository)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when {
                        app.lock.isLocked -> LockScreen(
                            prefs = app.prefs,
                            onUnlocked = { app.lock.unlock() },
                        )

                        offerRestore == null -> Box(Modifier.fillMaxSize())

                        // L'accueil passe **avant** la reprise de sauvegarde :
                        // « as-tu une sauvegarde ? » est la premiere phrase la
                        // plus froide qu'on puisse adresser a quelqu'un qui
                        // ouvre l'application pour la premiere fois. La reprise
                        // reste a un bouton de la, sur la premiere page.
                        !welcomeDone -> WelcomeScreen(
                            onRestore = {
                                welcomeDone = true
                                app.prefs.onboardingDone = true
                                offerRestore = true
                            },
                            onDone = {
                                welcomeDone = true
                                offerRestore = false
                            },
                        )

                        offerRestore == true -> WelcomeRestoreScreen(
                            onFinished = { offerRestore = false },
                        )

                        !helloDone -> HelloBurstScreen(
                            firstName = app.prefs.firstName,
                            onDone = {
                                app.helloPlayed = true
                                helloDone = true
                            },
                        )

                        else -> AppNavigation(
                            pendingDestination = pendingDestination,
                            onDestinationConsumed = { pendingDestination = null },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        applySecureFlag()
        dayByDayApp.lock.onEnterForeground()
    }

    override fun onStop() {
        super.onStop()
        dayByDayApp.lock.onEnterBackground()
    }

    companion object {
        /** Nom de l'extra qui porte la destination demandee par une notification. */
        const val EXTRA_OPEN = "daybyday.open"

        /** Ouvrir le bilan de la semaine. */
        const val OPEN_WEEK = "semaine"
    }

    /** Empeche les captures d'ecran et masque l'app dans la liste des recentes. */
    fun applySecureFlag() {
        if (dayByDayApp.prefs.blockScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
