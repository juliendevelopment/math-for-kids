package be.arte.mathforkids.game

import be.arte.mathforkids.math.Generator
import be.arte.mathforkids.math.Mode
import be.arte.mathforkids.math.Problem
import be.arte.mathforkids.math.Spec
import kotlin.random.Random

data class GameConfig(
    val spec: Spec,
    val mode: Mode,
    /** Number of zombies to defeat; ignored when [durationSec] > 0. */
    val zombies: Int,
    /** Seconds for a zombie to walk the whole lawn. */
    val crossSec: Float,
    val boss: Boolean = false,
    /** Timed session (training): zombies keep coming until the time is up. */
    val durationSec: Int = 0,
    /** Problems the child got wrong before; some of them come back in this game. */
    val review: List<Problem> = emptyList(),
    val lanes: Int = 3,
)

enum class ZombieType { NORMAL, CONE, BUCKET, FAST, BOSS }

class Zombie(
    val id: Int,
    val lane: Int,
    var x: Float,
    val speed: Float,
    var problem: Problem,
    var choices: List<Int>,
    val type: ZombieType,
    var hp: Int,
    val retry: Boolean,
) {
    val maxHp: Int = hp
    /** Shots already flying towards this zombie. */
    var incoming: Int = 0
    /** Seconds since it was defeated, or -1 while alive. */
    var dying: Float = -1f
    var hitFlash: Float = 0f
    var age: Float = 0f
    /** Bubbles already tried and wrong for the current problem. */
    val wrongChoices: MutableSet<Int> = mutableSetOf()
    var missedOnce: Boolean = false
    val alive: Boolean get() = dying < 0f
    val answerable: Boolean get() = alive && incoming < hp
}

class Shot(val lane: Int, var x: Float, val targetId: Int)

/** Short text floating over the lawn (the solution of a missed problem, "Raté !"…). */
class Floater(val text: String, val x: Float, val lane: Int, val good: Boolean, var age: Float = 0f)

enum class GameEvent { SHOOT, POP, WRONG, HEART, WIN, LOSE, BOSS_HIT, BOSS_SPAWN }

class PopFx(val x: Float, val lane: Int, val big: Boolean, var age: Float = 0f)

/**
 * The whole game simulation, free of Android so it runs in JVM tests.
 * The lawn is one-dimensional per lane: x = 1 is the right edge where zombies appear,
 * x = 0 is the house.
 */
class GameEngine(val config: GameConfig, private val rnd: Random = Random.Default) {
    val zombies = mutableListOf<Zombie>()
    val shots = mutableListOf<Shot>()
    val floaters = mutableListOf<Floater>()
    val pops = mutableListOf<PopFx>()
    val events = mutableListOf<GameEvent>()

    var hearts = MAX_HEARTS; private set
    var correct = 0; private set
    var wrong = 0; private set
    var kills = 0; private set
    var time = 0f; private set
    var finished = false; private set
    var won = false; private set
    /** Recoil animation clock per lane (seconds since the launcher last fired). */
    val laneFired = FloatArray(config.lanes) { 99f }

    /** Problems answered wrongly or that reached the house. */
    val missed = mutableListOf<Problem>()
    /** Problems solved without any mistake. */
    val solved = mutableListOf<Problem>()

    private var nextId = 1
    private var spawned = 0
    private var sinceSpawn = 99f
    private var bossSpawned = false
    private val retryQueue = ArrayDeque<Problem>()
    private val reviewPool = config.review.shuffled(rnd).toMutableList()
    private val recent = ArrayDeque<String>()

    private val timed get() = config.durationSec > 0
    private val maxAlive get() = if (config.mode == Mode.MULTI) 5 else 3
    private val spawnEvery get() = config.crossSec * if (config.mode == Mode.MULTI) 0.2f else 0.32f

