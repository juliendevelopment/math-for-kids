package be.arte.mathforkids.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import be.arte.mathforkids.data.Rules
import be.arte.mathforkids.data.Shop
import be.arte.mathforkids.data.ShopCategory
import be.arte.mathforkids.data.Store
import be.arte.mathforkids.math.Curriculum
import java.util.UUID

@Composable
fun ProfilesScreen(store: Store, onPick: (String) -> Unit) {
    val profiles = store.data.profiles
    var creating by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Page("Math contre Zombies", onBack = null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Label(if (profiles.isEmpty()) "Bienvenue ! Crée ton profil pour jouer." else "Qui joue ?", size = 40.sp, color = Color.White)
                Spacer(Modifier.height(28.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (p in profiles) {
                        Panel(Modifier.width(190.dp).clickable { onPick(p.id) }) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                AvatarView(p.avatar, 130.dp, ring = Pal.blue)
                                Spacer(Modifier.height(8.dp))
                                Label(p.name, size = 26.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StarIcon(24.dp)
                                    Label(" ${p.totalStars}", size = 20.sp)
                                }
                            }
                        }
                    }
                    if (profiles.size < 8) {
                        Panel(Modifier.width(190.dp).clickable { creating = true }, color = Color.White.copy(alpha = 0.7f)) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.height(130.dp).width(130.dp).clip(CircleShape).background(Pal.green), contentAlignment = Alignment.Center) {
                                    Label("+", size = 80.sp, color = Color.White)
                                }
                                Spacer(Modifier.height(8.dp))
                                Label("Nouveau", size = 26.sp)
                                Label("joueur", size = 20.sp, color = Pal.grey)
                            }
                        }
                    }
                }
            }
        }
        if (creating) {
            ProfileEditor(null, onCancel = { creating = false }, onDelete = {}) { p ->
                store.addProfile(p)
                creating = false
                onPick(p.id)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditor(existing: Profile?, onCancel: () -> Unit, onDelete: () -> Unit, onSave: (Profile) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var avatar by remember { mutableStateOf(existing?.avatar ?: "chat") }
    var grade by remember { mutableStateOf(existing?.grade ?: 0) }
    var speed by remember { mutableStateOf(existing?.speed ?: 1) }
    var confirmDelete by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Modal(onDismiss = null) {
        Column(Modifier.width(680.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AvatarView(avatar, 70.dp, ring = Pal.blue)
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Color.White)
                        .border(3.dp, Pal.blue, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (name.isEmpty()) Label("Ton prénom", size = 28.sp, color = Pal.grey)
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it.take(14).replace("\n", "") },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Pal.ink),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Label("Ton avatar", color = Pal.grey)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in Shop.items.filter { it.category == ShopCategory.AVATAR && (it.price == 0 || existing?.owned?.contains(it.id) == true) }) {
                    AvatarView(item.id, 62.dp, Modifier.clickable { avatar = item.id }, ring = if (avatar == item.id) Pal.orange else Color.White)
                }
            }
            Label("Ton année (pour savoir où commencer l'aventure)", color = Pal.grey)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Curriculum.grades.forEachIndexed { i, (label, _) -> Chip(label, grade == i, { grade = i }, fontSize = 16.sp) }
            }
            Label("Vitesse des zombies", color = Pal.grey)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tranquille", "Normale", "Rapide").forEachIndexed { i, l -> Chip(l, speed == i, { speed = i }, color = Pal.purple, fontSize = 16.sp) }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GameButton("Annuler", Pal.grey, onCancel, fontSize = 20.sp)
                if (existing != null) {
                    GameButton(if (confirmDelete) "Oui, tout effacer" else "Supprimer", Pal.red, { if (confirmDelete) onDelete() else confirmDelete = true }, fontSize = 20.sp)
                }
                Spacer(Modifier.weight(1f))
                GameButton("C'est parti !", Pal.green, {
                    val clean = name.trim()
                    onSave((existing ?: Profile(id = UUID.randomUUID().toString(), name = clean)).copy(name = clean, avatar = avatar, grade = grade, speed = speed))
                }, enabled = name.isNotBlank(), fontSize = 22.sp)
            }
        }
    }
}

