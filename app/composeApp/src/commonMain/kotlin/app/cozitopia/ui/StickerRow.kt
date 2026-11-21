package app.cozitopia.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cozitopia.composeapp.generated.resources.Res
import cozitopia.composeapp.generated.resources.sticker_cat
import cozitopia.composeapp.generated.resources.sticker_dog
import cozitopia.composeapp.generated.resources.sticker_heart
import cozitopia.composeapp.generated.resources.sticker_plant
import cozitopia.composeapp.generated.resources.sticker_rainbow
import cozitopia.composeapp.generated.resources.sticker_socks
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

val STICKERS: List<DrawableResource> = listOf(
    Res.drawable.sticker_cat,
    Res.drawable.sticker_plant,
    Res.drawable.sticker_dog,
    Res.drawable.sticker_rainbow,
    Res.drawable.sticker_heart,
    Res.drawable.sticker_socks,
)

@Composable
fun StickerRow(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            STICKERS.forEach { sticker -> CozySticker(sticker, size = 52.dp) }
        }
    }
}

@Composable
fun CozySticker(drawable: DrawableResource, size: Dp = 64.dp) {
    var taps by remember { mutableIntStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = if (taps % 2 == 1) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sticker-scale",
    )
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { taps += 1 },
    )
}
