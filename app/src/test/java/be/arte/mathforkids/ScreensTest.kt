package be.arte.mathforkids

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import be.arte.mathforkids.audio.AudioEngine
import be.arte.mathforkids.data.Profile
import be.arte.mathforkids.data.SaveData
import be.arte.mathforkids.data.Mistake
import be.arte.mathforkids.data.Store
import be.arte.mathforkids.data.TrainingSetup
import be.arte.mathforkids.math.Curriculum
import be.arte.mathforkids.math.Kind
import be.arte.mathforkids.math.Mode
import be.arte.mathforkids.ui.App
import be.arte.mathforkids.ui.GameScreen
import be.arte.mathforkids.ui.HomeScreen
import be.arte.mathforkids.ui.MapScreen
import be.arte.mathforkids.ui.ProgressScreen
import be.arte.mathforkids.ui.ShopScreen
import be.arte.mathforkids.ui.TrainingScreen
import be.arte.mathforkids.ui.levelRequest
import be.arte.mathforkids.ui.trainingRequest
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files

/**
 * Renders every screen on a 10" landscape tablet and saves a PNG in app/build/screens/.
 * It proves the screens compose and draw without crashing; how they look is judged on the PNGs.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1280dp-h800dp-land-mdpi")
class ScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val out = File("build/screens").apply { mkdirs() }
    private val audio = AudioEngine()

    private val kid = Profile(
        id = "k", name = "Léa", avatar = "renard", grade = 1, coins = 245,
        stars = (0..9).associate { "0-$it" to if (it % 3 == 0) 2 else 3 } + mapOf("1-0" to 3, "1-1" to 2, "1-2" to 1),
        owned = setOf("renard", "cactus", "etoile"), launcher = "cactus", projectile = "etoile",
        badges = setOf("premier_zombie", "chasseur", "premiere_etoile", "boss", "serie3"),
        kills = 312, correct = 640, wrong = 71, playSeconds = 7400, gamesWon = 21, lastPlayDay = 0, streak = 3, bestStreak = 4,
        mistakes = mapOf(
            "7 × 8" to Mistake(56, Kind.TABLES, 4, 0), "6 × 9" to Mistake(54, Kind.TABLES, 3, 1),
            "13 − 6" to Mistake(7, Kind.SUB, 2, 0), "8 + ? = 15" to Mistake(7, Kind.ADD, 1, 2),
        ),
        favorites = listOf(TrainingSetup("Devoirs de mardi", setOf(Kind.TABLES), tables = setOf(6, 7, 8), mode = Mode.KEYPAD, minutes = 5)),
    )

    private fun store(vararg profiles: Profile): Store {
        val dir = Files.createTempDirectory("mfk").toFile()
        File(dir, "save.json").writeText(Json.encodeToString(SaveData.serializer(), SaveData(profiles.toList())))
        return Store(File(dir, "save.json"))
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        // captureToImage() times out under Robolectric; drawing the view hierarchy works.
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun show(name: String, content: @Composable () -> Unit) {
        rule.setContent { content() }
        shot(name)
    }

    @Test fun profilesEmpty() = show("01-profils-vide") { App(store(), audio) }

    @Test fun profilesAndCreation() {
        val s = store(kid, Profile("t", "Tom", avatar = "panda"))
        rule.setContent { App(s, audio) }
        shot("02-profils")
        rule.onNodeWithText("Nouveau").performClick()
        shot("03-nouveau-profil")
    }

    @Test fun home() {
        val s = store(kid)
        show("04-accueil") { HomeScreen(kid, s, {}, {}) }
    }

    @Test fun map() = show("05-carte-monde1") { MapScreen(kid, 1, {}, {}, {}) }

    @Test fun mapLocked() = show("06-carte-verrouillee") { MapScreen(kid, 5, {}, {}, {}) }

    private fun game(name: String, level: String, seconds: Long) {
        rule.mainClock.autoAdvance = false
        val req = levelRequest(Curriculum.level(level)!!, kid)
        rule.setContent { GameScreen(req, kid.launcher, kid.projectile, audio, { null }, true, {}, {}) }
        rule.mainClock.advanceTimeBy(seconds * 1000)
        shot(name)
    }

    @Test fun gameChoice() = game("07-jeu-bulles", "0-1", 9)
    @Test fun gameKeypad() = game("08-jeu-clavier", "3-4", 12)
    @Test fun gameMulti() = game("09-jeu-multi", "1-7", 16)
    @Test fun gameLost() = game("10-jeu-perdu", "0-0", 120)

    @Test fun training() {
        val s = store(kid)
        show("11-entrainement") { TrainingScreen(kid, s, {}, {}) }
    }

    @Test fun trainingGame() {
        rule.mainClock.autoAdvance = false
        val req = trainingRequest(kid.favorites[0], kid)!!
        rule.setContent { GameScreen(req, "canon", "feu", audio, { null }, false, {}, {}) }
        rule.mainClock.advanceTimeBy(14_000)
        shot("12-jeu-entrainement")
    }

    @Test fun shop() {
        val s = store(kid)
        show("13-boutique") { ShopScreen(kid, s, audio) {} }
    }

    @Test fun progress() = show("14-progres") { ProgressScreen(kid) {} }

    @Test fun bossMap() = show("15-carte-tables") { MapScreen(kid.copy(grade = 5), 3, {}, {}, {}) }

    @Test fun boss() {
        rule.mainClock.autoAdvance = false
        val base = levelRequest(Curriculum.level("4-9")!!, kid)
        val req = base.copy(config = base.config.copy(zombies = 0))
        rule.setContent { GameScreen(req, "tournesol", "pois", audio, { null }, true, {}, {}) }
        rule.mainClock.advanceTimeBy(9_000)
        shot("16-boss")
    }
}
