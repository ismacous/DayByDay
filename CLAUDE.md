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
   Le manifeste contient `<uses-permission android:name="…INTERNET"
   tools:node="remove"/>` : la permission est **retirée** si une bibliothèque la
   réclame dans son propre manifeste. La règle n'est donc plus tenue par la
   vigilance mais par la compilation.
   En revanche, **intégrer des fichiers** (polices, images, animations) est
   permis : ils sont téléchargés au moment d'écrire le code, vérifiés, et
   embarqués dans l'APK — rien n'est récupéré à l'exécution.
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
- `work/` — rappel du soir, bilan du lundi matin, sauvegarde automatique.
- `ui/` — écrans Compose. Navigation par onglets : Mois, Année, Bilan, Argent,
  Réglages ; le journal d'une journée et la recherche s'ouvrent par-dessus.

## Points délicats déjà rencontrés

- **Clavier** : sur l'écran d'une journée, `imePadding()` doit rester **avant**
  `verticalScroll()`, sinon le texte passe sous le clavier.
- **Rappel du soir** : `DailyScheduler.rescheduleAll` est appelé dans
  `DayByDayApp.onCreate()`, donc **à chaque ouverture**. Avec
  `CANCEL_AND_REENQUEUE`, le compte à rebours repartait de zéro chaque fois et
  le rappel de 21 h n'arrivait jamais. On ne reprogramme donc que si l'heure a
  changé (`Prefs.scheduledReminder`), sinon `KEEP` — qui garde la tâche en place
  et la recrée seulement si elle a disparu. Même règle pour la sauvegarde
  automatique. Reste la cause qui n'est pas dans le code : Samsung endort les
  applications, d'où la ligne « Mise en veille par Android » et le rappel
  d'essai dans les Réglages.
- **Titre des onglets** : il vit **au-dessus** du `NavHost` (`TabHeader` dans
  `AppNavigation`), pas dans chaque écran. Avant, passer du bilan à l'argent
  détruisait « Mon bilan » pour reconstruire « Mon argent » et tout l'en-tête
  clignotait, alors que les deux commencent par le même mot au même endroit.
  `MorphingTitle` anime chaque mot séparément : « Mon » ne bouge pas du tout.
  Conséquence : les écrans d'onglet ne portent **ni titre ni
  `statusBarsPadding`** — l'en-tête s'en charge. Les écrans qui s'ouvrent
  par-dessus (journée, journal, recherche, semaine) gardent les leurs.
- **Fond de l'application** : `ScreenBackground` est posé **une fois**, autour de
  la navigation (`AppNavigation`), donc derrière l'en-tête, les écrans et la
  barre du bas. Quand chaque écran portait le sien, la place prise par l'en-tête
  et par la barre restait en dehors et laissait deux bandes plates et opaques.
  Les écrans gardent leur appel à `ScreenBackground` : à l'intérieur d'un fond,
  il ne fait plus qu'une boîte (`LocalInsideBackground`) — repeindre deux fois
  coûterait double et foncerait les halos.
- **Rien ne réserve la place de la barre du bas** : il n'y a plus de `Scaffold`,
  justement parce que son rôle était de retirer au contenu la hauteur de la
  barre — ce qui laissait un bandeau vide en travers de la page, sous lequel les
  cartes étaient coupées net. La barre flotte par-dessus, le contenu défile
  dessous, et ce sont les écrans d'onglet qui finissent par un `BottomBarSpace()`
  pour que leur dernière carte reste lisible. Le même principe qu'en haut avec
  `TAB_HEADER_HEIGHT`, à l'envers.
- **En-tête qui s'efface** : il flotte au-dessus des écrans et recule quand on
  descend, via un `NestedScrollConnection` posé autour du `NavHost`. Aucun écran
  n'a rien à déclarer : listes et colonnes annoncent toutes leur défilement de
  cette façon. Les écrans d'onglet réservent sa place par un
  `Spacer(TAB_HEADER_HEIGHT)` en tête de leur contenu, et cette hauteur est la
  **même pour les quatre** — une hauteur variable ferait sauter le contenu au
  changement d'onglet.
