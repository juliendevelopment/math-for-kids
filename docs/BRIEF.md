# Brief produit — Math contre Zombies

Application Android pour les enfants de la maison : un « Plants vs Zombies » où l'on tire sur les zombies en répondant à des calculs. En français, pensée pour tablette en paysage (jouable sur téléphone en paysage), Android 8.0+.

## Décisions (session de cadrage du 2026-11-10)

| Sujet | Décision |
|---|---|
| Public | 5 à 11 ans et plus (1re primaire → 6e primaire) |
| Profils | Plusieurs profils sans mot de passe : prénom, avatar, année scolaire, vitesse des zombies. L'année fixe le monde de départ ; les mondes précédents restent ouverts. |
| Mécanique | Dans chaque monde : niveaux 1-3 une question à la fois + 3 bulles ; 4-6 une question + clavier ; 7-9 et boss un calcul par zombie, on tape le nombre et le zombie le plus proche qui a cette réponse est touché. |
| Échec | 3 cœurs. Une mauvaise réponse ne coûte rien ; un zombie qui atteint la maison enlève un cœur, montre la solution et le calcul revient une fois. À 0 cœur on rejoue sans rien perdre. |
| Étoiles | 3★ = 3 cœurs et ≥ 90 % de bonnes réponses, 2★ = ≥ 75 %, 1★ = niveau gagné. 1★ débloque le niveau suivant, battre le boss ouvre le monde suivant. |
| Parcours | Amis de 10 → Additions → Soustractions → Tables → Multiplications → Divisions → Calculs à trous → Grand mélange. 10 niveaux par monde, parties de 2-3 min. |
| Contenu | Entiers, réponses ≥ 0 : additions/soustractions jusqu'à 1000, tables jusqu'à 12, ×10/×100, 2 chiffres × 1 chiffre, divisions exactes. Pas de décimaux, négatifs, restes ni priorités. |
| Entraînement | Libre d'accès : types de calcul, trous, plage de nombres, tables, mode de réponse, vitesse, durée ; favoris nommés par profil. |
| Récompenses | Étoiles, pièces (1/zombie, 5/étoile, bonus boss et entraînement), boutique cosmétique (avatars, lanceurs, projectiles), 17 badges, jours d'affilée. |
| Suivi | Écran « Mes progrès » : temps, étoiles, réponses, calculs les plus ratés. Les calculs ratés reviennent dans les parties (~25 %) jusqu'à 2 réussites sans faute. |
| Graphismes | Vectoriel dessiné en code, zombies ronds et rigolos, un décor par monde. |
| Son | Bruitages + musique chiptune synthétisés (une variante par monde), volumes séparés. |
| Technique | Kotlin + Compose comme theme-launcher, 100 % hors-ligne, release signée par tag. |

## Questions ouvertes

- Vitesse et nombre de zombies par niveau : réglés au jugé, à recalibrer après les premières parties des enfants.
- Lecture vocale des calculs pour les 5-6 ans qui lisent peu : écartée en v1.
- Mondes supplémentaires (restes, décimaux, priorités) : à envisager plus tard.
