package com.ismael.daybyday.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismael.daybyday.data.DayCard
import com.ismael.daybyday.data.DayEntry
import com.ismael.daybyday.data.DoseTaken
import com.ismael.daybyday.data.DoseTime
import com.ismael.daybyday.data.Brushing
import com.ismael.daybyday.data.Prayer
import com.ismael.daybyday.data.Treatment
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/**
 * L'enveloppe d'une carte de l'ecran d'une journee.
 *
 * Ce qu'elle porte, et pourquoi :
 *
 * - **Un signe et une teinte** ([cardStyle]). Douze cartes blanches
 *   rigoureusement identiques obligent a lire chaque titre pour savoir ou l'on
 *   est. Une couleur et une icone se reconnaissent avant la lecture.
 * - **Un halo dans le coin** ([cardGlow]). Il est dessine, pas anime : douze
 *   halos qui derivent feraient douze animations pour un effet qu'on ne regarde
 *   pas. Il donne a la carte un volume et une lumiere, sans rien couter.
 * - **Une ligne de resume** sous le titre. Une carte repliee doit encore
 *   renseigner — « 9 h · bien dormi », « 3 sur 5 », « −14,90 € » — sinon la
 *   replier revient a l'effacer.
 *
 * Le contenu n'est pas compose quand la carte est repliee : une carte fermee ne
 * coute rien.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayCardShell(
    card: DayCard,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    /**
     * Maintenir le doigt sur l'en-tete ouvre « Organiser ma journee ».
     *
     * C'est le meme geste que sur un ecran d'accueil de telephone : on appuie
     * longtemps sur ce qu'on veut ranger. Il ne remplace pas le bouton en bas
     * de page — un raccourci qu'on ne devine pas ne doit jamais etre le seul
     * chemin — mais il tombe sous le doigt au moment exact ou l'envie vient,
     * c'est-a-dire en regardant la carte qui derange.
     */
    onOrganize: () -> Unit = {},
    /**
     * La carte est-elle **validee** pour cette journee ?
     *
     * Relire la veille — « est-ce que j'ai bien coche mes prieres, mes sorties,
     * mes depenses ? » — n'est pas la meme chose que remplir la journee, et ca
     * ne laissait aucune trace : on recommencait la meme relecture le
     * lendemain. Un geste lateral sur la carte la valide, le meme geste la
     * rouvre.
     *
     * **Valider ferme la carte.** La premiere version se contentait de poser
     * une coche : la carte restait modifiable, donc rien n'empechait de changer
     * ce qu'on venait de relire, et la marque ne voulait plus dire grand-chose.
     * Une carte validee ne repond donc plus au doigt — ni ses pastilles, ni ses
     * champs, ni ses boutons. Elle reste entierement **lisible** : c'est une
     * page tournee, pas une page effacee.
     *
     * Deux chemins pour la rouvrir, et c'est voulu : le meme geste lateral, et
     * la coche de l'en-tete. Un geste qu'on ne devine pas ne doit jamais etre
     * le seul moyen de revenir en arriere.
     */
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    /** L'etat de la carte, en quelques mots. Rien a dire : `null`. */
    summary: String? = null,
    /**
     * Le degrade de la carte, quand elle doit se detacher des autres. Une
     * seule carte par ecran le porte : celle de l'humeur, qui est la raison
     * d'etre de la page.
     *
     * La couleur ne remplit pas la carte d'un bloc et ne s'arrete pas non plus
     * net sous le titre : elle **descend jusqu'au trait** qui precede « Moment
     * par moment », en s'effacant tout du long. Les deux versions precedentes
     * disaient chacune pourquoi : une carte entierement coloree faisait
     * disparaitre les tuiles de la meme couleur, et un simple bandeau coupait
     * trop brutalement. Un fondu long dit la couleur du jour sans jamais
     * concurrencer ce qu'on vient regler dessus.
     */
    accent: List<Color>? = null,
    /**
     * L'encre de l'en-tete, quand elle est imposee. Sert au degrade de
     * l'application, qui se porte en blanc partout ailleurs : le calcul
     * automatique choisirait du sombre, et l'en-tete ne ressemblerait plus aux
     * autres surfaces fortes.
     */
    accentInk: Color? = null,
    content: @Composable () -> Unit,
) {
    val style = cardStyle(card)
    // L'encre de l'en-tete se choisit sur **tout** le degrade, pas sur sa
    // premiere couleur : le vert des bonnes journees part d'un vert moyen et
    // finit clair, et une encre choisie sur le depart s'efface a l'arrivee.
    val onAccent = accent?.let { accentInk ?: readableOnAll(it) }
    val surface = MaterialTheme.colorScheme.surface
    val shape = MaterialTheme.shapes.large
    val density = LocalDensity.current
    val bandPx = remember(density) { with(density) { MOOD_BAND.toPx() } }
    val moodBrush = remember(accent, surface, bandPx) {
        accent?.let {
            Brush.verticalGradient(
                // Le titre garde la couleur pleine ; tout le reste du chemin
                // n'est plus qu'un long effacement vers le blanc de la carte.
                0f to it.first(),
                0.22f to it.last(),
                1f to surface,
                startY = 0f,
                endY = bandPx,
            )
        }
    }

    val haptics = LocalHapticFeedback.current
    val checkEdge by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(Motion.NORMAL),
        label = "verrouillee",
    )

    // Le liseré de lumiere, et le sceau qui le pose.
    //
    // Ce que remplacent ces deux-la : un trait vert plaque a gauche et une
    // petite coche dans le coin. Les deux disaient la chose — la carte est
    // validee — et aucun des deux ne la **faisait**. Verrouiller est un geste ;
    // il lui fallait un moment, pas deux marques posees sans transition.
    //
    // Le liseré court tout le tour de la carte et s'efface vers l'interieur :
    // il cerne sans envahir, et le contenu se lit au travers. Il monte d'un
    // coup au moment du geste, puis redescend a une valeur de repos ou il ne
    // fait plus que tenir le contour.
    val rim = remember { Animatable(if (checked) RIM_REST else 0f) }

    // Le sceau : l'avancee de l'animation (0 → 1) et le sens. `null` = rien a
    // l'ecran, et c'est **l'etat normal d'une carte verrouillee**. Une icone
    // posee au milieu en permanence rendrait la carte illisible, ce qui est
    // exactement ce qu'on ne veut pas : verrouillee, elle doit encore se lire.
    val sealStep = remember { Animatable(0f) }
    var sealing by remember { mutableStateOf<Boolean?>(null) }

    // La premiere composition ne joue rien : ouvrir une journee deja verrouillee
    // ne doit pas rejouer le sceau. Meme regle que les medailles — au passage,
    // jamais a l'affichage.
    var firstPass by remember { mutableStateOf(true) }
    LaunchedEffect(checked) {
        if (firstPass) {
            firstPass = false
            rim.snapTo(if (checked) RIM_REST else 0f)
            return@LaunchedEffect
        }
        sealing = checked
        launch {
            sealStep.snapTo(0f)
            sealStep.animateTo(1f, tween(SEAL_MS, easing = LinearEasing))
            sealing = null
        }
        if (checked) {
            // Le liseré s'allume vite et fort, puis se retire jusqu'au repos :
            // c'est ce retrait qui donne l'impression d'un sceau qui prend.
            rim.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
            rim.animateTo(RIM_REST, tween(430, easing = FastOutSlowInEasing))
        } else {
            rim.animateTo(0.75f, tween(120, easing = LinearEasing))
            rim.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
        }
    }
    // Le decalage du doigt, en pixels. Une simple valeur et non une animation :
    // pendant le geste, la carte suit le doigt, il n'y a rien a animer. C'est
    // seulement au relachement qu'elle revient toute seule.
    var slide by remember { mutableFloatStateOf(0f) }
    val swipePx = with(density) { CHECK_SWIPE.toPx() }

    // Le geste lateral, dans les **deux** sens. Un sens pour valider et l'autre
    // pour rouvrir obligerait a se souvenir lequel est lequel ; ici on pousse
    // la carte, elle bascule, et on la repousse pour revenir. C'est le geste
    // que fait la main sans y penser.
    //
    // Il est garde a part parce qu'il sert **deux fois** : sur la carte, et sur
    // le voile qui la ferme une fois validee. Sans ca, valider une carte
    // rendrait impossible de la rouvrir au meme endroit qu'on l'a fermee.
    // Le retour de la carte a sa place est lance depuis la portee de **la
    // carte**, et surtout pas depuis celle du geste.
    //
    // C'est le bug qui laissait une carte coincee de travers : le voile qui
    // ferme une carte validee porte lui aussi ce geste, et deverrouiller le
    // fait disparaitre a l'instant meme ou le doigt se leve. La portee du
    // geste meurt avec lui, l'animation de retour est annulee au milieu, et
    // `slide` reste sur sa derniere valeur — la carte ne revient jamais. La
    // portee de la carte, elle, survit a la disparition du voile.
    val cardScope = rememberCoroutineScope()
    val slideGesture = Modifier.draggable(
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { delta ->
            // La carte ne part jamais tres loin : au-dela du seuil, on a deja
            // ce qu'on est venu chercher, et la laisser filer hors de l'ecran
            // ferait croire qu'elle s'en va.
            val limit = swipePx * 1.4f
            slide = (slide + delta).coerceIn(-limit, limit)
        },
        onDragStopped = {
            if (kotlin.math.abs(slide) >= swipePx) {
                // Une secousse au moment ou ca bascule : on le sent avant de
                // le voir.
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            cardScope.launch {
                animate(slide, 0f, animationSpec = tween(Motion.NORMAL)) { value, _ ->
                    slide = value
                }
            }
        },
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(slideGesture)
            // Lu **dans** la couche graphique : la carte glisse sans que rien
            // ne soit remesure a chaque image.
            .graphicsLayer { translationX = slide }
            .brandShadow(elevation = 10.dp, shape = shape, color = style.tint)
            .clip(shape)
            .background(surface)
            .then(if (moodBrush != null) Modifier.background(moodBrush) else Modifier.cardGlow(style))
            // Le liseré. Quatre degrades, un par bord, qui partent pleins au
            // bord et s'eteignent vers le centre : la carte est **cernee**, pas
            // recouverte, et on lit au travers. Les coins, ou deux degrades se
            // croisent, sont un peu plus lumineux — c'est ce qu'on veut, un
            // contour se tient par ses coins.
            //
            // Dessine apres le contenu et dans le meme decoupage : il suit les
            // coins arrondis de la carte sans qu'on ait rien a calculer.
            .drawWithContent {
                drawContent()
                val strength = rim.value
                if (strength <= 0f) return@drawWithContent
                val depth = RIM_DEPTH.toPx().coerceAtMost(size.minDimension / 2f)
                val edge = Verified.copy(alpha = RIM_ALPHA * strength)
                val fade = Verified.copy(alpha = 0f)

                drawRect(
                    brush = Brush.verticalGradient(listOf(edge, fade), 0f, depth),
                    size = Size(size.width, depth),
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(fade, edge), size.height - depth, size.height,
                    ),
                    topLeft = Offset(0f, size.height - depth),
                    size = Size(size.width, depth),
                )
                drawRect(
                    brush = Brush.horizontalGradient(listOf(edge, fade), 0f, depth),
                    size = Size(depth, size.height),
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(fade, edge), size.width - depth, size.width,
                    ),
                    topLeft = Offset(size.width - depth, 0f),
                    size = Size(depth, size.height),
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Le fond d'une carte verrouillee s'eteint, mais a peine : le
                // gris n'est plus ce qui porte le message, c'est le liseré. Un
                // gris franc etait terne, et Ismael avait raison de ne pas
                // l'aimer — il eteignait la carte au lieu de la fermer.
                .graphicsLayer { alpha = 1f - 0.06f * checkEdge },
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onToggleCollapse,
                    onLongClickLabel = "Organiser ma journée",
                    onLongClick = {
                        // Une secousse au moment ou le maintien est reconnu :
                        // sans elle, on ne sait pas si le geste a pris avant de
                        // voir l'ecran changer.
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOrganize()
                    },
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        onAccent?.copy(alpha = 0.20f) ?: style.tint.copy(alpha = 0.13f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(style.emoji, fontSize = 19.sp)
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onAccent ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                // Le resume se **remplace** en glissant plutot qu'en sautant :
                // c'est la ligne qui change le plus souvent de l'ecran, a
                // chaque verre d'eau et a chaque priere cochee, et un texte qui
                // change d'un coup sous le doigt donne l'impression d'un bug.
                AnimatedContent(
                    targetState = summary.orEmpty(),
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(tween(Motion.NORMAL)) +
                                slideInVertically(tween(Motion.NORMAL)) { it / 2 },
                            initialContentExit = fadeOut(tween(Motion.QUICK)) +
                                slideOutVertically(tween(Motion.NORMAL)) { -it / 2 },
                            // Meme raison qu'au titre : sans ca, la boite se
                            // redimensionne en decoupant, et un resume plus long
                            // apparait tronque pendant le changement.
                            sizeTransform = SizeTransform(clip = false),
                        )
                    },
                    label = "resume",
                ) { value ->
                    if (value.isNotBlank()) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelMedium,
                            color = onAccent?.copy(alpha = 0.88f) ?: style.tint,
                            maxLines = 1,
                        )
                    }
                }
            }
            // Le cadenas est aussi un **bouton**. Le geste lateral ne se
            // devine pas, et un geste qu'on ne devine pas ne doit jamais etre
            // le seul chemin : une fois la carte verrouillee, il est la, et il
            // s'appuie pour revenir en arriere.
            //
            // Un cadenas et plus une coche : une coche dit « c'est fait », un
            // cadenas dit « c'est ferme ». C'est la deuxieme qui est vraie
            // depuis qu'une carte validee ne se modifie plus, et le signe doit
            // dire ce que le bouton fait.
            if (checked) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Verified.copy(alpha = 0.16f))
                        .clickable(onClickLabel = "Rouvrir la carte") {
                            onCheckedChange(false)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Carte verrouillée",
                        tint = Verified,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        onAccent?.copy(alpha = 0.18f) ?: style.tint.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Un seul chevron qui **pivote**, plutot que deux images qui
                // se remplacent : la fleche montre le chemin que la carte va
                // prendre, et le mouvement le dit mieux qu'un changement
                // d'icone.
                val turn by animateFloatAsState(
                    targetValue = if (collapsed) 0f else 180f,
                    animationSpec = tween(Motion.NORMAL),
                    label = "chevron",
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (collapsed) "Déplier" else "Replier",
                    tint = onAccent ?: style.tint,
                    modifier = Modifier
                        .size(19.dp)
                        .graphicsLayer { rotationZ = turn },
                )
            }
        }

            if (!collapsed) {
                Box {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            // Le contenu d'une carte verrouillee se retire un
                            // peu, sans plus. Il etait franchement grise quand
                            // le gris etait le seul signe ; maintenant que le
                            // liseré tient le contour et que le sceau a montre
                            // le geste, l'eteindre davantage ne dirait rien de
                            // plus et rendrait la carte terne pour rien.
                            .graphicsLayer { alpha = 1f - 0.26f * checkEdge },
                    ) {
                        content()
                    }

                    // Le voile d'une carte verrouillee.
                    //
                    // Il ne cache rien — il est transparent, tout se lit au
                    // travers. Il **prend la place du contenu sous le doigt**, et
                    // c'est tout ce qu'on lui demande : plus une pastille qui
                    // bascule, plus un champ qui ouvre le clavier, plus un bouton
                    // qui repond. Valider veut dire « j'ai relu, c'est en ordre » ;
                    // pouvoir modifier juste apres enlevait tout son sens a la
                    // marque.
                    //
                    // Il ne **consomme** rien, et c'est le point delicat. Compose
                    // ne retient qu'un seul chemin sous le doigt : etre le dernier
                    // enfant de la boite suffit a ce que le contenu ne soit meme
                    // pas atteint. Consommer en plus ferait une carte validee sur
                    // laquelle la page ne defile plus — le doigt tomberait dans un
                    // trou noir des qu'il passe dessus.
                    //
                    // Ce qui traverse donc encore : le glissement lateral ci-dessous
                    // (qui, lui, rouvre la carte) et le defilement vertical de la
                    // page, qui vit plus haut.
                    if (checked) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) awaitPointerEvent()
                                    }
                                }
                                .then(slideGesture)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }
        }

        // Le sceau : il arrive, il tourne, il s'en va. Il n'est **jamais** la
        // au repos — une icone posee au milieu d'une carte verrouillee la
        // rendrait illisible, et une carte verrouillee doit encore se lire.
        sealing?.let { locking -> SealMark(locking = locking, step = sealStep.value) }
    }
}

