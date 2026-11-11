package be.arte.mathforkids.data

import be.arte.mathforkids.math.Curriculum

class Badge(val id: String, val name: String, val desc: String, val symbol: String, val earned: (Profile) -> Boolean)

object Badges {
    private fun bosses(p: Profile) = Curriculum.worlds.count { (p.stars["${it.index}-${Curriculum.LEVELS_PER_WORLD - 1}"] ?: 0) > 0 }

    val all: List<Badge> = listOf(
        Badge("premier_zombie", "Premier zombie", "Toucher son premier zombie", "1") { it.kills >= 1 },
        Badge("chasseur", "Chasseur", "Toucher 100 zombies", "100") { it.kills >= 100 },
        Badge("exterminateur", "Exterminateur", "Toucher 500 zombies", "500") { it.kills >= 500 },
        Badge("legende", "Légende", "Toucher 2000 zombies", "2000") { it.kills >= 2000 },
        Badge("premiere_etoile", "Première étoile", "Gagner une étoile", "★") { it.totalStars >= 1 },
        Badge("constellation", "Constellation", "Gagner 30 étoiles", "30★") { it.totalStars >= 30 },
        Badge("galaxie", "Galaxie", "Gagner 100 étoiles", "100★") { it.totalStars >= 100 },
        Badge("sans_faute", "Sans faute", "Gagner une partie sans aucune erreur", "0") { it.perfectGames >= 1 },
        Badge("oeil_de_lynx", "Œil de lynx", "Gagner 10 parties sans aucune erreur", "10×") { it.perfectGames >= 10 },
        Badge("boss", "Tombeur de boss", "Battre un zombie-boss", "B") { bosses(it) >= 1 },
        Badge("tous_les_boss", "Roi du jardin", "Battre tous les boss", "8B") { bosses(it) >= Curriculum.worlds.size },
        Badge("serie3", "3 jours d'affilée", "Jouer 3 jours de suite", "3j") { it.bestStreak >= 3 },
        Badge("serie7", "Une semaine !", "Jouer 7 jours de suite", "7j") { it.bestStreak >= 7 },
        Badge("entrainement", "À l'entraînement", "Terminer un entraînement", "E") { it.trainings >= 1 },
        Badge("sportif", "Grand sportif", "Terminer 10 entraînements", "10E") { it.trainings >= 10 },
        Badge("cerveau", "Super cerveau", "Donner 1000 bonnes réponses", "1000") { it.correct >= 1000 },
        Badge("collection", "Collectionneur", "Acheter 5 objets dans la boutique", "5") { it.owned.size >= 5 },
    )
}

enum class ShopCategory(val label: String) { AVATAR("Avatars"), LAUNCHER("Lanceurs"), PROJECTILE("Projectiles") }

/** Cosmetic only: nothing sold here makes the math easier. */
data class ShopItem(val id: String, val name: String, val category: ShopCategory, val price: Int)

object Shop {
    val items: List<ShopItem> = listOf(
        ShopItem("chat", "Chat", ShopCategory.AVATAR, 0),
        ShopItem("chien", "Chien", ShopCategory.AVATAR, 0),
        ShopItem("lapin", "Lapin", ShopCategory.AVATAR, 0),
        ShopItem("ours", "Ours", ShopCategory.AVATAR, 0),
        ShopItem("panda", "Panda", ShopCategory.AVATAR, 0),
        ShopItem("grenouille", "Grenouille", ShopCategory.AVATAR, 0),
        ShopItem("renard", "Renard", ShopCategory.AVATAR, 120),
        ShopItem("lion", "Lion", ShopCategory.AVATAR, 180),
        ShopItem("pingouin", "Pingouin", ShopCategory.AVATAR, 180),
        ShopItem("robot", "Robot", ShopCategory.AVATAR, 300),
        ShopItem("licorne", "Licorne", ShopCategory.AVATAR, 450),
        ShopItem("dragon", "Dragon", ShopCategory.AVATAR, 600),
        ShopItem("tournesol", "Tournesol", ShopCategory.LAUNCHER, 0),
        ShopItem("tulipe", "Tulipe", ShopCategory.LAUNCHER, 150),
        ShopItem("cactus", "Cactus", ShopCategory.LAUNCHER, 250),
        ShopItem("champignon", "Champignon", ShopCategory.LAUNCHER, 350),
        ShopItem("canon", "Canon", ShopCategory.LAUNCHER, 500),
        ShopItem("pois", "Petit pois", ShopCategory.PROJECTILE, 0),
        ShopItem("etoile", "Étoile", ShopCategory.PROJECTILE, 100),
        ShopItem("coeur", "Cœur", ShopCategory.PROJECTILE, 150),
        ShopItem("tomate", "Tomate", ShopCategory.PROJECTILE, 200),
        ShopItem("neige", "Boule de neige", ShopCategory.PROJECTILE, 300),
        ShopItem("feu", "Boule de feu", ShopCategory.PROJECTILE, 400),
    )