- **Carte de l'humeur** : la couleur du jour descend en fondu depuis le haut de
  la carte jusqu'au trait qui précède « Moment par moment » (`MOOD_BAND`, mesuré
  en **points** et non en fraction de hauteur : la carte grandit avec son
  contenu). Trois versions ont été nécessaires. Toute la carte colorée : les
  quatre tuiles de choix portent ces mêmes couleurs, donc une tuile verte sur un
  fond vert disparaissait. Un simple bandeau sous le titre : la coupure était
  trop brutale. Le fondu long marche parce que **les tuiles ont un liseré
  blanc** — c'est lui qui les sépare de ce qui passe dessous, et sans lui le
  premier défaut reviendrait.
- **Barre du bas** : une bille saute en arc jusqu'à l'onglet choisi, et une
  encoche la suit dans le bord haut de la barre. La forme de la barre change à
  chaque image : elle est donc posée dans un `graphicsLayer` (voir « animations
  saccadées »). Le creux est une **différence de chemins**, pas un cercle posé
  par-dessus. Une conséquence connue : Android ne sait pas projeter d'ombre
  depuis un contour non convexe, donc la barre n'en a pas.
- **Cartes de « Ma journée »** (`ui/CardKit.kt`) : chaque carte a une **teinte**
  et un **signe** (`cardStyle`), un halo dessiné dans son coin (`cardGlow`, un
  `drawBehind` immobile : douze halos animés feraient douze animations pour un
  effet qu'on ne regarde pas), et une **ligne de résumé** sous son titre — une
  carte repliée doit encore renseigner, sinon la replier revient à l'effacer.
  Six teintes seulement, réparties pour que deux cartes voisines n'aient jamais
  la même : c'est la répétition d'une petite palette qui fait un système, pas
  douze couleurs différentes. Règle de forme : **une question, une réponse, une
  forme**. Un choix parmi trois est un `SegmentedChoice`, pas trois pastilles
  (rien n'empêchait de cocher « bien dormi » *et* « mal dormi ») ; huit verres
  d'eau sont huit verres, pas un compteur à flèches ; cinq prières sont cinq
  perles ; des démarches sont des cases à cocher. Les étiquettes ne s'affichent
  plus en bloc au bas de chaque carte : chacune vit sous la question qu'elle
  précise, dans la forme qui lui convient.
- **Prières** : cinq oui-ou-non par journée, rangés en **masque de bits** dans
  une seule colonne (`DayEntry.prayerMask`, migration 11→12). Les `bit` de
  `Prayer` ne doivent jamais changer : c'est eux qui sont écrits. La colonne
  accepte `null`, et ça compte — une journée d'avant cette version n'est pas une
  journée sans prière, c'est une journée dont on ne sait rien. Testé dans
  `PrayerTest`.
- **Recherche** : elle croise le texte, la couleur, ce qu'on a fait et les
  étiquettes (`data/DaySearch.kt`, testé dans `DaySearchTest`). Deux règles de
  sens : plusieurs couleurs se lisent « ou », plusieurs étiquettes « et ». Et un
  piège : `DaySearch.fold` (qui enlève accents et majuscules) plie **lettre par
  lettre**. Normaliser la chaîne entière puis jeter les accents la raccourcit,
  et l'extrait affiché autour du mot trouvé serait décalé d'autant de lettres
  accentuées qu'il y a avant lui.
- **Bilan de la semaine** : il raconte des **faits** (« bougé 3 jours »), jamais
  des corrélations. Sur sept jours, comparer « les jours où tu as bougé » aux
  autres n'a aucun sens statistique — ces rapprochements restent dans « Ce qui va
  avec tes bonnes journées », qui travaille sur des mois. La seule comparaison
  faite ici est avec la **semaine précédente**. La semaine va du lundi au
  dimanche (`WeekReviewBuilder.mondayOf`), et le lundi matin on raconte celle qui
  vient de finir, pas celle qui commence. Testé dans `WeekReviewTest`.
- **Importance d'un canal de notification** : elle ne se change plus une fois le
  canal créé — Android la confie à l'utilisateur et ignore toute modification du
  code. Faire passer le rappel en bandeau (comme un SMS) a donc demandé un
  **nouveau** canal (`rappel_quotidien_bandeau`) et la suppression de l'ancien,
  sinon deux lignes « Rappel quotidien » restent dans les réglages du téléphone.
- **Encre imposée d'une carte** : la carte de l'humeur porte toujours un
  dégradé, y compris avant qu'une couleur soit choisie — on arrive dessus en
  touchant une carte en dégradé, on doit tomber sur la même. Sur le dégradé de
  l'application, l'encre est imposée en blanc (`accentInk`) : `readableOnAll`
  choisirait du sombre, correct au contraste mais différent des autres cartes
  fortes.
- **Encre d'un en-tête coloré** : `readableOnAll` choisit noir ou blanc sur
  **toutes** les couleurs d'un dégradé, au contraste réel (WCAG), et non sur sa
  seule couleur de départ : le vert des bonnes journées part d'un vert moyen où
  le blanc passe encore, et finit clair où il disparaît. Il n'y a plus de
  `LocalCardInk` : plus aucun contenu de carte ne repose sur de la couleur —
  seul l'en-tête de la carte d'humeur le fait, et il choisit son encre lui-même.
- **Animations saccadées** : une valeur animée lue **pendant la composition**
  coûte une recomposition — et si elle nourrit une taille (l'épaisseur d'un
  `border`, par exemple), une remesure — à chaque image. L'anneau d'aujourd'hui
  dans le calendrier saccadait pour ça. Il est maintenant dessiné à la main dans
  un `drawWithContent`, où la lecture n'invalide que le dessin. Deuxième piège du
  même endroit : `rememberInfiniteTransition` était appelé dans les trente-cinq
  cases alors qu'une seule s'anime — trente-quatre horloges tournaient pour rien.
  La cause principale était pourtant ailleurs : `ScreenBackground` redessinait
  **quatre dégradés plein écran à chaque image**, sur tous les écrans, en
  permanence. Rien ne saccadait à cause de l'élément qu'on regardait, c'est le
  fond qui mangeait le temps de tout le monde. Chaque halo vit maintenant dans sa
  propre couche, peinte une fois, que l'animation ne fait que **déplacer**
  (`graphicsLayer`) — le téléphone sait faire ça sans rien repeindre. Règle
  générale : une valeur animée se lit dans `graphicsLayer` ou dans un `draw*`,
  jamais dans la composition.
- **Dégradés et petites surfaces** : un dégradé sur une pastille de deux
  centimètres ne se lit pas comme une matière mais comme une autre couleur — le
  coin clair d'un vert moyen faisait passer une semaine correcte pour un 10/10.
  Les dégradés sont réservés aux grandes surfaces ; `AverageChip` et `WeekScore`
  sont des aplats.
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
- **Double appui sur un mot** : Compose ne le déclenche pas dans ces champs,
  ni sur le titre (sans mise en forme) ni sur le texte, et monter la version de
  Compose n'y change rien. Il est réécrit dans `ui/DoubleTapWord.kt` : les
  appuis sont observés dans la passe `Initial` sans être consommés, sauf le
  deuxième d'un double appui, sinon le champ replace le curseur et efface la
  sélection. Le mot est cherché **à partir du curseur**, jamais du point
  touché : le premier appui l'a déjà posé au bon endroit, et convertir des
  coordonnées d'écran en position dans un texte qui défile est faux dès que le
  champ a défilé. Les bornes du mot sont dans `RichText.wordAt`, testées.
- **Hauteur du panneau d'outils** : deux pièges. (1) L'encart du clavier
  (`WindowInsets.ime`) contient déjà la barre de navigation, pas le panneau :
  celui-ci doit donc mesurer `hauteur du clavier − max(ime, barre de
  navigation)`, sinon il dépasse le clavier de la hauteur de cette barre.
  (2) Cette hauteur de clavier doit être le **maximum** vu depuis l'ouverture
  de l'écran. En se fermant, l'encart passe par toutes les valeurs
  intermédiaires ; retenir « la dernière au-dessus d'un seuil » gardait la
  valeur du seuil, et le panneau rétrécissait à chaque aller-retour jusqu'à
  n'être qu'un bandeau écrasé en bas de l'écran.
- **Sélection et panneaux** : ouvrir un panneau retire le focus du champ (c'est
  ce qui ferme le clavier), et perdre le focus **efface la sélection** — le
  champ appelle lui-même `onValueChange` avec une sélection vide. On ne pouvait
  donc plus colorer un texte déjà écrit. La sélection est mise de côté dans
  `heldSelection` avant de lâcher le focus, et c'est elle qui sert de cible
  tant qu'un panneau est ouvert ; le `onValueChange` ne l'efface que panneau
  fermé, sinon la désélection automatique la balaierait aussitôt. Un champ sans
  focus ne peint pas non plus sa sélection : `SpanTransformation` la redessine,
  sinon on choisit une couleur à l'aveugle.
- **Photos du journal** : elles sont **posées librement** sur la page, pas
  insérées dans le fil du texte. Écrire un paragraphe de plus ne les déplace
  pas — c'est voulu, et c'est l'inverse d'un traitement de texte. Trois
  conséquences dans le code : les positions sont en points **dans la page**,
  pas dans l'écran, donc le texte et les photos doivent vivre dans le même
  `verticalScroll` ; `Modifier.offset` ne fait pas grandir le parent, donc la
  hauteur de page est imposée par le champ de texte (`heightIn`) à partir de
  `Placement.lowestEdge` ; et une photo passée derrière le texte n'est plus
  cliquable (le champ est devant), donc elle se reprend par ses vignettes dans
  le menu « + ». Toutes les règles de position, taille, rotation et
  aimantation sont dans `data/Placement.kt`, sans rien d'Android, et testées
  dans `PlacementTest`.
- **Gestes sur une photo** : deux pièges qui donnent le même symptôme — l'image
  revient en place toute seule. (1) `pointerInput` n'installe son détecteur
  qu'une fois : la photo capturée y reste figée à ce qu'elle était au premier
  appui, alors que chaque événement n'apporte que le déplacement **depuis le
  précédent**. Il faut relire la photo courante par `rememberUpdatedState`.
  (2) L'aimantation doit porter sur un centre **brut** accumulé pendant le
  geste, jamais sur la position déjà aimantée : sinon chaque petit pas retombe
  sur le même point de grille et la photo paraît collée. C'est ce que fait
  `Placement.apply`.
- **Lignes de la page ≠ grille des photos** : le lignage (`JournalPaper`) est
  toujours visible et sert à écrire ; la grille (`PhotoGrid`) n'apparaît que
  pendant qu'on déplace une image. Couper le lignage ne coupe pas
  l'aimantation. Quatre choses à ne pas défaire :
  1. Les deux quadrillages sont **accordés** : l'écart entre deux lignes vaut
     `Placement.LINE_STEPS` pas de grille et la marge du haut `TOP_STEPS`, donc
     chaque ligne d'écriture tombe pile sur une ligne de grille (testé).
  2. La `lineHeight` du texte vient d'une mesure en **points**
     (`LINE_SPACING.toSp()`), pas d'une valeur en `sp` : sinon agrandir les
     caractères dans Android décale le texte de ses lignes.
  3. `lineHeightStyle = Bottom + Trim.None` est obligatoire. Par défaut Compose
     répartit l'espace autour du texte et rogne celui de la première ligne :
     le texte flotte alors au-dessus des lignes, d'un écart différent partout.
  4. Le lignage et la grille se dessinent en `Modifier.matchParentSize()`, pas
     à une hauteur calculée. C'est le champ de texte qui décide de la hauteur
     de la page ; une hauteur fixée d'avance laissait le bas sans lignes dès
     que le texte dépassait — d'où les lignes qui semblaient s'arrêter après un
     titre ou disparaître à l'ouverture du clavier.
  Les titres occupent un nombre **entier** de lignes (`TextStyleKind.lineSpan`,
  posé en `ParagraphStyle` par `buildAnnotatedStringWithSpans`), donc le rythme
  reprend exactement après. Attention : Compose refuse des `ParagraphStyle` qui
  se recouvrent, et rien n'empêche deux titres au même endroit — d'où le tri et
  le filtrage dans `headingParagraphs`.
- **Autocollants** : `MediaShape.FREE` ne recadre ni ne rogne, donc un PNG
  détouré garde sa silhouette. Le contour blanc suit cette silhouette, pas le
  cadre : `StickerImage` redessine la même image huit fois autour, teintée en
  blanc (`BlendMode.SrcIn`), avant l'originale. Un seul décodage sert aux neuf
  passes.
- **Polices** : trois fichiers dans `res/font/` (Caveat, Lora, Poppins, sous
  licence OFL, voir `POLICES.md`), plus deux familles d'Android. Ils sont dans
  l'APK : rien n'est téléchargé, la règle « aucune permission INTERNET » tient.
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
