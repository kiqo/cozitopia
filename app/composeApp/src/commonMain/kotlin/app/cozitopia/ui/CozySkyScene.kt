package app.cozitopia.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput

private const val DESIGN_WIDTH = 1000f
private const val DESIGN_HEIGHT = 520f
private const val FACE_TAP_LEFT = 680f
private const val FACE_TAP_TOP = 80f
private const val FACE_TAP_RIGHT = 840f
private const val FACE_TAP_BOTTOM = 260f
private const val WINDOW_TAP_LEFT = 600f
private const val WINDOW_TAP_TOP = 300f
private const val WINDOW_TAP_RIGHT = 800f
private const val WINDOW_TAP_BOTTOM = 440f

private val Sky = Color(0xFFE8856C)
private val Cream = Color(0xFFFFF7EC)
private val Mint = Color(0xFFCBE7D2)
private val MintDeep = Color(0xFF6FA983)
private val Brick = Color(0xFFD0604A)
private val Peach = Color(0xFFF4A58F)
private val Ink = Color(0xFF22304A)
private val Mustard = Color(0xFFF2C14E)
private val WindowOff = Color(0xFF22304A)

@Composable
fun CozySkyScene(modifier: Modifier = Modifier) {
    var faceTaps by remember { mutableIntStateOf(0) }
    var windowsLit by remember { mutableStateOf(false) }
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift-value",
    )
    val faceLift by animateFloatAsState(
        targetValue = if (faceTaps % 2 == 1) -16f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "face-lift",
    )
    val windowColor by animateColorAsState(
        targetValue = if (windowsLit) Mustard else WindowOff,
        label = "window-color",
    )

    Canvas(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .aspectRatio(DESIGN_WIDTH / DESIGN_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val x = tap.x / size.width * DESIGN_WIDTH
                    val y = tap.y / size.height * DESIGN_HEIGHT
                    if (x in FACE_TAP_LEFT..FACE_TAP_RIGHT && y in FACE_TAP_TOP..FACE_TAP_BOTTOM) faceTaps += 1
                    if (x in WINDOW_TAP_LEFT..WINDOW_TAP_RIGHT && y in WINDOW_TAP_TOP..WINDOW_TAP_BOTTOM) {
                        windowsLit = !windowsLit
                    }
                }
            },
    ) {
        drawRect(Sky)
        drawCloud(cx = 160f + drift, cy = 120f, scale = 60f, color = Mint)
        drawCloud(cx = 470f - drift, cy = 90f, scale = 50f, color = Cream)
        drawCloud(cx = 280f, cy = 300f, scale = 230f, color = Cream)
        drawCloud(cx = 900f + drift, cy = 300f, scale = 90f, color = Mint)
        drawCloud(cx = 50f, cy = 320f, scale = 60f, color = Peach)
        drawHill()
        drawTree(x = 70f, base = 440f, height = 190f, leaves = Mint)
        drawTree(x = 480f, base = 440f, height = 230f, leaves = MintDeep)
        drawSmallHouse()
        drawBrickHouse(windowColor)
        drawTree(x = 960f, base = 440f, height = 150f, leaves = Mint)
        translate(top = px(faceLift)) {
            drawCloud(cx = 760f, cy = 190f, scale = 130f, color = Cream)
            drawSmileyFace()
        }
    }
}

private fun DrawScope.px(value: Float): Float = value * size.width / DESIGN_WIDTH

private fun DrawScope.drawCloud(cx: Float, cy: Float, scale: Float, color: Color) {
    drawCircle(color, px(scale * 0.55f), Offset(px(cx - scale * 0.5f), px(cy)))
    drawCircle(color, px(scale * 0.75f), Offset(px(cx), px(cy - scale * 0.2f)))
    drawCircle(color, px(scale * 0.55f), Offset(px(cx + scale * 0.5f), px(cy)))
    drawRoundRect(
        color = color,
        topLeft = Offset(px(cx - scale * 1.05f), px(cy)),
        size = Size(px(scale * 2.1f), px(scale * 0.55f)),
        cornerRadius = CornerRadius(px(scale * 0.28f)),
    )
}

private fun DrawScope.drawSmileyFace() {
    drawCircle(Ink, px(8f), Offset(px(735f), px(180f)))
    drawCircle(Ink, px(8f), Offset(px(785f), px(180f)))
    drawCircle(Peach, px(10f), Offset(px(718f), px(200f)))
    drawCircle(Peach, px(10f), Offset(px(802f), px(200f)))
    drawArc(
        color = Ink,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(px(735f), px(190f)),
        size = Size(px(50f), px(30f)),
        style = Stroke(width = px(5f), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawHill() {
    drawOval(
        color = MintDeep,
        topLeft = Offset(px(-80f), px(400f)),
        size = Size(px(420f), px(260f)),
    )
    drawOval(
        color = MintDeep,
        topLeft = Offset(px(600f), px(410f)),
        size = Size(px(460f), px(240f)),
    )
}

private fun DrawScope.drawTree(x: Float, base: Float, height: Float, leaves: Color) {
    drawRect(
        color = Ink,
        topLeft = Offset(px(x - 4f), px(base - height * 0.45f)),
        size = Size(px(8f), px(height * 0.45f)),
    )
    val crownY = base - height * 0.7f
    val radius = height * 0.26f
    drawCircle(leaves, px(radius), Offset(px(x), px(crownY)))
    drawCircle(leaves, px(radius * 0.85f), Offset(px(x - radius * 0.9f), px(crownY + radius * 0.4f)))
    drawCircle(leaves, px(radius * 0.85f), Offset(px(x + radius * 0.9f), px(crownY + radius * 0.4f)))
    drawCircle(leaves, px(radius * 0.8f), Offset(px(x), px(crownY - radius * 0.9f)))
}

private fun DrawScope.drawSmallHouse() {
    val roof = Path().apply {
        moveTo(px(290f), px(380f))
        lineTo(px(340f), px(330f))
        lineTo(px(390f), px(380f))
        close()
    }
    drawPath(roof, Brick)
    drawRect(Peach, topLeft = Offset(px(300f), px(380f)), size = Size(px(80f), px(60f)))
    drawRect(Cream, topLeft = Offset(px(328f), px(395f)), size = Size(px(24f), px(24f)))
}

private fun DrawScope.drawBrickHouse(windowColor: Color) {
    drawRect(MintDeep, topLeft = Offset(px(590f), px(300f)), size = Size(px(220f), px(40f)))
    drawRect(Brick, topLeft = Offset(px(600f), px(340f)), size = Size(px(200f), px(100f)))
    listOf(620f, 700f).forEach { windowX ->
        drawRect(windowColor, topLeft = Offset(px(windowX), px(355f)), size = Size(px(40f), px(40f)))
    }
    drawRoundRect(
        color = Ink,
        topLeft = Offset(px(680f), px(380f)),
        size = Size(px(40f), px(60f)),
        cornerRadius = CornerRadius(px(20f)),
    )
}