    val total: Int get() = config.zombies + if (config.boss) 1 else 0
    /** 0..1 for the progress bar. */
    val progress: Float
        get() = if (timed) (time / config.durationSec).coerceIn(0f, 1f) else kills.toFloat() / total.coerceAtLeast(1)
    val accuracy: Float get() = if (correct + wrong == 0) 1f else correct.toFloat() / (correct + wrong)

    /** The zombie whose problem is the current question in one-question modes. */
    val active: Zombie? get() = zombies.filter { it.answerable }.minByOrNull { it.x }

    fun update(dt: Float) {
        if (finished) {
            animate(dt)
            return
        }
        time += dt
        sinceSpawn += dt
        for (i in laneFired.indices) laneFired[i] += dt
        spawn()

        for (z in zombies) {
            z.age += dt
            if (z.hitFlash > 0f) z.hitFlash -= dt
            if (z.alive) z.x -= z.speed * dt
        }
        moveShots(dt)

        val arrived = zombies.filter { it.alive && it.x <= 0f }
        for (z in arrived) {
            zombies.remove(z)
            if (z.incoming > 0) { // a shot was about to land: count it as defeated
                shots.removeAll { it.targetId == z.id }
                kill(z)
                continue
            }
            hearts--
            events += GameEvent.HEART
            floaters += Floater(solution(z.problem), 0.12f, z.lane, good = false)
            noteMiss(z)
            if (z.type == ZombieType.BOSS) {
                // The boss takes a heart and steps back instead of leaving.
                z.x = 0.7f
                zombies += z
            } else if (!z.retry && !timed) {
                // The unsolved problem comes back once, on an extra zombie.
                retryQueue += z.problem
                spawned--
            }
            if (hearts <= 0) {
                finish(false)
                return
            }
        }
        animate(dt)

        val allOut = if (timed) time >= config.durationSec else spawned >= config.zombies && (!config.boss || bossSpawned)
        if (allOut && zombies.none { it.alive } && shots.isEmpty()) finish(true)
    }

    private fun animate(dt: Float) {
        zombies.forEach { if (!it.alive) it.dying += dt }
        zombies.removeAll { it.dying > 0.5f }
        floaters.forEach { it.age += dt }
        floaters.removeAll { it.age > 2.4f }
        pops.forEach { it.age += dt }
        pops.removeAll { it.age > 0.7f }
    }

    private fun moveShots(dt: Float) {
        val it = shots.iterator()
        while (it.hasNext()) {
            val s = it.next()
            val target = zombies.firstOrNull { z -> z.id == s.targetId && z.alive }
            if (target == null) {
                it.remove()
                continue
            }
            s.x += SHOT_SPEED * dt
            if (s.x >= target.x) {
                it.remove()
                target.incoming--
                hit(target)
            }
        }
    }

    private fun hit(z: Zombie) {
        z.hitFlash = 0.25f
        if (z.hp > 1) {
            z.hp--
            z.x = (z.x + 0.1f).coerceAtMost(0.95f)
            setProblem(z, nextProblem())
            pops += PopFx(z.x, z.lane, big = false)
            events += GameEvent.BOSS_HIT
        } else {
            kill(z)
        }
    }

    private fun kill(z: Zombie) {
        z.hp = 0
        z.dying = 0f
        kills++
        if (z !in zombies) zombies += z
        pops += PopFx(z.x.coerceAtLeast(0.02f), z.lane, big = z.type == ZombieType.BOSS)
        events += GameEvent.POP
    }

