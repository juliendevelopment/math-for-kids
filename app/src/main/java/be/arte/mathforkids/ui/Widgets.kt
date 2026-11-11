package be.arte.mathforkids.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Pal {
    val ink = Color(0xFF2B2B3A)
    val green = Color(0xFF5CB85C)
    val blue = Color(0xFF3FA7E0)
    val orange = Color(0xFFFF9838)
    val pink = Color(0xFFEC6FA0)
    val purple = Color(0xFF8E6BD1)
    val red = Color(0xFFE2574C)
    val yellow = Color(0xFFFFD54F)
    val grey = Color(0xFFB0B7C3)
    val paper = Color(0xFFFFFBF2)
    val bgTop = Color(0xFF8EDBFF)
    val bgBottom = Color(0xFFC9F5B5)
}

/** Chunky toy-like button with a darker "base" under it. */
@Composable
fun GameButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = 24.sp,
    padding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
    leading: (@Composable () -> Unit)? = null,
) {
    val c = if (enabled) color else Pal.grey
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .clip(shape)
            .background(lerp(c, Color.Black, 0.3f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(bottom = 6.dp)
            .clip(shape)
            .background(c)
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            leading?.invoke()
            Text(text, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Pal.paper, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .border(3.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        content = content,
    )
}

/** Selectable pill used for options in forms. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Pal.blue, fontSize: TextUnit = 18.sp) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else Color.White)
            .border(2.dp, if (selected) lerp(color, Color.Black, 0.25f) else Pal.grey, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = 46.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Color.White else Pal.ink, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, size: TextUnit = 18.sp, color: Color = Pal.ink, weight: FontWeight = FontWeight.Bold, align: TextAlign? = null) {
    Text(text, modifier, color = color, fontSize = size, fontWeight = weight, textAlign = align)
}

@Composable
fun StarIcon(size: Dp, filled: Boolean = true, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        star(center, this.size.minDimension * 0.48f, if (filled) Pal.yellow else Color(0xFFDDDDDD), if (filled) Color(0xFFB8860B) else Color(0xFFAAAAAA))
    }
}

@Composable
fun CoinIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { coin(center, this.size.minDimension * 0.48f) }
}

@Composable
fun HeartIcon(size: Dp, filled: Boolean = true, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { heart(center, this.size.minDimension * 0.42f, if (filled) Pal.red else Color(0xFFCFCFCF)) }
}

@Composable
fun AvatarView(id: String, size: Dp, modifier: Modifier = Modifier, ring: Color = Color.White) {
    Canvas(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFBDE7FF))
            .border(size / 18, ring, CircleShape),
    ) {
        avatar(id, Offset(center.x, center.y + this.size.height * 0.08f), this.size.minDimension * 0.5f)
    }
}

@Composable
fun Stars(count: Int, size: Dp, modifier: Modifier = Modifier) {
    Row(modifier) { for (i in 1..3) StarIcon(size, filled = i <= count) }
}

@Composable
fun CoinCount(coins: Int, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinIcon(size)
        Label("$coins", size = 22.sp)
    }
}

/** Common page: soft gradient, back button, title and a slot on the right of the title bar. */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)?,
    trailing: @Composable RowScope.() -> Unit = {},
    background: Brush = Brush.verticalGradient(listOf(Pal.bgTop, Pal.bgBottom)),
    content: @Composable BoxScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(background).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                GameButton("◀", Pal.orange, onBack, fontSize = 22.sp, padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp))
                Spacer(Modifier.width(16.dp))
            }
            Text(title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 4f), 6f)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = trailing)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxSize(), content = content)
    }
}

/** In-tree modal (a real Dialog window would bring the system bars back). */
@Composable
fun Modal(onDismiss: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val noRipple = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = noRipple, indication = null) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        val inner = remember { MutableInteractionSource() }
        Panel(Modifier.padding(24.dp).clickable(interactionSource = inner, indication = null) {}) {
            content()
        }
    }
}