    fun item(id: String): ShopItem? = items.firstOrNull { it.id == id }
    fun owns(p: Profile, item: ShopItem): Boolean = item.price == 0 || item.id in p.owned

    fun buy(p: Profile, item: ShopItem): Profile =
        if (owns(p, item) || p.coins < item.price) p
        else Rules.withBadges(p.copy(coins = p.coins - item.price, owned = p.owned + item.id)).first

    fun equip(p: Profile, item: ShopItem): Profile = if (!owns(p, item)) p else when (item.category) {
        ShopCategory.AVATAR -> p.copy(avatar = item.id)
        ShopCategory.LAUNCHER -> p.copy(launcher = item.id)
        ShopCategory.PROJECTILE -> p.copy(projectile = item.id)
    }
}

object Rules {
    const val COINS_PER_KILL = 1
    const val COINS_PER_STAR = 5
    const val COINS_BOSS = 10
    const val COINS_TRAINING = 5
    private const val MAX_MISTAKES = 150

    fun withBadges(p: Profile): Pair<Profile, List<Badge>> {
        val fresh = Badges.all.filter { it.id !in p.badges && it.earned(p) }
        return p.copy(badges = p.badges + fresh.map { it.id }) to fresh
    }

    /** Folds a finished game into the profile. [today] is the local epoch day. */
    fun apply(p: Profile, o: GameOutcome, today: Long): Reward {
        var coins = o.kills * COINS_PER_KILL
        var stars = p.stars
        var newBest = false
        if (o.won) {
            if (o.levelId != null) {
                coins += o.stars * COINS_PER_STAR
                if (o.stars > (stars[o.levelId] ?: 0)) {
                    stars = stars + (o.levelId to o.stars)
                    newBest = true
                }
            } else coins += COINS_TRAINING
            if (o.bossBeaten) coins += COINS_BOSS
        }

        val mistakes = p.mistakes.toMutableMap()
        for (m in o.missed) {
            val old = mistakes[m.text]
            mistakes[m.text] = Mistake(m.answer, m.kind, (old?.misses ?: 0) + 1, 0)
        }
        for (s in o.solved) {
            val old = mistakes[s.text] ?: continue
            mistakes[s.text] = old.copy(fixes = old.fixes + 1)
        }
        val kept = if (mistakes.size <= MAX_MISTAKES) mistakes else
            mistakes.entries.sortedByDescending { it.value.misses - it.value.fixes }.take(MAX_MISTAKES).associate { it.key to it.value }

        val streak = when (p.lastPlayDay) {
            today -> p.streak.coerceAtLeast(1)
            today - 1 -> p.streak + 1
            else -> 1
        }
        val perfect = o.won && o.wrong == 0 && o.missed.isEmpty() && o.correct > 0

        val updated = p.copy(
            stars = stars,
            coins = p.coins + coins,
            kills = p.kills + o.kills,
            correct = p.correct + o.correct,
            wrong = p.wrong + o.wrong,
            playSeconds = p.playSeconds + o.seconds,
            gamesWon = p.gamesWon + if (o.won) 1 else 0,
            perfectGames = p.perfectGames + if (perfect) 1 else 0,
            trainings = p.trainings + if (o.won && o.levelId == null) 1 else 0,
            lastPlayDay = today,
            streak = streak,
            bestStreak = maxOf(p.bestStreak, streak),
            mistakes = kept,
        )
        val (final, badges) = withBadges(updated)
        return Reward(final, coins, badges, newBest)
    }

    /** The streak shown on screen: it is broken once a full day was skipped. */
    fun currentStreak(p: Profile, today: Long): Int = if (p.lastPlayDay >= today - 1) p.streak else 0
}
