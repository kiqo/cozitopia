package app.cozitopia.ui.homes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cozitopia.domain.DEFAULT_HOME_NAME
import app.cozitopia.domain.Role
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.key

@Composable
fun HomesScreen(viewModel: HomesViewModel, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    var newHomeName by remember { mutableStateOf(DEFAULT_HOME_NAME) }
    val current = state.current
    var renamed by remember(current?.home?.id, current?.home?.name) {
        mutableStateOf(current?.home?.name.orEmpty())
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.invitations.isNotEmpty()) {
            Text(text = strings(StringKey.PENDING_INVITATIONS), style = MaterialTheme.typography.titleLarge)
            state.invitations.forEach { invitation ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = strings(
                                StringKey.INVITATION_TEXT,
                                "home" to invitation.homeName,
                                "role" to strings(invitation.role.key()),
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { viewModel.accept(invitation.id) }) { Text(strings(StringKey.ACCEPT)) }
                        OutlinedButton(onClick = { viewModel.decline(invitation.id) }) {
                            Text(strings(StringKey.DECLINE))
                        }
                    }
                }
            }
        }
        Text(text = strings(StringKey.HOMES_TITLE), style = MaterialTheme.typography.titleLarge)
        state.memberships.forEach { membership ->
            val isCurrent = membership.home.id == current?.home?.id
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${membership.home.name} (${strings(membership.role.key())})",
                        modifier = Modifier.weight(1f),
                    )
                    if (isCurrent) {
                        Text(text = strings(StringKey.CURRENT_HOME), color = MaterialTheme.colorScheme.secondary)
                    } else {
                        OutlinedButton(onClick = { viewModel.select(membership.home.id) }) {
                            Text(strings(StringKey.SELECT_HOME))
                        }
                    }
                }
            }
        }
        if (current != null && current.role == Role.ADMIN) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = renamed,
                    onValueChange = { renamed = it },
                    label = { Text(strings(StringKey.HOME_NAME)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { viewModel.rename(current.home.id, renamed) },
                    enabled = renamed.isNotBlank() && renamed.trim() != current.home.name,
                ) {
                    Text(strings(StringKey.RENAME_HOME))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newHomeName,
                onValueChange = { newHomeName = it },
                label = { Text(strings(StringKey.HOME_NAME)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { viewModel.create(newHomeName) }) { Text(strings(StringKey.CREATE_HOME)) }
        }
    }
}
