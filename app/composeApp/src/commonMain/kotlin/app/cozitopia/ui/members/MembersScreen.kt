package app.cozitopia.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cozitopia.data.Member
import app.cozitopia.data.Membership
import app.cozitopia.domain.Role
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.key
import app.cozitopia.ui.CreamCard
import app.cozitopia.ui.HintText
import app.cozitopia.ui.HomieTones
import app.cozitopia.ui.InitialAvatar
import app.cozitopia.ui.LabeledTextField
import app.cozitopia.ui.LocalWideLayout
import app.cozitopia.ui.MenuAction
import app.cozitopia.ui.MoreButton
import app.cozitopia.ui.NeutralTone
import app.cozitopia.ui.PageHeader
import app.cozitopia.ui.PrimaryButton
import app.cozitopia.ui.ScreenColumn
import app.cozitopia.ui.SecondaryButton
import app.cozitopia.ui.SectionCard
import app.cozitopia.ui.SectionTitle
import app.cozitopia.ui.SelectField
import app.cozitopia.ui.SoftChip
import app.cozitopia.ui.TertiaryButton
import app.cozitopia.ui.toneOf

@Composable
fun MembersScreen(viewModel: MembersViewModel, myUserId: String, membership: Membership) {
    val strings = LocalStrings.current
    val members by viewModel.members.collectAsState()
    LaunchedEffect(viewModel) { viewModel.refresh() }
    val isAdmin = membership.role == Role.ADMIN
    var confirmingLeave by remember { mutableStateOf(false) }
    val wide = LocalWideLayout.current

    val memberList: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle(strings(StringKey.MEMBERS_SECTION), trailing = members.size.toString())
            SectionCard(Modifier.fillMaxWidth(), padding = 8.dp) {
                members.forEachIndexed { index, member ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    MemberRow(member, members, member.homie.userId == myUserId, isAdmin, viewModel)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(strings(StringKey.LEAVE_HOME), { confirmingLeave = true })
                HintText(strings(StringKey.LEAVE_HINT))
            }
        }
    }
    val sidePanel: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (isAdmin) {
                SectionTitle(strings(StringKey.INVITE_SECTION))
                InviteCard(viewModel)
                HintText(strings(StringKey.ADMIN_NOTE))
            } else {
                SectionCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer, bordered = false) {
                    Text(strings(StringKey.INVITE_SECTION), style = MaterialTheme.typography.headlineSmall)
                    Text(strings(StringKey.HOMIE_NOTE), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    ScreenColumn {
        PageHeader(strings(StringKey.MEMBERS_TITLE), strings(StringKey.SUBTITLE_MEMBERS))
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                memberList(Modifier.weight(1.6f))
                sidePanel(Modifier.weight(1f))
            }
        } else {
            memberList(Modifier.fillMaxWidth())
            sidePanel(Modifier.fillMaxWidth())
        }
    }

    if (confirmingLeave) {
        AlertDialog(
            onDismissRequest = { confirmingLeave = false },
            containerColor = CreamCard,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(strings(StringKey.LEAVE_HOME), style = MaterialTheme.typography.headlineSmall) },
            text = { Text(strings(StringKey.LEAVE_CONFIRM, "home" to membership.home.name)) },
            confirmButton = {
                PrimaryButton(strings(StringKey.LEAVE_HOME), {
                    confirmingLeave = false
                    viewModel.leave(myUserId)
                })
            },
            dismissButton = { TertiaryButton(strings(StringKey.CANCEL), { confirmingLeave = false }) },
        )
    }
}

@Composable
private fun MemberRow(member: Member, members: List<Member>, isMe: Boolean, isAdmin: Boolean, viewModel: MembersViewModel) {
    val strings = LocalStrings.current
    val homie = member.homie
    val wide = LocalWideLayout.current
    val roleControl: @Composable (Modifier) -> Unit = { modifier ->
        if (isAdmin) {
            SelectField(
                label = null,
                selected = strings(homie.role.key()),
                options = Role.entries,
                optionLabel = { strings(it.key()) },
                onSelect = { viewModel.changeRole(homie.userId, it) },
                modifier = modifier.width(140.dp),
            )
        } else {
            SoftChip(strings(homie.role.key()), if (homie.role == Role.ADMIN) HomieTones[0] else NeutralTone, modifier)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InitialAvatar(homie.name, toneOf(homie.userId, members.map { it.homie }))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (isMe) "${homie.name} · ${strings(StringKey.YOU)}" else homie.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (wide) roleControl(Modifier)
            if (isAdmin && !isMe) {
                MoreButton(
                    strings(StringKey.MORE_ACTIONS),
                    listOf(MenuAction(strings(StringKey.REMOVE)) { viewModel.remove(homie.userId) }),
                )
            }
        }
        if (!wide) roleControl(Modifier.padding(start = 60.dp))
    }
}

@Composable
private fun InviteCard(viewModel: MembersViewModel) {
    val strings = LocalStrings.current
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.HOMIE) }
    SectionCard(Modifier.fillMaxWidth()) {
        Text(strings(StringKey.INVITE_TITLE), style = MaterialTheme.typography.headlineSmall)
        LabeledTextField(
            label = strings(StringKey.EMAIL),
            value = email,
            onValueChange = { email = it },
            placeholder = "homie@example.com",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        SelectField(
            label = strings(StringKey.ROLE),
            selected = strings(role.key()),
            options = Role.entries,
            optionLabel = { strings(it.key()) },
            onSelect = { role = it },
        )
        PrimaryButton(
            text = strings(StringKey.INVITE),
            onClick = {
                viewModel.invite(email, role)
                email = ""
            },
            enabled = email.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}
