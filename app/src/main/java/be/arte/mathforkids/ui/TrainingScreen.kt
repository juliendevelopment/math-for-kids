package be.arte.mathforkids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.arte.mathforkids.data.Profile
import be.arte.mathforkids.data.Store
import be.arte.mathforkids.data.TrainingSetup
import be.arte.mathforkids.math.Kind
import be.arte.mathforkids.math.Mode

private val KIND_LABELS = listOf(
    Kind.FRIENDS to "Amis de 10",
    Kind.ADD to "Additions",
    Kind.SUB to "Soustractions",
    Kind.TABLES to "Tables",
    Kind.MUL to "Grandes multiplications",
    Kind.DIV to "Divisions",
)
private val MODE_LABELS = listOf(Mode.CHOICE to "Bulles à choix", Mode.KEYPAD to "Clavier", Mode.MULTI to "Un calcul par zombie")

fun TrainingSetup.summary(): String {
    val what = KIND_LABELS.filter { it.first in kinds }.joinToString(", ") { (k, l) ->
        when (k) {
            Kind.TABLES, Kind.DIV -> "$l ${tables.sorted().joinToString(" ")}"
            Kind.ADD, Kind.SUB -> "$l ≤ $range"
            else -> l
        }
    }
    val mode = MODE_LABELS.first { it.first == mode }.second.lowercase()
    return what + (if (holes) " · à trous" else "") + " · $mode · $minutes min"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingScreen(profile: Profile, store: Store, onBack: () -> Unit, onPlay: (TrainingSetup) -> Unit) {
    var setup by remember { mutableStateOf(profile.favorites.firstOrNull() ?: TrainingSetup()) }
    var favName by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val usesRange = setup.kinds.any { it == Kind.ADD || it == Kind.SUB || it == Kind.FRIENDS }
    val usesTables = setup.kinds.any { it == Kind.TABLES || it == Kind.DIV }
    val ready = setup.kinds.isNotEmpty() && (!usesTables || setup.tables.isNotEmpty())

    Page("Entraînement", onBack) {
        Row(Modifier.fillMaxSize().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Panel(Modifier.weight(1.7f).fillMaxHeight()) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label("Quels calculs ?", color = Pal.grey)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for ((k, l) in KIND_LABELS) {
                            Chip(l, k in setup.kinds, { setup = setup.copy(kinds = if (k in setup.kinds) setup.kinds - k else setup.kinds + k) }, fontSize = 16.sp)
                        }
                        Chip("Calculs à trous", setup.holes, { setup = setup.copy(holes = !setup.holes) }, color = Pal.pink, fontSize = 16.sp)
                    }
                    if (usesRange) {
                        Label("Nombres jusqu'à", color = Pal.grey)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (r in listOf(10, 20, 100, 1000)) Chip("$r", setup.range == r, { setup = setup.copy(range = r) }, color = Pal.green, fontSize = 16.sp)
                        }
                    }
                    if (usesTables) {
                        Label("Quelles tables ?", color = Pal.grey)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (t in 1..12) {
                                Chip("$t", t in setup.tables, { setup = setup.copy(tables = if (t in setup.tables) setup.tables - t else setup.tables + t) }, color = Pal.orange, fontSize = 16.sp)
                            }
                            Chip("Toutes", setup.tables.size >= 9, { setup = setup.copy(tables = if (setup.tables.size >= 9) emptySet() else (2..10).toSet()) }, color = Pal.orange, fontSize = 16.sp)
                        }
                    }
                    Label("Comment répondre ?", color = Pal.grey)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((m, l) in MODE_LABELS) Chip(l, setup.mode == m, { setup = setup.copy(mode = m) }, color = Pal.purple, fontSize = 16.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Label("Vitesse des zombies", color = Pal.grey)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Tranquille", "Normale", "Rapide").forEachIndexed { i, l -> Chip(l, setup.speed == i, { setup = setup.copy(speed = i) }, fontSize = 16.sp) }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Label("Durée (minutes)", color = Pal.grey)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (m in listOf(2, 3, 5, 10)) Chip("$m", setup.minutes == m, { setup = setup.copy(minutes = m) }, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                GameButton("Jouer !", Pal.green, { onPlay(setup) }, Modifier.fillMaxWidth(), enabled = ready, fontSize = 34.sp)
                Spacer(Modifier.height(10.dp))
                Label("Mes favoris", color = Pal.grey)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, Pal.grey, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        if (favName.isEmpty()) Label("Nom du favori", size = 16.sp, color = Pal.grey)
                        BasicTextField(
                            favName, { favName = it.take(22).replace("\n", "") }, singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Pal.ink),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    GameButton("+", Pal.blue, {
                        val saved = setup.copy(name = favName.trim())
                        store.updateProfile(profile.id) { p -> p.copy(favorites = (listOf(saved) + p.favorites.filterNot { it.name == saved.name }).take(12)) }
                        favName = ""
                        focus.clearFocus()
                    }, enabled = ready && favName.isNotBlank(), fontSize = 20.sp, padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(6.dp))
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile.favorites.isEmpty()) Label("Règle un entraînement, donne-lui un nom et appuie sur + pour le retrouver ici.", size = 15.sp, color = Pal.grey, weight = FontWeight.Normal)
                    for (f in profile.favorites) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (f.copy(name = "") == setup.copy(name = "")) Color(0xFFDDF3FF) else Color.White)
                                .clickable { setup = f }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Label(f.name, size = 18.sp)
                                Label(f.summary(), size = 13.sp, color = Pal.grey, weight = FontWeight.Normal)
                            }
                            GameButton("▶", Pal.green, { onPlay(f) }, fontSize = 16.sp, padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp))
                            Spacer(Modifier.padding(3.dp))
                            GameButton("×", Pal.red, { store.updateProfile(profile.id) { p -> p.copy(favorites = p.favorites - f) } }, fontSize = 16.sp, padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
