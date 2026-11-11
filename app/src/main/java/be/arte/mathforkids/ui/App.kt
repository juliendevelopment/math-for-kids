package be.arte.mathforkids.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import be.arte.mathforkids.audio.AudioEngine
import be.arte.mathforkids.audio.Sfx
import be.arte.mathforkids.data.Profile
import be.arte.mathforkids.data.Rules
import be.arte.mathforkids.data.Store
import be.arte.mathforkids.data.TrainingSetup
import be.arte.mathforkids.data.toSpec
import be.arte.mathforkids.game.GameConfig
import be.arte.mathforkids.math.Curriculum
import be.arte.mathforkids.math.Kind
import be.arte.mathforkids.math.Level
import be.arte.mathforkids.math.Mode
import java.time.LocalDate

sealed interface Screen {
    data object Profiles : Screen
    data object Home : Screen
    data class Map(val world: Int) : Screen
    data class Game(val request: GameRequest) : Screen
    data object Training : Screen
    data object Shop : Screen
    data object Progress : Screen
}

/** The screen is always laid out as if it were this tall, so tablets and phones look alike. */
const val VIRTUAL_HEIGHT_DP = 600f
private const val MIN_VIRTUAL_WIDTH_DP = 800f

fun speedFactor(speed: Int): Float = when (speed) {
    0 -> 1.35f
    2 -> 0.75f
    else -> 1f
}

fun today(): Long = LocalDate.now().toEpochDay()

fun levelRequest(level: Level, p: Profile): GameRequest {
    val world = Curriculum.worlds[level.world]
    return GameRequest(
        GameConfig(
            spec = level.spec, mode = level.mode, zombies = level.zombies,
            crossSec = level.crossSec * speedFactor(p.speed), boss = level.boss,
            review = p.reviewFor(level.spec.kinds()),
        ),
        levelId = level.id,
        title = "${world.name} — " + if (level.boss) "le boss" else "niveau ${level.index + 1}",
        world = level.world,
    )
}

fun trainingRequest(setup: TrainingSetup, p: Profile): GameRequest? {
    val spec = setup.toSpec() ?: return null
    val base = when (setup.mode) {
        Mode.CHOICE -> 26f
        Mode.KEYPAD -> 30f
        Mode.MULTI -> 40f
    }
    val hard = Kind.MUL in setup.kinds || (setup.range >= 100 && (Kind.ADD in setup.kinds || Kind.SUB in setup.kinds))
    val think = (if (hard) 1.6f else 1.1f) * if (setup.holes) 1.15f else 1f
    return GameRequest(
        GameConfig(
            spec = spec, mode = setup.mode, zombies = 0, crossSec = base * think * speedFactor(setup.speed),
            durationSec = setup.minutes * 60, review = p.reviewFor(spec.kinds()),
        ),
        levelId = null,
        title = "Entraînement",
        world = 0,
    )
}

@Composable
fun App(store: Store, audio: AudioEngine) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = minOf(constraints.maxHeight / VIRTUAL_HEIGHT_DP, constraints.maxWidth / MIN_VIRTUAL_WIDTH_DP)
        CompositionLocalProvider(LocalDensity provides Density(scale.coerceAtLeast(0.5f), 1f)) {
            Navigation(store, audio)
        }
    }
}

@Composable
private fun Navigation(store: Store, audio: AudioEngine) {
    var profileId by rememberSaveable { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf<Screen>(Screen.Profiles) }
    val profile = store.profile(profileId)
    val current = if (profile == null) Screen.Profiles else screen

    val data = store.data
    LaunchedEffect(data.sfxVolume, data.musicVolume, current) {
        audio.sfxVolume = data.sfxVolume
        audio.musicVolume = data.musicVolume
        audio.tune = if (current is Screen.Game) AudioEngine.tuneForWorld(current.request.world) else AudioEngine.MENU_TUNE
    }
    fun go(s: Screen) {
        audio.play(Sfx.CLICK)
        screen = s
    }

    when (current) {
        Screen.Profiles -> ProfilesScreen(store) {
            profileId = it
            go(Screen.Home)
        }
        Screen.Home -> {
            BackHandler { go(Screen.Profiles); profileId = null }
            HomeScreen(profile!!, store, onNavigate = ::go, onSwitch = { profileId = null; go(Screen.Profiles) })
        }
        is Screen.Map -> {
            BackHandler { go(Screen.Home) }
            MapScreen(profile!!, current.world, onWorld = { screen = Screen.Map(it) }, onBack = { go(Screen.Home) }) { level ->
                go(Screen.Game(levelRequest(level, profile)))
            }
        }
        is Screen.Game -> {
            val request = current.request
            val level = request.levelId?.let(Curriculum::level)
            val next = level?.let {
                Curriculum.worlds[it.world].levels.getOrNull(it.index + 1) ?: Curriculum.worlds.getOrNull(it.world + 1)?.levels?.first()
            }
            val back = if (level != null) Screen.Map(level.world) else Screen.Training
            key(request) {
                GameScreen(
                    request = request,
                    launcherId = profile!!.launcher,
                    projectileId = profile.projectile,
                    audio = audio,
                    onResult = { outcome ->
                        store.profile(profile.id)?.let { p ->
                            val reward = Rules.apply(p, outcome, today())
                            store.updateProfile(p.id) { reward.profile }
                            reward
                        }
                    },
                    hasNext = next != null,
                    onNext = { next?.let { n -> store.profile(profile.id)?.let { p -> go(Screen.Game(levelRequest(n, p))) } } },
                    onExit = { go(back) },
                )
            }
        }
        Screen.Training -> {
            BackHandler { go(Screen.Home) }
            TrainingScreen(profile!!, store, onBack = { go(Screen.Home) }) { setup ->
                trainingRequest(setup, profile)?.let { go(Screen.Game(it)) }
            }
        }
        Screen.Shop -> {
            BackHandler { go(Screen.Home) }
            ShopScreen(profile!!, store, audio) { go(Screen.Home) }
        }
        Screen.Progress -> {
            BackHandler { go(Screen.Home) }
            ProgressScreen(profile!!) { go(Screen.Home) }
        }
    }
}
