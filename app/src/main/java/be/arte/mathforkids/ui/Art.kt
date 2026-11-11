package be.arte.mathforkids.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import be.arte.mathforkids.game.ZombieType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Every picture in the app is drawn here with plain shapes: no image files. */

private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

fun DrawScope.text(s: String, cx: Float, cy: Float, sizePx: Float, color: Color) {
    textPaint.textSize = sizePx
    textPaint.color = color.toArgb()
    drawContext.canvas.nativeCanvas.drawText(s, cx, cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
}

fun measureText(s: String, sizePx: Float): Float {
    textPaint.textSize = sizePx
    return textPaint.measureText(s)
}

private val INK = Color(0xFF2B2B3A)

fun DrawScope.star(c: Offset, r: Float, color: Color, outline: Color? = Color(0xFFB8860B)) {
    val p = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + i * PI / 5
        val x = c.x + rad * cos(a).toFloat()
        val y = c.y + rad * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
    if (outline != null) drawPath(p, outline, style = Stroke(r * 0.12f))
}

fun DrawScope.heart(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y + r * 0.9f)
        cubicTo(c.x - r * 1.6f, c.y - r * 0.2f, c.x - r * 0.7f, c.y - r * 1.3f, c.x, c.y - r * 0.4f)
        cubicTo(c.x + r * 0.7f, c.y - r * 1.3f, c.x + r * 1.6f, c.y - r * 0.2f, c.x, c.y + r * 0.9f)
        close()
    }
    drawPath(p, color)
    drawPath(p, lerp(color, Color.Black, 0.35f), style = Stroke(r * 0.12f))
}

fun DrawScope.coin(c: Offset, r: Float) {
    drawCircle(Color(0xFFB8860B), r, c)
    drawCircle(Color(0xFFFFD54F), r * 0.86f, c)
    drawCircle(Color(0xFFFFB300), r * 0.6f, c, style = Stroke(r * 0.12f))
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.14f, Offset(c.x - r * 0.35f, c.y - r * 0.35f))
}

private fun DrawScope.eye(c: Offset, r: Float, look: Offset = Offset.Zero) {
    drawCircle(Color.White, r, c)
    drawCircle(INK, r, c, style = Stroke(r * 0.15f))
    drawCircle(INK, r * 0.45f, Offset(c.x + look.x * r, c.y + look.y * r))
}

/**
 * A friendly round zombie walking to the left. [footY] is the ground line, [h] its height.
 * [dying] goes 0→1 while it pops away.
 */