/**
 * Le cadenas qui se pose, ou qui saute.
 *
 * Deux mouvements pour une seule image, et c'est ce qui les oppose :
 *
 * - **Verrouiller** : il arrive grand et se **resserre** jusqu'a sa taille, en
 *   se redressant d'un demi-tour. Un sceau qu'on presse.
 * - **Deverrouiller** : il part de sa taille et **s'ouvre** en grandissant,
 *   en partant dans l'autre sens. Un sceau qui saute.
 *
 * Moins de sept dixiemes de seconde en tout : passe ce delai, ce n'est plus un
 * retour de geste, c'est une animation qu'on attend.
 *
 * Aucune ecoute de doigt : ce n'est qu'un dessin. Le voile qui bloque
 * reellement la carte est ailleurs, et il ne depend pas de cette animation —
 * une carte doit etre fermee avant la fin de son animation, pas apres.
 */
@Composable
private fun BoxScope.SealMark(locking: Boolean, step: Float) {
    // Une entree franche, une longue tenue, une sortie douce. Sans la tenue,
    // l'oeil n'a pas le temps de reconnaitre le signe.
    val fade = when {
        step < 0.16f -> step / 0.16f
        step < 0.55f -> 1f
        else -> 1f - (step - 0.55f) / 0.45f
    }.coerceIn(0f, 1f)

    // Le resserrement se joue en premier et s'arrete a mi-course ; la sortie,
    // elle, s'etale sur toute la duree. Verrouiller se termine sur une image
    // nette, deverrouiller se termine sur une image qui s'echappe.
    val settle = (step / 0.45f).coerceIn(0f, 1f)
    val eased = FastOutSlowInEasing.transform(settle)
    val scale = if (locking) 2.1f - 1.1f * eased else 1f + 1.3f * FastOutSlowInEasing.transform(step)
    val turn = if (locking) -150f * (1f - eased) else 150f * FastOutSlowInEasing.transform(step)
    val ink = if (locking) Verified else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(76.dp)
            .graphicsLayer {
                alpha = fade
                scaleX = scale
                scaleY = scale
                rotationZ = turn
            }
            .clip(CircleShape)
            // Un disque derriere, sinon le signe se perd sur un contenu
            // charge. Un aplat et pas un degrade radial : un rayon calcule a
            // partir d'une valeur animee passe par zero, et Android refuse un
            // degrade de rayon nul.
            .background(ink.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(34.dp),
        )
    }
}

