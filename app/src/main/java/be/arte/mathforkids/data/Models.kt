package be.arte.mathforkids.data

import be.arte.mathforkids.math.Kind
import be.arte.mathforkids.math.Mode
import be.arte.mathforkids.math.Problem
import be.arte.mathforkids.math.Spec
import kotlinx.serialization.Serializable

/** A problem the child got wrong: [misses] times in total, [fixes] clean answers since the last miss. */
@Serializable
data class Mistake(val answer: Int, val kind: Kind, val misses: Int = 0, val fixes: Int = 0)

/** Saved "Entraînement" settings. Speed: 0 slow, 1 normal, 2 fast. */
@Serializable
data class TrainingSetup(
    val name: String = "",
    val kinds: Set<Kind> = setOf(Kind.ADD),
    val holes: Boolean = false,
    val range: Int = 20,
    val tables: Set<Int> = setOf(2, 3, 4, 5),
    val mode: Mode = Mode.CHOICE,
    val speed: Int = 1,
    val minutes: Int = 3,
)

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val avatar: String = "chat",
    /** Index in Curriculum.grades. */
    val grade: Int = 0,
    /** Zombie speed preference: 0 slow, 1 normal, 2 fast. */
    val speed: Int = 1,
    /** Best stars per level id ("world-level"). */
    val stars: Map<String, Int> = emptyMap(),
    val coins: Int = 0,
    val owned: Set<String> = emptySet(),
    val launcher: String = "tournesol",
    val projectile: String = "pois",
    val badges: Set<String> = emptySet(),
    val kills: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val playSeconds: Long = 0,
    val gamesWon: Int = 0,
    val perfectGames: Int = 0,
    val trainings: Int = 0,
    val lastPlayDay: Long = -1,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val mistakes: Map<String, Mistake> = emptyMap(),
    val favorites: List<TrainingSetup> = emptyList(),
) {
    val totalStars: Int get() = stars.values.sum()

    /** Problems still to be reviewed that fit a game about [kinds]. */
    fun reviewFor(kinds: Set<Kind>, limit: Int = 6): List<Problem> =
        mistakes.entries
            .filter { it.value.fixes < FIXES_TO_CLEAR && it.value.kind in kinds }
            .sortedByDescending { it.value.misses }
            .take(limit)
            .map { Problem(it.key, it.value.answer, it.value.kind) }

    companion object {
        const val FIXES_TO_CLEAR = 2
    }
}

@Serializable
data class SaveData(
    val profiles: List<Profile> = emptyList(),
    val sfxVolume: Float = 0.6f,
    val musicVolume: Float = 0.3f,
)

/** What one finished game contributes to a profile. */
data class GameOutcome(
    /** Adventure level id, or null for a training session. */
    val levelId: String?,
    val won: Boolean,
    val stars: Int,
    val kills: Int,
    val correct: Int,
    val wrong: Int,
    val seconds: Int,
    val bossBeaten: Boolean,
    val missed: List<Problem>,
    val solved: List<Problem>,
)

data class Reward(val profile: Profile, val coins: Int, val newBadges: List<Badge>, val newBest: Boolean)

/** Training settings → a problem spec. Returns null when nothing playable is selected. */
fun TrainingSetup.toSpec(): Spec? {
    val t = tables.sorted().ifEmpty { listOf(2) }
    val parts = kinds.sortedBy { it.ordinal }.mapNotNull { k ->
        when (k) {
            Kind.FRIENDS -> Spec(Kind.FRIENDS, max = if (range >= 100) 100 else if (range >= 20) 20 else 10, step = if (range >= 100) 10 else 1, form = 3)
            Kind.ADD -> Spec(Kind.ADD, max = range, step = if (range >= 1000) 10 else 1, hole = holes)
            Kind.SUB -> Spec(Kind.SUB, max = range, step = if (range >= 1000) 10 else 1, hole = holes)
            Kind.TABLES -> Spec(Kind.TABLES, tables = t, maxFactor = if (t.any { it > 10 }) 12 else 10, hole = holes)
            Kind.DIV -> Spec(Kind.DIV, tables = t.filter { it > 1 }.ifEmpty { listOf(2) }, maxFactor = if (t.any { it > 10 }) 12 else 10, hole = holes)
            Kind.MUL -> Spec(
                Kind.MIX,
                parts = listOf(
                    Spec(Kind.MUL, aMin = 2, aMax = 99, tables = listOf(10, 100), hole = holes),
                    Spec(Kind.MUL, aMin = 11, aMax = 99, tables = (2..9).toList(), hole = holes),
                ),
            )
            Kind.MIX -> null
        }
    }
    return when (parts.size) {
        0 -> null
        1 -> parts[0]
        else -> Spec(Kind.MIX, parts = parts)
    }
}
