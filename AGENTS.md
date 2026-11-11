# AGENTS.md — math-for-kids

Jeu Android « Math contre Zombies » pour les enfants (5-11 ans), en français, tablette en paysage.
Kotlin + Compose, une seule app module `:app`, tout est dessiné en Canvas (aucune image) et tout le son est synthétisé (aucun fichier audio).

## Vérification obligatoire

```sh
export ANDROID_HOME=$HOME/dev/apps/android-sdk   # JDK 17 déjà actif (asdf)
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Un build vert ne prouve ni le plaisir de jeu ni la lisibilité : la vitesse des zombies, la taille des calculs et le son se jugent sur la tablette avec les enfants.

## Carte du code

- `math/` — génération des calculs (`Problems.kt`) et le parcours : 8 mondes × 10 niveaux, boss au 10e (`Curriculum.kt`). Le texte d'un calcul utilise `+ − × ÷` et `?` pour le trou.
- `game/GameEngine.kt` — toute la simulation, sans Android : testée en JVM (`GameTest`).
- `ScreensTest` (Robolectric) rend chaque écran sur une tablette 10" et écrit des PNG dans `app/build/screens/` (artefact CI) : regarder ces captures après tout changement visuel.
- `data/` — profils, récompenses, badges, boutique, sauvegarde JSON locale (`files/save.json`).
- `audio/` — un seul `AudioTrack` qui mixe la musique chiptune et les bruitages.
- `ui/` — écrans Compose. `App.kt` force une densité virtuelle (600 dp de haut) pour que téléphone et tablette aient la même mise en page.

## Règles projet

- Entiers seulement, réponses jamais négatives ; un test vérifie chaque calcul de chaque niveau.
- Les étoiles récompensent la précision, jamais la vitesse. Une erreur ne coûte pas de cœur.
- La boutique est purement cosmétique : rien ne doit rendre le calcul plus facile.
- Aucun réseau, aucun analytics, aucune permission. Les données des enfants restent sur l'appareil.
- Ne jamais casser une sauvegarde existante : champs nouveaux avec valeur par défaut (`ignoreUnknownKeys`).
- Signing : clé hors du dépôt, sous `~/agent-managed/keys/android/math-for-kids/` (0700/0600), injectée par variables d'environnement. Ne jamais committer de keystore ni de `local.properties`.
- Release : tag `android-vX.Y.Z` (procédure dans `README.md`) ; ne jamais déplacer un tag dont la release existe.
- `docs/BRIEF.md` est la référence produit ; une décision non tranchée y est listée comme question ouverte — ne pas l'inventer en silence.