fun DrawScope.zombie(cx: Float, footY: Float, h: Float, type: ZombieType, walk: Float, flash: Boolean, dying: Float = 0f) {
    val s = (1f - dying).coerceIn(0f, 1f)
    if (s <= 0f) return
    scale(s, s, Offset(cx, footY - h / 2)) {
        rotate(dying * 60f + sin(walk) * 4f, Offset(cx, footY)) {
            val boss = type == ZombieType.BOSS
            val skin = if (flash) Color.White else when (type) {
                ZombieType.FAST -> Color(0xFFB5D66B)
                ZombieType.BOSS -> Color(0xFF7FA650)
                else -> Color(0xFF9CCC65)
            }
            val dark = lerp(skin, Color.Black, 0.35f)
            val shirt = when (type) {
                ZombieType.BOSS -> Color(0xFF7B4FB0)
                ZombieType.FAST -> Color(0xFFE2574C)
                ZombieType.BUCKET -> Color(0xFF4F86C6)
                else -> Color(0xFF8D6E63)
            }
            val headR = h * 0.27f
            val head = Offset(cx, footY - h + headR + h * 0.04f)
            val bodyTop = head.y + headR * 0.75f
            val bodyH = h * 0.3f
            val bodyW = h * 0.36f
            val swing = sin(walk) * h * 0.06f
            // Shadow and legs
            drawOval(Color.Black.copy(alpha = 0.18f), Offset(cx - h * 0.25f, footY - h * 0.04f), Size(h * 0.5f, h * 0.09f))
            val legW = h * 0.09f
            drawLine(Color(0xFF4E5D78), Offset(cx - bodyW * 0.22f, bodyTop + bodyH * 0.8f), Offset(cx - bodyW * 0.22f - swing, footY - legW / 2), legW, StrokeCap.Round)
            drawLine(Color(0xFF4E5D78), Offset(cx + bodyW * 0.22f, bodyTop + bodyH * 0.8f), Offset(cx + bodyW * 0.22f + swing, footY - legW / 2), legW, StrokeCap.Round)
            // Body
            drawRoundRect(shirt, Offset(cx - bodyW / 2, bodyTop), Size(bodyW, bodyH), CornerRadius(bodyW * 0.3f))
            drawRoundRect(lerp(shirt, Color.Black, 0.3f), Offset(cx - bodyW / 2, bodyTop), Size(bodyW, bodyH), CornerRadius(bodyW * 0.3f), style = Stroke(h * 0.015f))
            // Arms stretched forward
            val armY = bodyTop + bodyH * 0.3f
            val armW = h * 0.075f
            drawLine(dark, Offset(cx - bodyW * 0.2f, armY + armW), Offset(cx - h * 0.36f, armY + armW + swing * 0.6f), armW, StrokeCap.Round)
            drawLine(skin, Offset(cx - bodyW * 0.3f, armY), Offset(cx - h * 0.4f, armY - swing * 0.6f), armW, StrokeCap.Round)
            // Head
            drawCircle(skin, headR, head)
            drawCircle(dark, headR, head, style = Stroke(h * 0.018f))
            eye(Offset(head.x - headR * 0.45f, head.y - headR * 0.15f), headR * 0.3f, Offset(-0.3f, 0.1f))
            eye(Offset(head.x + headR * 0.25f, head.y - headR * 0.2f), headR * 0.22f, Offset(-0.3f, 0.1f))
            // Goofy mouth with one tooth
            val mouth = Offset(head.x - headR * 0.6f, head.y + headR * 0.3f)
            drawRoundRect(Color(0xFF5D2E46), mouth, Size(headR * 0.95f, headR * 0.36f), CornerRadius(headR * 0.18f))
            drawRect(Color.White, Offset(mouth.x + headR * 0.25f, mouth.y), Size(headR * 0.2f, headR * 0.2f))
            when (type) {
                ZombieType.CONE -> {
                    val p = Path().apply {
                        moveTo(head.x, head.y - headR * 2.0f)
                        lineTo(head.x - headR * 0.6f, head.y - headR * 0.78f)
                        lineTo(head.x + headR * 0.6f, head.y - headR * 0.78f)
                        close()
                    }
                    drawPath(p, Color(0xFFFF8A3D))
                    drawLine(Color.White, Offset(head.x - headR * 0.3f, head.y - headR * 1.35f), Offset(head.x + headR * 0.3f, head.y - headR * 1.35f), headR * 0.16f)
                    drawLine(Color(0xFFE56A1F), Offset(head.x - headR * 0.8f, head.y - headR * 0.78f), Offset(head.x + headR * 0.8f, head.y - headR * 0.78f), headR * 0.18f, StrokeCap.Round)
                }
                ZombieType.BUCKET -> {
                    drawRoundRect(Color(0xFFB0BEC5), Offset(head.x - headR * 0.7f, head.y - headR * 1.55f), Size(headR * 1.4f, headR * 0.9f), CornerRadius(headR * 0.12f))
                    drawLine(Color(0xFF78909C), Offset(head.x - headR * 0.8f, head.y - headR * 0.68f), Offset(head.x + headR * 0.8f, head.y - headR * 0.68f), headR * 0.16f, StrokeCap.Round)
                }
                ZombieType.FAST -> {
                    drawLine(Color(0xFFE2574C), Offset(head.x - headR * 0.92f, head.y - headR * 0.55f), Offset(head.x + headR * 0.92f, head.y - headR * 0.55f), headR * 0.22f)
                    drawLine(Color(0xFFE2574C), Offset(head.x + headR * 0.9f, head.y - headR * 0.55f), Offset(head.x + headR * 1.4f, head.y - headR * 0.8f + swing), headR * 0.14f, StrokeCap.Round)
                }
                ZombieType.BOSS -> {
                    val y0 = head.y - headR * 0.82f
                    val p = Path().apply {
                        moveTo(head.x - headR * 0.7f, y0)
                        lineTo(head.x - headR * 0.8f, y0 - headR * 0.75f)
                        lineTo(head.x - headR * 0.35f, y0 - headR * 0.35f)
                        lineTo(head.x, y0 - headR * 0.9f)
                        lineTo(head.x + headR * 0.35f, y0 - headR * 0.35f)
                        lineTo(head.x + headR * 0.8f, y0 - headR * 0.75f)
                        lineTo(head.x + headR * 0.7f, y0)
                        close()
                    }
                    drawPath(p, Color(0xFFFFD54F))
                    drawPath(p, Color(0xFFB8860B), style = Stroke(headR * 0.06f))
                    drawCircle(Color(0xFFE2574C), headR * 0.1f, Offset(head.x, y0 - headR * 0.3f))
                }
                ZombieType.NORMAL -> {
                    for (i in -1..1) drawLine(dark, Offset(head.x + i * headR * 0.25f, head.y - headR * 0.92f), Offset(head.x + i * headR * 0.4f, head.y - headR * 1.25f), headR * 0.1f, StrokeCap.Round)
                }
            }
            if (boss) drawCircle(Color(0xFFFFD54F), h * 0.03f, Offset(cx, bodyTop + bodyH * 0.4f))
        }
    }
}

