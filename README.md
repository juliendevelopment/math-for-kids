# Math contre Zombies

Jeu Android pour apprendre le calcul mental en défendant sa maison contre des zombies rigolos. En français, pour tablette en paysage, 100 % hors-ligne.

- **Aventure** : 8 mondes (amis de 10, additions, soustractions, tables, multiplications, divisions, calculs à trous, mélange) de 10 niveaux, avec un boss à la fin de chaque monde.
- **Entraînement** : sessions réglables (types de calcul, tables, plage, mode de réponse, vitesse, durée) et favoris.
- **Profils** par enfant, pièces, boutique cosmétique, badges, jours d'affilée et écran de progrès avec les calculs à revoir.

Produit : `docs/BRIEF.md`. Règles pour les agents : `AGENTS.md`.

## Build

```sh
export ANDROID_HOME=$HOME/dev/apps/android-sdk
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
python3 -m unittest discover -s scripts -p 'test_*.py'
```

## CI et releases

- `.github/workflows/android-build.yml` : à chaque push sur `main` et PR — tests, lint, APK debug en artefact. Aucun secret.
- `.github/workflows/android-release.yml` : sur tag `android-vX.Y.Z`.
  1. Écrire `docs/releases/vX.Y.Z.md`.
  2. Commit sur `main`, push, attendre **Android Build** vert sur ce SHA exact.
  3. `git tag -a android-vX.Y.Z -m "…" && git push origin android-vX.Y.Z`.
  4. Le workflow vérifie que le commit est sur `main` avec build vert, calcule `versionCode = MAJOR*10000 + MINOR*100 + PATCH`, signe, vérifie l'APK (`scripts/check_android_apk.py` : certificat, package, version, SDK, alignement 16 Ko) et publie `MathContreZombies-vX.Y.Z.apk` + `SHA256SUMS`. Il refuse de remplacer une release existante.

## Signing

Clé dédiée : `~/agent-managed/keys/android/math-for-kids/` (`release.p12` + `signing.json`, 0700/0600), copiée dans les secrets du dépôt `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`. Empreinte SHA-256 figée : `scripts/android-signing-cert.sha256`. **Perdre cette clé = plus de mise à jour possible sans désinstaller.** Ne jamais la réutiliser pour une autre app.

## Vérification sur appareil

À vérifier sur la tablette : lisibilité des calculs, vitesse des zombies pour chaque âge, clavier de saisie du prénom, son et musique, retour d'arrière-plan pendant une partie (pause automatique).
