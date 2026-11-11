package be.arte.mathforkids.math

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
enum class Kind { FRIENDS, ADD, SUB, TABLES, MUL, DIV, MIX }

/**
 * Describes a family of mental-math problems. Only whole numbers and non-negative answers.
 *
 * - FRIENDS: complements to [max] ("amis de 10"), operands multiples of [step];
 *   [form] 0 = "a + ? = T", 1 = hole on either side, 2 = "T − a", 3 = any of them.
 * - ADD / SUB: operands multiples of [step], sum / first operand at most [max].
 * - TABLES: one factor from [tables], the other in 1..[maxFactor].
 * - MUL: first factor in [aMin]..[aMax] (multiples of [step]), second from [tables].
 * - DIV: exact division, divisor from [tables], quotient in 1..[maxFactor].
 * - MIX: one of [parts] at random.
 *
 * [hole] turns the problem into a "calcul à trous" (an operand is the unknown).
 */
@Serializable
data class Spec(
    val kind: Kind,
    val max: Int = 10,
    val step: Int = 1,
    val tables: List<Int> = emptyList(),
    val maxFactor: Int = 10,
    val aMin: Int = 1,
    val aMax: Int = 9,
    val hole: Boolean = false,
    val form: Int = 0,
    val parts: List<Spec> = emptyList(),
) {
    /** Base kinds this spec can produce (MIX flattened). */
    fun kinds(): Set<Kind> = if (kind == Kind.MIX) parts.flatMap { it.kinds() }.toSet() else setOf(kind)
}

@Serializable
data class Problem(val text: String, val answer: Int, val kind: Kind)

object Generator {
    const val PLUS = "+"
    const val MINUS = "−"
    const val TIMES = "×"
    const val DIVIDE = "÷"

    fun generate(spec: Spec, rnd: Random): Problem = when (spec.kind) {
        Kind.FRIENDS -> friends(spec, rnd)
        Kind.ADD -> add(spec, rnd)
        Kind.SUB -> sub(spec, rnd)
        Kind.TABLES -> tables(spec, rnd)
        Kind.MUL -> mul(spec, rnd)
        Kind.DIV -> div(spec, rnd)
        Kind.MIX -> generate(spec.parts[rnd.nextInt(spec.parts.size)], rnd)
    }

    private fun friends(s: Spec, rnd: Random): Problem {
        val n = (s.max / s.step).coerceAtLeast(2)
        val a = s.step * rnd.nextInt(1, n)
        val form = when (s.form) {
            0 -> 0
            1 -> rnd.nextInt(2)
            2 -> 2
            else -> rnd.nextInt(3)
        }
        val text = when (form) {
            0 -> "$a $PLUS ? = ${s.max}"
            1 -> "? $PLUS $a = ${s.max}"
            else -> "${s.max} $MINUS $a"
        }
        return Problem(text, s.max - a, Kind.FRIENDS)
    }

    private fun add(s: Spec, rnd: Random): Problem {
        val n = (s.max / s.step).coerceAtLeast(2)
        // Keep big ranges from producing mostly tiny sums.
        val low = if (n > 20) n / 5 else 2
        val sum = rnd.nextInt(low, n + 1)
        val a = rnd.nextInt(1, sum)
        val b = sum - a
        val (x, y, t) = Triple(a * s.step, b * s.step, sum * s.step)
        return if (!s.hole) Problem("$x $PLUS $y", t, Kind.ADD)
        else if (rnd.nextBoolean()) Problem("$x $PLUS ? = $t", y, Kind.ADD)
        else Problem("? $PLUS $y = $t", x, Kind.ADD)
    }

    private fun sub(s: Spec, rnd: Random): Problem {
        val n = (s.max / s.step).coerceAtLeast(2)
        val low = if (n > 20) n / 5 else 2
        val a = rnd.nextInt(low, n + 1)
        val b = rnd.nextInt(1, a)
        val (x, y, r) = Triple(a * s.step, b * s.step, (a - b) * s.step)
        return if (!s.hole) Problem("$x $MINUS $y", r, Kind.SUB)
        else if (rnd.nextBoolean()) Problem("$x $MINUS ? = $r", y, Kind.SUB)
        else Problem("? $MINUS $y = $r", x, Kind.SUB)
    }

    private fun factor(max: Int, rnd: Random): Int {
        var f = rnd.nextInt(1, max + 1)
        if (f == 1 && max > 2) f = rnd.nextInt(1, max + 1) // ×1 stays possible but rare
        return f
    }

    private fun tables(s: Spec, rnd: Random): Problem {
        val t = s.tables[rnd.nextInt(s.tables.size)]
        val f = factor(s.maxFactor, rnd)
        val p = t * f
        return if (!s.hole) {
            if (rnd.nextBoolean()) Problem("$t $TIMES $f", p, Kind.TABLES) else Problem("$f $TIMES $t", p, Kind.TABLES)
        } else if (rnd.nextBoolean()) Problem("$t $TIMES ? = $p", f, Kind.TABLES)
        else Problem("? $TIMES $t = $p", f, Kind.TABLES)
    }

    private fun mul(s: Spec, rnd: Random): Problem {
        val lo = (s.aMin + s.step - 1) / s.step
        val hi = (s.aMax / s.step).coerceAtLeast(lo)
        val a = s.step * rnd.nextInt(lo, hi + 1)
        val b = s.tables[rnd.nextInt(s.tables.size)]
        val p = a * b
        return if (!s.hole) Problem("$a $TIMES $b", p, Kind.MUL)
        else Problem("? $TIMES $b = $p", a, Kind.MUL)
    }

    private fun div(s: Spec, rnd: Random): Problem {
        val d = s.tables[rnd.nextInt(s.tables.size)]
        val q = factor(s.maxFactor, rnd)
        val p = d * q
        return if (!s.hole) Problem("$p $DIVIDE $d", q, Kind.DIV)
        else if (rnd.nextBoolean()) Problem("? $DIVIDE $d = $q", p, Kind.DIV)
        else Problem("$p $DIVIDE ? = $q", d, Kind.DIV)
    }

    /** Three answer bubbles: the right one and two plausible wrong ones, shuffled. */
    fun choices(p: Problem, rnd: Random): List<Int> {
        val a = p.answer
        val near = mutableListOf(a + 1, a - 1, a + 2, a - 2)
        if (a >= 10) near += listOf(a + 10, a - 10)
        val pool = near.filter { it >= 0 && it != a }.distinct().shuffled(rnd)
        return (pool.take(2) + a).shuffled(rnd)
    }
}
