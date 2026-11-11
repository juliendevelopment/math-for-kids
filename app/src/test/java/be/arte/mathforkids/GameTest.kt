package be.arte.mathforkids

import be.arte.mathforkids.data.GameOutcome
import be.arte.mathforkids.data.Profile
import be.arte.mathforkids.data.Rules
import be.arte.mathforkids.data.Shop
import be.arte.mathforkids.data.TrainingSetup
import be.arte.mathforkids.data.toSpec
import be.arte.mathforkids.game.GameConfig
import be.arte.mathforkids.game.GameEngine
import be.arte.mathforkids.math.Curriculum
import be.arte.mathforkids.math.Generator
import be.arte.mathforkids.math.Kind
import be.arte.mathforkids.math.Mode
import be.arte.mathforkids.math.Problem
import be.arte.mathforkids.math.Spec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private val dt = 1f / 60f

    private fun run(e: GameEngine, maxSec: Float = 600f, each: (GameEngine) -> Unit = {}) {
        var t = 0f
        while (!e.finished && t < maxSec) {
            e.update(dt)
            each(e)
            t += dt
        }
    }

    /** A perfect player answering every visible problem as soon as it appears. */
    private fun perfect(e: GameEngine) {
        if (e.config.mode == Mode.MULTI) {
            e.zombies.filter { it.answerable && it.x < 0.95f }.forEach { e.answer(it.problem.answer) }
        } else e.active?.takeIf { it.x < 0.95f }?.let { e.answer(it.problem.answer) }
    }

    @Test
    fun perfectPlayerWinsEveryLevelWithThreeStars() {
        for (w in Curriculum.worlds) for (l in w.levels) {
            val e = GameEngine(GameConfig(l.spec, l.mode, l.zombies, l.crossSec, l.boss), Random(l.id.hashCode()))
            run(e, each = ::perfect)
            assertTrue("level ${l.id} did not finish", e.finished)
            assertTrue("level ${l.id} lost", e.won)
            assertEquals("level ${l.id}", 3, e.stars)
            assertEquals(e.total, e.kills)
        }
    }

    @Test
    fun idlePlayerLosesAfterThreeHeartsAndSeesSolutions() {
        val l = Curriculum.worlds[0].levels[0]
        val e = GameEngine(GameConfig(l.spec, l.mode, l.zombies, l.crossSec, l.boss), Random(5))
        run(e)
        assertTrue(e.finished)
        assertFalse(e.won)
        assertEquals(0, e.hearts)
        assertEquals(0, e.stars)
        assertEquals(3, e.missed.size)
    }

    @Test
    fun wrongBubbleIsRememberedAndCostsNoHeart() {
        val e = GameEngine(GameConfig(Spec(Kind.ADD, max = 10), Mode.CHOICE, 5, 20f), Random(9))
        while (e.active == null) e.update(dt)
        val z = e.active!!
        val bad = z.choices.first { it != z.problem.answer }
        assertFalse(e.answer(bad))
        assertTrue(bad in z.wrongChoices)
        assertEquals(GameEngine.MAX_HEARTS, e.hearts)
        assertEquals(listOf(z.problem), e.missed)
        assertTrue(e.answer(z.problem.answer))
        assertFalse("a problem fixed after a mistake is not 'solved'", z.problem in e.solved)
    }

    @Test
    fun multiModeHitsTheClosestMatchingZombie() {
        val e = GameEngine(GameConfig(Spec(Kind.ADD, max = 10), Mode.MULTI, 10, 30f), Random(2))
        repeat(600) { e.update(dt) }
        val alive = e.zombies.filter { it.answerable }
        assertTrue(alive.size >= 2)
        val target = alive.minByOrNull { it.x }!!
        assertTrue(e.answer(target.problem.answer))
        assertEquals(1, target.incoming)
        // A number nobody carries is a harmless miss.
        val unused = (0..50).first { n -> alive.none { it.problem.answer == n } }
        assertFalse(e.answer(unused))
        assertEquals(GameEngine.MAX_HEARTS, e.hearts)
    }

    @Test
    fun bossNeedsSeveralHits() {
        val e = GameEngine(GameConfig(Spec(Kind.ADD, max = 10), Mode.KEYPAD, 1, 20f, boss = true), Random(4))
        var bossHits = 0
        run(e) { g ->
            perfect(g)
            bossHits += g.events.count { it == be.arte.mathforkids.game.GameEvent.BOSS_HIT }
            g.events.clear()
        }
        assertTrue(e.won)
        assertEquals(GameEngine.BOSS_HP - 1, bossHits)
    }

    @Test
    fun timedTrainingEndsOnTime() {
        val spec = TrainingSetup(kinds = setOf(Kind.TABLES), tables = setOf(7)).toSpec()!!
        val e = GameEngine(GameConfig(spec, Mode.KEYPAD, 0, 30f, durationSec = 60), Random(8))
        run(e, each = ::perfect)
        assertTrue(e.won)
        assertTrue(e.time in 60f..75f)
        assertTrue(e.kills > 5)
    }

    @Test
    fun reviewProblemsComeBack() {
        val review = listOf(Problem("7 × 8", 56, Kind.TABLES))
        val e = GameEngine(GameConfig(Spec(Kind.TABLES, tables = listOf(2)), Mode.KEYPAD, 30, 20f, review = review), Random(11))
        var seen = false
        run(e) { g ->
            if (g.zombies.any { it.problem.text == "7 × 8" }) seen = true
            perfect(g)
        }
        assertTrue(seen)
    }

    @Test
    fun trainingSetupMapsToSpecs() {
        assertNull(TrainingSetup(kinds = emptySet()).toSpec())
        val mix = TrainingSetup(kinds = setOf(Kind.ADD, Kind.DIV), tables = setOf(3), holes = true).toSpec()
        assertNotNull(mix)
        assertEquals(setOf(Kind.ADD, Kind.DIV), mix!!.kinds())
        val rnd = Random(0)
        repeat(100) { assertTrue('?' in Generator.generate(mix, rnd).text) }
    }

    private fun outcome(level: String?, stars: Int, missed: List<Problem> = emptyList(), solved: List<Problem> = emptyList(), wrong: Int = 0) =
        GameOutcome(level, won = stars > 0 || level == null, stars = stars, kills = 10, correct = 10, wrong = wrong, seconds = 120, bossBeaten = false, missed = missed, solved = solved)

    @Test
    fun rewardsKeepBestStarsAndCountCoins() {
        var p = Profile("a", "Léa")
        val r1 = Rules.apply(p, outcome("0-0", 2), today = 100)
        assertEquals(10 + 2 * Rules.COINS_PER_STAR, r1.coins)
        assertTrue(r1.newBest)
        p = Rules.apply(r1.profile, outcome("0-0", 1), today = 100).profile
        assertEquals(2, p.stars["0-0"])
        assertTrue("premier_zombie" in p.badges)
        assertTrue("premiere_etoile" in p.badges)
    }

    @Test
    fun streakCountsConsecutiveDays() {
        var p = Profile("a", "Léa")
        p = Rules.apply(p, outcome(null, 0), 100).profile
        p = Rules.apply(p, outcome(null, 0), 101).profile
        p = Rules.apply(p, outcome(null, 0), 102).profile
        assertEquals(3, p.streak)
        assertTrue("serie3" in p.badges)
        assertEquals(0, Rules.currentStreak(p, 105))
        p = Rules.apply(p, outcome(null, 0), 105).profile
        assertEquals(1, p.streak)
        assertEquals(3, p.bestStreak)
    }

    @Test
    fun mistakesAreReviewedUntilFixedTwice() {
        val m = Problem("6 × 9", 54, Kind.TABLES)
        var p = Rules.apply(Profile("a", "Léa"), outcome(null, 0, missed = listOf(m)), 1).profile
        assertEquals(listOf(m), p.reviewFor(setOf(Kind.TABLES)))
        assertTrue(p.reviewFor(setOf(Kind.ADD)).isEmpty())
        p = Rules.apply(p, outcome(null, 0, solved = listOf(m)), 1).profile
        assertEquals(1, p.reviewFor(setOf(Kind.TABLES)).size)
        p = Rules.apply(p, outcome(null, 0, solved = listOf(m)), 1).profile
        assertTrue(p.reviewFor(setOf(Kind.TABLES)).isEmpty())
    }

    @Test
    fun shopIsCosmeticAndChecksCoins() {
        val p = Profile("a", "Léa", coins = 100)
        val cactus = Shop.item("cactus")!!
        assertEquals(p, Shop.buy(p, cactus))
        val rich = p.copy(coins = 300)
        val bought = Shop.equip(Shop.buy(rich, cactus), cactus)
        assertEquals(50, bought.coins)
        assertEquals("cactus", bought.launcher)
        assertEquals(bought, Shop.buy(bought, cactus))
    }
}
