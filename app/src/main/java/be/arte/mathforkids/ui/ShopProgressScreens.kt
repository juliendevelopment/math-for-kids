package be.arte.mathforkids.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.arte.mathforkids.audio.AudioEngine
import be.arte.mathforkids.audio.Sfx
import be.arte.mathforkids.data.Badges
import be.arte.mathforkids.data.Profile
import be.arte.mathforkids.data.Rules
import be.arte.mathforkids.data.Shop
import be.arte.mathforkids.data.ShopCategory
import be.arte.mathforkids.data.ShopItem
import be.arte.mathforkids.data.Store
import be.arte.mathforkids.game.GameEngine
import be.arte.mathforkids.math.Curriculum
import be.arte.mathforkids.math.Problem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopScreen(profile: Profile, store: Store, audio: AudioEngine, onBack: () -> Unit) {
    var category by remember { mutableStateOf(ShopCategory.LAUNCHER) }
    Page("Boutique", onBack, trailing = { CoinCount(profile.coins) }) {
        Column(Modifier.fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (c in ShopCategory.entries) Chip(c.label, category == c, { category = c }, color = Pal.pink, fontSize = 20.sp)
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                for (item in Shop.items.filter { it.category == category }) {
                    val owned = Shop.owns(profile, item)
                    val equipped = when (item.category) {
                        ShopCategory.AVATAR -> profile.avatar
                        ShopCategory.LAUNCHER -> profile.launcher
                        ShopCategory.PROJECTILE -> profile.projectile
                    } == item.id
                    Panel(Modifier.width(176.dp), color = if (equipped) Color(0xFFE3F8D8) else Pal.paper) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ItemPreview(item, 96.dp)
                            Label(item.name, size = 18.sp)
                            when {
                                equipped -> GameButton("Choisi", Pal.green, {}, enabled = false, fontSize = 16.sp, padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp))
                                owned -> GameButton("Choisir", Pal.blue, {
                                    audio.play(Sfx.CLICK)
                                    store.updateProfile(profile.id) { Shop.equip(it, item) }
                                }, fontSize = 16.sp, padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp))
                                else -> GameButton("${item.price}", Pal.orange, {
                                    audio.play(Sfx.COIN)
                                    store.updateProfile(profile.id) { Shop.equip(Shop.buy(it, item), item) }
                                }, enabled = profile.coins >= item.price, fontSize = 18.sp, padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), leading = { CoinIcon(22.dp) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemPreview(item: ShopItem, size: Dp) {
    if (item.category == ShopCategory.AVATAR) {
        AvatarView(item.id, size)
        return
    }
    Canvas(Modifier.size(size).clip(RoundedCornerShape(18.dp)).background(Color(0xFFBDE7FF))) {
        val s = this.size.minDimension
        if (item.category == ShopCategory.LAUNCHER) launcher(item.id, center.x, s * 0.95f, s * 0.85f, 0f)
        else projectile(item.id, center, s * 0.24f, 0.1f)
    }
}

@Composable
fun BadgeMedal(symbol: String, earned: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension * 0.4f
        val c = Offset(center.x, center.y - r * 0.12f)
        val gold = if (earned) Pal.yellow else Color(0xFFD9D9D9)
        val rim = if (earned) Color(0xFFB8860B) else Color(0xFFAAAAAA)
        val ribbon = if (earned) Pal.red else Color(0xFFBDBDBD)
        drawRoundRect(ribbon, Offset(c.x - r * 0.55f, c.y + r * 0.3f), Size(r * 0.45f, r * 0.95f), CornerRadius(r * 0.08f))
        drawRoundRect(ribbon, Offset(c.x + r * 0.1f, c.y + r * 0.3f), Size(r * 0.45f, r * 0.95f), CornerRadius(r * 0.08f))
        drawCircle(rim, r, c)
        drawCircle(gold, r * 0.86f, c)
        val ts = r * (if (symbol.length > 3) 0.5f else if (symbol.length > 2) 0.62f else 0.85f)
        text(symbol, c.x, c.y, ts, if (earned) Color(0xFF7A5200) else Color(0xFF8A8A8A))
    }
}

private fun duration(seconds: Long): String {
    val m = seconds / 60
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(profile: Profile, onBack: () -> Unit) {
    Page("Les progrès de ${profile.name}", onBack) {
        Row(Modifier.fillMaxSize().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val answers = profile.correct + profile.wrong
                    Stat("Étoiles", "${profile.totalStars} / ${Curriculum.totalStars}")
                    Stat("Temps de jeu", duration(profile.playSeconds))
                    Stat("Zombies touchés", "${profile.kills}")
                    Stat("Bonnes réponses", if (answers == 0) "—" else "${profile.correct} (${profile.correct * 100 / answers} %)")
                    Stat("Parties gagnées", "${profile.gamesWon}")
                    Stat("Jours d'affilée", "${Rules.currentStreak(profile, today())} (record ${profile.bestStreak})")
                    Spacer(Modifier.height(6.dp))
                    Label("L'aventure", color = Pal.grey)
                    for (w in Curriculum.worlds) {
                        val s = w.levels.sumOf { profile.stars[it.id] ?: 0 }
                        val max = w.levels.size * 3
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Label(w.name, Modifier.weight(1f), size = 15.sp)
                            Box(Modifier.width(90.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE4E4E4))) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(s.toFloat() / max).background(Pal.yellow))
                            }
                            Label(" $s/$max", Modifier.width(52.dp), size = 14.sp, align = TextAlign.End)
                        }
                    }
                }
            }
            Panel(Modifier.weight(0.9f).fillMaxHeight()) {
                Label("Calculs à revoir", size = 20.sp)
                Label("Ils reviennent plus souvent dans les parties.", size = 13.sp, color = Pal.grey, weight = FontWeight.Normal)
                Spacer(Modifier.height(6.dp))
                val list = profile.mistakes.entries.sortedWith(
                    compareBy({ it.value.fixes >= Profile.FIXES_TO_CLEAR }, { -it.value.misses }),
                ).take(14)
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (list.isEmpty()) Label("Aucune erreur pour l'instant. Bravo !", size = 16.sp, color = Pal.green)
                    for ((text, m) in list) {
                        val fixed = m.fixes >= Profile.FIXES_TO_CLEAR
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (fixed) Color(0xFFE3F8D8) else Color(0xFFFFF3C4)).padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Label(GameEngine.solution(Problem(text, m.answer, m.kind)), Modifier.weight(1f), size = 18.sp)
                            Label(if (fixed) "maîtrisé" else "raté ${m.misses} fois", size = 13.sp, color = if (fixed) Pal.green else Pal.red)
                        }
                    }
                }
            }
            Panel(Modifier.weight(1.1f).fillMaxHeight()) {
                Label("Badges  ${profile.badges.size} / ${Badges.all.size}", size = 20.sp)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    Modifier.verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (b in Badges.all) {
                        val earned = b.id in profile.badges
                        Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            BadgeMedal(b.symbol, earned, 56.dp)
                            Label(b.name, size = 12.sp, align = TextAlign.Center, color = if (earned) Pal.ink else Pal.grey)
                            Label(b.desc, size = 10.sp, align = TextAlign.Center, color = Pal.grey, weight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(name: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Label(name, Modifier.weight(1f), size = 16.sp, color = Pal.grey)
        Label(value, size = 18.sp)
    }
}
