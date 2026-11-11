package be.arte.mathforkids.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.arte.mathforkids.data.Profile
import be.arte.mathforkids.game.ZombieType
import be.arte.mathforkids.math.Curriculum
import be.arte.mathforkids.math.Level
import be.arte.mathforkids.math.Mode
import kotlin.math.sin

@Composable
fun MapScreen(profile: Profile, worldIndex: Int, onWorld: (Int) -> Unit, onBack: () -> Unit, onPlay: (Level) -> Unit) {
    val world = Curriculum.worlds[worldIndex.coerceIn(0, Curriculum.worlds.lastIndex)]
    val look = WORLD_LOOKS[world.index % WORLD_LOOKS.size]
    val startWorld = Curriculum.grades[profile.grade.coerceIn(0, Curriculum.grades.lastIndex)].second
    val worldStars = world.levels.sumOf { profile.stars[it.id] ?: 0 }
    val open = Curriculum.isUnlocked(world.levels[0], startWorld, profile.stars)

    Page(
        "Monde ${world.index + 1} — ${world.name}", onBack,
        trailing = {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.9f)).padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StarIcon(28.dp)
                Label(" $worldStars / ${world.levels.size * 3}", size = 22.sp)
            }
        },
        background = Brush.verticalGradient(listOf(look.skyTop, look.skyBottom)),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GameButton("◀", Pal.orange, { onWorld(world.index - 1) }, enabled = world.index > 0, padding = PaddingValues(horizontal = 14.dp, vertical = 30.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                BoxWithConstraints(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(26.dp)).border(4.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(26.dp)),
                ) {
                    val w = maxWidth
                    val h = maxHeight
                    val n = world.levels.size
                    fun nodeX(i: Int): Dp = w * ((i + 0.6f) / (n + 0.4f))
                    fun nodeY(i: Int): Dp = h * (0.56f + 0.2f * sin(i * 1.25f + world.index))
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Brush.verticalGradient(listOf(look.skyBottom, look.grassA, look.grassB), 0f, size.height))
                        drawRect(look.grassA, Offset(0f, size.height * 0.3f), Size(size.width, size.height * 0.7f))
                        for (k in 0 until 9) drawOval(
                            look.grassB, Offset(size.width * (k * 0.13f - 0.05f), size.height * (0.24f + (k % 3) * 0.02f)),
                            Size(size.width * 0.22f, size.height * 0.16f),
                        )
                        for (i in 0 until n - 1) {
                            drawLine(
                                Color.White.copy(alpha = 0.85f),
                                Offset(nodeX(i).toPx(), nodeY(i).toPx()), Offset(nodeX(i + 1).toPx(), nodeY(i + 1).toPx()),
                                strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 18.dp.toPx())),
                            )
                        }
                    }
                    Label(world.place, Modifier.padding(16.dp), size = 26.sp, color = if (look.night) Color.White else Pal.ink)
                    if (!open) {
                        Box(
                            Modifier.align(Alignment.TopCenter).padding(top = 60.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.9f)).padding(12.dp),
                        ) { Label("Bats le boss du monde précédent pour ouvrir ce monde !", size = 20.sp) }
                    }
                    for (level in world.levels) {
                        val unlocked = Curriculum.isUnlocked(level, startWorld, profile.stars)
                        val stars = profile.stars[level.id] ?: 0
                        val d = if (level.boss) 96.dp else 70.dp
                        Column(
                            Modifier.offset(nodeX(level.index) - d / 2, nodeY(level.index) - d / 2),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LevelNode(level, unlocked, stars > 0, d) { onPlay(level) }
                            if (unlocked) Stars(stars, if (level.boss) 26.dp else 21.dp) else Spacer(Modifier.height(21.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    for (wld in Curriculum.worlds) {
                        Box(
                            Modifier.padding(horizontal = 5.dp).size(if (wld.index == world.index) 22.dp else 14.dp).clip(CircleShape)
                                .background(if (wld.index == world.index) Pal.orange else Color.White.copy(alpha = 0.8f))
                                .clickable { onWorld(wld.index) },
                        )
                    }
                }
            }
            GameButton("▶", Pal.orange, { onWorld(world.index + 1) }, enabled = world.index < Curriculum.worlds.lastIndex, padding = PaddingValues(horizontal = 14.dp, vertical = 30.dp))
        }
    }
}

@Composable
private fun LevelNode(level: Level, unlocked: Boolean, done: Boolean, d: Dp, onClick: () -> Unit) {
    val color = when {
        !unlocked -> Pal.grey
        level.boss -> Pal.purple
        level.mode == Mode.CHOICE -> Pal.green
        level.mode == Mode.KEYPAD -> Pal.blue
        else -> Pal.orange
    }
    Canvas(
        Modifier.size(d).clip(CircleShape).clickable(enabled = unlocked, onClick = onClick),
    ) {
        val r = size.minDimension / 2
        drawCircle(lerp(color, Color.Black, 0.3f), r, center)
        drawCircle(color, r * 0.9f, Offset(center.x, center.y - r * 0.06f))
        drawCircle(Color.White.copy(alpha = if (done) 0.95f else 0.6f), r * 0.9f, Offset(center.x, center.y - r * 0.06f), style = Stroke(r * 0.1f))
        when {
            !unlocked -> { // padlock
                drawArc(Color.White, 180f, 180f, false, Offset(center.x - r * 0.22f, center.y - r * 0.45f), Size(r * 0.44f, r * 0.5f), style = Stroke(r * 0.1f))
                drawRoundRect(Color.White, Offset(center.x - r * 0.33f, center.y - r * 0.2f), Size(r * 0.66f, r * 0.5f), CornerRadius(r * 0.08f))
            }
            level.boss -> zombie(center.x + r * 0.08f, center.y + r * 0.7f, r * 1.25f, ZombieType.BOSS, 0f, false)
            else -> text("${level.index + 1}", center.x, center.y - r * 0.04f, r * 0.95f, Color.White)
        }
    }
}