private fun DrawScope.face(c: Offset, r: Float, mouthOpen: Float) {
    eye(Offset(c.x - r * 0.1f, c.y - r * 0.2f), r * 0.2f, Offset(0.3f, 0f))
    eye(Offset(c.x + r * 0.5f, c.y - r * 0.2f), r * 0.2f, Offset(0.3f, 0f))
    drawCircle(INK, r * (0.14f + 0.14f * mouthOpen), Offset(c.x + r * 0.3f, c.y + r * 0.35f))
}

/** The plant (or machine) guarding a lane. [recoil] 1 → just fired, 0 → at rest. */
fun DrawScope.launcher(id: String, cx: Float, footY: Float, h: Float, recoil: Float) {
    val r = h * 0.3f
    val head = Offset(cx - recoil * h * 0.08f, footY - h * 0.68f)
    val stem = Color(0xFF4CAF50)
    drawOval(Color.Black.copy(alpha = 0.18f), Offset(cx - h * 0.25f, footY - h * 0.04f), Size(h * 0.5f, h * 0.09f))
    if (id == "canon") {
        drawCircle(Color(0xFF5D4037), h * 0.2f, Offset(cx - h * 0.05f, footY - h * 0.2f))
        drawCircle(Color(0xFF8D6E63), h * 0.08f, Offset(cx - h * 0.05f, footY - h * 0.2f))
        rotate(-18f, Offset(cx, footY - h * 0.35f)) {
            drawRoundRect(Color(0xFF546E7A), Offset(cx - h * 0.3f - recoil * h * 0.08f, footY - h * 0.55f), Size(h * 0.62f, h * 0.3f), CornerRadius(h * 0.12f))
            drawRoundRect(Color(0xFF37474F), Offset(cx + h * 0.24f - recoil * h * 0.08f, footY - h * 0.58f), Size(h * 0.1f, h * 0.36f), CornerRadius(h * 0.04f))
        }
        return
    }
    if (id != "cactus" && id != "champignon") {
        drawLine(stem, Offset(cx, footY - h * 0.05f), Offset(head.x, head.y + r * 0.6f), h * 0.07f, StrokeCap.Round)
        drawOval(stem, Offset(cx - h * 0.26f, footY - h * 0.24f), Size(h * 0.24f, h * 0.12f))
        drawOval(stem, Offset(cx + h * 0.02f, footY - h * 0.2f), Size(h * 0.24f, h * 0.12f))
    }
    when (id) {
        "tulipe" -> {
            val pink = Color(0xFFEC6FA0)
            for (i in -1..1) {
                val p = Path().apply {
                    moveTo(head.x + i * r * 0.55f - r * 0.45f, head.y)
                    lineTo(head.x + i * r * 0.75f, head.y - r * 1.5f)
                    lineTo(head.x + i * r * 0.55f + r * 0.45f, head.y)
                    close()
                }
                drawPath(p, if (i == 0) pink else lerp(pink, Color.Black, 0.15f))
            }
            drawCircle(pink, r, head)
            face(head, r, recoil)
        }
        "cactus" -> {
            val g = Color(0xFF43A047)
            drawLine(g, Offset(cx - h * 0.3f, footY - h * 0.42f), Offset(cx - h * 0.3f, footY - h * 0.6f), h * 0.1f, StrokeCap.Round)
            drawLine(g, Offset(cx - h * 0.3f, footY - h * 0.42f), Offset(cx, footY - h * 0.42f), h * 0.1f, StrokeCap.Round)
            drawRoundRect(g, Offset(head.x - r * 0.95f, footY - h * 0.98f), Size(r * 1.9f, h * 0.95f), CornerRadius(r * 0.95f))
            drawRoundRect(Color(0xFF2E7D32), Offset(head.x - r * 0.95f, footY - h * 0.98f), Size(r * 1.9f, h * 0.95f), CornerRadius(r * 0.95f), style = Stroke(h * 0.02f))
            drawCircle(Color(0xFFEC6FA0), r * 0.3f, Offset(head.x, footY - h * 0.98f))
            face(head, r, recoil)
        }
        "champignon" -> {
            drawRoundRect(Color(0xFFF5E6CA), Offset(head.x - r * 0.75f, footY - h * 0.6f), Size(r * 1.5f, h * 0.58f), CornerRadius(r * 0.5f))
            drawArc(Color(0xFFE2574C), 180f, 180f, true, Offset(head.x - r * 1.35f, footY - h * 1.0f), Size(r * 2.7f, h * 0.8f))
            drawCircle(Color.White, r * 0.22f, Offset(head.x - r * 0.6f, footY - h * 0.8f))
            drawCircle(Color.White, r * 0.28f, Offset(head.x + r * 0.3f, footY - h * 0.85f))
            face(Offset(head.x, footY - h * 0.32f), r * 0.85f, recoil)
        }
        else -> { // tournesol
            for (i in 0 until 12) {
                val a = i * PI / 6
                drawCircle(Color(0xFFFFC928), r * 0.36f, Offset(head.x + r * 1.05f * cos(a).toFloat(), head.y + r * 1.05f * sin(a).toFloat()))
            }
            drawCircle(Color(0xFF8D5A2B), r, head)
            face(head, r, recoil)
        }
    }
}

