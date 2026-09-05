# DayByDay — notes pour Claude

Application Android personnelle d'Ismael : noter chaque journée d'une couleur,
tenir un journal, suivre quelques habitudes, et repérer ce qui accompagne les
bonnes journées. Elle n'est pas distribuée : un seul utilisateur, un seul
téléphone (Samsung S25, Android 15).

## Ton et langue

- **Toute l'interface est en français et tutoie l'utilisateur.** Les nouveaux
  textes doivent suivre ce ton : direct, simple, sans jargon.
- Ismael n'est pas développeur. Les explications doivent rester concrètes, et
  les échanges se font en français.
- L'application sert à suivre un moral qui varie beaucoup. Rester factuel et
  sans jugement dans les libellés (par exemple « Journée très noire », pas
  « Mauvaise journée »).

## Règles à ne pas casser

1. **Aucune permission INTERNET.** C'est la garantie centrale de l'application :
   sans elle, aucune donnée ne peut sortir du téléphone, quoi qu'il arrive dans
   le code. Ne jamais l'ajouter, ni ajouter une dépendance qui en aurait besoin.
   Les pas et le temps d'écran sont lus **localement** (Health Connect et
   statistiques d'usage), jamais via un service en ligne.
2. **La couleur du jour reste subjective.** Elle vient des quatre moments
   (matin, après-midi, soir, nuit) ou d'un choix manuel — jamais du sport, des
   pas, des repas ou de l'argent. Ces données sont comparées à la couleur dans
   « Ce qui va avec tes bonnes journées » : les dériver l'une de l'autre rendrait
   la comparaison circulaire et sans valeur.
3. **La clé de signature est fixe et versionnée** (`keystore/daybyday.jks`,
   mots de passe en clair dans `app/build.gradle.kts`). C'est volontaire : elle
   permet aux mises à jour de s'installer par-dessus sans effacer les données.
   Ne pas la changer, ne pas la régénérer.
4. **Toute évolution du schéma passe par une migration Room explicite**, avec un
   test. L'utilisateur a des données réelles depuis la version 1 ; une migration
   destructive les perdrait. Voir `MigrationTest`, qui recrée une base v1 et
   vérifie qu'elle survit.
5. **Les étiquettes sont un catalogue fixe** (`data/TagCatalog.kt`), synchronisé
   au démarrage via un `slug` stable. Renommer une étiquette est sûr ; changer
   son slug casserait le lien avec les journées déjà marquées.
6. **Incrémenter `versionCode` et `versionName`** à chaque version livrée.

## Architecture

- Kotlin + Jetpack Compose (Material 3), base Room, WorkManager pour les tâches
  quotidiennes. Pas d'injection de dépendances : `DayByDayApp` expose le dépôt,
  les préférences et une portée de coroutine.
- `data/` — entités Room, DAO, dépôt, statistiques, sauvegarde, préférences.
- `health/` — lecture locale des pas (Health Connect) et du temps d'écran
  (statistiques d'usage, calculées à partir des **événements** pour ne pas
  compter deux fois les périodes qui se chevauchent).
- `work/` — rappel du soir et sauvegarde automatique quotidienne.
- `ui/` — écrans Compose. Navigation par onglets : Mois, Année, Bilan, Argent,
  Réglages ; le journal d'une journée et la recherche s'ouvrent par-dessus.

## Points délicats déjà rencontrés

- **Clavier** : sur l'écran d'une journée, `imePadding()` doit rester **avant**
  `verticalScroll()`, sinon le texte passe sous le clavier.
- **Sauvegarde automatique du jour** : dans le `DisposableEffect`, le jour est
  figé au démarrage de l'effet ; au moment du `onDispose`, `epochDay` peut déjà
  pointer vers le jour suivant alors que les champs contiennent l'ancien.
- **Temps d'écran** : les événements d'usage arrivent dans l'ordre PAUSED(A),
  RESUMED(B), STOPPED(A). Ne fermer une période que si l'événement concerne
  l'activité réellement au premier plan, sinon le STOPPED tardif de A referme
  la période de B et des heures disparaissent. La logique est isolée dans
  `ScreenTimeSource.foregroundMillis`, testée dans `ScreenTimeTest`.
- **Argent** : une correction de solde porte la catégorie `ADJUSTMENT` et n'est
  comptée ni dans les rentrées ni dans les dépenses. Sans ça, remettre le total
  juste après une dépense la comptait une seconde fois, à l'envers.
- **Journal** : le texte reste brut dans `note` ; la mise en forme vit à part
  dans `noteSpans`, en intervalles de caractères (`data/RichText.kt`). La
  recherche, l'export annuel et les aperçus lisent donc le texte sans rien
  savoir de la décoration. Comme aucun caractère n'est ajouté, la
  `VisualTransformation` de l'éditeur garde `OffsetMapping.Identity` : jamais
  de curseur décalé. Toute la logique d'intervalles est testée dans
  `RichTextTest`.
- **Tests instrumentés** : chaque test `runBlocking` doit déclarer `: Unit`,
  sinon JUnit refuse la classe entière. `CalendarUiTest` désactive l'écran de
  reprise de sauvegarde dans un `@BeforeClass`, avant que la règle ne lance
  l'activité.
- Les tests d'interface partagent la base réelle de l'émulateur : deux tests ne
  doivent pas toucher la même journée avec la même couleur (un second appui
  l'enlève).

## Construire et tester

```bash
./gradlew testDebugUnitTest      # tests unitaires JVM
./gradlew assembleDebug          # APK : app/build/outputs/apk/debug/
./gradlew connectedDebugAndroidTest   # tests sur émulateur ou appareil
```

Le job émulateur est fragile : l'émulateur des serveurs GitHub refuse souvent
de démarrer (« Timeout waiting for emulator to boot »), et le job échoue alors
sans avoir lancé un seul test. Le workflow réessaie une fois. Devant un échec
de ce job, vérifier d'abord si `adb` a seulement vu l'appareil : sans ça, ce
n'est pas le code qui est en cause.

Le SDK Android n'est pas toujours accessible depuis l'environnement de Claude
(`dl.google.com` peut être bloqué). Dans ce cas, la compilation et les tests se
font par GitHub Actions (`.github/workflows/build.yml`) : pousser, puis lire les
journaux du run. L'APK est publié en artefact `DayByDay-apk` ; le job APK et le
job émulateur sont indépendants, donc un échec de test ne prive pas Ismael de
son APK.

## Livrer une version

1. Incrémenter `versionCode` / `versionName`.
2. Pousser et attendre que les deux jobs soient verts.
3. Donner à Ismael le lien du run : il télécharge l'artefact **DayByDay-apk**,
   décompresse, installe. Grâce à la clé fixe, l'installation se fait par-dessus
   la précédente sans perte de données.
