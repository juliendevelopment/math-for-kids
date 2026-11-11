package be.arte.mathforkids.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import be.arte.mathforkids.audio.AudioEngine
import be.arte.mathforkids.audio.Sfx
import be.arte.mathforkids.data.GameOutcome
import be.arte.mathforkids.data.Reward
import be.arte.mathforkids.game.GameConfig
import be.arte.mathforkids.game.GameEngine
import be.arte.mathforkids.game.GameEvent
import be.arte.mathforkids.game.ZombieType
import be.arte.mathforkids.math.Mode
import kotlin.math.sin

/** What the bottom panel needs from the engine; compared by value so it recomposes only on change. */
private data class Hud(
    val hearts: Int = GameEngine.MAX_HEARTS,
    val progress: Int = 0,
    val question: String? = null,
    val choices: List<Int> = emptyList(),
    val wrongChoices: Set<Int> = emptySet(),
    val finished: Boolean = false,
)

data class GameRequest(
    val config: GameConfig,
    /** Adventure level id, null for training. */
    val levelId: String?,
    val title: String,
    val world: Int,
)

@Composable
fun GameScreen(
    request: GameRequest,
    launcherId: String,
    projectileId: String,
    audio: AudioEngine,
    onResult: (GameOutcome) -> Reward?,
    hasNext: Boolean,
    onNext: () -> Unit,
    onExit: () -> Unit,
) {
    var round by remember { mutableIntStateOf(0) }
    key(round) {
        GameRound(request, launcherId, projectileId, audio, onResult, hasNext, onNext, onExit, onReplay = { round++ })
    }
}

@Composable
private fun GameRound(
    request: GameRequest,
    launcherId: String,
    projectileId: String,
    audio: AudioEngine,
    onResult: (GameOutcome) -> Reward?,
    hasNext: Boolean,
    onNext: () -> Unit,
    onExit: () -> Unit,
    onReplay: () -> Unit,
) {
    val config = request.config
    val engine = remember { GameEngine(config) }
    var frame by remember { mutableLongStateOf(0L) }
    var hud by remember { mutableStateOf(Hud()) }
    var paused by remember { mutableStateOf(false) }
    var entry by remember { mutableStateOf("") }
    var wrongFlash by remember { mutableLongStateOf(-10_000L) }
    var reward by remember { mutableStateOf<Reward?>(null) }
    var outcome by remember { mutableStateOf<GameOutcome?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { if (!engine.finished) paused = true }
    BackHandler { if (engine.finished) onExit() else paused = !paused }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now
                if (!paused) {
                    engine.update(dt)
                    for (e in engine.events) when (e) {
                        GameEvent.SHOOT -> audio.play(Sfx.SHOOT)
                        GameEvent.POP, GameEvent.BOSS_HIT -> audio.play(Sfx.POP)
                        GameEvent.WRONG -> {
                            audio.play(Sfx.WRONG)
                            wrongFlash = frame
                        }
                        GameEvent.HEART -> audio.play(Sfx.HEART)
                        GameEvent.WIN -> audio.play(Sfx.WIN)
                        GameEvent.LOSE -> audio.play(Sfx.LOSE)
                        GameEvent.BOSS_SPAWN -> audio.play(Sfx.BOSS)
                    }
                    engine.events.clear()
                    frame++
                    val a = if (config.mode == Mode.MULTI) null else engine.active
                    hud = Hud(
                        engine.hearts, (engine.progress * 200).toInt(), a?.problem?.text,
                        a?.choices ?: emptyList(), a?.wrongChoices?.toSet() ?: emptySet(), engine.finished,
                    )
                    if (engine.finished && outcome == null) {
                        val o = GameOutcome(
                            levelId = request.levelId, won = engine.won, stars = engine.stars, kills = engine.kills,
                            correct = engine.correct, wrong = engine.wrong, seconds = engine.time.toInt(),
                            bossBeaten = engine.won && config.boss, missed = engine.missed.toList(), solved = engine.solved.toList(),
                        )
                        outcome = o
                        reward = onResult(o)
                    }
                }
            }
        }
    }

    fun submit(n: Int) {
        if (paused || engine.finished) return
        engine.answer(n)
        entry = ""
    }

    val look = WORLD_LOOKS[request.world % WORLD_LOOKS.size]
    Box(Modifier.fillMaxSize().background(Color(0xFF2B2B3A))) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Canvas(Modifier.fillMaxSize()) {
                    if (frame < 0L) return@Canvas // reading the frame counter redraws every simulated frame
                    drawGame(engine, look, launcherId, projectileId, config.mode)
                }
                // HUD over the sky
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 10.dp, vertical = 4.dp),
                    ) { for (i in 1..GameEngine.MAX_HEARTS) HeartIcon(34.dp, filled = i <= hud.hearts) }
                    Spacer(Modifier.width(16.dp))
                    ProgressBar(hud.progress / 200f, Modifier.weight(1f).height(26.dp))
                    Spacer(Modifier.width(16.dp))
                    GameButton("II", Pal.orange, { paused = true }, fontSize = 20.sp, padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp))
                }
            }
            AnswerPanel(config.mode, hud, entry, flashing = frame - wrongFlash < 20, onEntry = { entry = it }, onSubmit = ::submit)
        }

        if (paused && !hud.finished) {
            Modal {
                Label("Pause", size = 36.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Label(request.title, size = 18.sp, color = Pal.grey, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                GameButton("Reprendre", Pal.green, { paused = false }, Modifier.width(300.dp))
                Spacer(Modifier.height(10.dp))
                GameButton("Recommencer", Pal.blue, onReplay, Modifier.width(300.dp))
                Spacer(Modifier.height(10.dp))
                GameButton("Quitter", Pal.red, onExit, Modifier.width(300.dp))
            }
        }
        val o = outcome
        if (o != null) ResultOverlay(o, reward, request.levelId != null, hasNext, onNext, onReplay, onExit)
    }
}

