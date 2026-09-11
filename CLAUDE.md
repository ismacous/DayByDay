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
   Depuis la 4.8, la règle est **vérifiée à la compilation** : la tâche
   `verifyNoInternetDebug` (dans `app/build.gradle.kts`) lit le manifeste
   *fusionné*, celui qui part réellement dans l'APK, et fait échouer le build
   s'il contient encore la permission. C'est ce qui permet d'utiliser Lottie,
   qui déclare INTERNET dans son propre manifeste pour charger une animation
   depuis une adresse : la permission est retirée à la fusion, et on le sait.
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
   son slug casserait le lien avec les journées déjà marquées. Une étiquette
   n'a de sens que si **aucune donnée ne porte déjà l'information** : « grosse
   dépense » doublait le détail des mouvements, « recherche d'emploi » ne
   distinguait pas un jour à une candidature d'un jour à six (c'est un nombre,
   `jobApplications`), et « cuisine maison » ne disait rien de plus que
   « bien mangé ».
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
- `ui/` — écrans Compose. La page du journal est une suite de **blocs**
  (`PageBlocks`, `BlockViews`) ; le reste est classique.
  Navigation par onglets : Mois, Année, Bilan, Argent,
  Réglages ; le journal d'une journée et la recherche s'ouvrent par-dessus.

## Points délicats déjà rencontrés

- **Clavier** : sur l'écran d'une journée, `imePadding()` doit rester **avant**
  `verticalScroll()`, sinon le texte passe sous le clavier.
- **Rappel du soir : une alarme, pas une tâche périodique.** Trois versions ont
  été nécessaires, et les deux premières traitaient des symptômes. (1)
  `rescheduleAll` est appelé à chaque ouverture ; avec `CANCEL_AND_REENQUEUE` le
  compte à rebours repartait de zéro, donc ouvrir l'app à 20 h repoussait le
  rappel à demain. (2) On ne reprogrammait plus que si l'heure changeait — et le
  rappel arrivait quand Android le décidait, c'est-à-dire souvent jamais.
  La cause réelle : **une tâche périodique n'a pas d'heure, elle a une période.**
  « Une fois par jour » veut dire « une fois quelque part dans chaque tranche de
  24 h », et le système la place où ça l'arrange — ou nulle part, sur un Samsung
  qui endort les applications. Le rappel et le bilan du lundi passent donc par
  `ReminderAlarm` (`setExactAndAllowWhileIdle`, permission `USE_EXACT_ALARM`),
  qui vise un **instant absolu** : reprogrammer devient sans effet, ce qui rend
  le bug (1) impossible par construction. `ReminderReceiver` **réarme** après
  chaque sonnerie plutôt que d'utiliser une répétition automatique — une
  répétition dérive (elle ajoute 24 h à l'heure où elle a tourné, pas à l'heure
  prévue) et finirait au milieu de la nuit — et il réarme aussi au démarrage du
  téléphone, après une mise à jour et au changement d'heure, trois moments où
  Android efface les alarmes. La notification part **du receveur**, pas d'une
  tâche : l'alarme a déjà réveillé le processus, et chaque intermédiaire de plus
  est un endroit de plus où Samsung peut couper. La sauvegarde automatique, elle,
  reste sur WorkManager : personne ne regarde une sauvegarde partir. Les règles
  d'heure sont testées dans `ReminderAlarmTest`. Reste la cause qui n'est pas
  dans le code : Samsung endort les applications, d'où la ligne « Mise en veille
  par Android », le rappel d'essai, et la ligne « Prochain rappel » des Réglages
  — qui sépare « rien n'est programmé » de « c'est programmé mais étouffé ».
