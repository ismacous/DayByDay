package com.ismael.daybyday.coach

/**
 * Le catalogue de phrases. Chaque situation en propose plusieurs, et la
 * variante tourne a chaque fois : c'est ce qui evite l'impression de parler a
 * un repondeur.
 *
 * **La regle qui prime sur toutes les autres : une phrase doit se comprendre
 * seule.** Elle apparait dans une bulle sans titre, parfois dans une
 * notification, sans rien autour qui dise de quoi on parle. « Les cinq, 6
 * jours d'affilee » ne veut rien dire ; « Tes cinq prieres, six jours de
 * suite » oui. C'est pour ca que chaque situation declare ses [CoachRule.subjects]
 * et qu'un test verifie que **toutes** ses phrases les contiennent.
 *
 * **La deuxieme regle : une felicitation doit feliciter.** « Tes cinq prieres,
 * six jours de suite. C'est une regularite qui se remarque. » est un releve,
 * pas un compliment — Ismael l'a dit mieux que moi : ca fait « un resume en
 * attendant notre mort ». Un ami ne fait pas l'inventaire de ce que tu as
 * fait, il te dit que c'est bien et il te pousse. Chaque phrase de ton
 * [NudgeTone.PROUD] ou [NudgeTone.CHEER] porte donc un mot d'elan
 * ([LIFT_WORDS]) et un test le verifie : le garde-fou, pas la relecture.
 *
 * Les autres regles d'ecriture, a garder si tu en ajoutes :
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
 * - l'energie monte avec le ton, elle ne s'invente pas partout : on encourage
 *   fort quand ca va bien, on reste calme et present quand c'est noir. Crier
 *   « bravo » un jour noir serait la pire des fausses notes.
 *
 * Les valeurs entre accolades sont remplacees au moment de l'affichage :
 * `{n}`, `{quoi}`, `{constat}`, `{prenom}`, `{duree}`, `{mois}`, `{montant}`,
 * `{moment}`, `{ecart}`.
 */
object CoachMessages {

    /**
     * Les mots d'elan : de la vie, de l'energie, de la volonte.
     *
     * Chaque phrase de felicitation doit en contenir au moins un. Ce n'est pas
     * une liste de synonymes a rallonge — vingt mots suffisent, et les revoir
     * de temps en temps vaut mieux que de laisser passer une phrase plate.
     */
    val LIFT_WORDS = listOf(
        "bravo", "chapeau", "respect", "fier", "force", "continue", "garde",
        "savoure", "kiffe", "énorme", "solide", "réussi", "gagné", "mérit",
        "profite", "tiens bon", "allez", "beau", "belle", "paye", "assure",
    )