@Composable
fun HomeScreen(profile: Profile, store: Store, onNavigate: (Screen) -> Unit, onSwitch: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var sound by remember { mutableStateOf(false) }
    val startWorld = Curriculum.grades[profile.grade.coerceIn(0, Curriculum.grades.lastIndex)].second
    Box(Modifier.fillMaxSize()) {
        Page("Bonjour ${profile.name} !", onBack = null, trailing = {
            CoinCount(profile.coins)
            GameButton("Son", Pal.purple, { sound = true }, fontSize = 18.sp)
            GameButton("Joueurs", Pal.orange, onSwitch, fontSize = 18.sp)
        }) {
            Row(Modifier.fillMaxSize().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Panel(Modifier.width(250.dp).fillMaxHeight()) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvatarView(profile.avatar, 150.dp, ring = Pal.blue)
                        Label(profile.name, size = 30.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarIcon(30.dp)
                            Label(" ${profile.totalStars} / ${Curriculum.totalStars}", size = 22.sp)
                        }
                        val streak = Rules.currentStreak(profile, today())
                        Label(
                            when {
                                streak >= 2 -> "$streak jours d'affilée !"
                                streak == 1 -> "1 jour : reviens demain !"
                                else -> "Joue chaque jour !"
                            },
                            size = 18.sp, color = Pal.orange,
                        )
                        Label("${profile.badges.size} badges", size = 18.sp, color = Pal.purple)
                        Spacer(Modifier.weight(1f))
                        GameButton("Mon profil", Pal.blue, { editing = true }, fontSize = 18.sp)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        val frontier = (startWorld until Curriculum.worlds.size).lastOrNull { w ->
                            Curriculum.isUnlocked(Curriculum.worlds[w].levels[0], startWorld, profile.stars)
                        } ?: startWorld
                        GameButton("Aventure", Pal.green, { onNavigate(Screen.Map(frontier)) }, Modifier.weight(1.3f).fillMaxHeight(), fontSize = 46.sp)
                        GameButton("Entraînement", Pal.blue, { onNavigate(Screen.Training) }, Modifier.weight(1f).fillMaxHeight(), fontSize = 32.sp)
                    }
                    Row(Modifier.weight(0.7f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        GameButton("Boutique", Pal.pink, { onNavigate(Screen.Shop) }, Modifier.weight(1f).fillMaxHeight(), fontSize = 30.sp)
                        GameButton("Mes progrès", Pal.purple, { onNavigate(Screen.Progress) }, Modifier.weight(1f).fillMaxHeight(), fontSize = 30.sp)
                    }
                }
            }
        }
        if (editing) {
            ProfileEditor(profile, onCancel = { editing = false }, onDelete = { store.deleteProfile(profile.id) }) { p ->
                store.updateProfile(profile.id) { it.copy(name = p.name, avatar = p.avatar, grade = p.grade, speed = p.speed) }
                editing = false
            }
        }
        if (sound) {
            val levels = listOf("Coupé" to 0f, "Doux" to 0.3f, "Moyen" to 0.6f, "Fort" to 1f)
            Modal(onDismiss = { sound = false }) {
                Label("Son", size = 32.sp)
                Spacer(Modifier.height(10.dp))
                Label("Bruitages", color = Pal.grey)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((l, v) in levels) Chip(l, store.data.sfxVolume == v, { store.update { it.copy(sfxVolume = v) } })
                }
                Spacer(Modifier.height(10.dp))
                Label("Musique", color = Pal.grey)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((l, v) in levels) Chip(l, store.data.musicVolume == v, { store.update { it.copy(musicVolume = v) } }, color = Pal.purple)
                }
                Spacer(Modifier.height(16.dp))
                GameButton("OK", Pal.green, { sound = false }, Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
