package app.cozitopia.ui.homes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import app.cozitopia.data.Membership
import app.cozitopia.domain.Invitation
import app.cozitopia.domain.Role
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.key
import app.cozitopia.ui.CreamCard
import app.cozitopia.ui.EmptyState
import app.cozitopia.ui.HintText
import app.cozitopia.ui.HomieTones
import app.cozitopia.ui.LabeledTextField
import app.cozitopia.ui.LocalWideLayout
import app.cozitopia.ui.NeutralTone
import app.cozitopia.ui.PageHeader
import app.cozitopia.ui.PrimaryButton
import app.cozitopia.ui.ScreenColumn
import app.cozitopia.ui.SecondaryButton
import app.cozitopia.ui.SectionCard
import app.cozitopia.ui.SectionTitle
import app.cozitopia.ui.SoftChip
import app.cozitopia.ui.TertiaryButton

private sealed interface NameEdit {
    data object Create : NameEdit
    data class Rename(val membership: Membership) : NameEdit
}

@Composable
fun HomesScreen(viewModel: HomesViewModel) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    var nameEdit by remember { mutableStateOf<NameEdit?>(null) }
    val wide = LocalWideLayout.current

    ScreenColumn {
        PageHeader(strings(StringKey.HOMES_TITLE), strings(StringKey.SUBTITLE_HOMES)) { modifier ->
            PrimaryButton(strings(StringKey.CREATE_HOME), { nameEdit = NameEdit.Create }, modifier)
        }
        if (state.memberships.isEmpty() && state.invitations.isEmpty()) {
            EmptyState(Icons.Outlined.Cottage, strings(StringKey.NO_HOMES_TITLE), strings(StringKey.NO_HOMES_BODY))
            return@ScreenColumn
        }
        val homes: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.memberships.isNotEmpty()) SectionTitle(strings(StringKey.YOUR_HOMES))
                state.memberships.forEach { membership ->
                    HomeCard(
                        membership = membership,
                        isCurrent = membership.home.id == state.current?.home?.id,
                        onSelect = { viewModel.select(membership.home.id) },
                        onRename = { nameEdit = NameEdit.Rename(membership) },
                    )
                }
            }
        }
        val invitations: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionTitle(strings(StringKey.PENDING_INVITATIONS))
                state.invitations.forEach { invitation ->
                    InvitationCard(invitation, { viewModel.accept(invitation.id) }, { viewModel.decline(invitation.id) })
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                homes(Modifier.weight(1.6f))
                if (state.invitations.isNotEmpty()) invitations(Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            }
        } else {
            homes(Modifier.fillMaxWidth())
            if (state.invitations.isNotEmpty()) invitations(Modifier.fillMaxWidth())
        }
        HintText(strings(StringKey.HOMES_NOTE))
    }

    when (val edit = nameEdit) {
        null -> Unit
        NameEdit.Create -> NameDialog(
            title = strings(StringKey.CREATE_HOME),
            initial = "",
            onConfirm = { viewModel.create(it) },
            onDismiss = { nameEdit = null },
        )
        is NameEdit.Rename -> NameDialog(
            title = strings(StringKey.RENAME_HOME),
            initial = edit.membership.home.name,
            onConfirm = { viewModel.rename(edit.membership.home.id, it) },
            onDismiss = { nameEdit = null },
        )
    }
}

@Composable
private fun HomeCard(membership: Membership, isCurrent: Boolean, onSelect: () -> Unit, onRename: () -> Unit) {
    val strings = LocalStrings.current
    SectionCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isCurrent) SoftChip(strings(StringKey.CURRENT_HOME), HomieTones[1])
            Spacer(Modifier.weight(1f))
            SoftChip(strings(membership.role.key()), if (membership.role == Role.ADMIN) HomieTones[0] else NeutralTone)
        }
        Text(membership.home.name, style = MaterialTheme.typography.headlineSmall)
        when {
            isCurrent && membership.role == Role.ADMIN -> TertiaryButton(strings(StringKey.RENAME_HOME), onRename)
            !isCurrent -> TertiaryButton(strings(StringKey.SELECT_HOME), onSelect)
        }
    }
}

@Composable
private fun InvitationCard(invitation: Invitation, onAccept: () -> Unit, onDecline: () -> Unit) {
    val strings = LocalStrings.current
    SectionCard(Modifier.fillMaxWidth()) {
        Text(invitation.homeName, style = MaterialTheme.typography.headlineSmall)
        Text(
            strings(StringKey.INVITED_AS, "role" to strings(invitation.role.key())),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(strings(StringKey.ACCEPT), onAccept)
            TertiaryButton(strings(StringKey.DECLINE), onDecline)
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CreamCard,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { LabeledTextField(strings(StringKey.HOME_NAME), name, { name = it }) },
        confirmButton = {
            PrimaryButton(
                text = strings(StringKey.SAVE),
                onClick = {
                    onConfirm(name)
                    onDismiss()
                },
                enabled = name.isNotBlank() && name.trim() != initial,
            )
        },
        dismissButton = { TertiaryButton(strings(StringKey.CANCEL), onDismiss) },
    )
}