    private val messages: Map<CoachRule, List<String>> = mapOf(

        // --- La journee bouclee ------------------------------------------
        // Une vingtaine de phrases : c'est la seule qui peut revenir tous les
        // jours, donc c'est elle qui s'userait le plus vite.

        CoachRule.DAY_COMPLETE to listOf(
            "Toutes tes cartes sont vérifiées, la journée est bouclée. Bravo, tu es allé au bout !",
            "Journée complète, carte par carte. Chapeau, franchement !",
            "Tu as relu toute ta journée. Aller au bout comme ça, c'est une vraie force !",
            "Et voilà, toute la journée est vérifiée. Bravo, rien ne traîne derrière toi !",
            "Toutes les cartes sont cochées. Ta journée est rangée, savoure ça !",
            "Journée bouclée, chaque carte passée en revue. Continue comme ça, c'est du solide !",
            "Tout est vérifié pour aujourd'hui. Bravo, tu peux souffler l'esprit tranquille !",
            "Chaque carte de la journée y est passée. Respect, c'est du beau travail !",
            "La journée est complète. Même un jour ordinaire, tenir ça, c'est fort — bravo !",
            "Toutes tes cartes sont vérifiées. Continue comme ça, dans six mois tu verras la différence !",
            "Journée entièrement vérifiée. Tu construis quelque chose de solide, jour après jour !",
            "Tout est à jour dans ta journée. Bravo, c'est plié pour aujourd'hui !",
            "Tes cartes sont toutes cochées. Une journée de plus au propre, chapeau !",
            "Journée complète. Tenir ça un jour comme un autre, c'est une sacrée force !",
            "Toutes les cartes sont passées. Ta journée est en ordre, bravo !",
            "Tu as bouclé la journée entière. Ça ne se fait pas par hasard : chapeau !",
            "Chaque carte est vérifiée. Belle rigueur — ta journée est nickel !",
            "Journée terminée et relue en entier. Bravo, tu gères vraiment !",
            "Toutes tes cartes sont revues. Ce geste-là, personne ne le voit à part toi et moi : respect !",
            "La journée est complète. Rien à rattraper demain, savoure !",
            "Tout est coché, ta journée est finie pour de bon. Allez, bravo !",
        ),

        // --- Soutien quand c'est dur -------------------------------------
        // Ici, pas d'elan : quelqu'un qui crie « bravo » un jour noir n'a pas
        // compris ou il met les pieds. Ce qu'on apporte, c'est une presence.

        CoachRule.DAYS_VERY_DARK to listOf(
            "Ça fait {n} jours que tes journées sont très sombres. Tu n'as rien à prouver à personne, et tu n'as pas à tenir ça tout seul. Si tu veux parler à quelqu'un, même en pleine nuit, le 3114 répond gratuitement.",
            "{n} jours de suite dans le noir, c'est énorme à porter. Tenir jusqu'ici, c'est déjà quelque chose. Le 3114 est là si tu as besoin d'une voix maintenant, à n'importe quelle heure.",
            "Je vois {n} journées très noires d'affilée. Je ne peux pas t'enlever ça, mais tu n'es pas obligé de le traverser seul : le 3114, c'est gratuit, jour et nuit, et personne ne te juge.",
            "Ça dure depuis {n} jours et c'est lourd. Appeler quelqu'un n'est pas un aveu de faiblesse — le 3114 existe exactement pour ces moments-là.",
            "{n} jours comme ça, c'est beaucoup trop pour une seule personne. Si tu peux en parler à quelqu'un aujourd'hui, à ta moitié, à ta famille, ou au 3114, fais-le.",
        ),

        CoachRule.BLACK_STREAK to listOf(
            "{n} journées noires de suite. Je ne vais pas te dire que ça va aller, mais je le note avec toi : c'est dur en ce moment.",
            "Ça fait {n} jours que tes journées sont très lourdes. Tu n'as rien de plus à faire aujourd'hui que de la passer.",
            "{n} journées sombres à la suite. Si tu as pu ouvrir l'appli, c'est déjà que tu tiens quelque chose.",
            "{n} journées noires d'affilée. Ça arrive, et ça passe, même si là tout de suite ça ne se voit pas.",
            "{n} journées noires. Aujourd'hui, le minimum suffit : boire, manger un truc, respirer. Le reste peut attendre.",
            "C'est la {n}e journée noire de suite. Tu as le droit de mettre tout le reste en pause.",
        ),

        CoachRule.BLACK_DAY to listOf(
            "Tu as noté ta journée en noir. Merci de l'avoir quand même posée — c'est déjà beaucoup un jour comme ça.",
            "Journée très noire aujourd'hui. Pas de conseil, pas de « il faut que » : juste, je l'ai vue.",
            "Une journée noire, ça se traverse, ça ne se répare pas dans la soirée. Pose-toi.",
            "C'était noir aujourd'hui. Ça ne dit rien de qui tu es, juste de comment était cette journée-là.",
            "Journée noire. Demain sera un autre carré sur le calendrier.",
            "Ta journée est en noir. Si tu as quelqu'un à qui envoyer un message ce soir, même court, c'est peut-être le moment.",
        ),

        CoachRule.ANXIETY to listOf(
            "Tu as coché une crise d'angoisse aujourd'hui. Essaie de ralentir ta respiration : souffler plus longtemps qu'inspirer aide parfois à faire redescendre.",
            "Une crise d'angoisse, c'est noté. Ça passe toujours, même quand pendant l'épisode on est persuadé du contraire.",
            "L'angoisse d'aujourd'hui est enregistrée. Rien à analyser ce soir, tu peux juste te mettre au calme.",
            "Crise d'angoisse aujourd'hui. Tu n'as pas à comprendre pourquoi maintenant, ça peut attendre un jour plus clair.",
            "Tu as marqué ton angoisse, et ça sert : dans quelques semaines, tu verras dans quels moments elle revient.",
        ),

        CoachRule.CRIED to listOf(
            "Tu as pleuré aujourd'hui. C'est le corps qui évacue, pas un échec.",
            "Tu as pleuré aujourd'hui. Bois un verre d'eau, c'est bête mais ça fait du bien après.",
            "Tu as noté que tu as pleuré plutôt que de faire comme si de rien n'était. C'est bien de l'avoir dit.",
            "Des larmes aujourd'hui. Sois aussi doux avec toi que tu le serais avec un pote dans le même état.",
            "Tu as pleuré aujourd'hui. Ça arrive aux gens solides aussi.",
        ),

        CoachRule.RED_DAY to listOf(
            "Journée difficile aujourd'hui. Elle est notée, tu peux la poser maintenant.",
            "Ta journée est en rouge. C'était dur, et tu n'as pas besoin d'en faire plus.",
            "Journée difficile notée. Qu'elle soit rouge et pas noire, c'est déjà une info.",
            "Dure, cette journée. Repose-toi si tu peux, tu as fait le nécessaire en la notant.",
            "Journée compliquée aujourd'hui. Tu n'as pas à la rattraper ce soir.",
        ),

        CoachRule.HARD_WEEK to listOf(
            "Ta semaine est basse dans l'ensemble. Ce n'est pas toi qui fais mal les choses, c'est une période difficile.",
            "Semaine lourde. Si tu as un rendez-vous médical possible dans les jours qui viennent, ça peut valoir le coup d'en parler.",
            "Cette semaine est plus sombre que d'habitude. Baisse tes attentes pour les jours qui restent, ça suffira.",
            "La semaine a été dure. Regarde ton calendrier sur plusieurs mois plutôt que sur ces sept jours : ça bouge tout le temps.",
            "Ta semaine a été rude. Ce que tu arrives quand même à faire pendant ces semaines-là compte double.",
        ),

        // --- Sante et soin de soi ----------------------------------------
        // Des propositions, jamais des ordres — mais proposees comme un pote
        // le ferait, avec un peu d'entrain.

        CoachRule.MEDS_MISSING to listOf(
            "Aucune prise de traitement cochée depuis {n} jours. Tu les as pris ?",
            "Ton traitement n'est plus coché depuis {n} jours — peut-être juste un oubli de case ?",
            "{n} jours sans prise de traitement cochée. Si tu l'as pris, coche-le ; sinon, c'est le moment idéal.",
            "Petit point traitement : rien de coché depuis {n} jours.",
            "Ça fait {n} jours que ton traitement n'est plus coché. Pas de sermon, juste un coup de coude amical.",
        ),

        CoachRule.SHOWER_MISSING to listOf(
            "Ça fait {n} jours sans douche cochée. Tu te sens d'attaque pour en prendre une ?",
            "Allez, une douche ? Même rapide, on en ressort toujours un peu mieux.",
            "Pas de douche cochée depuis {n} jours. Si c'est trop, l'eau froide sur le visage, c'est déjà une victoire.",
            "Une idée sans obligation : une bonne douche. C'est souvent le truc qui relance une journée.",
            "Tu penses pouvoir prendre une douche aujourd'hui ? Si c'est non, c'est non, on en reparlera.",
            "{n} jours sans douche cochée. Ça arrive quand l'énergie n'est pas là. Aujourd'hui, c'est jouable ?",
        ),

        CoachRule.BRUSHING_LOW to listOf(
            "Tu n'as pas coché de brossage de dents depuis {n} jours. Un seul, ce soir, et c'est gagné.",
            "{n} jours sans te brosser les dents d'après tes cartes. Deux minutes, et c'est plié.",
            "Tes dents attendent depuis {n} jours. Même un brossage sur trois, c'est mieux que zéro.",
            "Rien de coché côté brossage de dents depuis {n} jours. Sans reproche : juste au cas où ça se serait perdu dans les journées.",
            "{n} jours sans brossage de dents coché. Si tu passes devant le lavabo tout à l'heure, saute dessus.",
        ),

        CoachRule.SHORT_NIGHTS to listOf(
            "Tes dernières nuits tournent autour de {duree} de sommeil. Beaucoup de choses paraissent pires quand on dort peu.",
            "Tu dors environ {duree} par nuit ces derniers jours. Ne juge pas trop tes journées, elles partent avec un handicap.",
            "Des nuits courtes en ce moment : {duree} en moyenne. Si tu peux lever le pied aujourd'hui, fais-le.",
            "Tu dors autour de {duree} par nuit ces temps-ci. Une sieste courte en début d'après-midi ne casse pas la nuit suivante.",
            "{duree} de sommeil par nuit, c'est peu. C'est peut-être le premier levier à tirer avant tout le reste.",
        ),

        CoachRule.LATE_NIGHTS to listOf(
            "Tu t'es couché après minuit {n} nuits d'affilée. Pas de leçon, juste un constat.",
            "Ça fait {n} nuits que tu te couches très tard. Tu veux tenter de couper les écrans plus tôt ce soir ?",
            "{n} nuits tardives à la suite. Les journées d'après le sentent souvent.",
            "Couché très tard {n} nuits de suite. Si demain est libre, ce n'est pas grave du tout.",
            "{n} soirs de suite couché passé minuit. Ton corps s'en souvient même si toi non.",
        ),

        CoachRule.WATER_LOW to listOf(
            "Moins de quatre verres d'eau depuis {n} jours. Un verre maintenant, tant que tu y penses ?",
            "Tu bois peu d'eau depuis {n} jours. La fatigue et le mal de tête viennent souvent de là avant de venir d'ailleurs.",
            "Peu d'eau ces {n} derniers jours. Rien d'obligatoire, c'est juste le plus facile à corriger.",
            "Ça fait {n} jours que ton compteur d'eau reste bas. Un verre, et c'est déjà mieux.",
            "Tu bois peu d'eau en ce moment. Pose un verre à côté de toi, ça marche mieux que d'y penser.",
        ),

        // --- Ce qui remonte ----------------------------------------------
        // Le coeur du sujet : quand ca va, on le dit fort.

        CoachRule.FIRST_GREEN to listOf(
            "Première journée verte depuis {n} jours ! Bravo, celle-là je la note en gras.",
            "{n} jours que j'attendais ça : une journée verte. Savoure-la, tu l'as méritée !",
            "Enfin une journée verte, après {n} jours. Chapeau d'avoir tenu jusqu'ici !",
            "Une journée verte après {n} jours de traversée. C'est la preuve que ça bouge encore — continue !",
            "{n} jours, et te voilà avec une journée verte. Garde bien en tête ce qu'il y avait dedans !",
        ),

        CoachRule.BACK_AFTER_BREAK to listOf(
            "Content de te revoir ! {n} jours sans rien noter, et alors : on reprend où tu veux.",
            "Te revoilà. Pas besoin de rattraper les {n} jours manquants, on repart d'aujourd'hui.",
            "{n} jours d'absence, aucune importance. Ton calendrier n'est pas un devoir à rendre.",
            "Ça faisait {n} jours. Si tu te souviens des couleurs de ces jours-là, remplis-les ; sinon on repart d'ici.",
            "De retour après {n} jours, et c'est très bien comme ça. Les trous font partie de l'histoire eux aussi.",
        ),

        CoachRule.GREEN_STREAK to listOf(
            "{n} journées vertes d'affilée ! Bravo, regarde bien ce que tu fais de différent en ce moment.",
            "{n} bonnes journées à la suite, et ce n'est pas un hasard. Continue exactement pareil !",
            "{n} journées vertes de suite. Chapeau — c'est le genre de chose qu'on ne remarque pas tout seul.",
            "{n} bonnes journées d'affilée. Savoure, tu l'as gagné !",
            "{n} journées vertes d'affilée. Franchement bravo, profite sans culpabiliser.",
        ),

        CoachRule.REBOUND to listOf(
            "Ta journée remonte franchement par rapport à hier. Bravo, ça fait plaisir à voir !",
            "Hier était dur, aujourd'hui beaucoup moins. Chapeau d'avoir traversé ça !",
            "Belle remontée par rapport à hier. Garde ça en tête le prochain jour sombre !",
            "Grosse différence avec hier, et dans le bon sens. Bravo !",
            "Tu es bien au-dessus d'hier. Ça, c'est ta force : ça repart — continue !",
        ),

        CoachRule.NOTING_STREAK to listOf(
            "{n} jours notés d'affilée ! Bravo, beaucoup ne tiennent pas trois jours.",
            "{n} jours de suite sans en louper un. Chapeau, ça commence à faire de vraies données !",
            "{n} jours notés d'affilée. Continue, c'est ça qui rend ton bilan vraiment utile !",
            "{n} jours à la suite dans ton calendrier. Rien que pour la régularité : respect.",
            "{n} jours notés sans interruption. Tu construis un truc solide, même quand tu n'en as pas l'impression !",
        ),

        CoachRule.PRAYER_STREAK to listOf(
            "{n} jours d'affilée avec tes cinq prières ! Bravo, c'est une sacrée régularité.",
            "Tes cinq prières, {n} jours de suite. Chapeau — tiens bon comme ça !",
            "{n} jours que tu tiens tes cinq prières. C'est une vraie force, continue !",
            "Cinq prières sur cinq, {n} jours de suite. Franchement, respect.",
            "{n} jours complets côté prières. Bravo, garde ce rythme !",
        ),

        CoachRule.GREEN_DAY to listOf(
            "Bonne journée aujourd'hui ! Qu'est-ce qui l'a rendue comme ça ? Deux mots dans le journal et tu le sauras dans six mois.",
            "Une journée verte aujourd'hui. Prends-la et savoure-la !",
            "Bonne journée ! Ce sont celles-là qu'on oublie le plus vite : écris-en un bout.",
            "Ta journée est verte, et ça compte autant que les mauvaises. Profite !",
            "Belle journée aujourd'hui. Rien d'autre à ajouter, savoure.",
        ),

        CoachRule.JOB_EFFORT to listOf(
            "{n} candidatures envoyées cette semaine. Bravo, c'est du vrai travail !",
            "{n} candidatures en sept jours. Le résultat ne dépend pas de toi, l'envoi si — et tu l'as fait, chapeau !",
            "{n} candidatures cette semaine. Garde ce chiffre en tête les jours où tu as l'impression de ne rien faire !",
            "Belle semaine de recherche : {n} candidatures envoyées. Continue, ça finit par payer !",
            "{n} candidatures envoyées cette semaine. Chacune t'a coûté quelque chose : respect.",
        ),

        CoachRule.BETTER_WEEK to listOf(
            "Ta semaine passe au-dessus de la précédente. Bravo, ça monte !",
            "Cette semaine est meilleure que la dernière. Continue comme ça !",
            "Semaine au-dessus de la précédente. Ça ne se sent pas toujours sur le moment, et pourtant c'est réel — bravo !",
            "Ta semaine est en hausse. Garde exactement ce rythme !",
            "Meilleure semaine que la précédente. Chapeau, c'est toi qui l'as construite.",
        ),

        CoachRule.PRAYERS_ALL to listOf(
            "Tes cinq prières sont faites aujourd'hui. Bien joué, savoure ça !",
            "Cinq prières sur cinq aujourd'hui. Belle journée de tenue !",
            "Journée complète côté prières. Bravo à toi !",
            "Tes cinq prières y sont pour aujourd'hui. Chapeau !",
        ),

        CoachRule.BEST_MONTH to listOf(
            "Ton meilleur mois de cette dernière année, c'est {mois}. Bravo !",
            "Aucun mois des douze derniers n'est monté aussi haut que {mois}. Chapeau !",
            "Le mois de {mois} passe devant tous les autres. Va voir ton bilan, ça se voit bien — bravo !",
            "Meilleure moyenne des douze derniers mois : {mois}. Franchement, bravo.",
            "Le mois de {mois} tient le haut du classement. Respect, garde cette dynamique !",
        ),

        // --- Ce qui va avec les bonnes journees --------------------------

        CoachRule.FACTOR_SUGGESTION to listOf(
            "J'ai remarqué que tes journées sont souvent meilleures {constat}. Tu penses pouvoir {quoi} aujourd'hui ?",
            "Sur l'ensemble de tes journées, ça va plutôt mieux {constat}. Ça te dit de {quoi} ?",
            "Petite idée du jour : {quoi}. Tes bonnes journées vont souvent avec ça.",
            "Tes journées notées montrent que ça se passe mieux {constat}. Aujourd'hui, {quoi}, c'est jouable ?",
            "Si tu cherches quoi faire : {quoi}. Dans tes journées, ça revient souvent quand ça va bien.",
            "En moyenne, {constat}, tes journées sont plus hautes. Tu veux tenter de {quoi} ?",
        ),

        CoachRule.FACTOR_TODAY to listOf(
            "Tu l'as fait aujourd'hui, et {constat} tes journées se passent souvent mieux. Bien vu !",
            "C'est coché aujourd'hui — d'après tes journées, {constat} c'est plutôt bon signe. Continue !",
            "Joli : {constat}, la moyenne de tes journées est plus haute que d'habitude.",
            "Ça revient souvent dans tes bonnes journées, et c'est coché aujourd'hui. Bien joué !",
            "D'habitude, {constat}, tes journées se passent rarement mal. Et c'est coché aujourd'hui !",
        ),

        CoachRule.FACTOR_HEAVY to listOf(
            "À noter, sans jugement : {constat}, tes journées sont en moyenne un peu plus basses.",
            "Juste une observation : {constat}, la moyenne de tes journées descend un peu. À toi de voir ce que tu en fais.",
            "En moyenne, {constat}, tes journées sont un peu plus dures. Ça ne veut pas dire que c'est la cause.",
            "Sur tes données, {constat} tes journées tirent un peu vers le bas. C'est un constat, pas un reproche.",
            "Pour info seulement : {constat}, tes journées sont un peu en dessous de ta moyenne.",
        ),

        // --- Habitudes du quotidien --------------------------------------

        CoachRule.ALONE_STREAK to listOf(
            "{n} jours sans personne de noté dans tes journées. Un message à quelqu'un, même trois mots, ça compte comme du contact.",
            "Ça fait {n} jours que tu n'as vu personne d'après tes cartes. Tu as quelqu'un à qui écrire aujourd'hui ?",
            "{n} jours en solo d'après tes journées. Pas besoin de voir quelqu'un en vrai : un appel de deux minutes, ça suffit parfois.",
            "Personne de noté depuis {n} jours. Si l'idée te pèse, laisse tomber ; sinon, pense à ta moitié ou à un pote.",
            "{n} jours seul d'après tes cartes. C'est le genre de truc qui s'installe sans qu'on le décide.",
        ),

        CoachRule.STAYED_IN to listOf(
            "{n} jours sans sortir. Même cinq minutes devant la porte, ça compte comme être sorti.",
            "Ça fait {n} jours que tu restes à la maison. Tu te sens de faire un tour dehors, juste au bout de la rue ?",
            "{n} jours sans sortir de chez toi. L'air du dehors, c'est souvent moins pire que ce qu'on imagine avant d'y aller.",
            "Pas sorti depuis {n} jours. Une course à faire, une poubelle à descendre ? Ça fait le job.",
            "{n} jours sans mettre le nez dehors. Si c'est non aujourd'hui, c'est non.",
        ),

        CoachRule.NO_MOVEMENT to listOf(
            "{n} jours sans rien bouger. Pas besoin de séance : dix minutes de marche, c'est déjà « un peu bougé ».",
            "Ça fait {n} jours que tu n'as pas bougé d'après tes cartes. Tu te sens de bouger un peu aujourd'hui ?",
            "{n} jours sans bouger. Un corps qui reste immobile, ça joue sur le moral aussi.",
            "Tu n'as rien bougé depuis {n} jours. Même s'étirer debout deux minutes compte.",
            "{n} jours sans bouger. Et si tu mettais juste de la musique en rangeant un truc ?",
        ),

        CoachRule.BAD_SLEEP to listOf(
            "Tu as mal dormi plusieurs fois ces derniers jours. Beaucoup de choses paraissent pires après une mauvaise nuit.",
            "Ton sommeil n'est pas bon en ce moment. Si tu peux lever le pied aujourd'hui, fais-le.",
            "Plusieurs nuits difficiles ces jours-ci. Ne juge pas trop tes journées, elles partent avec un handicap.",
            "Ça fait plusieurs nuits que tu dors mal. Une sieste courte en début d'après-midi ne casse pas la nuit suivante.",
            "Tu dors mal depuis quelques jours. C'est peut-être le premier truc à regarder avant le reste.",
        ),

        CoachRule.HIGH_SCREEN to listOf(
            "{duree} sur ton téléphone hier, bien au-dessus de ton habitude. Rien d'obligatoire, c'est juste pour info.",
            "Grosse journée d'écran hier : {duree}. Ça arrive surtout les jours où on n'a pas envie d'être ailleurs.",
            "{duree} d'écran hier, c'est plus que ta moyenne. Peut-être une pause de dix minutes sans le téléphone ?",
            "Ton téléphone a pris {duree} hier. Aucun jugement, tu fais ce que tu veux de ce chiffre.",
            "{duree} d'écran hier. Parfois c'est exactement ce dont on a besoin, parfois non — toi seul sais.",
            "Beaucoup de téléphone en ce moment : {duree} hier. Le poser dans une autre pièce dix minutes, ça marche bien.",
        ),

        CoachRule.LOW_STEPS to listOf(
            "Très peu de pas aujourd'hui. Un tour du pâté de maisons et le compteur bouge déjà.",
            "Ton compteur de pas est bas pour l'heure qu'il est. Ça te dit de le faire monter un peu ?",
            "Journée sans beaucoup de pas. Ce n'est pas grave, c'est juste un chiffre.",
            "Peu de pas pour l'instant aujourd'hui. Même dans l'appart, ça compte.",
            "Compteur de pas bas aujourd'hui. Aucun objectif à tenir, c'est juste une info.",
        ),

        // --- Argent ---------------------------------------------------------

        CoachRule.MONEY_INCOME to listOf(
            "{montant} de rentré ces derniers jours ! Si tu peux en mettre une part de côté tout de suite, c'est le meilleur moment.",
            "Tu as noté {montant} de rentrée. Décider maintenant ce que tu en gardes, c'est bien plus facile que dans deux semaines.",
            "{montant} sont rentrés récemment. Rien d'obligatoire — juste, c'est maintenant que le choix est le plus simple.",
            "Une rentrée de {montant} est notée. Tu veux en bloquer un bout avant qu'il se dilue ?",
            "{montant} de rentré, et c'est noté. Ça se verra dans ton bilan du mois !",
        ),

        CoachRule.MONEY_SAVING to listOf(
            "Ton mois est dans le vert de {montant} pour l'instant. C'est exactement le genre de mois où mettre de côté ne fait pas mal.",
            "Il te reste {montant} de plus que ce que tu as dépensé ce mois-ci. Une partie pourrait dormir ailleurs.",
            "{montant} d'avance sur ton mois. Si tu en gardes ne serait-ce qu'un dixième, tu ne le sentiras pas passer.",
            "Ton mois est positif de {montant}. Rien ne t'oblige à y toucher.",
        ),

        CoachRule.MONEY_HEAVY to listOf(
            "Tes dépenses du mois sont {montant} au-dessus du mois dernier à la même date. C'est un chiffre, pas un reproche.",
            "Ce mois-ci tu as dépensé {montant} de plus que le mois dernier à ce stade. Il y a peut-être une bonne raison.",
            "{montant} de dépenses en plus par rapport au mois dernier à la même date. Juste pour que tu le saches avant la fin du mois.",
            "Mois plus lourd que le précédent : {montant} de dépenses en plus à date.",
        ),

        CoachRule.MONEY_QUIET to listOf(
            "Aucun mouvement d'argent noté depuis {n} jours. Si des choses sont passées, c'est le moment de les rattraper.",
            "Ça fait {n} jours sans rien noter dans ton argent. Le suivi ne vaut que s'il est à jour.",
            "{n} jours sans mouvement enregistré. Deux minutes maintenant t'éviteront de chercher plus tard.",
            "Rien de noté côté argent depuis {n} jours. Rien d'urgent, mais les trous se rattrapent mal.",
        ),

        CoachRule.BIG_SPENDING to listOf(
            "Grosse dépense aujourd'hui. Le fait de l'écrire, c'est déjà la moitié du travail.",
            "Une grosse dépense est notée aujourd'hui. Tu la retrouveras dans ton onglet Argent, pas besoin d'y penser en plus.",
            "Dépense importante enregistrée. Ça arrive, et c'est mieux dans l'appli que dans un coin de ta tête.",
            "Grosse dépense notée. Un budget, ça se regarde sur le mois, pas sur la journée.",
        ),

        CoachRule.CALM_MONEY to listOf(
            "Tes dépenses du mois sont plus calmes que celles du mois dernier à la même date. Bien joué !",
            "Tu dépenses moins que le mois dernier pour l'instant. Ça se voit dans ton onglet Argent !",
            "Le mois est plus tranquille que le précédent côté dépenses. Continue comme ça !",
            "Moins de dépenses que le mois dernier à ce stade du mois. Joli coup !",
        ),

        CoachRule.JOB_PAUSE to listOf(
            "Pas de candidature envoyée depuis {n} jours. Ce n'est pas une course, et une pause est parfois exactement ce qu'il faut.",
            "{n} jours sans candidature. Si l'énergie revient aujourd'hui, une seule suffirait.",
            "Ça fait {n} jours sans candidature envoyée. Sans pression : le compteur est là pour toi, pas contre toi.",
            "{n} jours sans candidature. Chercher du travail, c'est épuisant — reprendre quand tu peux, c'est très bien.",
        ),

        // --- Bilan -----------------------------------------------------------

        CoachRule.STATS_TOP_FACTOR to listOf(
            "Le lien le plus net de tout ton suivi : {constat}, tes journées gagnent {ecart} point en moyenne.",
            "Sur toutes tes journées notées, c'est {constat} que l'écart est le plus grand : {ecart} point de mieux.",
            "Ce qui ressort le plus de tes journées : {constat}, elles montent de {ecart} point. Ça ne prouve pas une cause, mais ça se remarque.",
            "En moyenne, {constat}, tes journées gagnent {ecart} point. C'est le plus gros écart de tout ton suivi.",
        ),

        CoachRule.STATS_HARD_PART to listOf(
            "Sur tes journées notées, c'est le {moment} qui revient le plus souvent en bas. Savoir quel moment est le plus dur aide à s'organiser autour.",
            "Le {moment} est ton moment le plus difficile en moyenne. Ce n'est pas une fatalité, c'est une info à utiliser.",
            "Ton {moment} est régulièrement plus bas que le reste de la journée. De quoi prévoir quelque chose de doux à ce moment-là.",
            "D'après tes quatre moments, c'est le {moment} qui pèse le plus. Ça se voit sur l'ensemble, pas sur un jour.",
        ),

        CoachRule.STATS_TREND_UP to listOf(
            "Tes trente derniers jours sont au-dessus des trente précédents de {ecart} point. Bravo, la pente est bonne !",
            "Sur les trente derniers jours, ta moyenne a monté de {ecart} point. Continue, ça paye !",
            "Tes trente derniers jours vont mieux que les trente d'avant : {ecart} point de plus. Chapeau, ça ne se sent pas au jour le jour !",
            "La tendance de tes trente derniers jours monte de {ecart} point. C'est lent, et c'est comme ça que ça marche — garde le cap !",
        ),

        CoachRule.STATS_TREND_DOWN to listOf(
            "Tes trente derniers jours sont en dessous des trente précédents de {ecart} point. Ce n'est pas un échec, c'est une période.",
            "Sur les trente derniers jours, ta moyenne a baissé de {ecart} point. Ça monte et ça descend, tu le sais mieux que moi.",
            "Tes trente derniers jours sont plus bas que le mois d'avant de {ecart} point. Je te le dis parce que le voir aide parfois à comprendre pourquoi on fatigue.",
            "La tendance de tes trente derniers jours descend de {ecart} point. Sois indulgent avec toi ces temps-ci.",
        ),

        CoachRule.STATS_YOUNG to listOf(
            "Tu as {n} journées notées pour l'instant. À partir d'une trentaine, les comparaisons du bilan commencent vraiment à vouloir dire quelque chose.",
            "Encore {n} journées notées seulement. Le bilan devient intéressant vers trente ou quarante, laisse-lui le temps.",
            "{n} journées dans ton suivi. C'est un bon début — les liens entre ce que tu fais et tes journées ont juste besoin de plus de matière.",
        ),

        // --- Remplir l'application ---------------------------------------

        CoachRule.GAPS to listOf(
            "Il manque {n} jours récents dans ton calendrier. Si tu t'en souviens, une couleur suffit.",
            "{n} journées ne sont pas notées ces derniers jours. Aucune obligation, le calendrier supporte les trous.",
            "{n} jours sans couleur récemment. Tu veux en remplir un vite fait ?",
            "Quelques trous dans ton calendrier : {n} jours. Remplis-les seulement si ça te dit.",
            "Il reste {n} jours sans couleur. Un seul suffirait déjà à boucher un trou.",
        ),

        CoachRule.EMPTY_JOURNAL to listOf(
            "Rien d'écrit dans ton journal depuis {n} jours. Une phrase suffit, ce n'est pas un roman.",
            "{n} jours sans une ligne dans le journal. Les relire plus tard, ça vaut vraiment le coup.",
            "Ton journal est vide depuis {n} jours. Même « journée bof » c'est déjà une trace.",
            "Ça fait {n} jours que tu n'as rien écrit. Tu veux poser deux mots sur aujourd'hui ?",
            "{n} jours sans journal. Écris comme ça vient, personne ne le lira jamais à part toi.",
        ),

        CoachRule.EMPTY_PARTS to listOf(
            "Tu as mis une couleur sans remplir les quatre moments. Ce sont eux qui rendent ton bilan parlant.",
            "Matin, après-midi, soir, nuit : remplir ces quatre moments permet de voir à quel moment ça bascule chez toi.",
            "Si tu veux, détaille les quatre moments de ta journée : la couleur du jour se recalculera toute seule.",
            "Les quatre moments de ta journée sont vides. C'est facultatif, mais c'est là que se cachent les vraies infos.",
            "Tu peux découper ta journée en quatre moments. Souvent, tout ne s'est pas passé pareil du matin au soir.",
        ),

        // --- Petites choses -----------------------------------------------

        CoachRule.BIRTHDAY to listOf(
            "Joyeux anniversaire {prenom} ! Bravo pour cette année de plus, elle t'a coûté et tu es là.",
            "Bon anniversaire {prenom} ! Quoi qu'il y ait eu cette année, tu es encore debout : respect.",
            "C'est ton anniversaire aujourd'hui ! Savoure la journée, tu l'as bien méritée.",
            "Joyeux anniversaire ! Aujourd'hui tu ne te justifies devant personne, tu profites.",
            "Bon anniversaire {prenom} ! Verte ou pas, cette journée est à toi : profite bien.",
        ),

        CoachRule.NEW_MONTH to listOf(
            "Nouveau mois : {mois} commence. Page blanche sur ton calendrier.",
            "Le mois de {mois} commence aujourd'hui. Rien à rattraper du mois d'avant.",
            "On est au premier jour de {mois}. Le mois précédent est rangé dans ton bilan.",
            "Le mois de {mois} démarre aujourd'hui. Aucun objectif imposé.",
            "Nouveau mois qui s'ouvre : {mois}. On repart de zéro sur la grille.",
        ),

        CoachRule.PHOTO_ADDED to listOf(
            "Tu as gardé une photo de cette journée. Ton toi de dans un an te remerciera !",
            "Photo ajoutée à ta journée. C'est le genre de truc qu'on est bien content de retrouver plus tard.",
            "Une image de plus dans l'appli, et elle reste sur ton téléphone, nulle part ailleurs.",
            "Belle idée d'avoir mis une photo sur cette journée !",
            "Photo ajoutée. Les journées avec une image sont celles qu'on relit le plus.",
        ),

        CoachRule.WEIGHT_TRACKED to listOf(
            "Tu notes ton poids régulièrement ce mois-ci. Ta courbe commence à vouloir dire quelque chose !",
            "Suivi du poids bien tenu ce mois-ci. Regarde la tendance, pas le chiffre du jour.",
            "Plusieurs pesées notées ce mois-ci. C'est cette régularité-là qui rend ta courbe de poids lisible.",
            "Tu tiens ton suivi de poids ce mois-ci. Le chiffre bouge d'un jour à l'autre, c'est normal.",
        ),

        CoachRule.WEEKEND to listOf(
            "Le week-end arrive. Rien d'obligatoire dedans.",
            "Fin de semaine ! Si tu peux te garder un moment tranquille, c'est le bon timing.",
            "Bientôt le week-end. Prévois-toi un truc simple qui te fait plaisir.",
            "Le week-end commence. Le repos compte comme une activité.",
            "Week-end en approche. Un truc à toi, même petit, ça change la couleur d'un samedi.",
        ),
    )

    /** Nombre total de phrases ecrites, pratique pour le test de catalogue. */
    val phraseCount: Int get() = messages.values.sumOf { it.size }

    fun variantsFor(rule: CoachRule): List<String> = messages[rule].orEmpty()

    /**
     * Fabrique le message d'une situation.
     *
     * @param variant compteur qui tourne : chaque apparition prend la phrase
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
