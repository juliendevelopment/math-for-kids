package be.arte.mathforkids.math

import kotlinx.serialization.Serializable

/** How the child answers and how zombies are targeted. */
@Serializable
enum class Mode {
    /** One question at a time (the closest zombie), three answer bubbles. */
    CHOICE,
    /** One question at a time, numeric keypad. */
    KEYPAD,
    /** Every zombie carries its own problem; typing a number hits the matching zombie. */
    MULTI,
}

/** A difficulty step inside a world. [think] stretches zombie travel time for harder mental work. */
data class Stage(val spec: Spec, val think: Float = 1f)

data class Level(
    val world: Int,
    val index: Int,
    val spec: Spec,
    val mode: Mode,
    val zombies: Int,
    val crossSec: Float,
    val boss: Boolean,
) {
    val id: String get() = "$world-$index"
}

data class World(val index: Int, val name: String, val place: String, val stages: List<Stage>) {
    val levels: List<Level> = PLAN.mapIndexed { i, (mode, stageIdx) ->
        val boss = i == PLAN.lastIndex
        val picked = stageIdx.map { stages[it] }
        val spec = if (picked.size == 1) picked[0].spec else Spec(Kind.MIX, parts = picked.map { it.spec })
        val think = picked.maxOf { it.think }
        val base = when (mode) {
            Mode.CHOICE -> 26f
            Mode.KEYPAD -> 30f
            Mode.MULTI -> 40f
        }
        Level(
            world = index, index = i, spec = spec, mode = mode,
            zombies = if (boss) 12 else if (mode == Mode.MULTI) 18 else 15,
            crossSec = base * think * (1f - 0.02f * i),
            boss = boss,
        )
    }

    companion object {
        /** Per world: discover with bubbles, practise on the keypad, then master with one problem per zombie. */
        private val PLAN: List<Pair<Mode, List<Int>>> = listOf(
            Mode.CHOICE to listOf(0),
            Mode.CHOICE to listOf(1),
            Mode.CHOICE to listOf(2),
            Mode.KEYPAD to listOf(0, 1),
            Mode.KEYPAD to listOf(2),
            Mode.KEYPAD to listOf(3),
            Mode.MULTI to listOf(1, 2),
            Mode.MULTI to listOf(3),
            Mode.MULTI to listOf(4),
            Mode.MULTI to listOf(0, 1, 2, 3, 4),
        )
    }
}

object Curriculum {
    private val T2_9 = (2..9).toList()
    private val T2_12 = (2..12).toList()

