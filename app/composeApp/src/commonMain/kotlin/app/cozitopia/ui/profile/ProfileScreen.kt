package app.cozitopia.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.key
import app.cozitopia.ui.FieldLabel
import app.cozitopia.ui.HintText
import app.cozitopia.ui.LabeledTextField
import app.cozitopia.ui.LocalWideLayout
import app.cozitopia.ui.PageHeader
import app.cozitopia.ui.PrimaryButton
import app.cozitopia.ui.ScreenColumn
import app.cozitopia.ui.SectionCard
import app.cozitopia.ui.SelectField
import app.cozitopia.ui.TertiaryButton
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

private val HALF_HOURS = (0 until 48).map { LocalTime(it / 2, it % 2 * 30) }

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, email: String, onSignOut: () -> Unit) {
    val strings = LocalStrings.current
    val saved by viewModel.displayName.collectAsState()
    val window by viewModel.window.collectAsState()
    var displayName by remember(saved) { mutableStateOf(saved) }
    val wide = LocalWideLayout.current

    val forms: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SectionCard(Modifier.fillMaxWidth()) {
                Text(strings(StringKey.PERSONAL_DETAILS), style = MaterialTheme.typography.headlineSmall)
                LabeledTextField(strings(StringKey.DISPLAY_NAME), displayName, { displayName = it })
                Column {
                    FieldLabel(strings(StringKey.EMAIL))
                    Text(email, style = MaterialTheme.typography.bodyLarge)
                }
            }
            SectionCard(Modifier.fillMaxWidth()) {
                Text(strings(StringKey.CHORE_TIME), style = MaterialTheme.typography.headlineSmall)
                SelectField(
                    label = strings(StringKey.PREFERRED_WEEKDAY),
                    selected = strings(window.weekday.key()),
                    options = DayOfWeek.entries,
                    optionLabel = { strings(it.key()) },
                    onSelect = { viewModel.setWindow(window.copy(weekday = it)) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    SelectField(
                        label = strings(StringKey.PREFERRED_START),
                        selected = window.start.toString(),
                        options = HALF_HOURS,
                        optionLabel = { it.toString() },
                        onSelect = { viewModel.setWindow(window.copy(start = it)) },
                        modifier = Modifier.weight(1f),
                    )
                    SelectField(
                        label = strings(StringKey.LATEST_END),
                        selected = window.end.toString(),
                        options = HALF_HOURS,
                        optionLabel = { it.toString() },
                        onSelect = { viewModel.setWindow(window.copy(end = it)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    val aside: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, bordered = false) {
                Text(strings(StringKey.RHYTHM_TITLE), style = MaterialTheme.typography.headlineSmall)
                Text(strings(StringKey.RHYTHM_BODY), style = MaterialTheme.typography.bodyMedium)
            }
            HintText(strings(StringKey.HALF_HOUR_NOTE))
            TertiaryButton(strings(StringKey.LOG_OUT), onSignOut)
        }
    }

    ScreenColumn {
        PageHeader(strings(StringKey.PROFILE_TITLE), strings(StringKey.SUBTITLE_PROFILE)) { modifier ->
            PrimaryButton(
                text = strings(StringKey.SAVE_CHANGES),
                onClick = { viewModel.save(displayName) },
                enabled = displayName.isNotBlank() && displayName.trim() != saved,
                modifier = modifier,
            )
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                forms(Modifier.weight(1.6f))
                aside(Modifier.weight(1f))
            }
        } else {
            forms(Modifier.fillMaxWidth())
            aside(Modifier.fillMaxWidth())
        }
    }
}
