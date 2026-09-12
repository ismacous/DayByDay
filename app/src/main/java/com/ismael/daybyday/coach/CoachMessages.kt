package com.ismael.daybyday.coach

/**
 * Le catalogue de phrases. Chaque situation en propose plusieurs, et la
 * variante tourne a chaque fois : c'est ce qui evite l'impression de parler a
 * un repondeur.
 *
 * Regles d'ecriture, a garder si tu en ajoutes :
 * - on tutoie, on reste simple, on ne fait jamais la morale ;
 * - un constat n'est jamais un reproche (« ca fait trois jours », pas « tu
 *   n'as toujours pas ») ;
 * - une proposition est toujours une question, et laisse la porte ouverte a
 *   un non ;
 * - on ne promet rien sur ce qui va arriver, et on ne fait aucun diagnostic ;
 * - **aucune phrase ne signale une serie brisee.** Les series ne servent qu'a
 *   souligner ce qui va bien ; annoncer qu'elle est cassee punirait exactement
 *   les journees qu'il ne faut pas punir. C'est la meme regle que pour les
 *   medailles.
 *
 * Les valeurs entre accolades sont remplacees au moment de l'affichage :
 * `{n}`, `{quoi}`, `{constat}`, `{prenom}`, `{duree}`, `{mois}`.
 */
object CoachMessages {