    private fun spawn() {
        val alive = zombies.count { it.alive }
        val remaining = if (timed) time < config.durationSec else spawned < config.zombies
        if (remaining) {
            val due = sinceSpawn >= spawnEvery || (alive == 0 && sinceSpawn >= 0.7f)
            if (due && alive < maxAlive) {
                spawned++
                sinceSpawn = 0f
                val fromRetry = retryQueue.isNotEmpty() && (rnd.nextFloat() < 0.5f || spawned >= config.zombies)
                val p = if (fromRetry) retryQueue.removeFirst() else nextProblem()
                val roll = rnd.nextFloat()
                val late = spawned > 5
                val type = when {
                    late && roll < 0.15f -> ZombieType.FAST
                    roll < 0.4f -> ZombieType.CONE
                    roll < 0.55f -> ZombieType.BUCKET
                    else -> ZombieType.NORMAL
                }
                val speed = (1f / config.crossSec) * if (type == ZombieType.FAST) 1.3f else 1f
                add(Zombie(nextId++, pickLane(), 1f, speed, p, Generator.choices(p, rnd), type, 1, fromRetry))
            }
        } else if (config.boss && !bossSpawned && alive == 0 && !timed) {
            bossSpawned = true
            val p = nextProblem()
            add(Zombie(nextId++, config.lanes / 2, 1f, 0.55f / config.crossSec, p, Generator.choices(p, rnd), ZombieType.BOSS, BOSS_HP, false))
            events += GameEvent.BOSS_SPAWN
        }
    }

    private fun add(z: Zombie) {
        zombies += z
        remember(z.problem)
    }

    private fun pickLane(): Int {
        val load = IntArray(config.lanes)
        zombies.filter { it.alive && it.x > 0.6f }.forEach { load[it.lane]++ }
        val min = load.min()
        val free = load.indices.filter { load[it] == min }
        return free[rnd.nextInt(free.size)]
    }

    private fun remember(p: Problem) {
        recent += p.text
        if (recent.size > 4) recent.removeFirst()
    }

    private fun setProblem(z: Zombie, p: Problem) {
        z.problem = p
        z.choices = Generator.choices(p, rnd)
        z.wrongChoices.clear()
        z.missedOnce = false
        remember(p)
    }

    private fun nextProblem(): Problem {
        if (reviewPool.isNotEmpty() && rnd.nextFloat() < 0.25f) return reviewPool.removeAt(reviewPool.lastIndex)
        val onLawn = zombies.filter { it.alive }.map { it.problem.text }
        var p = Generator.generate(config.spec, rnd)
        var tries = 0
        while ((p.text in recent || p.text in onLawn) && tries++ < 8) p = Generator.generate(config.spec, rnd)
        return p
    }

    private fun noteMiss(z: Zombie) {
        if (!z.missedOnce) {
            z.missedOnce = true
            missed += z.problem
        }
    }

    /** Submits an answer. Returns true when it shoots a zombie. */
    fun answer(n: Int): Boolean {
        if (finished) return false
        val target = if (config.mode == Mode.MULTI) {
            zombies.filter { it.answerable && it.problem.answer == n }.minByOrNull { it.x }
        } else {
            val a = active ?: return false
            if (a.problem.answer == n) a else {
                a.wrongChoices += n
                noteMiss(a)
                null
            }
        }
        if (target == null) {
            wrong++
            events += GameEvent.WRONG
            if (config.mode == Mode.MULTI) {
                floaters += Floater("Raté !", 0.2f, config.lanes / 2, good = false, age = 1.2f)
            }
            return false
        }
        correct++
        if (!target.missedOnce) solved += target.problem
        target.incoming++
        shots += Shot(target.lane, 0f, target.id)
        laneFired[target.lane] = 0f
        events += GameEvent.SHOOT
        return true
    }

    private fun finish(win: Boolean) {
        finished = true
        won = win
        events += if (win) GameEvent.WIN else GameEvent.LOSE
    }

    /** 0 when lost; otherwise precision decides, never raw speed. */
    val stars: Int
        get() = when {
            !won -> 0
            hearts == MAX_HEARTS && accuracy >= 0.9f -> 3
            accuracy >= 0.75f -> 2
            else -> 1
        }

    companion object {
        const val MAX_HEARTS = 3
        const val BOSS_HP = 6
        const val SHOT_SPEED = 2.2f

        /** "7 × ? = 56" → "7 × 8 = 56"; "7 × 8" → "7 × 8 = 56". */
        fun solution(p: Problem): String =
            if ('?' in p.text) p.text.replace("?", p.answer.toString()) else "${p.text} = ${p.answer}"
    }
}