/**
 * Jusqu'ou descend la couleur du jour sur la carte de l'humeur.
 *
 * Mesuree depuis le haut de la carte, en points et non en fraction de sa
 * hauteur : la carte grandit avec son contenu, et une fraction ferait remonter
 * ou descendre la fin du fondu selon qu'un moment est rempli ou non. La valeur
 * amene le blanc juste au trait qui precede « Moment par moment ».
 */
private val MOOD_BAND = 210.dp

/**
 * Le sommeil de la nuit qui a mene a cette journee. Rien n'est impose : sans
 * montre ni saisie, la carte reste une seule ligne discrete.
 */
@Composable
fun SleepCardBody(
    startMinutes: Int?,
    endMinutes: Int?,
    fromDevice: Boolean,
    tint: Color,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TimeButton(
            label = "Couché à",
            minutes = startMinutes,
            tint = tint,
            onClick = onPickStart,
            modifier = Modifier.weight(1f),
        )
        TimeButton(
            label = "Levé à",
            minutes = endMinutes,
            tint = tint,
            onClick = onPickEnd,
            modifier = Modifier.weight(1f),
        )
    }

    val duration = durationMinutes(startMinutes, endMinutes)
    Spacer(Modifier.height(12.dp))

    if (duration == null) {
        Text(
            text = "Appuie sur une heure pour noter ta nuit. Si ta montre ou ton " +
                "téléphone l'enregistre dans Health Connect, elle se remplit toute seule.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        // La nuit dessinee : un axe de dix-huit heures a dix-huit heures, ou une
        // nuit ordinaire tombe d'un seul tenant. Le chiffre dit combien, la
        // barre dit **quand** — et c'est le quand qui se compare d'un jour a
        // l'autre.
        SleepBar(startMinutes = startMinutes!!, endMinutes = endMinutes!!, tint = tint)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
                Text(
                    text = if (fromDevice) {
                        "Lu sur ton téléphone. Touche une heure pour corriger."
                    } else {
                        sleepComment(duration)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClear) { Text("Effacer") }
        }
    }
}

