package app.cozitopia.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CozyColors = lightColorScheme(
    primary = Color(0xFF8A5A44),
    secondary = Color(0xFF6B7F5E),
    background = Color(0xFFFFF8F2),
    surface = Color(0xFFFFF8F2),
)

@Composable
fun CozitopiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CozyColors, content = content)
}