fun DrawScope.projectile(id: String, c: Offset, r: Float, spin: Float) {
    when (id) {
        "etoile" -> rotate(spin * 200f, c) { star(c, r * 1.3f, Color(0xFFFFD54F)) }
        "coeur" -> heart(c, r * 0.9f, Color(0xFFE2574C))
        "tomate" -> {
            drawCircle(Color(0xFFE53935), r, c)
            drawCircle(Color(0xFF2E7D32), r * 0.3f, Offset(c.x, c.y - r * 0.85f))
            drawCircle(Color.White.copy(alpha = 0.5f), r * 0.22f, Offset(c.x - r * 0.35f, c.y - r * 0.3f))
        }
        "neige" -> {
            drawCircle(Color(0xFFB3E5FC), r, c)
            drawCircle(Color.White, r * 0.8f, Offset(c.x - r * 0.1f, c.y - r * 0.1f))
        }
        "feu" -> {
            for (i in 1..3) drawCircle(Color(0xFFFF7043).copy(alpha = 0.3f), r * (1f - i * 0.15f), Offset(c.x - i * r * 0.7f, c.y))
            drawCircle(Color(0xFFFF5722), r, c)
            drawCircle(Color(0xFFFFEB3B), r * 0.55f, c)
        }
        else -> {
            drawCircle(Color(0xFF2E7D32), r, c)
            drawCircle(Color(0xFF7CD54A), r * 0.85f, c)
            drawCircle(Color.White.copy(alpha = 0.6f), r * 0.22f, Offset(c.x - r * 0.35f, c.y - r * 0.35f))
        }
    }
}