@Composable
private fun TimeButton(
    label: String,
    minutes: Int?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = minutes?.let { formatClock(it) } ?: "—:—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Les cinq prieres de la journee, en perles.
 *
 * Cinq grandes lignes a cocher tenaient la moitie de l'ecran pour cinq
 * oui-ou-non, et se lisaient une par une. Une rangee de perles dit la meme
 * chose en une ligne, dans l'ordre du jour, et se lit sans lire.
 *
 * La ligne du dessous compte, sans commenter : cinq sur cinq n'est pas felicite,
 * deux sur cinq n'est pas reproche, et rien n'est vert ni rouge. L'application
 * constate, elle ne note pas.
 *
 * `null` et zero ne veulent pas dire la meme chose : une journee ou rien n'a
 * ete touche est une journee dont on ne sait rien, et le texte le dit.
 */
@Composable
fun PrayerCardBody(
    mask: Int?,
    tint: Color,
    date: LocalDate,
    jumua: Boolean?,
    onJumua: (Boolean) -> Unit,
    onToggle: (Prayer, Boolean) -> Unit,
) {
    val done = mask ?: 0
    val count = Prayer.entries.count { done and it.bit != 0 }

    PrayerBeads(
        mask = mask,
        tint = tint,
        onToggle = onToggle,
        date = date,
        jumua = jumua,
        onJumua = onJumua,
    )

    Spacer(Modifier.height(14.dp))
    TrackBar(progress = count / Prayer.entries.size.toFloat(), tint = tint)
    Spacer(Modifier.height(8.dp))
    Text(
        text = when {
            mask == null -> "Rien de coché pour l'instant."
            count == 0 -> "Aucune prière cochée."
            count == Prayer.entries.size -> "Les cinq prières."
            else -> "$count sur ${Prayer.entries.size}."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * L'hygiene du jour : la douche, et les trois brossages.
 *
 * Deux gestes seulement, et c'est deliberé. Une carte d'hygiene qui demande
 * dix choses devient une corvee — exactement ce dont on n'a pas besoin un jour
 * noir, qui est justement le jour ou cette carte compte. Elle se masque comme
 * les traitements pour qui n'en a pas l'usage.
 */
@Composable
fun HygieneCardBody(
    showered: Boolean?,
    brushMask: Int?,
    tint: Color,
    onShower: (Boolean) -> Unit,
    onBrushing: (Brushing, Boolean) -> Unit,
) {
    BigCheck(
        label = "Douché",
        hint = "Certains jours, c'est un vrai effort.",
        checked = showered == true,
        tint = tint,
        onToggle = onShower,
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Brossage des dents",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    BrushingBeads(mask = brushMask, tint = tint, onToggle = onBrushing)

    val count = Brushing.entries.count { (brushMask ?: 0) and it.bit != 0 }
    Spacer(Modifier.height(12.dp))
    TrackBar(progress = count / Brushing.entries.size.toFloat(), tint = tint)
    Spacer(Modifier.height(8.dp))
    Text(
        text = when {
            brushMask == null && showered == null -> "Rien de coché pour l'instant."
            count == 0 -> "Aucun brossage coché."
            count == Brushing.entries.size -> "Les trois brossages."
            else -> "$count sur ${Brushing.entries.size}."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Les traitements du jour. Chaque prise attendue est une case a cocher : la
 * ligne n'existe en base que si la prise a eu lieu, donc "pas encore pris" ne
 * consomme rien.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TreatmentsCardBody(
    treatments: List<Treatment>,
    taken: List<DoseTaken>,
    tint: Color,
    onToggle: (Treatment, DoseTime, Boolean) -> Unit,
    onEdit: (Treatment) -> Unit,
    onAdd: () -> Unit,
) {
    val active = treatments.filter { it.active }

    if (active.isEmpty()) {
        Text(
            text = "Aucun traitement pour l'instant. Ajoute ce que tu prends et tu " +
                "pourras cocher chaque prise, matin, midi, soir ou nuit.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        val done = taken.map { it.treatmentId to it.timeKey }.toSet()
        active.forEachIndexed { index, treatment ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(treatment.name, style = MaterialTheme.typography.bodyLarge)
                    if (treatment.dose.isNotBlank()) {
                        Text(
                            treatment.dose,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = { onEdit(treatment) }) { Text("Modifier") }
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                treatment.times.forEach { time ->
                    val checked = (treatment.id to time.key) in done
                    DoseChip(
                        time = time,
                        checked = checked,
                        tint = tint,
                        onClick = { onToggle(treatment, time, !checked) },
                    )
                }
            }
        }

        val expected = active.sumOf { it.times.size }
        val doneCount = taken.count { dose -> active.any { it.id == dose.treatmentId } }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                expected == 0 -> "Aucune prise prévue."
                doneCount >= expected -> "Tout est pris pour aujourd'hui."
                else -> "$doneCount prise(s) sur $expected."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (doneCount >= expected && expected > 0) {
                tint
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }

    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onAdd,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Ajouter un traitement")
    }
}

@Composable
private fun DoseChip(time: DoseTime, checked: Boolean, tint: Color, onClick: () -> Unit) {
    CardChip(
        label = "${time.emoji} ${time.label}",
        selected = checked,
        tint = tint,
        onClick = onClick,
    )
}

/** Creation ou modification d'un traitement. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TreatmentDialog(
    initial: Treatment?,
    onDismiss: () -> Unit,
    onSave: (Treatment) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var dose by remember { mutableStateOf(initial?.dose.orEmpty()) }
    var mask by remember { mutableIntStateOf(initial?.timesMask ?: DoseTime.MORNING.bit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau traitement" else "Modifier le traitement") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it.take(40) },
                    label = { Text("Dose (optionnel)") },
                    placeholder = { Text("1 comprimé") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text("Quand le prends-tu ?", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DoseTime.entries.forEach { time ->
                        ChoiceChip(
                            label = "${time.emoji} ${time.label}",
                            selected = mask and time.bit != 0,
                            onClick = { mask = mask xor time.bit },
                        )
                    }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) {
                        Text(
                            "Supprimer ce traitement",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && mask != 0,
                onClick = {
                    onSave(
                        (initial ?: Treatment(name = "")).copy(
                            name = name.trim(),
                            dose = dose.trim(),
                            timesMask = mask,
                        )
                    )
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Selecteur d'heure qui rend des minutes depuis minuit. */
fun showClock(context: Context, initialMinutes: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        true,
    ).show()
}

/** Duree d'une nuit, en tenant compte du passage de minuit. */
fun durationMinutes(startMinutes: Int?, endMinutes: Int?): Int? {
    val start = startMinutes ?: return null
    val end = endMinutes ?: return null
    val length = if (end >= start) end - start else end + DayEntry.MINUTES_PER_DAY - start
    return if (length in 1 until DayEntry.MINUTES_PER_DAY) length else null
}

fun formatClock(minutes: Int): String =
    String.format(Locale.FRANCE, "%02d:%02d", (minutes / 60) % 24, minutes % 60)

fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "${hours} h" else "${hours} h ${String.format(Locale.FRANCE, "%02d", rest)}"
}

fun formatLitres(glasses: Int): String =
    String.format(Locale.FRANCE, "%.2f L", glasses * 0.25)

/**
 * Reperes de duree, volontairement larges : la carte informe, elle ne juge pas
 * et ne remplace pas un avis medical.
 */
private fun sleepComment(minutes: Int): String = when {
    minutes < 5 * 60 -> "Nuit très courte."
    minutes < 7 * 60 -> "Nuit courte."
    minutes <= 9 * 60 -> "Nuit dans la moyenne."
    else -> "Nuit longue."
}

/**
 * Le vert de « c'est verifie ».
 *
 * Le meme que celui des bonnes journees ([DayColor.GREEN]) : l'application n'a
 * pas besoin d'un septieme vert, et celui-la veut deja dire « ca va » partout
 * ailleurs.
 */
val Verified = Color(0xFF15C48E)

/**
 * Jusqu'ou le liseré d'une carte verrouillee descend vers l'interieur.
 *
 * Assez pour qu'il se voie, assez peu pour qu'il ne mange pas le texte : la
 * marge des cartes fait seize points, donc l'essentiel de la lumiere tombe
 * dans le vide autour du contenu.
 */
private val RIM_DEPTH = 26.dp

/** L'intensite du liseré a plein. */
private const val RIM_ALPHA = 0.40f

/**
 * La valeur ou le liseré se stabilise une fois la carte verrouillee.
 *
 * Il ne s'eteint pas : c'est lui qui remplace le trait vert plaque a gauche, et
 * c'est le seul repere permanent d'une carte fermee, avec le cadenas de
 * l'en-tete.
 */
private const val RIM_REST = 0.42f

/** La duree du sceau, du depart a la disparition. */
private const val SEAL_MS = 660

/**
 * De combien il faut pousser une carte pour la faire basculer.
 *
 * Assez pour qu'un doigt qui derape en faisant defiler la page ne coche rien,
 * assez peu pour que le geste reste un geste et non un deplacement.
 */
private val CHECK_SWIPE = 72.dp