@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier) {
    Canvas(modifier) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(Color.White.copy(alpha = 0.85f), size = size, cornerRadius = r)
        val w = (size.width - 8f) * fraction.coerceIn(0f, 1f)
        if (w > size.height / 2) drawRoundRect(Pal.green, Offset(4f, 4f), Size(w, size.height - 8f), r)
        drawRoundRect(Color.Black.copy(alpha = 0.25f), size = size, cornerRadius = r, style = Stroke(3f))
    }
}

@Composable
private fun AnswerPanel(mode: Mode, hud: Hud, entry: String, flashing: Boolean, onEntry: (String) -> Unit, onSubmit: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(176.dp).background(Color(0xFF3B3B52)).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val question = hud.question
        if (mode == Mode.CHOICE) {
            QuestionBox(question?.let { if ('?' in it) it else "$it = ?" } ?: "…", flashing, Modifier.weight(1f).fillMaxHeight())
            for (c in hud.choices) {
                GameButton(
                    "$c", listOf(Pal.blue, Pal.pink, Pal.purple)[hud.choices.indexOf(c) % 3], { onSubmit(c) },
                    Modifier.width(150.dp).fillMaxHeight().padding(vertical = 14.dp),
                    enabled = c !in hud.wrongChoices, fontSize = 52.sp,
                )
            }
        } else {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mode == Mode.KEYPAD) {
                    QuestionBox(question?.let { if ('?' in it) it else "$it =" } ?: "…", false, Modifier.fillMaxWidth().weight(1f), small = true)
                } else {
                    Label("Tape la réponse d'un zombie !", size = 20.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                }
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp))
                        .background(if (flashing) Color(0xFFFFCDD2) else Color.White)
                        .border(4.dp, if (flashing) Pal.red else Pal.blue, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) { Label(entry.ifEmpty { "?" }, size = 44.sp, color = if (entry.isEmpty()) Pal.grey else Pal.ink) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in listOf("12345", "67890")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (d in row) {
                            GameButton("$d", Pal.blue, { if (entry.length < 4) onEntry(entry + d) }, Modifier.width(78.dp).height(74.dp), fontSize = 34.sp, padding = PaddingValues(0.dp))
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("⌫", Pal.orange, { onEntry(entry.dropLast(1)) }, Modifier.width(100.dp).height(74.dp), fontSize = 30.sp, padding = PaddingValues(0.dp))
                GameButton("OK", Pal.green, { entry.toIntOrNull()?.let(onSubmit) }, Modifier.width(100.dp).height(74.dp), enabled = entry.isNotEmpty(), fontSize = 30.sp, padding = PaddingValues(0.dp))
            }
        }
    }
}