/** Confetti burst where a zombie popped. [t] goes 0→1. */
fun DrawScope.pop(c: Offset, r: Float, t: Float, big: Boolean) {
    val colors = listOf(Color(0xFFFFD54F), Color(0xFFEC6FA0), Color(0xFF4FC3F7), Color(0xFF9CCC65), Color(0xFFFF8A3D))
    val n = if (big) 22 else 12
    for (i in 0 until n) {
        val a = i * 2 * PI / n + (i % 3) * 0.3
        val d = r * (0.3f + t * (1.1f + (i % 4) * 0.25f)) * if (big) 1.8f else 1f
        val p = Offset(c.x + d * cos(a).toFloat(), c.y + d * sin(a).toFloat() + t * t * r * 0.8f)
        drawCircle(colors[i % colors.size].copy(alpha = (1f - t).coerceIn(0f, 1f)), r * 0.13f * (1f - t * 0.5f), p)
    }
    if (t < 0.3f) drawCircle(Color.White.copy(alpha = 0.8f * (1f - t / 0.3f)), r * (0.4f + t * 2f), c)
}

/** Round animal (or robot) face used as the player's avatar. */
fun DrawScope.avatar(id: String, c: Offset, r: Float) {
    val fr = r * 0.72f
    fun ears(color: Color, inner: Color, pointed: Boolean, spread: Float = 0.62f, up: Float = 0.62f, size: Float = 0.36f) {
        for (sx in listOf(-1f, 1f)) {
            val e = Offset(c.x + sx * fr * spread, c.y - fr * up)
            if (pointed) {
                val p = Path().apply {
                    moveTo(e.x - fr * size, e.y + fr * 0.3f)
                    lineTo(e.x + sx * fr * 0.12f, e.y - fr * size * 1.5f)
                    lineTo(e.x + fr * size, e.y + fr * 0.3f)
                    close()
                }
                drawPath(p, color)
                scale(0.55f, 0.55f, Offset(e.x, e.y)) { drawPath(p, inner) }
            } else {
                drawCircle(color, fr * size, e)
                drawCircle(inner, fr * size * 0.55f, e)
            }
        }
    }
    fun eyes(dy: Float = -0.12f, size: Float = 0.13f) {
        for (sx in listOf(-1f, 1f)) {
            val e = Offset(c.x + sx * fr * 0.36f, c.y + fr * dy)
            drawCircle(INK, fr * size, e)
            drawCircle(Color.White, fr * size * 0.35f, Offset(e.x - fr * 0.04f, e.y - fr * 0.04f))
        }
    }
    fun smile(y: Float = 0.3f) {
        drawArc(INK, 20f, 140f, false, Offset(c.x - fr * 0.25f, c.y + fr * (y - 0.2f)), Size(fr * 0.5f, fr * 0.36f), style = Stroke(fr * 0.07f, cap = StrokeCap.Round))
    }
    fun nose(color: Color = INK, y: Float = 0.14f) = drawOval(color, Offset(c.x - fr * 0.11f, c.y + fr * y - fr * 0.07f), Size(fr * 0.22f, fr * 0.15f))
    fun head(color: Color) {
        drawCircle(color, fr, c)
        drawCircle(lerp(color, Color.Black, 0.3f), fr, c, style = Stroke(fr * 0.05f))
    }

    when (id) {
        "chien" -> {
            val col = Color(0xFFD2A06D)
            head(col)
            for (sx in listOf(-1f, 1f)) drawOval(Color(0xFF7A5230), Offset(c.x + sx * fr * 0.85f - fr * 0.25f, c.y - fr * 0.75f), Size(fr * 0.5f, fr * 1.05f))
            drawOval(Color(0xFFF5E6CA), Offset(c.x - fr * 0.42f, c.y), Size(fr * 0.84f, fr * 0.7f))
            eyes(); nose(); smile(0.38f)
        }
        "lapin" -> {
            for (sx in listOf(-1f, 1f)) {
                drawOval(Color(0xFFF2F2F2), Offset(c.x + sx * fr * 0.38f - fr * 0.2f, c.y - fr * 1.7f), Size(fr * 0.4f, fr * 1.2f))
                drawOval(Color(0xFFF8BBD0), Offset(c.x + sx * fr * 0.38f - fr * 0.1f, c.y - fr * 1.55f), Size(fr * 0.2f, fr * 0.85f))
            }
            head(Color(0xFFF2F2F2))
            eyes(); nose(Color(0xFFEC6FA0)); smile()
        }
        "ours" -> {
            ears(Color(0xFF9C6B3F), Color(0xFFD9A066), false)
            head(Color(0xFF9C6B3F))
            drawOval(Color(0xFFD9A066), Offset(c.x - fr * 0.38f, c.y + fr * 0.02f), Size(fr * 0.76f, fr * 0.6f))
            eyes(); nose(); smile(0.36f)
        }
        "panda" -> {
            ears(INK, INK, false)
            head(Color(0xFFFAFAFA))
            for (sx in listOf(-1f, 1f)) drawOval(INK, Offset(c.x + sx * fr * 0.36f - fr * 0.22f, c.y - fr * 0.36f), Size(fr * 0.44f, fr * 0.5f))
            for (sx in listOf(-1f, 1f)) drawCircle(Color.White, fr * 0.09f, Offset(c.x + sx * fr * 0.36f, c.y - fr * 0.12f))
            nose(); smile()
        }
        "grenouille" -> {
            val g = Color(0xFF7CC576)
            for (sx in listOf(-1f, 1f)) drawCircle(g, fr * 0.36f, Offset(c.x + sx * fr * 0.48f, c.y - fr * 0.72f))
            head(g)
            for (sx in listOf(-1f, 1f)) eye(Offset(c.x + sx * fr * 0.48f, c.y - fr * 0.72f), fr * 0.24f)
            drawArc(INK, 10f, 160f, false, Offset(c.x - fr * 0.6f, c.y - fr * 0.3f), Size(fr * 1.2f, fr * 0.8f), style = Stroke(fr * 0.07f, cap = StrokeCap.Round))
            for (sx in listOf(-1f, 1f)) drawCircle(Color(0xFFF8BBD0), fr * 0.1f, Offset(c.x + sx * fr * 0.62f, c.y + fr * 0.1f))
        }
        "renard" -> {
            ears(Color(0xFFEF7F3A), INK, true)
            head(Color(0xFFEF7F3A))
            for (sx in listOf(-1f, 1f)) drawOval(Color.White, Offset(c.x + sx * fr * 0.42f - fr * 0.36f, c.y + fr * 0.02f), Size(fr * 0.72f, fr * 0.72f))
            eyes(); nose(); smile(0.36f)
        }
        "lion" -> {
            for (i in 0 until 14) {
                val a = i * 2 * PI / 14
                drawCircle(Color(0xFFD9822B), fr * 0.34f, Offset(c.x + fr * 1.02f * cos(a).toFloat(), c.y + fr * 1.02f * sin(a).toFloat()))
            }
            head(Color(0xFFFFCC66))
            drawOval(Color(0xFFFFF3D6), Offset(c.x - fr * 0.36f, c.y + fr * 0.04f), Size(fr * 0.72f, fr * 0.56f))
            eyes(); nose(Color(0xFF8D5A2B)); smile(0.36f)
        }
        "pingouin" -> {
            head(Color(0xFF37474F))
            drawOval(Color.White, Offset(c.x - fr * 0.7f, c.y - fr * 0.45f), Size(fr * 1.4f, fr * 1.3f))
            eyes()
            val p = Path().apply {
                moveTo(c.x - fr * 0.2f, c.y + fr * 0.1f)
                lineTo(c.x + fr * 0.2f, c.y + fr * 0.1f)
                lineTo(c.x, c.y + fr * 0.42f)
                close()
            }
            drawPath(p, Color(0xFFFF9800))
        }
        "robot" -> {
            drawLine(Color(0xFF78909C), Offset(c.x, c.y - fr * 0.8f), Offset(c.x, c.y - fr * 1.25f), fr * 0.08f)
            drawCircle(Color(0xFFE2574C), fr * 0.13f, Offset(c.x, c.y - fr * 1.3f))
            drawRoundRect(Color(0xFFB0BEC5), Offset(c.x - fr * 0.9f, c.y - fr * 0.8f), Size(fr * 1.8f, fr * 1.6f), CornerRadius(fr * 0.3f))
            drawRoundRect(Color(0xFF607D8B), Offset(c.x - fr * 0.9f, c.y - fr * 0.8f), Size(fr * 1.8f, fr * 1.6f), CornerRadius(fr * 0.3f), style = Stroke(fr * 0.06f))
            for (sx in listOf(-1f, 1f)) {
                drawCircle(Color(0xFF4FC3F7), fr * 0.22f, Offset(c.x + sx * fr * 0.4f, c.y - fr * 0.15f))
                drawCircle(INK, fr * 0.09f, Offset(c.x + sx * fr * 0.4f, c.y - fr * 0.15f))
            }
            drawRoundRect(INK, Offset(c.x - fr * 0.4f, c.y + fr * 0.3f), Size(fr * 0.8f, fr * 0.16f), CornerRadius(fr * 0.08f))
        }
        "licorne" -> {
            val horn = Path().apply {
                moveTo(c.x - fr * 0.18f, c.y - fr * 0.8f)
                lineTo(c.x, c.y - fr * 1.75f)
                lineTo(c.x + fr * 0.18f, c.y - fr * 0.8f)
                close()
            }
            drawPath(horn, Color(0xFFFFD54F))
            ears(Color.White, Color(0xFFF8BBD0), true, spread = 0.7f)
            head(Color(0xFFFDFDFD))
            val mane = listOf(Color(0xFFEC6FA0), Color(0xFFBA68C8), Color(0xFF4FC3F7))
            mane.forEachIndexed { i, col -> drawCircle(col, fr * 0.26f, Offset(c.x - fr * 0.85f, c.y - fr * 0.4f + i * fr * 0.42f)) }
            eyes(); nose(Color(0xFFEC6FA0), 0.2f); smile(0.36f)
        }
        "dragon" -> {
            val g = Color(0xFF4DB6AC)
            for (sx in listOf(-1f, 1f)) {
                val p = Path().apply {
                    moveTo(c.x + sx * fr * 0.25f, c.y - fr * 0.8f)
                    lineTo(c.x + sx * fr * 0.6f, c.y - fr * 1.5f)
                    lineTo(c.x + sx * fr * 0.75f, c.y - fr * 0.6f)
                    close()
                }
                drawPath(p, Color(0xFFFFD54F))
            }
            head(g)
            drawOval(Color(0xFF80CBC4), Offset(c.x - fr * 0.5f, c.y + fr * 0.05f), Size(fr * 1.0f, fr * 0.7f))
            for (sx in listOf(-1f, 1f)) eye(Offset(c.x + sx * fr * 0.36f, c.y - fr * 0.2f), fr * 0.2f)
            for (sx in listOf(-1f, 1f)) drawCircle(INK, fr * 0.06f, Offset(c.x + sx * fr * 0.14f, c.y + fr * 0.25f))
            smile(0.5f)
        }
        else -> { // chat
            ears(Color(0xFFF6A04D), Color(0xFFF8BBD0), true)
            head(Color(0xFFF6A04D))
            eyes(); nose(Color(0xFFEC6FA0)); smile()
            for (sx in listOf(-1f, 1f)) for (dy in listOf(-0.05f, 0.1f)) {
                drawLine(INK, Offset(c.x + sx * fr * 0.5f, c.y + fr * (0.22f + dy)), Offset(c.x + sx * fr * 0.95f, c.y + fr * (0.18f + dy * 2.5f)), fr * 0.035f, StrokeCap.Round)
            }
        }
    }
}

