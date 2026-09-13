package com.ismael.daybyday.ui

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ismael.daybyday.BuildConfig
import com.ismael.daybyday.data.Backup
import com.ismael.daybyday.data.DATABASE_VERSION
import com.ismael.daybyday.data.DatabaseContents
import com.ismael.daybyday.data.DayColor
import com.ismael.daybyday.dayByDayApp
import com.ismael.daybyday.health.HealthConnectSource
import com.ismael.daybyday.health.ScreenTimeSource
import com.ismael.daybyday.work.DailyScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.dayByDayApp
    val prefs = app.prefs
    val repository = app.repository
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var firstName by remember { mutableStateOf(prefs.firstName) }
    var heightText by remember { mutableStateOf(prefs.heightCm.toString()) }
    var birthDate by remember { mutableStateOf(prefs.birthDate) }

    var autoBackupEnabled by remember { mutableStateOf(prefs.autoBackupEnabled) }
    var autoBackupHour by remember { mutableIntStateOf(prefs.autoBackupHour) }
    var autoBackupMinute by remember { mutableIntStateOf(prefs.autoBackupMinute) }
    var autoBackupFolder by remember { mutableStateOf(prefs.autoBackupFolder) }
    var lastAutoBackup by remember { mutableLongStateOf(prefs.lastAutoBackupAt) }

    var lockEnabled by remember { mutableStateOf(prefs.lockEnabled) }
    var biometricEnabled by remember { mutableStateOf(prefs.biometricEnabled) }
    var blockScreenshots by remember { mutableStateOf(prefs.blockScreenshots) }
    var hasPin by remember { mutableStateOf(prefs.hasPin) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }
    var searchingBackup by remember { mutableStateOf(false) }
    var stepsGranted by remember { mutableStateOf(false) }
    var screenGranted by remember { mutableStateOf(false) }
    var permissionsChecked by remember { mutableIntStateOf(0) }
    var notificationsGranted by remember { mutableStateOf(true) }
    var batteryUnrestricted by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var contents by remember { mutableStateOf<DatabaseContents?>(null) }
    var exportYear by remember { mutableIntStateOf(LocalDate.now().year) }

    val healthAvailable = remember { HealthConnectSource.isAvailable(context) }
    val appVersion = remember { AppVersion.of(context) }

    val biometricAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(busy) {
        contents = withContext(Dispatchers.IO) { repository.contents() }
    }

    LaunchedEffect(permissionsChecked) {
        stepsGranted = HealthConnectSource.hasPermission(context)
        screenGranted = ScreenTimeSource.hasPermission(context)
        notificationsGranted = notificationsAllowed(context)
        batteryUnrestricted = isBatteryUnrestricted(context)
    }

    // Les autorisations se changent dans les reglages d'Android, hors de
    // l'application : on les relit a chaque retour pour ne jamais afficher un
    // etat perime.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permissionsChecked += 1
        }
    }

    val healthPermissions = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        stepsGranted = granted.containsAll(HealthConnectSource.permissions)
        permissionsChecked += 1
    }

    val pickBackupFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            prefs.autoBackupFolder = uri.toString()
            autoBackupFolder = uri.toString()
            DailyScheduler.scheduleAutoBackup(context, prefs)
            scope.launch { snackbar.showSnackbar("Dossier de sauvegarde enregistré.") }
        }
    }

    val exportBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = runCatching { Backup.export(context, repository, uri) }
                busy = false
                snackbar.showSnackbar(
                    result.fold(
                        onSuccess = { "Sauvegarde créée : ${it.days} jour(s), ${it.mediaFiles} média(s)." },
                        onFailure = { "Échec de la sauvegarde : ${it.message}" },
                    )
                )
            }
        }
    }

    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = runCatching { Backup.import(context, repository, uri) }
                busy = false
                MediaLoader.clear()
                snackbar.showSnackbar(
                    result.fold(
                        onSuccess = { "Restauration terminée : ${it.days} jour(s), ${it.mediaFiles} média(s)." },
                        onFailure = { "Échec de la restauration : ${it.message}" },
                    )
                )
            }
        }
    }

    val exportText = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = runCatching {
                    Backup.exportYearText(context, repository, exportYear, uri)
                }
                busy = false
                snackbar.showSnackbar(
                    result.fold(
                        onSuccess = { "Résumé de $exportYear exporté ($it jour(s))." },
                        onFailure = { "Échec de l'export : ${it.message}" },
                    )
                )
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        ScreenBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(TAB_HEADER_HEIGHT))

            // La carte forte de l'ecran, et elle ne dit qu'une chose : rien ne
            // sort du telephone. C'est la promesse de l'application, elle merite
            // mieux qu'une ligne perdue dans « A propos ».
            Appear(index = 0) {
                HeroCard {
                    // Blanc, comme sur les autres cartes en degrade de
                    // l'application : c'est la meme carte forte, elle doit se
                    // lire pareil d'un ecran a l'autre.
                    val ink = Color.White
                    Text(
                        text = if (firstName.isBlank()) "Tout reste ici" else "$firstName, tout reste ici",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ink,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tes journées, tes photos et tes notes ne quittent jamais ce " +
                            "téléphone. L'application n'a même pas le droit d'aller sur " +
                            "Internet : ce n'est pas un réglage, c'est impossible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.86f),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        HeroPill("🔒 Aucun accès réseau", ink)
                        Spacer(Modifier.width(8.dp))
                        HeroPill("v${appVersion.name}", ink)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // --- Profil ---------------------------------------------------
            SectionCard(title = "Toi", index = 1) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it.take(20)
                        prefs.firstName = firstName
                    },
                    label = { Text("Ton prénom") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { input ->
                        heightText = input.filter { it.isDigit() }.take(3)
                        heightText.toIntOrNull()?.let { prefs.heightCm = it }
                    },
                    label = { Text("Ta taille") },
                    suffix = { Text("cm") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date de naissance", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            Dates.dayShort(birthDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = {
                        showDatePicker(context, birthDate) { picked ->
                            birthDate = picked
                            prefs.birthDate = picked
                        }
                    }) { Text("Modifier") }
                }
                val age = java.time.Period.between(birthDate, LocalDate.now()).years
                Text(
                    "Tu as $age ans. Ta taille sert à calculer ton IMC dans le bilan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- Autorisations --------------------------------------------
            SectionCard(title = "Autorisations", index = 2) {
                Text(
                    "Ce que l'application a le droit de lire sur le téléphone. " +
                        "Tout est lu en local et reste dedans : sans permission " +
                        "Internet, rien ne peut sortir d'ici. Appuie sur une ligne " +
                        "pour l'ouvrir dans les réglages Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                PermissionRow(
                    emoji = "👟",
                    title = "Nombre de pas",
                    status = when {
                        !healthAvailable ->
                            "Health Connect n'est pas installé sur ce téléphone."
                        stepsGranted -> "Tes pas sont lus dans Health Connect, où Samsung Health les écrit."
                        else -> "Samsung Health écrit tes pas dans Health Connect."
                    },
                    granted = stepsGranted,
                    available = healthAvailable,
                    onClick = {
                        if (healthAvailable && !stepsGranted) {
                            healthPermissions.launch(HealthConnectSource.permissions)
                        } else {
                            openSystemScreen(context, HealthConnectSource.settingsIntent())
                        }
                    },
                )

                Spacer(Modifier.height(8.dp))

                PermissionRow(
                    emoji = "📱",
                    title = "Temps sur les applis",
                    status = if (screenGranted) {
                        "Accès aux données d'utilisation accordé."
                    } else {
                        "À activer dans « Accès aux données d'utilisation »."
                    },
                    granted = screenGranted,
                    onClick = { openSystemScreen(context, ScreenTimeSource.settingsIntent(context)) },
                )

                Spacer(Modifier.height(8.dp))

                PermissionRow(
                    emoji = "🔔",
                    title = "Notifications",
                    status = if (notificationsGranted) {
                        "Le rappel du soir peut s'afficher."
                    } else {
                        "Bloquées : le rappel du soir ne s'affichera pas."
                    },
                    granted = notificationsGranted,
                    onClick = { openSystemScreen(context, notificationSettingsIntent(context)) },
                )

                Spacer(Modifier.height(8.dp))

                // Samsung met les applications en veille au bout de quelques
                // jours sans usage, et une application endormie ne sonne
                // jamais. C'est la premiere chose a verifier quand le rappel
                // du soir n'arrive pas, et ca ne se regle que dans Android —
                // d'ou sa place ici, avec les autres autorisations.
                PermissionRow(
                    emoji = "🔋",
                    title = "Mise en veille par Android",
                    status = if (batteryUnrestricted) {
                        "L'application peut se réveiller le soir."
                    } else {
                        "Samsung peut l'endormir : mets-la en « Sans restriction »."
                    },
                    granted = batteryUnrestricted,
                    onClick = { openSystemScreen(context, batterySettingsIntent(context)) },
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- Sauvegarde automatique -----------------------------------
            SectionCard(title = "Sauvegarde automatique", index = 3) {
                Text(
                    "Une sauvegarde par jour dans le dossier de ton choix. Le fichier " +
                        "précédent est remplacé, donc ça ne prend pas de place en plus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                SettingSwitchRow(
                    title = "Sauvegarder tous les jours",
                    subtitle = folderLabel(context, autoBackupFolder),
                    checked = autoBackupEnabled,
                    enabled = autoBackupFolder != null,
                    onCheckedChange = { enabled ->
                        autoBackupEnabled = enabled
                        prefs.autoBackupEnabled = enabled
                        DailyScheduler.scheduleAutoBackup(context, prefs)
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Heure de la sauvegarde",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        showTimePicker(context, autoBackupHour, autoBackupMinute) { hour, minute ->
                            autoBackupHour = hour
                            autoBackupMinute = minute
                            prefs.autoBackupHour = hour
                            prefs.autoBackupMinute = minute
                            DailyScheduler.scheduleAutoBackup(context, prefs)
                        }
                    }) {
                        Text(formatTime(autoBackupHour, autoBackupMinute))
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { pickBackupFolder.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (autoBackupFolder == null) "Choisir le dossier" else "Changer de dossier")
                }
                if (autoBackupFolder != null) {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            busy = true
                            scope.launch {
                                val result = runCatching {
                                    Backup.exportToFolder(
                                        context,
                                        repository,
                                        Uri.parse(autoBackupFolder),
                                    )
                                }
                                busy = false
                                result.onSuccess {
                                    prefs.lastAutoBackupAt = System.currentTimeMillis()
                                    lastAutoBackup = prefs.lastAutoBackupAt
                                }
                                snackbar.showSnackbar(
                                    result.fold(
                                        onSuccess = { "Sauvegarde faite : ${it.days} jour(s)." },
                                        onFailure = { "Échec : ${it.message}" },
                                    )
                                )
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sauvegarder maintenant")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (lastAutoBackup == 0L) {
                        "Aucune sauvegarde automatique pour l'instant."
                    } else {
                        "Dernière sauvegarde : ${formatDateTime(lastAutoBackup)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- Sauvegarde manuelle --------------------------------------
            SectionCard(title = "Sauvegarde manuelle", index = 4) {
                Button(
                    onClick = { exportBackup.launch("DayByDay-${LocalDate.now()}.zip") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exporter une sauvegarde (.zip)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { searchingBackup = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Chercher une sauvegarde dans un dossier")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        importBackup.launch(
                            arrayOf("application/zip", "application/octet-stream", "*/*")
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Choisir un fichier de sauvegarde")
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Resume annuel --------------------------------------------
            SectionCard(title = "Résumé annuel", index = 5) {
                Text(
                    "Exporte une année entière en texte (titres, notes, détails, " +
                        "statistiques) pour préparer ta vidéo de fin d'année.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { exportYear -= 1 }) { Text("−") }
                    Text(exportYear.toString(), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { exportYear += 1 }) { Text("+") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { exportText.launch("DayByDay-$exportYear.txt") },
                        enabled = !busy,
                    ) {
                        Text("Exporter $exportYear")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Confidentialite ------------------------------------------
            SectionCard(title = "Confidentialité", index = 6) {
                SettingSwitchRow(
                    title = "Verrouiller l'application",
                    subtitle = if (hasPin) {
                        "Code demandé à l'ouverture et après 15 s en arrière-plan."
                    } else {
                        "Définis d'abord un code à 4 chiffres ou plus."
                    },
                    checked = lockEnabled,
                    enabled = hasPin,
                    onCheckedChange = {
                        lockEnabled = it
                        prefs.lockEnabled = it
                        app.lock.refresh()
                    },
                )
                SettingSwitchRow(
                    title = "Empreinte / reconnaissance",
                    subtitle = if (biometricAvailable) {
                        "Déverrouiller avec la biométrie du téléphone."
                    } else {
                        "Aucune biométrie configurée sur ce téléphone."
                    },
                    checked = biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable,
                    onCheckedChange = {
                        biometricEnabled = it
                        prefs.biometricEnabled = it
                    },
                )
                SettingSwitchRow(
                    title = "Bloquer les captures d'écran",
                    subtitle = if (hasPin) {
                        "Masque aussi l'aperçu dans la liste des applis récentes."
                    } else {
                        "Actif en permanence tant qu'aucun code n'est défini. " +
                            "Définis un code pour pouvoir l'autoriser."
                    },
                    checked = blockScreenshots,
                    enabled = hasPin,
                    onCheckedChange = {
                        prefs.blockScreenshots = it
                        blockScreenshots = prefs.blockScreenshots
                        (context.findActivity() as? MainActivity)?.applySecureFlag()
                    },
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showPinDialog = true }) {
                        Text(if (hasPin) "Changer le code" else "Définir un code")
                    }
                    if (hasPin) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            prefs.clearPin()
                            prefs.lockEnabled = false
                            hasPin = false
                            lockEnabled = false
                            blockScreenshots = prefs.blockScreenshots
                            app.lock.refresh()
                            (context.findActivity() as? MainActivity)?.applySecureFlag()
                        }) {
                            Text("Supprimer le code")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- A propos -------------------------------------------------
            SectionCard(title = "À propos", index = 7) {
                InfoRow("Version", "${appVersion.name} (build ${appVersion.code})")
                InfoRow("Terminée le", formatDateTime(BuildConfig.BUILD_TIME))
                InfoRow("Identifiant", appVersion.packageName)
                InfoRow("Android", "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}")
                InfoRow("Appareil", "${Build.MANUFACTURER} ${Build.MODEL}")
                InfoRow("Base de données", "schéma v$DATABASE_VERSION")

                val stored = contents
                InfoRow(
                    "Contenu",
                    if (stored == null) {
                        "Lecture…"
                    } else {
                        "${stored.days} journée(s) · ${stored.moneyEntries} mouvement(s) · " +
                            "${stored.mediaFiles} média(s)"
                    },
                )
                InfoRow(
                    "Espace des médias",
                    if (stored == null) "Lecture…" else formatBytes(stored.mediaBytes),
                )
                InfoRow("Permissions", "notifications · pas · sommeil · temps d'écran")
                InfoRow("Accès réseau", "aucun — permission INTERNET absente")
            }

            Spacer(Modifier.height(16.dp))

            // --- Effacer --------------------------------------------------
            SectionCard(title = "Effacer mes données", index = 8) {
                Text(
                    "Supprime définitivement toutes les journées, notes, photos, " +
                        "vidéos et mouvements d'argent. C'est irréversible : fais " +
                        "d'abord une sauvegarde si tu hésites.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showEraseDialog = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Effacer toutes mes données", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(48.dp))
            BottomBarSpace()
        }
        }
    }

    FolderBackupRestorer(
        active = searchingBackup,
        onDismiss = { searchingBackup = false },
        onRestored = { summary ->
            searchingBackup = false
            autoBackupFolder = prefs.autoBackupFolder
            autoBackupEnabled = prefs.autoBackupEnabled
            scope.launch {
                snackbar.showSnackbar(
                    "Restauration terminée : ${summary.days} jour(s), ${summary.mediaFiles} média(s)."
                )
            }
        },
    )

    if (showPinDialog) {
        PinDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                prefs.setPin(pin)
                hasPin = true
                blockScreenshots = prefs.blockScreenshots
                if (!prefs.lockEnabled) {
                    prefs.lockEnabled = true
                    lockEnabled = true
                }
                showPinDialog = false
                scope.launch { snackbar.showSnackbar("Code enregistré.") }
            },
        )
    }

    if (showEraseDialog) {
        EraseDialog(
            contents = contents,
            onDismiss = { showEraseDialog = false },
            onConfirm = {
                showEraseDialog = false
                busy = true
                scope.launch {
                    repository.clearEverything()
                    MediaLoader.clear()
                    busy = false
                    snackbar.showSnackbar("Toutes les données ont été effacées.")
                }
            },
        )
    }
}

/**
 * Effacement definitif : la confirmation demande de recopier un mot, pour que
 * ce ne soit jamais le resultat d'un appui de travers.
 */
@Composable
private fun EraseDialog(
    contents: DatabaseContents?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    val word = "EFFACER"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tout effacer ?") },
        text = {
            Column {
                Text(
                    if (contents == null) {
                        "Toutes tes journées, notes, photos, vidéos et mouvements " +
                            "d'argent seront supprimés."
                    } else {
                        "${contents.days} journée(s), ${contents.mediaFiles} média(s) et " +
                            "${contents.moneyEntries} mouvement(s) d'argent seront supprimés."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "C'est définitif : rien ne pourra être récupéré, sauf depuis " +
                        "une sauvegarde faite avant.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it.take(10) },
                    label = { Text("Écris $word pour confirmer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = typed.trim().uppercase(Locale.FRANCE) == word,
            ) {
                Text("Tout effacer", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/**
 * Une autorisation du telephone : son etat en clair, et un appui qui ouvre
 * l'ecran Android correspondant pour l'activer ou la retirer quand on veut.
 */
@Composable
private fun PermissionRow(
    emoji: String,
    title: String,
    status: String,
    granted: Boolean,
    onClick: () -> Unit,
    available: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            // Une carte est blanche : une ligne blanche dessus ne se voit pas.
            // Le gris tres pale du theme suffit a la detacher sans faire un
            // deuxieme fond.
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(enabled = available, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                StatePill(granted = granted, available = available)
            }
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (available) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ouvrir dans les réglages",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Une pastille posee sur la carte en degrade : le blanc translucide de l'encre. */
@Composable
private fun HeroPill(label: String, ink: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = ink,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ink.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun StatePill(granted: Boolean, available: Boolean) {
    val label = when {
        !available -> "Indisponible"
        granted -> "Autorisé"
        else -> "À activer"
    }
    val color = when {
        !available -> MaterialTheme.colorScheme.onSurfaceVariant
        granted -> DayColor.GREEN.color
        else -> DayColor.ORANGE.color
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Une ligne « intitulé — valeur » de la fiche technique. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = pin.length >= 4 && pin == confirmation

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Code de l'application") },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { input -> pin = input.filter { it.isDigit() }.take(8) },
                    label = { Text("Code (4 à 8 chiffres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { input -> confirmation = input.filter { it.isDigit() }.take(8) },
                    label = { Text("Confirmer le code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                if (pin.isNotEmpty() && confirmation.isNotEmpty() && pin != confirmation) {
                    Text(
                        "Les deux codes ne correspondent pas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = valid) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Version installee, lue dans le paquet plutot que codee en dur. */
data class AppVersion(val name: String, val code: Long, val packageName: String) {
    companion object {
        fun of(context: Context): AppVersion {
            val info = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            val code = when {
                info == null -> 0L
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> info.longVersionCode
                else -> @Suppress("DEPRECATION") info.versionCode.toLong()
            }
            return AppVersion(
                name = info?.versionName ?: "inconnue",
                code = code,
                packageName = context.packageName,
            )
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/**
 * Android laisse-t-il l'application se reveiller ? Sur Samsung, une application
 * « optimisee » est endormie apres quelques jours, et ses taches de fond ne
 * s'executent plus : c'est la cause la plus frequente d'un rappel qui n'arrive
 * jamais, et rien dans le code de l'application ne peut la contourner.
 */
private fun isBatteryUnrestricted(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(android.os.PowerManager::class.java)
    manager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}.getOrDefault(true)

/**
 * La liste des applications et de leur optimisation de batterie. On ouvre la
 * liste plutot que la demande directe : celle-ci exigerait une permission de
 * plus dans le manifeste, et le manifeste de cette application se garde court.
 */
private fun batterySettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun showTimePicker(
    context: Context,
    hour: Int,
    minute: Int,
    onPicked: (Int, Int) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, pickedHour, pickedMinute -> onPicked(pickedHour, pickedMinute) },
        hour,
        minute,
        true,
    ).show()
}

private fun showDatePicker(
    context: Context,
    current: LocalDate,
    onPicked: (LocalDate) -> Unit,
) {
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> onPicked(LocalDate.of(year, month + 1, dayOfMonth)) },
        current.year,
        current.monthValue - 1,
        current.dayOfMonth,
    ).show()
}

private fun folderLabel(context: Context, folderUri: String?): String {
    if (folderUri == null) return "Choisis d'abord un dossier de destination."
    val name = runCatching {
        DocumentFile.fromTreeUri(context, Uri.parse(folderUri))?.name
    }.getOrNull()
    return "Dossier : ${name ?: "sélectionné"} · fichier ${Backup.AUTO_BACKUP_NAME}"
}

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.FRANCE, "%02d:%02d", hour, minute)

private fun formatDateTime(millis: Long): String {
    val dateTime = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        ZoneId.systemDefault(),
    )
    return dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRANCE))
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.FRANCE, "%.2f Go", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format(Locale.FRANCE, "%.1f Mo", bytes / 1_000_000.0)
    bytes >= 1_000 -> "${bytes / 1_000} Ko"
    else -> "$bytes octets"
}
