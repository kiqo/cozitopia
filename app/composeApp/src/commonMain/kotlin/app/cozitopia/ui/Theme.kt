package app.cozitopia.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cozitopia.composeapp.generated.resources.Res
import cozitopia.composeapp.generated.resources.fraunces
import org.jetbrains.compose.resources.Font

val Navy = Color(0xFF22304A)
val CreamCard = Color(0xFFFFFCF7)

private val CozyColors = lightColorScheme(
    primary = Color(0xFFC4533F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBD9CC),
    onPrimaryContainer = Color(0xFF8D3425),
    secondary = Color(0xFF3E7A56),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE8D4),
    onSecondaryContainer = Color(0xFF254F37),
    tertiary = Color(0xFFE9B43C),
    onTertiary = Navy,
    tertiaryContainer = Color(0xFFF8E5B0),
    onTertiaryContainer = Color(0xFF5A4100),
    background = Color(0xFFFFF7EC),
    onBackground = Navy,
    surface = Color(0xFFFFF7EC),
    onSurface = Navy,
    surfaceVariant = Color(0xFFF4EADF),
    onSurfaceVariant = Color(0xFF5B5057),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = CreamCard,
    surfaceContainer = Color(0xFFF4EADF),
    surfaceContainerHigh = Color(0xFFF1E4D6),
    surfaceContainerHighest = Color(0xFFEDDDCC),
    inverseSurface = Navy,
    inverseOnSurface = Color(0xFFFFF7EC),
    outline = Color(0xFF88786D),
    outlineVariant = Color(0xFFB8A89C),
    error = Color(0xFF9F302B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBD9CC),
    onErrorContainer = Color(0xFF8D3425),
)

@Immutable
data class Tone(val container: Color, val content: Color)

val HomieTones = listOf(
    Tone(Color(0xFFFBD9CC), Color(0xFF8D3425)),
    Tone(Color(0xFFCFE8D4), Color(0xFF254F37)),
    Tone(Color(0xFFF8E5B0), Color(0xFF5A4100)),
    Tone(Color(0xFFDDE3EE), Navy),
)

val NeutralTone = Tone(Color(0xFFF1E8DF), Color(0xFF5B5057))

@Composable
private fun cozyTypography(): Typography {
    val serif = FontFamily(
        Font(
            Res.font.fraunces,
            FontWeight.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(SERIF_WEIGHT),
                FontVariation.Setting("SOFT", SERIF_SOFTNESS),
                FontVariation.Setting("WONK", 0f),
            ),
        ),
    )
    val base = Typography()
    fun TextStyle.serif(size: Int, line: Int) = copy(fontFamily = serif, fontSize = size.sp, lineHeight = line.sp)
    fun TextStyle.sans(size: Int, line: Int, weight: FontWeight) =
        copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)
    return base.copy(
        displayLarge = base.displayLarge.serif(64, 76),
        displayMedium = base.displayMedium.serif(48, 58),
        displaySmall = base.displaySmall.serif(40, 50),
        headlineLarge = base.headlineLarge.serif(36, 46),
        headlineMedium = base.headlineMedium.serif(30, 40),
        headlineSmall = base.headlineSmall.serif(26, 34),
        titleLarge = base.titleLarge.serif(22, 30),
        titleMedium = base.titleMedium.sans(16, 24, FontWeight.SemiBold),
        titleSmall = base.titleSmall.sans(14, 20, FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.sans(16, 24, FontWeight.Normal),
        bodyMedium = base.bodyMedium.sans(14, 20, FontWeight.Normal),
        bodySmall = base.bodySmall.sans(12, 17, FontWeight.Normal),
        labelLarge = base.labelLarge.sans(14, 20, FontWeight.SemiBold),
        labelMedium = base.labelMedium.sans(12, 17, FontWeight.Medium),
        labelSmall = base.labelSmall.sans(11, 15, FontWeight.SemiBold),
    )
}

private val CozyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private const val SERIF_WEIGHT = 420
private const val SERIF_SOFTNESS = 100f

@Composable
fun CozitopiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CozyColors,
        typography = cozyTypography(),
        shapes = CozyShapes,
        content = content,
    )
}