@Composable
private fun QuestionBox(text: String, flashing: Boolean, modifier: Modifier, small: Boolean = false) {
    Box(
        modifier.clip(RoundedCornerShape(18.dp)).background(if (flashing) Color(0xFFFFCDD2) else Pal.paper),
        contentAlignment = Alignment.Center,
    ) {
        val size = if (small) (if (text.length > 11) 26.sp else if (text.length > 8) 32.sp else 40.sp) else if (text.length > 12) 46.sp else 60.sp
        Label(text, size = size, align = TextAlign.Center)
    }
}

@Composable
private fun ResultOverlay(o: GameOutcome, reward: Reward?, adventure: Boolean, hasNext: Boolean, onNext: () -> Unit, onReplay: () -> Unit, onExit: () -> Unit) {
    Modal {
        Column(Modifier.widthIn(min = 460.dp, max = 620.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Label(if (o.won) "Bravo !" else "Les zombies sont passés…", size = 40.sp, color = if (o.won) Pal.green else Pal.red)
            if (o.won && adventure) Stars(o.stars, 76.dp)
            if (!o.won) Label("Ce n'est pas grave, on réessaie !", size = 20.sp)
            Spacer(Modifier.height(6.dp))
            val total = o.correct + o.wrong
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Label("${o.kills} zombies", size = 22.sp)
                if (total > 0) Label("${o.correct * 100 / total} % de bonnes réponses", size = 22.sp)
                if (reward != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Label("+${reward.coins} ", size = 22.sp, color = Color(0xFFB8860B))
                    CoinIcon(28.dp)
                }
            }
            if (reward != null && reward.newBadges.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (b in reward.newBadges.take(3)) {
                        BadgeMedal(b.symbol, true, 48.dp)
                        Label(b.name, size = 18.sp, color = Pal.purple)
                    }
                }
            }
            val review = o.missed.distinctBy { it.text }.take(4)
            if (review.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Label("À revoir :", size = 18.sp, color = Pal.grey)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (p in review) {
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF3C4)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Label(GameEngine.solution(p), size = 18.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameButton(if (adventure) "Carte" else "Retour", Pal.orange, onExit)
                GameButton("Rejouer", Pal.blue, onReplay)
                if (o.won && hasNext) GameButton("Suivant ▶", Pal.green, onNext)
            }
        }
    }
}