- **La place de l'en-tête se retient onglet par onglet.** Chaque onglet garde sa
  position de défilement quand on le quitte et la retrouve quand on y revient
  (`restoreState`), mais l'en-tête, lui, était remis en haut à chaque changement
  d'onglet. On revenait donc au milieu du mois avec le titre posé en travers des
  cartes. `AppNavigation` garde une position **par route** (`headerOffsets`) :
  les deux repartent ensemble. Un seul `NestedScrollConnection` sert tout le
  monde, et il lit la route courante par `rememberUpdatedState` — le refabriquer
  à chaque changement d'onglet couperait le défilement en cours.
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
- **Une donnée, un endroit** : sortir ou rester chez soi vivait dans « Dehors &
  écrans », coincé entre un temps d'écran et une étiquette « Dehors » perdue au
  milieu du ménage et des courses. C'est maintenant une question de la carte
  **Activité physique**, et « Écrans » ne parle plus que d'écrans. Même règle
  pour le poids (parti dans Santé) et les traitements (leur propre carte, qu'on
  masque les mois où l'on ne prend rien).
- **Le fond d'une carte** (`CardWeek` / `MoreButton`) : « voir la semaine »
  n'ajoute pas du détail sur aujourd'hui — ça n'aurait fait que rallonger la
  carte. Il répond à l'autre question, celle que la carte ne peut pas poser :
  « et les jours d'avant ? ». Sept barres (`MiniBars`), nourries par **une
  seule** requête (`observeDaysBetween` sur sept jours) partagée par toutes les
  cartes. Les barres du poids partent du plus bas de la semaine et non de zéro :
  autour de quatre-vingts kilos, partir de zéro donne sept barres identiques.
- **Badges** (`data/Badge.kt`, `ui/Celebration.kt`) : une médaille apparaît quand
  quelque chose est fait — les cinq prières, 6 000 pas, une vraie séance, être
  sorti, huit verres, une candidature, la journée écrite. Le fond est un fichier
  Lottie embarqué ; la médaille est **dessinée**, et c'est tout l'enjeu — un
  badge de jeu pris tel quel ressemble à un badge de jeu, pas à cette
  application. C'est un hexagone à **six facettes**, chacune éclairée selon son
  orientation par rapport à la lumière (en haut à gauche, comme partout
  ailleurs) : c'est ce calcul, et pas un contour de couleur, qui donne le
  relief. Ruban derrière, plaque intérieure vernie, reflet qui traverse, et un
  **tour sur elle-même** à l'arrivée — une pièce qui tourne montre qu'elle a une
  face et une épaisseur. L'emoji, lui, ne tourne pas : il resterait à l'envers
  la moitié du tour.
  Deux règles pour en ajouter un : il récompense un **geste**, et il reste
  atteignable un mauvais jour. Et **jamais de série ni de score cumulé** — une
  série brisée punirait exactement les journées qu'il ne faut pas punir.
  Quatre règles d'animation : elle se déclenche **au passage** d'un état à
  l'autre, jamais à l'affichage (sinon rouvrir la journée la rejouerait — d'où
  les gardes `stepsSeen` / `noteSeen` pour les valeurs qui arrivent après coup,
  et surtout le `loadedFor != epochDay` : avant que la journée ne revienne de
  la base, les champs sont vides, et le passage « vide → écrit » du chargement
  ressemble trait pour trait à celui de l'écriture) ;
  moins de deux secondes et demie ; un appui n'importe où la coupe ; et une seule
  horloge mène tout, lue uniquement dans `drawBehind` et `graphicsLayer`.
- **Emoji animés** (`res/raw/mood_*.json`, `res/raw/badge_*.json`) : les quatre
  visages des tuiles d'humeur et les huit signes des badges sont les emoji
  animés de Google (Noto), du vecteur pur sans image ni adresse dedans (les
  `assets` de certains fichiers sont des **compositions imbriquées**, pas des
  images — à vérifier avant d'en embarquer un). Google n'anime pas tous les
  emoji : les badges ont donc été choisis **parmi ceux qui existent en animé**,
  quitte à en détourner un (la Terre pour « tu es sorti », une fusée pour une
  candidature). Mieux vaut ça que huit médailles dont la moitié bougerait —
  une règle à moitié appliquée se voit plus qu'une règle absente. La tuile choisie joue son animation **une fois** puis reste
  dans sa pose ; les autres montrent la première image, atténuée. Quatre visages
  qui s'agitent en permanence feraient une vitrine, pas un choix. La progression
  passe en **lambda** à `LottieAnimation` : lue au dessin, elle ne recompose
  rien.
- **Une boucle d'animation doit être invisible à ses deux bouts.** Le reflet des
  tuiles d'humeur sautait : son dégradé avait un axe **oblique**, et hors de
  l'intervalle un dégradé se prolonge par sa couleur de bord — le coin bas-droit
  de la tuile tombait encore dans la partie claire à l'arrivée. L'axe est
  maintenant horizontal (donc la sortie ne dépend que de `x`), l'obliquité vient
  d'une `rotate` du dessin, et le trajet (`SHINE_FROM` / `SHINE_TO`) est calculé
  pour que la bande ait quitté la tuile **rotation comprise**.
- **Deux polices sur une ligne ne s'alignent que dans le même paragraphe.**
  `MorphingTitle` animait « Mon » et « bilan » séparément, pour que le premier
  mot ne bouge pas. Deux tentatives, deux échecs, et la leçon vaut pour toute
  animation : (1) alignés par le bas de leur boîte, la serif descendant plus
  bas, le second mot flottait ; (2) `Modifier.alignByBaseline()` réglait ça
  **à l'arrêt seulement** — le temps d'un changement, un `AnimatedContent`
  contient *deux* textes à la fois, celui qui part et celui qui arrive, et la
  ligne d'écriture qu'il annonce est celle du plus haut des deux : elle bouge à
  chaque image et les mots sautent. **Ne jamais poser `alignByBaseline()` sur un
  conteneur qui anime son contenu.** Troisième essai : les deux mots réunis dans
  un seul `Text` — aligné, stable, mais « Mon » se remettait à bouger, puisqu'un
  seul `Text` s'anime d'un bloc. La solution ne demande rien au parent : chaque
  mot réserve **lui-même** `BASELINE_GAP` sous sa propre ligne d'écriture
  (`Modifier.paddingFrom(LastBaseline, after = …)`). Les deux boîtes ont alors
  leur ligne d'écriture à la même distance de leur bas, et les aligner par le
  bas — une mesure fixe, que l'animation ne touche pas — aligne les lignes
  d'écriture exactement. Et `SizeTransform(clip = false)` est obligatoire sur ces
  `AnimatedContent` : sans lui, la boîte se redimensionne **en découpant**, et
  le mot le plus long apparaît tronqué au milieu du changement.
- **Organiser ma journée** (`ui/OrganizeCardsScreen.kt`) : on y entre par le
  bouton en bas de la journée, ou en **maintenant le doigt sur l'en-tête d'une
  carte** — le geste des écrans d'accueil, qui tombe sous le doigt au moment où
  l'envie vient. Un raccourci qu'on ne devine pas ne doit jamais être le seul
  chemin, d'où le bouton qui reste. Chaque rangée est la carte **en petit** — même teinte, même signe, même halo. On ne choisit pas une
  ligne de texte, on choisit une carte. On déplace en **maintenant puis tirant**
  (`detectDragGesturesAfterLongPress`), ce qui ne se bat pas avec le défilement.
  Deux choses à ne pas défaire : (1) les hauteurs de rangée sont **fixes**, et
  c'est ce qui permet de calculer un cran (`ROW_HEIGHT + ROW_GAP`) sans mesurer
  quoi que ce soit pendant le geste ; (2) `shift` **rend un booléen** et la
  boucle s'arrête dessus — sans ça, tirer au-delà de la première carte boucle à
  l'infini, la condition restant vraie alors que plus rien ne bouge. Les cartes
  rangées vivent dans la **même** `LazyColumn` sous un séparateur, avec la même
  clé : retirer une carte la fait donc *glisser* jusque là (`animateItem`), ce
  qui montre que rien n'est effacé.
- **Hygiène** (`Brushing`, migration 20→21) : la douche et les trois brossages,
  dans la même forme que les prières — un masque de bits pour les trois
  brossages, des perles à l'écran. Même question, même forme : leur donner une
  autre apparence aurait fait croire à une autre mécanique.
  **Deux gestes seulement**, et c'est délibéré : une carte d'hygiène qui en
  demande dix devient une corvée, et c'est le jour noir qu'elle sert. La douche
  est un **seul oui** qu'on coche, pas un « oui / non » à deux boutons — ne
  rien cocher *est* la réponse « non », et il y a des journées où l'on n'a pas
  envie de la dire à voix haute.
  Ses deux badges détournent leur signe (des bulles, des étincelles) : Google
  n'anime ni la douche, ni la brosse à dents, ni le savon — vérifié.
- **Prières** : cinq oui-ou-non par journée, rangés en **masque de bits** dans
  une seule colonne (`DayEntry.prayerMask`, migration 11→12). Les `bit` de
  `Prayer` ne doivent jamais changer : c'est eux qui sont écrits. La colonne
  accepte `null`, et ça compte — une journée d'avant cette version n'est pas une
  journée sans prière, c'est une journée dont on ne sait rien. Testé dans
  `PrayerTest`.
- **Mots-clés** (`data/Hashtag.kt`, `ui/JournalMarks.kt`, testé dans `HashtagTest`) :
  écrire `#mood` dans le journal en fait une étiquette, retrouvable partout.
  Le point qui décide de tout le reste : **ils ne sont enregistrés nulle part**.
  Un `#mood` est cinq caractères dans `note`, comme le reste du texte — donc la
  recherche, l'export annuel et les aperçus les voient sans rien savoir, effacer
  le `#` suffit à défaire l'étiquette, et il n'existe aucun moyen de
  désynchroniser une liste de mots-clés du texte qui les contient. C'est ce qui
  les sépare des étiquettes du catalogue : une étiquette se coche, un mot-clé
  s'invente en écrivant.
  Trois règles à ne pas défaire :
  1. Un `#` **collé à un mot** n'est pas un mot-clé (`do#5`, `C#`), et un `#`
     suivi de chiffres seuls est un numéro. Sans ça, une adresse ou un accord
     de musique deviendrait une étiquette.
  2. La couleur est **calculée à partir du nom** (`Hashtag.tint`), jamais tirée
     au sort ni enregistrée : `#mood` garde sa teinte dans toutes les pages,
     dans la recherche, et après une restauration sur un téléphone neuf. La
     stocker aurait demandé une table, une migration, et se serait perdue.
     Ce sont les **six teintes des cartes** (`Palette`), pas une palette de
     plus : une septième couleur inventée ici se verrait tout de suite.
  3. Sur la page, c'est la **pastille dessinée derrière** qui porte la couleur ;
     le mot garde l'encre du papier. Un mot coloré disparaîtrait sur l'ardoise,
     et Compose ne sait pas donner des coins arrondis à un `SpanStyle` — un fond
     de span est un rectangle, donc un surlignage, pas une étiquette. Les
     pastilles sont donc dessinées à partir de la mise en page du texte, lue
     **dans le `drawBehind`**, et le modificateur se pose **après** la marge du
     champ : posé avant, tout est décalé de la marge. Ailleurs (aperçu d'une
     carte, où il n'y a pas de pastille), c'est le texte qui prend la couleur.
- **La page du journal est une suite de blocs** (`data/JournalBlock.kt`,
  `ui/PageBlocks.kt`, `ui/BlockViews.kt`, testé dans `JournalBlockTest`). Elle
  était un seul long champ de texte, avec les vocaux rangés dessous et les
  citations dessinées à partir de la mise en page. Trois conséquences, et
  c'étaient les trois reproches : rien ne pouvait se glisser **entre** deux
  paragraphes, il n'y avait rien à attraper pour déplacer quoi que ce soit, et
  un trait de citation dessiné sur un intervalle de caractères n'appartenait à
  personne — il s'arrêtait au premier mot. Un bloc, lui, est une chose : il a
  une place, une hauteur, un bord, et on peut le prendre.
  Quatre règles portent tout le reste :
  1. **Un paragraphe est un bloc.** Entrée coupe le bloc en deux
     (`PageBlocks.splitAt`), effacer au tout début le recolle à celui du dessus
     (`PageBlocks.mergeBack`). La première version faisait l'inverse — un bloc
     de texte contenait plusieurs paragraphes, pour n'avoir qu'un champ par
     page — et c'était l'erreur : on pouvait déplacer les vocaux et les
     citations, mais pas ce qu'on avait écrit, et c'est justement ce qu'on veut
     déplacer. On ne pouvait même pas fabriquer un second bloc de texte.
     Les deux vont **ensemble** : sans le recollage, la page ne ferait que se
     découper. Le recollage passe par `onKeyEvent` sur la touche d'effacement,
     et il refuse d'avaler autre chose que du texte — effacer un enregistrement
     d'un coup de touche serait le pire des raccourcis.
     `PageBlocks.insertAt` coupe pareillement quand on pose une citation, un
     trait ou un vocal au milieu du texte.
  2. **`note` est une projection, pas une deuxième source.** La recherche,
     l'export de l'année, les aperçus et le PDF lisent une page à plat et ne
     savent rien des blocs. `JournalBlocks.flatten` recompose ce texte à chaque
     enregistrement, **à un seul endroit** (`DayRepository.saveJournal`). Le
     test qui compte est l'aller-retour `split` → `flatten` : si le calcul se
     décale d'un retour à la ligne, tous les intervalles de mise en forme se
     décalent avec, et ne pointent plus sur les bons mots.
  3. **Un bloc de texte vide est une ligne blanche**, pas un déchet. Il ne se
     jette donc plus (ni dans `PageBlocks.tidy`, ni dans `JournalBlocks.tidy`),
     sinon deux paragraphes qu'on venait de séparer se recolleraient tout
     seuls. Corollaire pour `flatten` : le séparateur est un **drapeau**, pas
     un « est-ce que ça finit déjà par un retour à la ligne ? » — deux blocs
     vides d'affilée doivent donner deux lignes vides.
  4. **Chaque bloc occupe un nombre entier de lignes du lignage.** Un vocal en
     fait deux (`VOICE_LINES`), un trait une. Sans ça, le texte qui suit un
     vocal ne retombe plus sur ses lignes — c'est exactement ce qui se voyait
     avant la refonte.
  Le déplacement se fait en **maintenant puis tirant** (`Modifier.blockDrag`),
  jamais par un simple glissement : sur une page qui défile, un glissement
  appartient au défilement. La liste est réordonnée **pendant** le geste, donc
  le trou qui s'ouvre *est* l'indicateur de dépôt, et il n'y a rien à dessiner.
  Quatre pièges, dont deux qui ont rendu la première version inutilisable :
  1. **La chaîne de modificateurs doit garder la même forme.** Le
     `graphicsLayer` du bloc n'était posé que *pendant* le geste. L'ajouter au
     démarrage changeait la forme de la chaîne, Compose recréait le détecteur
     d'appui, et le geste en cours était **annulé aussitôt** : le bloc
     sursautait puis restait sur place. La couche est donc posée en permanence
     et ne fait que changer de valeurs. Règle générale : ne jamais
     ajouter ni retirer un modificateur en réaction à un geste qui est en train
     d'avoir lieu.
  2. **Les fonctions passées à `pointerInput` passent par
     `rememberUpdatedState`** — `pointerInput` n'installe son détecteur qu'une
     fois et y fige ce qu'on lui a donné. Même piège que sur les photos.
  3. Le décalage se lit dans `graphicsLayer` : lu pendant la composition, il
     remesurerait la page à chaque image.
  4. La boucle de réordonnancement s'arrête sur une hauteur nulle — un bloc pas
     encore mesuré rendrait la condition vraie sans que rien ne bouge, et on
     tournerait pour toujours (le bug d'« Organiser ma journée »).
  **Une poignée qu'on ne voit pas est une poignée qui n'existe pas.** Chaque
  bloc a la sienne dans une marge à gauche (`BlockGutter`) : sans elle, une
  page en blocs ressemble trait pour trait à une page qui n'en a pas, et rien
  ne dit qu'il y a quelque chose à attraper. Elle vaut aussi pour les
  paragraphes — sur le texte lui-même, l'appui maintenu appartient à la
  sélection.
  Elle n'apparaît que sur le bloc **où l'on est**. Deuxième essai : la première
  version la montrait pâle sur chaque bloc, pour dire que la page en est faite.
  Ça le disait, mais une page de dix paragraphes devenait une colonne de points
  gris à côté d'une colonne de texte — le décor prenait le pas sur ce qu'on
  écrit. Une seule suffit, et elle désigne en plus ce qu'on va déplacer.
  **Deux gestes, deux rôles** : on la maintient pour déplacer, on l'effleure
  pour ouvrir `BlockMenu`. C'est le seul chemin vers « supprimer » pour un bloc
  qui n'a rien d'autre — une citation vide, un paragraphe de trop — et sans lui
  une citation posée par erreur restait sur la page pour toujours.
  **Seul un vocal demande confirmation.** Supprimer son bloc efface le fichier
  son, et aucune annulation ne le rendra ; tout le reste est dans l'historique,
  donc une question de plus n'y protégerait rien. Confirmer partout aurait
  surtout appris à répondre oui sans lire.
- **Citations et traits, depuis la refonte en blocs** : le trait d'une citation
  est une **forme posée à côté de son texte** (`QuoteBlockView`), plus un dessin
  calculé depuis la mise en page. Trois choses en découlent, et ce sont les
  trois qui manquaient : il fait toute la hauteur sans qu'on ait à la calculer,
  il a sa **propre couleur** (trait bleu sur texte noir, `StyleFamily.QUOTE_BAR`
  séparée de la couleur du texte), et il y a enfin quelque chose à attraper pour
  déplacer la citation — c'est lui. Ses réglages (couleur, fond) apparaissent
  **sous la citation** quand on écrit dedans (`QuotePalette`) : les mêmes
  réglages existaient déjà, rangés au fond d'un panneau qui ne s'ouvrait qu'au
  bon endroit, donc introuvables. Un trait de séparation est un bloc à lui seul
  (`RuleBlockView`), haut d'une ligne exactement.
  **Le trait est posé en `matchParentSize`, pas comme une colonne du `Row`.**
  Première version : un `Box` à gauche, en `fillMaxHeight()`. Il ne s'affichait
  pas du tout, et la raison vaut pour toute la page : `fillMaxHeight` ne
  remplit que si la hauteur **maximale est connue**, or une page qui défile a
  une hauteur maximale infinie — le trait se retrouvait haut de zéro. Posé
  par-dessus une boîte qui épouse la taille déjà calculée du texte, la
  contrainte est finie et le trait a enfin une hauteur.
  Ce qui **n'a pas changé** : rien n'est écrit dans le texte. Un `>` devant
  chaque ligne, ou une rangée de tirets, se retrouverait dans la recherche,
  dans l'export de l'année et dans l'aperçu de la carte. Une citation reste
  exactement le texte qu'on a écrit, et son cadre vit à côté.
  Dans le **texte à plat**, en revanche, citations et traits redeviennent des
  intervalles (`TextStyleKind.QUOTE`, les `isRule` posés sur un saut de ligne
  qui existe vraiment) : c'est ainsi que l'export PDF et les aperçus les
  dessinent, eux qui ne connaissent pas les blocs.
- **Annuler / refaire** (`ui/PageHistory.kt`) : ce qui est retenu est la **page
  entière**, pas la différence. Une page fait quelques kilo-octets et
  l'historique en garde soixante : c'est négligeable, et ça évite toute une
  classe de bugs — un historique de différences doit savoir défaire chaque
  opération à l'envers, et il suffit d'en oublier une (déplacer un bloc,
  changer la couleur d'une citation) pour qu'« annuler » abîme la page au lieu
  de la réparer. Tout ce qu'on tape n'est pas un pas : un pas se pose quand la
  **structure** change, ou après un silence. Sinon annuler reculerait d'une
  lettre.
- **Liens entre pages** (`data/PageLink.kt`, testé dans `PageLinkTest`) : un
  lien s'écrit `@07/09/2026` et se lit tel quel. Le remplacer à l'affichage par
  « mardi 7 septembre » casserait `OffsetMapping.Identity`, donc le curseur —
  la forme est donc choisie pour être lisible sans substitution. Une adresse de
  courriel n'en devient pas un (`@` collé à un mot), et une date qui n'existe
  pas reste du texte plutôt qu'un lien mort. L'appui est intercepté dans la
  passe `Initial` et n'est consommé **que** s'il tombe vraiment sur le lien,
  vérifié en ligne *et* en colonne : `getOffsetForPosition` répond toujours
  quelque chose, même pour un doigt posé loin sous le texte, et sans ces deux
  contrôles appuyer dans le vide ouvrirait un lien et le curseur ne se
  poserait plus.
- **Export PDF d'une page** (`ui/PagePdf.kt`) : la page est **redessinée**, pas
  photographiée — une capture serait floue à l'impression, coupée à la hauteur
  de l'écran, et sans texte. Deux choses le rendent simple, et trois pièges
  valent d'être retenus :
  1. Tout est mesuré dans une densité de **1**, donc un point de l'écran vaut
     une unité du PDF : les placements de photos et la hauteur de ligne
     s'utilisent tels quels. Une conversion oubliée quelque part est exactement
     ce qui envoie une photo à dix centimètres de sa place.
  2. Une page plus longue qu'une feuille n'est pas remise en page : c'est le
     **même dessin** sur chaque feuille, décalé et rogné. Le texte et les
     photos restent donc alignés entre eux d'une feuille à l'autre.
  3. `DrawScope.scale` pivote par défaut autour du **centre**, et `size` reste
     celle de la feuille entière quels que soient les décalages appliqués
     autour — d'où le pivot explicite à l'origine et la largeur passée en
     paramètre plutôt que lue dans `size`.
- **Recherche** : elle croise le texte, la couleur, ce qu'on a fait et les
  étiquettes (`data/DaySearch.kt`, testé dans `DaySearchTest`). Deux règles de
  sens : plusieurs couleurs se lisent « ou », plusieurs étiquettes « et ». Et un
  piège : `DaySearch.fold` (qui enlève accents et majuscules) plie **lettre par
  lettre**. Normaliser la chaîne entière puis jeter les accents la raccourcit,
  et l'extrait affiché autour du mot trouvé serait décalé d'autant de lettres
  accentuées qu'il y a avant lui.
- **La note est sur dix, mais elle est stockée sur trois.** Les quatre couleurs
  valent 0, 1, 2 et 3 (`DayColor.score`) — c'est ce qui est enregistré depuis la
  version 1, et ça ne bouge pas. La conversion en note sur dix se fait **à
  l'affichage seulement** (`outOfTen`, `formatAverage`) : « 1,7 sur 3 » ne dit à
  personne si c'est bien ou mal, « 5,7 sur 10 » si.
  Et la note **dit d'où elle vient** : une phrase sous le chiffre (`SCORE_CAPTION`)
  et une feuille « Comment c'est calculé ? ». Un chiffre seul laisse deviner ce
  qu'il compte, et on devine toujours quelque chose de plus compliqué que la
  vérité. La feuille explique aussi pourquoi ni les pas, ni le sport, ni l'argent
  n'entrent dans la note — c'est la règle 2, vue depuis l'écran : ce sont eux
  qu'on **compare** ensuite à la couleur.
- **La note : le ressenti, plus ce qu'on a fait** (`data/Deed.kt`, testé dans
  `DeedTest`). Dix points pour la couleur des journées, dix pour les gestes
  réussis. Trois règles, et elles sont des promesses faites à l'utilisateur —
  un calcul de note peut les trahir sans que personne ne s'en aperçoive :
  1. **Rien ne se perd.** Un geste manqué n'ajoute rien, il ne retire jamais.
     Une semaine difficile où l'on s'est quand même bougé ne tombe donc pas à
     zéro, et c'est tout l'intérêt.
  2. **Masquer une carte ne coûte rien.** Chaque geste appartient à une carte,
     et une carte masquée sort du calcul **des deux côtés de la fraction**. Les
     actions sont une part (gestes faits sur gestes possibles), pas un compte :
     sinon afficher trois cartes plutôt que dix baisserait la note sans qu'on
     ait rien fait de moins. C'est la question qu'Ismael a posée lui-même, et
     la seule réponse honnête.
  3. **Le ressenti reste intouchable.** Il ne vient que de la couleur, et rien
     de ce qu'on fait ne le corrige — c'est la règle 2 vue depuis la note.
  Les gestes comptés sont **les mêmes que les badges**, et ce n'est pas un
  hasard : les deux répondent à « qu'est-ce que tu as réussi à faire
  aujourd'hui ? ». Les faire diverger donnerait une médaille sans point, ou un
  point sans médaille, et personne ne saurait lequel compte.
  Sans aucune carte à geste affichée, la note reste **sur dix** : il n'y a rien
  à compter, et prétendre le contraire la plafonnerait à la moitié sans que
  rien ne l'explique.
- **Barre d'outils du journal : deux portes, pas une liste.** Il y avait un
  seul panneau, « Tous les outils », où l'alignement se trouvait derrière les
  titres, la taille, les blocs et les listes. Ismael l'a trouvé — en cherchant,
  et un réglage qu'on trouve en cherchant est un réglage mal rangé. On ne
  cherche pas de la même façon « poser quelque chose » et « changer l'allure de
  ce qui est déjà là » : ce sont maintenant `ToolPanel.INSERT` et
  `ToolPanel.FORMAT`, avec l'alignement en tête du second.
  La rangée du bas est passée de dix boutons à sept. Ce qui reste est soit une
  porte, soit un geste qu'on fait **sans arrêter d'écrire** (le micro, le gras,
  l'italique, les deux couleurs) ; souligné, barré et le mot-clé sont partis
  derrière une porte. Les réglages d'une citation ont quitté le panneau : ils
  apparaissent déjà sous la citation, et deux chemins pour la même chose
  allongeaient le panneau pour tout le monde.
- **Effacer au début d'un bloc : `onPreviewKeyEvent`, jamais `onKeyEvent`.**
  Le champ de texte **consomme** la touche d'effacement, donc l'événement ne
  remonte jamais jusqu'à un `onKeyEvent` posé au-dessus de lui — c'est pour ça
  que la première version ne faisait rien du tout. La passe « preview » descend
  depuis la racine et la donne en premier. Une citation redevient alors du
  texte, et ne disparaît qu'au coup suivant : la touche d'effacement défait la
  mise en forme avant de défaire le texte.
- **Alignement et taille dans le journal** : deux choses de nature différente,
  et c'est ce qui décide où elles vivent. L'alignement est une propriété du
  **bloc** (`BlockAlign`, colonne `alignCode`, migration 19→20) : centrer la
  moitié d'un paragraphe ne veut rien dire. La taille est un **intervalle**
  (`StyleFamily.SIZE`), comme le gras. Elle s'exprime en **em**, donc en part de
  la taille de base : grossir un mot suit le réglage de la page au lieu de le
  contredire, et les deux écarts (0,85 et 1,15) restent sous la hauteur d'une
  ligne du lignage, sans quoi le texte sortirait de ses lignes.
  L'alignement ne survit pas dans le texte à plat — comme les vocaux, il n'a
  pas d'équivalent en caractères. Le PDF ne le montre donc pas.
- **Toucher un mot-clé ouvre les journées qui le portent**
  (`ui/HashtagTap.kt` → `search?tag=…`). Écrire `#mood` en faisait déjà une
  étiquette retrouvable dans la recherche, mais il fallait *savoir* que la
  recherche avait un filtre « Mots-clés », l'ouvrir, et y retrouver son mot
  parmi les autres. Personne ne fait ça, et le mot est déjà sous le doigt.
  Mêmes précautions que pour un lien entre pages, et pour la même raison :
  `getOffsetForPosition` répond toujours quelque chose, donc sans la
  vérification en ligne **et** en colonne, appuyer dans le vide ouvrirait la
  recherche et le curseur ne se poserait plus au bas de la page.
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
- **La page suit le curseur** : le champ du journal ne défile pas lui-même — il
  **grandit**, et c'est la page autour de lui qui défile. Personne ne le fait
  donc à sa place : sans rien, écrire en bas de page tapait derrière le clavier.
  La position du curseur vient de `onTextLayout` puis `getCursorRect`, en
  coordonnées **du champ** : il faut y ajouter la marge du haut de la page pour
  retomber dans le repère du défilement. On ne recentre que si le curseur sort
  de la fenêtre, jamais sinon — un défilement à chaque lettre donnerait le mal
  de mer.
- **Boutons du journal et couleur du papier** : les outils de mise en forme
  vivent **sur** la page, pas à côté. Ils prennent donc leur encre du papier
  choisi (`LocalPaperInk` / `LocalPaperSurface` dans `ui/JournalToolbar.kt`),
  et non du thème : sur l'ardoise, des boutons noirs sur fond sombre
  disparaissaient. `paperAccent()` éclaircit en plus la couleur d'accent quand
  le papier est foncé, sinon le violet de l'application y devient une tache
  sourde.
- **Paramètres de la page** (`PaperSettingsSheet`) : un **panneau** qui monte du
  bas, pas une boîte de dialogue — une boîte assombrit la page et cache
  justement ce qu'on est en train de régler. Il porte un aperçu qui est la page
  elle-même, à sa vraie échelle et simplement coupée : une miniature aurait
  menti sur la taille du texte, qui est l'un des réglages. La police de base est
  rangée sous le **code** du style (`TextStyleKind.code`), jamais sous son rang
  dans la liste : en ajouter une plus tard ne doit pas changer les pages déjà
  écrites. Les tailles proposées restent toutes sous l'écart entre deux lignes
  (28 points) : le texte grandit, le lignage ne bouge pas.
- **Son des badges** (`ui/BadgeSound.kt`) : un fichier embarqué, joué seulement
  si le téléphone est en sonnerie normale — en silencieux ou en vibreur, une
  application qui sonne quand même est une application qu'on désinstalle. Le
  son part en même temps que la vibration, au **passage** d'état comme
  l'animation, jamais à l'affichage.
- **Le journal a un seul propriétaire.** Le titre, les blocs, et le texte à
  plat qu'ils produisent sont écrits **uniquement** par l'écran du journal
  (`DayRepository.saveJournal`) ; « Ma journée » écrit tout **sauf** ces trois
  champs (`saveDayKeepingJournal`) et n'en garde plus de copie — il les
  observe. Avant, les deux écrans en avaient chacun une copie et les
  réécrivaient : le dernier à enregistrer gagnait, et ce n'était pas celui qui
  avait édité. Effacer une page puis revenir à la journée remettait l'ancien
  texte, donc **une page était impossible à vider**. La correction est une
  séparation de propriété, pas un correctif d'ordre : aucun ordre
  d'enregistrement ne peut plus faire perdre ce qu'on vient d'écrire.
- **Vocaux** (`data/VoiceNote`, `ui/VoiceRecorder.kt`, `ui/VoiceNotes.kt`) : leur
  **propre table**, pas un média de plus. Un vocal ne se pose pas sur la page,
  ne se recadre pas, n'a ni forme ni inclinaison — et il a une durée, que rien
  dans une photo ne porte. Le ranger avec les images aurait demandé de le
  filtrer dans chaque endroit qui affiche un album, et il aurait fini en
  vignette cassée dans l'un d'eux. Trois règles :
  1. On enregistre **directement dans le fichier final** : un vocal de dix
     minutes recopié à la fin, c'est dix minutes de risque pour rien.
  2. La durée est **mesurée pendant**, jamais relue après — relire chaque
     fichier pour afficher une liste serait absurde, et un fichier abîmé
     rendrait toute la liste illisible au lieu d'une seule ligne.
  3. Moins d'une demi-seconde : c'est un appui raté, pas un vocal, et le
     fichier est jeté.
  Deux endroits qu'il est facile d'oublier, et qui **perdent des données**
  quand on les oublie : la sauvegarde (`Backup`, sinon un vocal disparaît à la
  première restauration sans que rien ne le signale) et « est-ce que cette
  journée est vide ? », qui décide de supprimer la ligne — d'où
  `DayRepository.hasAttachments`, qui pose la question à un seul endroit.
  Un vocal est un **bloc**, pas une image posée librement : il se range entre
  deux paragraphes, jamais par-dessus le texte et jamais de travers. On le
  déplace en maintenant le doigt puis en tirant, comme tous les blocs, et deux
  largeurs seulement (barre entière ou rétrécie, par une poignée sur la barre)
  — une barre de lecture n'a pas de proportions à respecter comme une photo, et
  la tirer au doigt donnerait surtout des largeurs bancales. Rangés en bande
  au-dessus du texte, ils n'étaient qu'une liste ; posés entre deux
  paragraphes, ils appartiennent à un moment.
  **On enregistre à la place où le vocal se posera.** Le bloc est créé au
  curseur au moment où le micro s'ouvre, à la forme et à la taille qu'aura la
  barre finale, avec un bouton rouge à l'emplacement du bouton d'écoute
  (`RecordingRow` et `VoiceNoteRow` partagent `VoiceShell`, et c'est le but :
  rien ne bouge à l'écran quand l'enregistrement s'arrête). Un témoin en haut
  de la page, puis un vocal qui apparaissait ailleurs et qu'il fallait
  descendre à la main, était la pire des deux moitiés.
  Le bloc de l'enregistrement en cours est un bloc vocal **sans son**
  (`voiceId` nul) : il n'est jamais enregistré (`PageBlocks.toStored` l'écarte),
  sinon fermer l'application au milieu d'une phrase laisserait une barre vide.
  Et comme le vocal est écrit en base dès qu'on s'arrête alors que les blocs ne
  le sont qu'en quittant l'écran, les deux peuvent diverger : un vocal sans
  bloc se range à la fin à l'ouverture suivante, un bloc sans son s'en va
  (`JournalBlocks.reconcile`). La page se répare plutôt que de payer une
  écriture complète à chaque fois qu'on parle.
  La **silhouette du son** (`data/Waveform.kt`, testée) est mesurée pendant
  l'enregistrement : `getMaxAmplitude` rend le plus fort depuis le dernier
  appel, donc un point régulier suffit sans rien décoder. Elle est
  **normalisée** — la plus haute barre vaut toujours le maximum, donc un vocal
  chuchoté se voit autant qu'un vocal crié : on ne dessine pas un volume, on
  dessine un rythme. Le sous-échantillonnage garde le **plus fort** de chaque
  tranche et non la moyenne, qui aplatit tout jusqu'à une ligne droite. Un
  vocal sans mesure (ceux d'avant) garde une silhouette neutre : une barre de
  lecture sans barres ne ressemble à rien.
  Les barres sont **dessinées**, jamais composées : cinquante-six petites vues
  à remesurer à chaque image de la lecture, pour des rectangles de deux points
  de large. L'avancée de la lecture est lue **dans le dessin**, comme toute
  valeur qui change à chaque image.
- **Icônes** : le projet n'embarque que le jeu **de base**
  (`material-icons-core`). Beaucoup d'icônes courantes n'y sont pas (le micro,
  par exemple) : on recopie alors le dessin dans `res/drawable/`, plutôt que
  d'ajouter le jeu complet, qui pèse plusieurs mégaoctets pour une icône.
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
- **Réglages d'une photo** (`PhotoToolsBar`) : c'était un tas de pastilles
  identiques sur trois rangées — quatre formes, un contour, un aimant, trois
  profondeurs — sans rien qui dise lesquelles allaient ensemble ni lesquelles
  s'excluaient, en blanc et violet du thème sur un papier ivoire. Les mêmes
  règles que les cartes de « Ma journée » le remettent d'aplomb : **une
  question, une réponse, une forme** (un choix parmi quatre est une piste
  segmentée, un oui-ou-non est un interrupteur), **chaque question porte son
  intitulé** (« Profondeur » était le seul à en avoir un, et c'est pour ça
  qu'il était le seul compréhensible), et **les couleurs viennent du papier**,
  comme la barre d'outils du journal.
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

- **Les médailles se déclenchent au passage, et « pas encore chargé » n'est pas
  « vide ».** C'est le même piège deux fois. Les champs d'une journée partent
  vides et se remplissent une fraction de seconde plus tard : sans garde, ouvrir
  une journée déjà écrite ressemble trait pour trait à l'écrire, et la médaille
  repart. La première version attendait `loadedFor` — le chargement des *autres*
  champs — ce qui laissait passer le cas où le journal, lui, n'avait pas encore
  répondu : deux lectures de la même journée, chacune de son côté, et rien ne
  disait laquelle arriverait la première. D'où une médaille qui revenait au
  hasard en passant d'un jour à l'autre avec les flèches. Le flux du journal
  porte donc **le jour auquel il répond** (`epochDay to entry`), ce qui distingue
  enfin « cette journée n'a pas de texte » de « la réponse n'est pas arrivée » —
  les deux valaient `null`. Toute nouvelle médaille lue depuis un flux doit faire
  pareil.
- **Le lignage du journal suit le pas *mesuré* du texte, pas celui qu'on
  demande.** La hauteur de ligne voulue (28 points) tombe presque toujours sur un
  nombre de pixels à virgule, et le moteur de texte arrondit **chaque ligne** au
  pixel. Un quart de pixel d'écart ne se voit pas sur trois lignes ; sur deux
  cents, le texte a glissé d'une demi-ligne et les traits passent au milieu des
  mots — plus la page était longue, plus le décalage était grand. `PageColumn`
  remonte donc l'écart réel entre deux lignes (`lineAdvance()`, mesuré sur un
  paragraphe **sans mise en forme** : un mot écrit plus grand fausserait tout), et
  `PaperLines` dessine à ce pas-là. La poignée des blocs et les traits de
  séparation prennent la même hauteur, sinon chaque paragraphe court ajouterait
  son pixel.
- **Une carte « vérifiée » n'est pas une carte remplie.** Le geste latéral sur
  une carte de la journée dit « je l'ai relue », rien de plus : ça ne remplit
  rien, ça ne compte **jamais** dans la note, et la marque vit dans la journée
  (`DayEntry.checkedCards`) et non dans les préférences — c'est une propriété de
  *ce jour-là*. Deux conséquences à ne pas défaire : une journée dont on n'a
  gardé que « j'ai relu » n'est pas vide (sinon `saveDay` l'efface en quittant
  l'écran et la marque disparaît), et la coche de l'en-tête doit rester, parce
  qu'un geste qu'on ne devine pas ne doit jamais être le seul chemin.
- **Le vendredi, le dhuhr s'appelle la jumu'a** (`Prayer.labelOn`). C'est un
  **nom**, pas une sixième prière : le masque, les bits, le compte des cinq et la
  médaille ne bougent pas, et un vendredi coché reste un dhuhr coché. C'est le
  modèle à suivre pour toute autre « carte d'événement » : changer ce qui
  s'affiche ce jour-là, jamais ce qui est enregistré.

- **Les tests d'interface doivent ecarter les deux ecrans d'accueil.** La
  reprise de sauvegarde (base vide) **et** l'animation « Salut … »
  (`helloPlayed`, remis a faux a chaque lancement du processus) passent devant
  le calendrier. Sans les deux lignes du `@BeforeClass` de `CalendarUiTest`, les
  six tests echouent ensemble sur « aucun noeud ne correspond » — ce qui
  ressemble a six bugs alors qu'il n'y en a qu'un, et pas dans l'application.
  Deux autres regles tirees de la meme panne : un jour du calendrier vit sous la
  carte du jour, donc `performScrollTo()` avant `performClick()` ; et les
  onglets se visent par leur repere (`tab-stats`, `tab-money`…) et non par leur
  nom, qui peut aussi etre ecrit dans la page.
- **Une seule liste de migrations** (`AppDatabase.ALL_MIGRATIONS`), utilisee par
  l'application *et* par les tests. Quand le test en gardait une copie, ajouter
  une migration sans la recopier donnait « A migration from 5 to 22 was required
  but not found » — un echec qui n'a rien a voir avec ce qu'on vient d'ecrire.

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

Le workflow ne passe **pas** `--stacktrace` : il ajoutait trois cents lignes
de pile Java après chaque échec, sous lesquelles les vraies erreurs de
compilation devenaient introuvables. Les lignes qui comptent sont celles qui
commencent par `e: file://…`, juste avant « BUILD FAILED ».

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
