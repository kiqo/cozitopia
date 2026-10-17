package app.cozitopia.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.cozitopia.domain.Role
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.key
import app.cozitopia.ui.DropdownSelect

@Composable
fun MembersScreen(
    viewModel: MembersViewModel,
    myUserId: String,
    myRole: Role,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val members by viewModel.members.collectAsState()
    val isAdmin = myRole == Role.ADMIN
    var inviteEmail by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf(Role.HOMIE) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = strings(StringKey.MEMBERS_TITLE), style = MaterialTheme.typography.titleLarge)
        members.forEach { member ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = member.homie.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = member.email, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isAdmin) {
                        DropdownSelect(
                            selected = strings(member.homie.role.key()),
                            options = Role.entries,
                            optionLabel = { strings(it.key()) },
                            onSelect = { viewModel.changeRole(member.homie.userId, it) },
                        )
                        IconButton(onClick = { viewModel.remove(member.homie.userId) }) {
                            Icon(Icons.Filled.Delete, contentDescription = strings(StringKey.REMOVE))
                        }
                    } else {
                        Text(text = strings(member.homie.role.key()))
                    }
                }
            }
        }
        if (isAdmin) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = inviteEmail,
                    onValueChange = { inviteEmail = it },
                    label = { Text(strings(StringKey.EMAIL)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                DropdownSelect(
                    selected = strings(inviteRole.key()),
                    options = Role.entries,
                    optionLabel = { strings(it.key()) },
                    onSelect = { inviteRole = it },
                )
                Button(
                    onClick = {
                        viewModel.invite(inviteEmail, inviteRole)
                        inviteEmail = ""
                    },
                    enabled = inviteEmail.isNotBlank(),
                ) {
                    Text(strings(StringKey.INVITE))
                }
            }
        }
        OutlinedButton(onClick = { viewModel.leave(myUserId) }) { Text(strings(StringKey.LEAVE_HOME)) }
    }
}
