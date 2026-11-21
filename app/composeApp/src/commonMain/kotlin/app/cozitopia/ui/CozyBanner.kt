package app.cozitopia.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cozitopia.composeapp.generated.resources.Res
import cozitopia.composeapp.generated.resources.cozy_cloud_sofa
import cozitopia.composeapp.generated.resources.cozy_village
import org.jetbrains.compose.resources.painterResource

@Composable
fun CozyBanner(modifier: Modifier = Modifier) {
    var wobbles by remember { mutableIntStateOf(0) }
    val scale by animateFloatAsState(
        targetValue = if (wobbles % 2 == 1) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "banner-scale",
    )
    Image(
        painter = painterResource(Res.drawable.cozy_village),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .aspectRatio(2.4f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .clickable { wobbles += 1 },
    )
}

@Composable
fun CozyEmptyState(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.cozy_cloud_sofa),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .aspectRatio(1344f / 768f)
            .clip(MaterialTheme.shapes.large),
    )
}
