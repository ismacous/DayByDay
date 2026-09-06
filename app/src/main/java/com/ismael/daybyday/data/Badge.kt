package com.ismael.daybyday.data

/**
 * Les petites choses qu'on se reconnait.
 *
 * Elles ne notent pas la journee et ne se cumulent nulle part : il n'y a ni
 * score, ni collection a completer, ni serie a ne pas casser. C'est voulu.
 * Une application qui suit un moral qui varie beaucoup ne peut pas se
 * permettre de faire perdre quelque chose un mauvais jour — une serie brisee
 * punirait exactement les journees ou il ne faut pas punir.
 *
 * Un badge apparait donc **au moment ou la chose est faite**, une fois, puis
 * disparait. Il felicite, il ne comptabilise pas.
 *
 * Deux regles pour en ajouter un :
 *
 * 1. **Il doit recompenser un geste, pas une donnee qui arrive toute seule.**
 *    Les pas font exception parce que marcher est un geste, meme si c'est le
 *    telephone qui compte.
 * 2. **Il doit rester atteignable un mauvais jour.** « Sortir » et « une
 *    candidature » en font partie ; « cinq candidatures dans la journee » n'en
 *    serait pas.
 */
enum class Badge(
    /** Identifiant stable, si un jour ces moments doivent etre enregistres. */
    val key: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
) {
    PRAYERS("prieres", "Les cinq prières", "La journée est complète.", "🕌"),
    STEPS("pas", "6 000 pas", "Tu as marché aujourd'hui.", "👟"),
    WORKOUT("seance", "Vraie séance", "Le corps a travaillé.", "💪"),
    OUTSIDE("sortie", "Tu es sorti", "La porte a été passée.", "🚪"),
    WATER("eau", "Huit verres", "Bien hydraté.", "💧"),
    APPLICATION("candidature", "Candidature envoyée", "Une de plus.", "📮"),
    WEEK_APPLICATIONS(
        "candidatures_semaine",
        "Cinq candidatures",
        "Sur les sept derniers jours.",
        "🎯",
    ),
    JOURNAL("journal", "Journée écrite", "Elle est gardée.", "✍️"),
}