    private val messages: Map<CoachRule, List<String>> = mapOf(

        // --- Soutien quand c'est dur -------------------------------------

        CoachRule.DAYS_VERY_DARK to listOf(
            "Ça fait {n} jours que c'est très sombre. Tu n'as rien à prouver à personne, et tu n'as pas à tenir ça tout seul. Si tu veux parler à quelqu'un, même en pleine nuit, le 3114 répond gratuitement.",
            "{n} jours de suite dans le noir, c'est énorme à porter. Tenir jusqu'ici, c'est déjà quelque chose. Le 3114 est là si tu as besoin d'une voix maintenant, à n'importe quelle heure.",
            "Je vois {n} journées très noires d'affilée. Je ne peux pas t'enlever ça, mais tu n'es pas obligé de le traverser seul : le 3114, c'est gratuit, jour et nuit, et personne ne te juge.",
            "Ça dure depuis {n} jours et c'est lourd. Appeler quelqu'un n'est pas un aveu de faiblesse — le 3114 existe exactement pour ces moments-là.",
            "{n} jours comme ça, c'est beaucoup trop pour une seule personne. Si tu peux en parler à quelqu'un aujourd'hui, à ta moitié, à ta famille, ou au 3114, fais-le.",
        ),

        CoachRule.BLACK_STREAK to listOf(
            "{n} journées noires de suite. Je ne vais pas te dire que ça va aller, mais je le note avec toi : c'est dur en ce moment.",
            "Ça fait {n} jours que c'est très lourd. Tu n'es pas obligé de faire quoi que ce soit de plus aujourd'hui que de passer la journée.",
            "{n} jours sombres à la suite. Si tu as pu ouvrir l'appli, c'est déjà que tu tiens quelque chose.",
            "Deux, trois jours noirs d'affilée, ça arrive et ça passe, même si là tout de suite ça ne se voit pas. Je reste là.",
            "{n} journées noires. Aujourd'hui, le minimum suffit : boire, manger un truc, respirer. Le reste peut attendre.",
            "C'est la {n}e journée noire de suite. Tu as le droit de mettre tout le reste en pause.",
        ),

        CoachRule.BLACK_DAY to listOf(
            "Journée très noire. Merci de l'avoir quand même notée — c'est déjà beaucoup un jour comme ça.",
            "Tu l'as mise en noir. Pas de conseil, pas de « il faut que » : juste, je l'ai vue.",
            "Une journée noire, ça se traverse, ça ne se répare pas dans la soirée. Pose-toi.",
            "C'était noir aujourd'hui. Ça ne dit rien de qui tu es, juste de comment était cette journée-là.",
            "Noir aujourd'hui. Demain sera un autre carré sur le calendrier.",
            "Journée très noire notée. Si tu as quelqu'un à qui envoyer un message ce soir, même court, c'est peut-être le moment.",
        ),

        CoachRule.ANXIETY to listOf(
            "Crise d'angoisse aujourd'hui. Essaie de ralentir ta respiration un moment : souffler plus longtemps qu'inspirer, ça aide parfois à faire redescendre.",
            "Tu as coché une crise d'angoisse. Ça passe toujours, même quand pendant l'épisode on est persuadé du contraire.",
            "L'angoisse d'aujourd'hui est notée. Rien à analyser ce soir, tu peux juste te mettre au calme.",
            "Crise d'angoisse. Tu n'as pas à comprendre pourquoi maintenant, ça peut attendre un jour plus clair.",
            "Tu l'as marquée. Ça sert : dans quelques semaines, tu verras dans quels moments ça revient.",
        ),

        CoachRule.CRIED to listOf(
            "Tu as pleuré aujourd'hui. C'est le corps qui évacue, pas un échec.",
            "Journée avec des larmes. Bois un verre d'eau, c'est bête mais ça fait du bien après.",
            "Tu as pleuré. Merci de l'avoir noté plutôt que de faire comme si de rien n'était.",
            "Des larmes aujourd'hui. Sois aussi doux avec toi que tu le serais avec un pote dans le même état.",
            "Tu as pleuré. Ça arrive aux gens solides aussi.",
        ),

        CoachRule.RED_DAY to listOf(
            "Journée difficile. Elle est notée, tu peux la poser maintenant.",
            "C'était dur aujourd'hui. Pas besoin d'en faire plus.",
            "Journée difficile notée. Le fait qu'elle soit rouge et pas noire, c'est déjà une info.",
            "Dure, celle-là. Repose-toi si tu peux.",
            "Journée compliquée. Tu n'as pas à la rattraper ce soir.",
        ),

        CoachRule.HARD_WEEK to listOf(
            "La semaine est basse dans l'ensemble. Ce n'est pas toi qui fais mal les choses, c'est une période difficile.",
            "Semaine lourde. Si tu as un rendez-vous médical possible dans les jours qui viennent, ça peut valoir le coup d'en parler.",
            "Cette semaine est plus sombre que d'habitude. Baisse tes attentes pour les jours qui restent, ça suffira.",
            "Sept jours plutôt durs. Regarde le calendrier sur plusieurs mois plutôt que sur cette semaine : ça bouge tout le temps.",
            "La semaine a été dure. Ce que tu arrives quand même à faire pendant ces semaines-là compte double.",
        ),

        // --- Sante et soin de soi ----------------------------------------

        CoachRule.MEDS_MISSING to listOf(
            "Rien de coché dans tes traitements depuis {n} jours. Tu les as pris ?",
            "Je ne vois aucune prise cochée depuis {n} jours — peut-être juste un oubli de case ?",
            "{n} jours sans prise cochée. Si tu les as pris, coche-les ; sinon, c'est peut-être le moment.",
            "Petit point traitements : rien de coché depuis {n} jours.",
            "Ça fait {n} jours sans rien de coché côté traitements. Pas de sermon, juste un rappel.",
        ),

        CoachRule.SHOWER_MISSING to listOf(
            "Ça fait {n} jours. Tu te sens d'attaque pour une douche aujourd'hui ?",
            "Une douche, ça ne te dit rien là tout de suite ? Même rapide, ça compte.",
            "Pas de douche cochée depuis {n} jours. Si c'est trop, se laver le visage à l'eau froide, c'est déjà quelque chose.",
            "Une idée, sans obligation : passer sous l'eau. Souvent on se sent un peu mieux après.",
            "Tu penses pouvoir prendre une douche aujourd'hui ? Si c'est non, c'est non, on en reparlera.",
            "{n} jours sans douche cochée. Ça arrive quand l'énergie n'est pas là. Aujourd'hui, possible ?",
        ),

        CoachRule.BRUSHING_LOW to listOf(
            "Aucun brossage coché depuis {n} jours. Un seul, ce soir, ça suffirait.",
            "{n} jours sans te brosser les dents d'après la carte. Deux minutes, et c'est fait.",
            "Les dents attendent depuis {n} jours. Même un brossage sur trois, c'est mieux que zéro.",
            "Rien de coché côté brossage depuis {n} jours. Sans reproche : juste au cas où ça se serait perdu dans les journées.",
            "{n} jours sans brossage. Si tu passes devant le lavabo tout à l'heure, c'est l'occasion.",
        ),

        CoachRule.SHORT_NIGHTS to listOf(
            "Tes dernières nuits tournent autour de {duree}. Beaucoup de choses paraissent pires quand on dort peu.",
            "{duree} de sommeil en moyenne ces derniers jours. Ne juge pas trop tes journées, elles partent avec un handicap.",
            "Des nuits courtes en ce moment ({duree} en moyenne). Si tu peux lever le pied aujourd'hui, fais-le.",
            "Tu dors autour de {duree} ces temps-ci. Une sieste courte en début d'après-midi ne casse pas la nuit suivante.",
            "{duree} par nuit, c'est peu. C'est peut-être le premier truc à regarder avant le reste.",
        ),

        CoachRule.LATE_NIGHTS to listOf(
            "{n} couchers après minuit d'affilée. Pas de leçon, juste un constat.",
            "Ça fait {n} nuits où tu te couches très tard. Tu veux essayer de couper les écrans un peu plus tôt ce soir ?",
            "{n} nuits tardives à la suite. Les journées d'après le sentent souvent.",
            "Couché très tard {n} fois de suite. Si demain est libre, ce n'est pas grave du tout.",
            "{n} soirs de suite passé minuit. Ton corps s'en souvient même si toi non.",
        ),

        CoachRule.WATER_LOW to listOf(
            "Moins de quatre verres d'eau depuis {n} jours. Un verre maintenant, tant que tu y penses ?",
            "{n} jours à boire peu. La fatigue et le mal de tête viennent souvent de là avant de venir d'ailleurs.",
            "Peu d'eau ces {n} derniers jours. Rien d'obligatoire, c'est juste le plus facile à corriger.",
            "Ça fait {n} jours que le compteur d'eau reste bas. Un verre, et c'est déjà mieux.",
            "Tu bois peu en ce moment. Pose un verre à côté de toi, ça marche mieux que d'y penser.",
        ),

        // --- Ce qui remonte ----------------------------------------------

        CoachRule.FIRST_GREEN to listOf(
            "Première journée verte depuis {n} jours. Je la note en gras.",
            "Ça faisait {n} jours qu'il n'y en avait pas eu une comme ça. Elle compte.",
            "Une verte, enfin, après {n} jours. Essaie de te souvenir de ce qu'il y avait dedans.",
            "{n} jours que j'attendais celle-là. Bien joué.",
            "Verte après {n} jours de traversée. C'est la preuve que ça bouge encore.",
        ),

        CoachRule.BACK_AFTER_BREAK to listOf(
            "Content de te revoir. {n} jours sans rien noter, et alors : on reprend où tu veux.",
            "Te revoilà. Pas besoin de rattraper les {n} jours manquants, commence par aujourd'hui.",
            "{n} jours d'absence, aucune importance. Le calendrier n'est pas un devoir.",
            "Ça faisait {n} jours. Si tu te souviens des couleurs de ces jours-là, tu peux les remplir ; sinon on repart d'ici.",
            "De retour après {n} jours. Les trous font partie de l'histoire eux aussi.",
        ),

        CoachRule.GREEN_STREAK to listOf(
            "{n} journées vertes d'affilée. Regarde ce que tu as fait de différent ces jours-là.",
            "Trois bonnes journées à la suite, ce n'est pas un hasard. Continue pareil.",
            "{n} vertes de suite. Je te le dis parce que c'est facile de ne pas le remarquer soi-même.",
            "{n} bonnes journées. Note dans ton journal ce qui tourne bien en ce moment, ça servira plus tard.",
            "{n} jours verts d'affilée. Profites-en sans culpabiliser.",
        ),

        CoachRule.REBOUND to listOf(
            "Nette remontée par rapport à hier. Ça fait du bien de le voir écrit.",
            "Hier était dur, aujourd'hui beaucoup moins. Voilà exactement pourquoi ça vaut le coup de noter.",
            "Ça remonte par rapport à hier. Ces bascules-là arrivent plus souvent que tu ne crois.",
            "Grosse différence avec hier, dans le bon sens.",
            "Aujourd'hui est bien au-dessus d'hier. Garde ça en tête le prochain jour noir.",
        ),

        CoachRule.NOTING_STREAK to listOf(
            "{n} jours notés d'affilée. Tenir un suivi comme ça, beaucoup n'y arrivent pas trois jours.",
            "Série de {n} jours sans en louper un. Ça commence à faire de vraies données.",
            "{n} jours d'affilée. C'est ça qui rend l'onglet Bilan utile.",
            "{n} jours à la suite. Rien que pour la régularité, chapeau.",
            "{n} jours notés sans interruption. Tu construis quelque chose, même les jours où tu n'en as pas l'impression.",
        ),

        CoachRule.PRAYER_STREAK to listOf(
            "{n} jours de suite avec les cinq prières. C'est une régularité qui se remarque.",
            "Les cinq, {n} jours d'affilée.",
            "{n} jours complets côté prières. Bien tenu.",
            "Cinq sur cinq depuis {n} jours.",
        ),

        CoachRule.GREEN_DAY to listOf(
            "Bonne journée notée. Qu'est-ce qui l'a rendue comme ça ? Deux mots dans le journal et tu le sauras dans six mois.",
            "Une verte aujourd'hui. Prends-la.",
            "Bonne journée. Ce sont celles-là qu'on oublie le plus vite : écris-en un bout.",
            "Journée verte. C'est aussi une info importante, pas juste les mauvaises.",
            "Une bonne. Rien d'autre à ajouter.",
        ),

        CoachRule.JOB_EFFORT to listOf(
            "{n} candidatures cette semaine. C'est du travail, même quand ça ne répond pas.",
            "{n} candidatures envoyées sur sept jours. Le résultat ne dépend pas de toi, l'envoi si — et tu l'as fait.",
            "{n} candidatures cette semaine. Garde ce chiffre en tête les jours où tu as l'impression de ne rien faire.",
            "Belle semaine côté recherche : {n} candidatures.",
            "{n} envoyées cette semaine. Chacune a coûté quelque chose, et ça se voit dans la courbe.",
        ),

        CoachRule.BETTER_WEEK to listOf(
            "Cette semaine est au-dessus de la précédente. Doucement, mais dans le bon sens.",
            "La semaine remonte par rapport à la dernière.",
            "Meilleure semaine que celle d'avant. Ça ne se sent pas toujours sur le moment, mais les chiffres le disent.",
            "Semaine en hausse. Continue comme tu fais.",
            "La moyenne de la semaine est meilleure que la précédente.",
        ),

        CoachRule.PRAYERS_ALL to listOf(
            "Les cinq prières faites aujourd'hui.",
            "Cinq sur cinq. C'est noté.",
            "Journée complète côté prières.",
            "Les cinq y sont.",
        ),

        CoachRule.BEST_MONTH to listOf(
            "{mois} est ton meilleur mois de cette dernière année.",
            "Meilleure moyenne mensuelle des douze derniers mois : {mois}.",
            "{mois} passe devant les autres mois de l'année. Va voir l'onglet Bilan, ça se voit bien.",
            "Aucun mois de cette année n'était monté aussi haut que {mois}.",
            "{mois} tient le haut du classement de tes derniers mois.",
            "Record de moyenne mensuelle battu avec {mois}.",
        ),

        // --- Ce qui va avec les bonnes journees --------------------------

        CoachRule.FACTOR_SUGGESTION to listOf(
            "J'ai remarqué que tes journées sont souvent meilleures {constat}. Tu penses pouvoir {quoi} aujourd'hui ?",
            "Sur l'ensemble de tes journées, ça va plutôt mieux {constat}. Ça te dit de {quoi} ?",
            "Petite idée du jour : {quoi}. Tes bonnes journées vont souvent avec ça.",
            "Tes notes montrent que ça se passe mieux {constat}. Aujourd'hui, {quoi}, c'est jouable ?",
            "Si tu cherches quoi faire : {quoi}. Chez toi, ça revient souvent dans les bonnes journées.",
            "{constat}, tes journées sont en moyenne plus hautes. Tu veux tenter de {quoi} ?",
        ),

        CoachRule.FACTOR_TODAY to listOf(
            "Tu l'as fait aujourd'hui, et {constat} ça se passe souvent mieux chez toi.",
            "C'est coché aujourd'hui — d'après tes journées, {constat} c'est plutôt bon signe.",
            "Bien vu : {constat}, ta moyenne est plus haute que d'habitude.",
            "Ça revient souvent dans tes bonnes journées, et c'est là aujourd'hui.",
            "Noté : {constat}, ça se passe rarement mal chez toi.",
            "Ça, c'est un de tes facteurs qui marchent. Il est coché aujourd'hui.",
        ),

        CoachRule.FACTOR_HEAVY to listOf(
            "À noter, sans jugement : {constat}, tes journées sont en moyenne un peu plus basses.",
            "Juste une observation : {constat}, la moyenne descend un peu chez toi. À toi de voir ce que tu en fais.",
            "{constat}, tes notes sont souvent un peu plus dures. Ça ne veut pas dire que c'est la cause.",
            "Sur tes données, {constat} ça tire un peu vers le bas. C'est un constat, pas un reproche.",
            "Pour info seulement : {constat}, tes moyennes sont un peu en dessous.",
            "Tes journées {constat} sont un peu plus basses en moyenne. Rien de plus à en conclure.",
        ),

        // --- Habitudes du quotidien --------------------------------------

        CoachRule.ALONE_STREAK to listOf(
            "{n} jours sans personne de noté. Un message à quelqu'un, même trois mots, ça compte comme du contact.",
            "Ça fait {n} jours sans voir grand monde. Tu as quelqu'un à qui tu pourrais écrire aujourd'hui ?",
            "{n} jours en solo d'après les cartes. Pas obligé de voir quelqu'un en vrai : un appel de deux minutes, ça suffit parfois.",
            "Personne depuis {n} jours. Si l'idée te pèse, laisse tomber ; sinon, pense à ta moitié ou à un pote.",
            "{n} jours seul. C'est le genre de truc qui s'installe sans qu'on le décide.",
        ),

        CoachRule.STAYED_IN to listOf(
            "{n} jours sans sortir. Même cinq minutes devant la porte, ça compte comme être sorti.",
            "Ça fait {n} jours à la maison. Tu te sens de faire un petit tour, juste au bout de la rue ?",
            "{n} jours dedans. L'air dehors, c'est souvent moins pire que ce qu'on imagine avant de sortir.",
            "Pas sorti depuis {n} jours. Une course à faire, une poubelle à descendre ? Ça fait le job.",
            "{n} jours sans mettre le nez dehors. Si c'est non aujourd'hui, c'est non.",
        ),

        CoachRule.NO_MOVEMENT to listOf(
            "{n} jours sans rien bouger. Pas besoin de séance : dix minutes de marche, c'est déjà « un peu bougé ».",
            "Ça fait {n} jours sans activité notée. Tu te sens de bouger un peu aujourd'hui ?",
            "{n} jours sans bouger. Le corps qui reste immobile, ça joue sur le moral aussi.",
            "Rien bougé depuis {n} jours. Même s'étirer debout deux minutes compte.",
            "{n} jours sans activité. Et si tu mettais juste de la musique en rangeant un truc ?",
        ),

        CoachRule.BAD_SLEEP to listOf(
            "Plusieurs mauvaises nuits ces derniers jours. Beaucoup de choses paraissent pires quand on a mal dormi.",
            "Le sommeil n'est pas bon en ce moment. Si tu peux lever le pied aujourd'hui, fais-le.",
            "Nuits difficiles ces jours-ci. Ne juge pas trop tes journées, elles partent avec un handicap.",
            "Ça fait plusieurs nuits compliquées. Une sieste courte en début d'après-midi ne casse pas la nuit suivante.",
            "Tu dors mal depuis quelques jours. C'est peut-être le premier truc à regarder avant le reste.",
        ),

        CoachRule.HIGH_SCREEN to listOf(
            "{duree} sur le téléphone hier, bien au-dessus de ton habitude. Rien d'obligatoire, c'est juste pour info.",
            "Grosse journée d'écran : {duree}. Ça arrive surtout les jours où on n'a pas envie d'être ailleurs.",
            "{duree} d'écran, c'est plus que ta moyenne. Peut-être une pause de dix minutes sans le téléphone ?",
            "Le téléphone a pris {duree}. Aucun jugement, tu fais ce que tu veux de ce chiffre.",
            "{duree} d'écran. Parfois c'est exactement ce dont on a besoin, parfois non — toi seul sais.",
            "Beaucoup de téléphone en ce moment ({duree}). Le poser dans une autre pièce dix minutes, ça marche bien.",
        ),

        CoachRule.LOW_STEPS to listOf(
            "Très peu de pas aujourd'hui. Un tour du pâté de maisons et le compteur bouge déjà.",
            "Le compteur de pas est bas pour l'heure qu'il est. Ça te dit de le faire monter un peu ?",
            "Journée sans beaucoup de pas. Ce n'est pas grave, c'est juste un chiffre.",
            "Peu de pas pour l'instant. Même dans l'appart, ça compte.",
            "Compteur de pas bas aujourd'hui. Aucun objectif à tenir, c'est juste une info.",
            "Pas beaucoup de pas pour le moment. Une petite marche, si le cœur t'en dit.",
        ),

        // --- Argent et travail --------------------------------------------

        CoachRule.BIG_SPENDING to listOf(
            "Grosse dépense aujourd'hui. Le fait de l'écrire, c'est déjà la moitié du travail.",
            "Une grosse sortie d'argent. Tu la verras dans l'onglet Argent, pas besoin d'y penser en plus.",
            "Dépense importante notée. Ça arrive, et c'est mieux dans l'appli que dans un coin de ta tête.",
            "Grosse dépense enregistrée. Le budget, ça se regarde sur le mois, pas sur la journée.",
            "C'est noté. Tu retrouveras le détail dans l'onglet Argent quand tu voudras.",
        ),

        CoachRule.CALM_MONEY to listOf(
            "Mois plus calme côté dépenses que le précédent, à la même date.",
            "Tu dépenses moins que le mois dernier pour l'instant. Ça se voit dans l'onglet Argent.",
            "Le budget du mois est plus tranquille que celui d'avant.",
            "Moins de dépenses que le mois dernier à ce stade.",
            "Côté argent, le mois est plus léger que le précédent.",
        ),

        CoachRule.JOB_PAUSE to listOf(
            "Pas de candidature depuis {n} jours. Ce n'est pas une course, et une pause est parfois exactement ce qu'il faut.",
            "{n} jours sans rien envoyer. Si l'énergie revient aujourd'hui, une seule suffirait.",
            "Ça fait {n} jours côté recherche. Sans pression : le compteur est là pour toi, pas contre toi.",
            "{n} jours sans candidature. Chercher du travail, c'est épuisant — reprendre quand tu peux, c'est très bien.",
        ),

        // --- Remplir l'application ---------------------------------------

        CoachRule.GAPS to listOf(
            "Il manque {n} jours récents. Si tu t'en souviens, une couleur suffit.",
            "{n} journées non notées ces derniers jours. Aucune obligation, le calendrier supporte les trous.",
            "{n} jours sans couleur récemment. Tu veux en remplir un vite fait ?",
            "Quelques trous ({n} jours). Remplis-les seulement si ça te dit.",
            "{n} jours vides dans la semaine. Ce n'est pas un devoir à rendre.",
            "Il reste {n} journées sans couleur. Une seule suffirait déjà à boucher un trou.",
        ),

        CoachRule.EMPTY_JOURNAL to listOf(
            "Rien d'écrit dans le journal depuis {n} jours. Une phrase suffit, ce n'est pas un roman.",
            "{n} jours sans une ligne. Les relire plus tard, ça vaut vraiment le coup.",
            "Le journal est vide depuis {n} jours. Même « journée bof » c'est déjà une trace.",
            "Ça fait {n} jours sans rien écrire. Tu veux poser deux mots sur aujourd'hui ?",
            "{n} jours sans journal. Écris comme ça vient, personne ne le lira jamais à part toi.",
            "Le journal attend depuis {n} jours. Trois mots, c'est déjà un souvenir sauvé.",
        ),

        CoachRule.EMPTY_PARTS to listOf(
            "Tu as mis une couleur sans remplir les moments. Les moments rendent l'onglet Bilan bien plus parlant.",
            "Matin, après-midi, soir, nuit : les remplir permet de voir à quel moment ça bascule chez toi.",
            "Si tu veux, détaille les quatre moments : la couleur du jour se recalculera toute seule.",
            "Couleur posée à la main. Les moments en dessous, c'est là que se cachent les vraies infos.",
            "Tu peux découper la journée en quatre moments. Souvent, tout ne s'est pas passé pareil du matin au soir.",
            "Les quatre moments sont vides. C'est facultatif, mais c'est ce qui rend le Bilan intéressant.",
        ),

        // --- Petites choses -----------------------------------------------

        CoachRule.BIRTHDAY to listOf(
            "Joyeux anniversaire {prenom}. Une journée à toi.",
            "C'est ton anniversaire aujourd'hui. Prends soin de toi un peu plus que d'habitude.",
            "Bon anniversaire {prenom}. Quoi qu'il y ait eu cette année, tu es encore là.",
            "Anniversaire aujourd'hui. Tu as le droit de te faire plaisir sans te justifier.",
            "Bon anniversaire. Que la journée soit verte ou pas, elle est à toi.",
            "C'est ton jour, {prenom}. Fais-en ce que tu veux, même rien.",
        ),

        CoachRule.NEW_MONTH to listOf(
            "Nouveau mois : {mois}. Page blanche sur le calendrier.",
            "{mois} commence. Rien à rattraper du mois d'avant.",
            "On est au début de {mois}. Le mois précédent est rangé dans le Bilan.",
            "Premier jour de {mois}.",
            "{mois} démarre aujourd'hui. Aucun objectif imposé.",
            "Nouveau mois qui s'ouvre : {mois}. On repart de zéro sur la grille.",
        ),

        CoachRule.PHOTO_ADDED to listOf(
            "Tu as gardé une image de cette journée. Ton toi de dans un an te remerciera.",
            "Photo ajoutée. C'est le genre de truc qu'on est content de retrouver plus tard.",
            "Un souvenir de plus dans l'appli, et il reste sur ton téléphone, nulle part ailleurs.",
            "Belle idée d'avoir mis une photo sur cette journée.",
            "Une image posée sur la journée. Elle reste dans l'espace privé de l'application.",
            "Média ajouté. Les journées avec une photo sont celles qu'on relit le plus.",
        ),

        CoachRule.WEIGHT_TRACKED to listOf(
            "Tu notes ton poids régulièrement ce mois-ci. La courbe du Bilan commence à vouloir dire quelque chose.",
            "Suivi du poids bien tenu ce mois-ci. Regarde la tendance, pas le chiffre du jour.",
            "Plusieurs pesées notées ce mois-ci. C'est la régularité qui rend la courbe lisible.",
            "Tu tiens le suivi du poids ce mois-ci. Le chiffre bouge d'un jour à l'autre, c'est normal.",
            "Pesées régulières ce mois-ci. Bien joué pour la constance.",
        ),

        CoachRule.WEEKEND to listOf(
            "Le week-end arrive. Rien d'obligatoire dedans.",
            "Fin de semaine. Si tu peux te garder un moment tranquille, c'est le bon timing.",
            "Bientôt le week-end. Prévois-toi un truc simple qui te fait plaisir.",
            "Le week-end est là. Le repos compte comme une activité.",
            "Week-end en approche. Un truc à toi, même petit, ça change la couleur d'un samedi.",
            "Fin de semaine. Tu n'as rien à rattraper ces deux jours-là.",
        ),

        CoachRule.HELLO to listOf(
            "Rien de spécial à signaler aujourd'hui. Je suis là quand même.",
            "Salut {prenom}. Journée normale, et c'est très bien comme ça.",
            "Pas de remarque particulière aujourd'hui. Passe une bonne journée.",
            "Tout est à jour. Tu peux fermer l'appli tranquille.",
            "Rien à signaler. Les journées sans histoire comptent aussi.",
        ),
    )

    /** Nombre total de phrases ecrites, pratique pour le test de catalogue. */
    val phraseCount: Int get() = messages.values.sumOf { it.size }

    fun variantsFor(rule: CoachRule): List<String> = messages[rule].orEmpty()

    /**
     * Fabrique le message d'une situation.
     *
     * @param variant compteur qui tourne : chaque affichage prend la phrase
     *   suivante, donc deux fois de suite la meme situation ne donne pas deux
     *   fois le meme texte.
     */
    fun render(candidate: NudgeCandidate, variant: Int, firstName: String): Nudge? {
        val variants = variantsFor(candidate.rule)
        if (variants.isEmpty()) return null
        val index = ((variant % variants.size) + variants.size) % variants.size
        val values = candidate.values + ("prenom" to firstName.trim().ifEmpty { "toi" })
        return Nudge(rule = candidate.rule, text = fill(variants[index], values))
    }

    /** Remplace les `{cles}` par leur valeur, et laisse le texte propre. */
    private fun fill(template: String, values: Map<String, String>): String {
        var text = template
        values.forEach { (key, value) -> text = text.replace("{$key}", value) }
        return text.replace(Regex("\\s+"), " ").trim()
    }
}