private fun DrawScope.drawGame(engine: GameEngine, look: WorldLook, launcherId: String, projectileId: String, mode: Mode) {
    val g = FieldGeometry(size.width, size.height, engine.config.lanes)
    field(look, g)
    val zh = g.laneH * 0.82f
    for (lane in 0 until g.lanes) {
        val recoil = (1f - engine.laneFired[lane] / 0.25f).coerceIn(0f, 1f)
        launcher(launcherId, g.launcherX, g.footY(lane), zh * 0.95f, recoil)
    }
    val active = if (mode == Mode.MULTI) null else engine.active
    val sorted = engine.zombies.sortedWith(compareBy({ it.lane }, { -it.x }))
    for (z in sorted) {
        val boss = z.type == ZombieType.BOSS
        val h = if (boss) zh * 1.45f else zh
        val x = g.x(z.x)
        val foot = g.footY(z.lane)
        zombie(x, foot, h, z.type, z.age * (if (z.type == ZombieType.FAST) 9f else 6f), z.hitFlash > 0f, if (z.alive) 0f else z.dying / 0.5f)
        if (!z.alive) continue
        if (boss) {
            val w = h * 0.6f
            drawRoundRect(Color.Black.copy(alpha = 0.4f), Offset(x - w / 2, foot + 4f), Size(w, g.laneH * 0.06f), CornerRadius(8f))
            drawRoundRect(Pal.red, Offset(x - w / 2, foot + 4f), Size(w * z.hp / z.maxHp, g.laneH * 0.06f), CornerRadius(8f))
        }
        // Speech bubble with the problem
        val showText = mode == Mode.MULTI || z === active
        if (!showText && !z.answerable) continue
        val label = if (showText) z.problem.text else "?"
        val ts = g.laneH * (if (showText) 0.2f else 0.16f)
        val tw = measureText(label, ts) + ts * 0.9f
        val by = (foot - h - ts * 0.9f).coerceAtLeast(ts * 0.9f)
        val bx = x.coerceIn(g.x0 + tw / 2, size.width - tw / 2 - 4f)
        val waiting = z.incoming >= z.hp
        val fill = if (waiting) Color(0xFFDFF5D0) else if (z === active) Color(0xFFFFF59D) else Color.White
        drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(bx - tw / 2, by - ts * 0.7f + 4f), Size(tw, ts * 1.4f), CornerRadius(ts * 0.5f))
        drawRoundRect(fill, Offset(bx - tw / 2, by - ts * 0.7f), Size(tw, ts * 1.4f), CornerRadius(ts * 0.5f))
        drawRoundRect(if (z === active) Pal.orange else Color(0xFF555566), Offset(bx - tw / 2, by - ts * 0.7f), Size(tw, ts * 1.4f), CornerRadius(ts * 0.5f), style = Stroke(ts * 0.09f))
        text(label, bx, by, ts, Pal.ink)
        if (z === active) {
            // Bouncing arrow so the child sees which zombie the question is about
            val ay = foot + g.laneH * 0.03f + sin(z.age * 6f) * 4f
            drawCircle(Pal.orange.copy(alpha = 0.35f), h * 0.3f, Offset(x, foot - h * 0.02f), style = Stroke(h * 0.04f))
            text("▲", x, ay + ts * 0.2f, ts * 0.6f, Pal.orange)
        }
    }
    for (s in engine.shots) {
        val y = g.footY(s.lane) - zh * 0.62f
        projectile(projectileId, Offset(g.x(s.x) - (g.x0 - g.launcherX) * (1f - s.x).coerceIn(0f, 1f) * 0.6f, y), zh * 0.11f, s.x)
    }
    for (p in engine.pops) pop(Offset(g.x(p.x), g.footY(p.lane) - zh * 0.5f), zh * 0.6f, p.age / 0.7f, p.big)
    for (f in engine.floaters) {
        val a = (1f - f.age / 2.4f).coerceIn(0f, 1f)
        val ts = g.laneH * 0.2f
        val y = g.footY(f.lane) - zh * 0.5f - f.age * g.laneH * 0.12f
        val w = measureText(f.text, ts) + ts
        val cx = (g.x(f.x)).coerceAtLeast(w / 2 + 8f)
        drawRoundRect((if (f.good) Pal.green else Pal.red).copy(alpha = a), Offset(cx - w / 2, y - ts * 0.75f), Size(w, ts * 1.5f), CornerRadius(ts * 0.5f))
        text(f.text, cx, y, ts, Color.White.copy(alpha = a))
    }
}