    val worlds: List<World> = listOf(
        World(
            0, "Les amis de 10", "Le jardin",
            listOf(
                // Only complements to 10 here: friends of 20 / 100 would be too much for this first world.
                Stage(Spec(Kind.FRIENDS, max = 10, form = 0)),
                Stage(Spec(Kind.FRIENDS, max = 10, form = 1)),
                Stage(Spec(Kind.FRIENDS, max = 10, form = 2)),
                Stage(Spec(Kind.FRIENDS, max = 10, form = 3)),
                Stage(Spec(Kind.FRIENDS, max = 10, form = 3)),
            ),
        ),
        World(
            1, "Les additions", "La plage",
            listOf(
                Stage(Spec(Kind.ADD, max = 10)),
                Stage(Spec(Kind.ADD, max = 20), 1.1f),
                Stage(Spec(Kind.ADD, max = 100, step = 10), 1.1f),
                Stage(Spec(Kind.ADD, max = 100), 1.5f),
                Stage(Spec(Kind.ADD, max = 1000, step = 10), 1.6f),
            ),
        ),
        World(
            2, "Les soustractions", "La forêt",
            listOf(
                Stage(Spec(Kind.SUB, max = 10)),
                Stage(Spec(Kind.SUB, max = 20), 1.15f),
                Stage(Spec(Kind.SUB, max = 100, step = 10), 1.15f),
                Stage(Spec(Kind.SUB, max = 100), 1.6f),
                Stage(Spec(Kind.SUB, max = 1000, step = 10), 1.7f),
            ),
        ),
        World(
            3, "Les tables", "La montagne",
            listOf(
                Stage(Spec(Kind.TABLES, tables = listOf(1, 2, 10))),
                Stage(Spec(Kind.TABLES, tables = listOf(3, 4, 5))),
                Stage(Spec(Kind.TABLES, tables = listOf(6, 7)), 1.1f),
                Stage(Spec(Kind.TABLES, tables = listOf(8, 9)), 1.1f),
                Stage(Spec(Kind.TABLES, tables = T2_12, maxFactor = 12), 1.2f),
            ),
        ),
        World(
            4, "Les multiplications", "Le désert",
            listOf(
                Stage(Spec(Kind.MUL, aMin = 2, aMax = 99, tables = listOf(10))),
                Stage(Spec(Kind.MUL, aMin = 2, aMax = 99, tables = listOf(10, 100))),
                Stage(Spec(Kind.MUL, aMin = 20, aMax = 90, step = 10, tables = T2_9), 1.2f),
                Stage(Spec(Kind.MUL, aMin = 11, aMax = 20, tables = listOf(2, 3, 4, 5)), 1.6f),
                Stage(Spec(Kind.MUL, aMin = 11, aMax = 99, tables = T2_9), 2f),
            ),
        ),
        World(
            5, "Les divisions", "La nuit",
            listOf(
                Stage(Spec(Kind.DIV, tables = listOf(2, 5, 10))),
                Stage(Spec(Kind.DIV, tables = listOf(3, 4)), 1.1f),
                Stage(Spec(Kind.DIV, tables = listOf(6, 7)), 1.15f),
                Stage(Spec(Kind.DIV, tables = listOf(8, 9)), 1.15f),
                Stage(Spec(Kind.DIV, tables = T2_12, maxFactor = 12), 1.25f),
            ),
        ),
        World(
            6, "Les calculs à trous", "L'espace",
            listOf(
                Stage(Spec(Kind.ADD, max = 10, hole = true), 1.1f),
                Stage(
                    Spec(Kind.MIX, parts = listOf(Spec(Kind.ADD, max = 20, hole = true), Spec(Kind.SUB, max = 20, hole = true))),
                    1.2f,
                ),
                Stage(Spec(Kind.TABLES, tables = listOf(2, 3, 4, 5), hole = true), 1.2f),
                Stage(
                    Spec(Kind.MIX, parts = listOf(Spec(Kind.TABLES, tables = T2_9, hole = true), Spec(Kind.DIV, tables = T2_9, hole = true))),
                    1.3f,
                ),
                Stage(
                    Spec(
                        Kind.MIX,
                        parts = listOf(
                            Spec(Kind.ADD, max = 100, hole = true), Spec(Kind.SUB, max = 100, hole = true),
                            Spec(Kind.TABLES, tables = T2_12, maxFactor = 12, hole = true), Spec(Kind.DIV, tables = T2_12, maxFactor = 12, hole = true),
                        ),
                    ),
                    1.7f,
                ),
            ),
        ),
        World(
            7, "Le grand mélange", "Le volcan",
            listOf(
                Stage(Spec(Kind.MIX, parts = listOf(Spec(Kind.ADD, max = 20), Spec(Kind.SUB, max = 20))), 1.1f),
                Stage(
                    Spec(Kind.MIX, parts = listOf(Spec(Kind.ADD, max = 100, step = 10), Spec(Kind.SUB, max = 100, step = 10), Spec(Kind.TABLES, tables = listOf(2, 3, 4, 5)))),
                    1.15f,
                ),
                Stage(
                    Spec(Kind.MIX, parts = listOf(Spec(Kind.ADD, max = 100), Spec(Kind.SUB, max = 100), Spec(Kind.TABLES, tables = T2_9), Spec(Kind.DIV, tables = T2_9))),
                    1.5f,
                ),
                Stage(
                    Spec(
                        Kind.MIX,
                        parts = listOf(
                            Spec(Kind.ADD, max = 100, hole = true), Spec(Kind.SUB, max = 100), Spec(Kind.TABLES, tables = T2_12, maxFactor = 12),
                            Spec(Kind.DIV, tables = T2_12, maxFactor = 12, hole = true), Spec(Kind.MUL, aMin = 2, aMax = 99, tables = listOf(10, 100)),
                        ),
                    ),
                    1.6f,
                ),
                Stage(
                    Spec(
                        Kind.MIX,
                        parts = listOf(
                            Spec(Kind.ADD, max = 1000, step = 10), Spec(Kind.SUB, max = 1000, step = 10), Spec(Kind.TABLES, tables = T2_12, maxFactor = 12, hole = true),
                            Spec(Kind.DIV, tables = T2_12, maxFactor = 12), Spec(Kind.MUL, aMin = 11, aMax = 99, tables = T2_9),
                        ),
                    ),
                    2f,
                ),
            ),
        ),
    )

    const val LEVELS_PER_WORLD = 10
    val totalStars: Int get() = worlds.size * LEVELS_PER_WORLD * 3

    fun level(id: String): Level? {
        val (w, l) = id.split("-").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return null
        return worlds.getOrNull(w)?.levels?.getOrNull(l)
    }

    /** School years offered at profile creation, and the world each one starts in. */
    val grades: List<Pair<String, Int>> = listOf(
        "1re primaire (CP)" to 0,
        "2e primaire (CE1)" to 1,
        "3e primaire (CE2)" to 3,
        "4e primaire (CM1)" to 4,
        "5e primaire (CM2)" to 5,
        "6e primaire et plus" to 6,
    )

    /**
     * Worlds up to the starting one are open; inside the frontier a level opens once the previous
     * one has a star, and a later world opens once the boss before it is beaten.
     */
    fun isUnlocked(level: Level, startWorld: Int, stars: Map<String, Int>): Boolean {
        if (level.world < startWorld) return true
        if (level.index > 0) return (stars["${level.world}-${level.index - 1}"] ?: 0) > 0
        if (level.world <= startWorld) return true
        return (stars["${level.world - 1}-${LEVELS_PER_WORLD - 1}"] ?: 0) > 0
    }
}
