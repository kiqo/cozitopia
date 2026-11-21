package app.cozitopia.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cozitopia.composeapp.generated.resources.Res
import cozitopia.composeapp.generated.resources.fraunces
import org.jetbrains.compose.resources.Font

private val CozyColors = lightColorScheme(
    primary = Color(0xFFC4533F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBD9CC),
    onPrimaryContainer = Color(0xFF5A2418),
    secondary = Color(0xFF3E7A56),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE8D4),
    onSecondaryContainer = Color(0xFF1B3D28),
    tertiary = Color(0xFFE9B43C),
    onTertiary = Color(0xFF3A2A00),
    tertiaryContainer = Color(0xFFFCEBB8),
    background = Color(0xFFFFF7EC),
    onBackground = Color(0xFF22304A),
    surface = Color(0xFFFFF7EC),
    onSurface = Color(0xFF22304A),
    surfaceVariant = Color(0xFFF3E4D6),
    surfaceContainerLowest = Color(0xFFFFFBF5),
    surfaceContainerLow = Color(0xFFFFF7EC),
    surfaceContainer = Color(0xFFFCEFE2),
    surfaceContainerHigh = Color(0xFFF9E6D6),
    surfaceContainerHighest = Color(0xFFF5DCCB),
    onSurfaceVariant = Color(0xFF5B5057),
    outline = Color(0xFFB8A89C),
)

@Composable
private fun cozyTypography(): Typography {
    val serif = FontFamily(Font(Res.font.fraunces, FontWeight.ExtraBold))
    val base = Typography()
    return Typography(
        displaySmall = base.displaySmall.copy(fontFamily = serif, fontWeight = FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = serif, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = serif, fontWeight = FontWeight.ExtraBold),
        titleLarge = base.titleLarge.copy(fontFamily = serif, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
    )
}

private val CozyShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun CozitopiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CozyColors,
        typography = cozyTypography(),
        shapes = CozyShapes,
        content = content,
    )
}