class WorldLook(val skyTop: Color, val skyBottom: Color, val grassA: Color, val grassB: Color, val night: Boolean = false)

val WORLD_LOOKS = listOf(
    WorldLook(Color(0xFF7FD3F7), Color(0xFFCDEFFF), Color(0xFF8BD05A), Color(0xFF7CC24C)), // jardin
    WorldLook(Color(0xFF58C6F2), Color(0xFFBFEFFF), Color(0xFFF6DD9A), Color(0xFFEFD083)), // plage
    WorldLook(Color(0xFF8ED1B0), Color(0xFFD8F3DC), Color(0xFF4F9D5B), Color(0xFF468F52)), // forêt
    WorldLook(Color(0xFF9CC9F0), Color(0xFFE8F4FF), Color(0xFFF3F8FC), Color(0xFFE2EDF5)), // montagne
    WorldLook(Color(0xFFFFC56B), Color(0xFFFFE7B8), Color(0xFFE9B872), Color(0xFFDFAB62)), // désert
    WorldLook(Color(0xFF1B2452), Color(0xFF3E4A8C), Color(0xFF3F7D5A), Color(0xFF38714F), night = true), // nuit
    WorldLook(Color(0xFF14102E), Color(0xFF3A2466), Color(0xFF7E6BB5), Color(0xFF7160A8), night = true), // espace
    WorldLook(Color(0xFF7A2E2E), Color(0xFFF08A4B), Color(0xFF8A5A44), Color(0xFF7D4F3B)), // volcan
)

