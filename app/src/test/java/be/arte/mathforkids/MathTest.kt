package be.arte.mathforkids

import be.arte.mathforkids.math.Curriculum
import be.arte.mathforkids.math.Generator
import be.arte.mathforkids.math.Kind
import be.arte.mathforkids.math.Problem
import be.arte.mathforkids.math.Spec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Independently evaluates "a op b". */
fun solve(text: String): Int {
    val t = text.split(" ")
    fun op(a: Int, o: String, b: Int) = when (o) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> { require(a % b == 0) { "inexact: $text" }; a / b }
        else -> error("op $o in $text")
    }
    require(t.size == 3) { "format: $text" }
    return op(t[0].toInt(), t[1], t[2].toInt())
}

class MathTest {
    private fun allSpecs(): List<Spec> = Curriculum.worlds.flatMap { w -> w.levels.map { it.spec } }

    @Test
    fun everyCurriculumProblemIsCorrectWholeAndPositive() {
        val rnd = Random(42)
        for (spec in allSpecs()) repeat(300) {
            val p = Generator.generate(spec, rnd)
            assertTrue("negative: $p", p.answer >= 0)
            assertTrue("too big: $p", p.answer <= 9900)
            val numbers = Regex("\\d+").findAll(p.text).map { it.value.toInt() }.toList()
            assertTrue("operand over 1000: $p", numbers.all { it <= 1000 })
            if ('?' in p.text) {
                // Putting the answer in the hole must give a true equation.
                val filled = p.text.replace("?", p.answer.toString())
                assertEquals(p.text, filled.substringAfter("= ").toInt(), solve(filled.substringBefore(" =")))
            } else {
                assertEquals(p.text, p.answer, solve(p.text))
            }
        }
    }

    @Test
    fun friendsOfTenAlwaysMakeTen() {
        val rnd = Random(1)
        repeat(200) {
            val p = Generator.generate(Spec(Kind.FRIENDS, max = 10, form = 3), rnd)
            assertTrue(p.answer in 1..9)
            assertTrue(p.text.contains("10"))
        }
    }

    @Test
    fun choicesContainAnswerOnceAndAreDistinctNonNegative() {
        val rnd = Random(3)
        for (a in listOf(0, 1, 2, 9, 10, 55, 999)) {
            val c = Generator.choices(Problem("x", a, Kind.ADD), rnd)
            assertEquals(3, c.size)
            assertEquals(3, c.toSet().size)
            assertEquals(1, c.count { it == a })
            assertTrue(c.all { it >= 0 })
        }
    }

    @Test
    fun curriculumShape() {
        assertEquals(8, Curriculum.worlds.size)
        for (w in Curriculum.worlds) {
            assertEquals(Curriculum.LEVELS_PER_WORLD, w.levels.size)
            assertTrue(w.levels.last().boss)
            assertEquals(1, w.levels.count { it.boss })
        }
        assertEquals(Curriculum.worlds[3].levels[4], Curriculum.level("3-4"))
    }

    @Test
    fun unlocking() {
        val first = Curriculum.worlds[0].levels
        assertTrue(Curriculum.isUnlocked(first[0], 0, emptyMap()))
        assertFalse(Curriculum.isUnlocked(first[1], 0, emptyMap()))
        assertTrue(Curriculum.isUnlocked(first[1], 0, mapOf("0-0" to 1)))
        // Starting in world 3: earlier worlds fully open, world 3 starts at level 1, world 4 locked.
        assertTrue(Curriculum.isUnlocked(first[9], 3, emptyMap()))
        assertTrue(Curriculum.isUnlocked(Curriculum.worlds[3].levels[0], 3, emptyMap()))
        assertFalse(Curriculum.isUnlocked(Curriculum.worlds[4].levels[0], 3, emptyMap()))
        assertTrue(Curriculum.isUnlocked(Curriculum.worlds[4].levels[0], 3, mapOf("3-9" to 2)))
    }
}
