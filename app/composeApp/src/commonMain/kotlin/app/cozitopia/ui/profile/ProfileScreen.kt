package app.cozitopia.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val saved by viewModel.displayName.collectAsState()
    var displayName by remember(saved) { mutableStateOf(saved) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = strings(StringKey.PROFILE_TITLE), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(strings(StringKey.DISPLAY_NAME)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { viewModel.save(displayName) }, enabled = displayName.isNotBlank()) {
            Text(strings(StringKey.SAVE))
        }
    }
}
