# Polices livrées avec l'application

Le journal propose plusieurs polices d'écriture. Trois d'entre elles sont des
fichiers rangés dans `app/src/main/res/font/` et embarqués dans l'APK ; les
deux autres sont celles qu'Android porte déjà.

Rien n'est téléchargé, ni à l'installation ni à l'usage : l'application n'a pas
la permission Internet et ne peut pas en avoir.

| Nom dans l'application | Police    | Licence                         |
|------------------------|-----------|---------------------------------|
| Manuscrite             | Caveat    | SIL Open Font License 1.1       |
| Serif                  | Lora      | SIL Open Font License 1.1       |
| Moderne                | Poppins   | SIL Open Font License 1.1       |
| Sans serif             | Android   | —                               |
| Machine à écrire       | Android   | —                               |

La licence OFL autorise l'usage, la redistribution et l'inclusion dans un
logiciel, y compris commercial, tant que les fichiers de police ne sont pas
vendus seuls. Les trois familles viennent de Google Fonts.

Une seule graisse est embarquée par police : le gras et l'italique sont
fabriqués par Android à partir d'elle. C'est ce qui évite de tripler le poids
de l'application pour une différence à peine visible sur un téléphone.
