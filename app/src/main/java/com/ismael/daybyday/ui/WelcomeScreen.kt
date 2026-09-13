package com.ismael.daybyday.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.ui.theme.Brand

/**
 * L'arrivee dans l'application, la toute premiere fois.
 *
 * Avant, la premiere chose qu'on voyait etait « Bonjour Ismael » — sur le
 * telephone d'Ismael c'est juste, partout ailleurs l'application dit bonjour a
 * un inconnu. Trois pages suffisent a corriger ca, et elles se limitent a ce
 * qu'aucun autre ecran ne peut dire :
 *
 * 1. **ce que fait l'application** — une couleur par jour, et ce qu'on en
 *    tire ;
 * 2. **comment on t'appelle** — la seule chose que le telephone ne sait pas ;
 * 3. **le droit de te faire signe** — l'autorisation de notification, demandee
 *    la une fois pour toutes plutot que par un interrupteur perdu dans les
 *    reglages.
 *
 * Ce qu'on ne fait **pas** : expliquer les cartes, le journal, les etiquettes
 * ou le bilan. Un mode d'emploi en six ecrans se saute, et l'application est
 * faite pour s'apprendre en s'en servant. La regle « pas de petits textes
 * explicatifs » vaut ici comme ailleurs — a la difference pres qu'une page
 * d'accueil a le droit de dire de quoi il s'agit, une fois.
 *
 * Rien n'est bloquant : on peut passer le prenom (l'application dira « toi »)
 * et refuser les notifications.
 */
@Composable
fun WelcomeScreen(onRestore: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.dayByDayApp.prefs

    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(prefs.firstName) }

    val finish = {
        prefs.firstName = name
        prefs.onboardingDone = true
        // On vient de proposer la reprise d'une sauvegarde : l'ecran qui la
        // propose de son cote n'a plus rien a demander.
        prefs.firstRunRestoreChecked = true
        onDone()
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Accordee ou non, on continue : personne ne doit rester coince sur
        // une page d'accueil parce qu'il a dit non.
        finish()
    }

    ScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp)
                .testTag("welcome"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(tween(Motion.NORMAL)) +
                            slideInHorizontally(tween(Motion.NORMAL)) { it / 6 },
                        initialContentExit = fadeOut(tween(Motion.QUICK)) +
                            slideOutHorizontally(tween(Motion.NORMAL)) { -it / 6 },
                        // Sans lui, la boite se redimensionne en decoupant et le
                        // texte apparait tronque au milieu du changement.
                        sizeTransform = SizeTransform(clip = false),
                    )
                },
                label = "accueil",
            ) { page ->
                when (page) {
                    0 -> IntroPage()
                    1 -> NamePage(name = name, onName = { name = it }, onNext = { step = 2 })
                    else -> NotificationsPage()
                }
            }

            Spacer(Modifier.weight(1f))

            Steps(current = step)

            Spacer(Modifier.height(20.dp))

            when (step) {
                0 -> {
                    BigButton(label = "Commencer", onClick = { step = 1 })
                    TextButton(onClick = onRestore) {
                        Text("J'ai déjà une sauvegarde")
                    }
                }

                1 -> {
                    BigButton(label = "Continuer", onClick = { step = 2 })
                    TextButton(onClick = { name = ""; step = 2 }) {
                        Text("Passer")
                    }
                }

                else -> {
                    BigButton(
                        label = "D'accord",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                finish()
                            }
                        },
                    )
                    TextButton(onClick = finish) {
                        Text("Pas maintenant")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Ce que fait l'application, en une phrase et quatre couleurs. */
@Composable
private fun IntroPage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DayColor.entries.forEachIndexed { index, color ->
                Appear(index = index) {
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(color.gradient)),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Title("Une couleur par jour")

        Spacer(Modifier.height(12.dp))

        Line(
            "Tu notes comment ta journée s'est passée. Au bout de quelques " +
                "semaines, l'application te montre ce qui revient avec les bonnes."
        )
    }
}

/** La seule chose que le telephone ne sait pas. */
@Composable
private fun NamePage(name: String, onName: (String) -> Unit, onNext: () -> Unit) {
    val focus = LocalFocusManager.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Title("On t'appelle comment ?")

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { onName(it.take(20)) },
            singleLine = true,
            placeholder = { Text("Ton prénom") },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                // Fermer le clavier ferme aussi la page : sinon la validation
                // laisse le clavier ouvert sur la page suivante, qui n'a rien
                // a y ecrire.
                focus.clearFocus()
                onNext()
            }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Le droit de faire signe, demande une fois. */
@Composable
private fun NotificationsPage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Title("Je peux te faire signe ?")

        Spacer(Modifier.height(18.dp))

        Meeting(emoji = "🌙", text = "Le soir, si la journée n'est pas notée")
        Meeting(emoji = "📊", text = "Le lundi matin, le bilan de ta semaine")
        Meeting(emoji = "💬", text = "Un mot quand il y a quelque chose à dire")
    }
}

@Composable
private fun Meeting(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Line(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Trois points : on sait ou on en est, et combien il en reste. */
@Composable
private fun Steps(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == current) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) {
                            Brand.Primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                        }
                    ),
            )
        }
    }
}

/** Le bouton qui fait avancer : le seul element vif de la page. */
@Composable
private fun BigButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(Brand.gradient), RoundedCornerShape(20.dp))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = readableOnAll(Brand.gradient),
            )
        }
    }
}