/** Sky, striped lawn and the house to protect. Returns nothing; geometry comes from [FieldGeometry]. */
fun DrawScope.field(look: WorldLook, g: FieldGeometry) {
    drawRect(Brush.verticalGradient(listOf(look.skyTop, look.skyBottom), 0f, g.top), Offset.Zero, Size(size.width, g.top + 1f))
    if (look.night) {
        for (i in 0 until 24) drawCircle(Color.White.copy(alpha = 0.8f), size.height * 0.004f, Offset((i * 137 % 100) / 100f * size.width, (i * 53 % 100) / 100f * g.top * 0.9f))
        drawCircle(Color(0xFFFFF4C2), g.top * 0.3f, Offset(size.width * 0.86f, g.top * 0.45f))
    } else {
        drawCircle(Color(0xFFFFE066), g.top * 0.32f, Offset(size.width * 0.88f, g.top * 0.45f))
        for (cx in listOf(0.38f, 0.63f)) {
            val c = Offset(size.width * cx, g.top * 0.5f)
            drawCircle(Color.White.copy(alpha = 0.85f), g.top * 0.22f, c)
            drawCircle(Color.White.copy(alpha = 0.85f), g.top * 0.17f, Offset(c.x - g.top * 0.26f, c.y + g.top * 0.06f))
            drawCircle(Color.White.copy(alpha = 0.85f), g.top * 0.17f, Offset(c.x + g.top * 0.26f, c.y + g.top * 0.06f))
        }
    }
    for (lane in 0 until g.lanes) {
        drawRect(if (lane % 2 == 0) look.grassA else look.grassB, Offset(0f, g.top + lane * g.laneH), Size(size.width, g.laneH + 1f))
        // Mowing stripes
        var x = g.x0
        var k = 0
        while (x < size.width) {
            if (k % 2 == 0) drawRect(Color.White.copy(alpha = 0.06f), Offset(x, g.top + lane * g.laneH), Size(g.laneH * 0.6f, g.laneH))
            x += g.laneH * 0.6f
            k++
        }
    }
    // House strip
    val wall = Color(0xFFFFE0B2)
    val w = g.x0 * 0.42f
    drawRect(wall, Offset(0f, g.top), Size(w, size.height - g.top))
    drawRect(Color(0xFFD7A86E), Offset(w - size.height * 0.012f, g.top), Size(size.height * 0.012f, size.height - g.top))
    val roof = Path().apply {
        moveTo(0f, g.top)
        lineTo(0f, g.top * 0.25f)
        lineTo(w * 1.25f, g.top)
        close()
    }
    drawPath(roof, Color(0xFFD9534F))
    for (lane in 0 until g.lanes) {
        val cy = g.top + (lane + 0.5f) * g.laneH
        drawRoundRect(Color(0xFF81D4FA), Offset(w * 0.2f, cy - g.laneH * 0.22f), Size(w * 0.5f, g.laneH * 0.4f), CornerRadius(w * 0.08f))
        drawRoundRect(Color(0xFF8D6E63), Offset(w * 0.2f, cy - g.laneH * 0.22f), Size(w * 0.5f, g.laneH * 0.4f), CornerRadius(w * 0.08f), style = Stroke(w * 0.05f))
    }
}

/** Maps the engine's 0..1 lawn onto the canvas. */
class FieldGeometry(val width: Float, val height: Float, val lanes: Int) {
    val top = height * 0.17f
    val laneH = (height - top) / lanes
    /** Canvas x of the house line (engine x = 0) and of the spawn edge (engine x = 1). */
    val x0 = width * 0.19f
    val x1 = width * 1.04f
    val launcherX = width * 0.125f
    fun x(e: Float) = x0 + (x1 - x0) * e
    fun footY(lane: Int) = top + (lane + 0.92f) * laneH
}
