package app.cozitopia.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cozitopia.domain.Assignee
import app.cozitopia.domain.AssigneeKind
import app.cozitopia.domain.Homie

val LocalWideLayout = staticCompositionLocalOf { false }

val MinTouchTarget = 48.dp
private const val PRESSED_SCALE = 0.96f

@Composable
private fun rememberSquish(): Pair<MutableInteractionSource, Float> {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "squish",
    )
    return source to scale
}

private fun Modifier.scaled(scale: Float): Modifier = graphicsLayer {
    scaleX = scale
    scaleY = scale
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val (source, scale) = rememberSquish()
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.heightIn(min = MinTouchTarget).scaled(scale),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    val (source, scale) = rememberSquish()
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = source,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CreamCard,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier.heightIn(min = MinTouchTarget).scaled(scale),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        contentPadding = PaddingValues(horizontal = 14.dp),
        modifier = modifier.heightIn(min = MinTouchTarget),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

data class MenuAction(val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

@Composable
fun MenuButton(text: String, actions: List<MenuAction>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        SecondaryButton(
            text = text,
            onClick = { expanded = true },
            trailingIcon = Icons.Filled.KeyboardArrowDown,
            modifier = Modifier.fillMaxWidth(),
        )
        CozyMenu(expanded, actions) { expanded = false }
    }
}

@Composable
fun MoreButton(contentDescription: String, actions: List<MenuAction>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.MoreHoriz,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        CozyMenu(expanded, actions) { expanded = false }
    }
}

@Composable
private fun CozyMenu(expanded: Boolean, actions: List<MenuAction>, onDismiss: () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = CreamCard,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        actions.forEach { action ->
            DropdownMenuItem(
                text = { Text(action.label, style = MaterialTheme.typography.bodyMedium) },
                enabled = action.enabled,
                onClick = {
                    onDismiss()
                    action.onClick()
                },
                modifier = Modifier.heightIn(min = MinTouchTarget),
            )
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = isError,
            placeholder = placeholder?.let { { Text(it) } },
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailing,
            shape = MaterialTheme.shapes.medium,
            colors = cozyFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun cozyFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = CreamCard,
    focusedContainerColor = CreamCard,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = Navy,
)

@Composable
fun <T> SelectField(
    label: String?,
    selected: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        if (label != null) FieldLabel(label)
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(CreamCard)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                    .clickable(role = Role.DropdownList, onClickLabel = label) { expanded = true }
                    .padding(horizontal = 16.dp),
            ) {
                Text(selected, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CozyMenu(expanded, options.map { MenuAction(optionLabel(it)) { onSelect(it) } }) { expanded = false }
        }
    }
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable (Modifier) -> Unit)? = null,
) {
    val wide = LocalWideLayout.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = if (wide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (wide && action != null) {
                Spacer(Modifier.width(24.dp))
                action(Modifier.defaultMinSize(minWidth = 172.dp))
            }
        }
        if (!wide && action != null) action(Modifier.fillMaxWidth())
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    color: Color = CreamCard,
    bordered: Boolean = true,
    padding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = MaterialTheme.shapes.extraLarge,
        border = if (bordered) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun SoftChip(text: String, tone: Tone, modifier: Modifier = Modifier, bordered: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 26.dp)
            .clip(MaterialTheme.shapes.small)
            .background(tone.container)
            .then(if (bordered) Modifier.border(1.dp, tone.content.copy(alpha = 0.35f), MaterialTheme.shapes.small) else Modifier)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = tone.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun InitialAvatar(name: String, tone: Tone, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clip(CircleShape).background(tone.container),
    ) {
        Text(initials(name), style = MaterialTheme.typography.titleSmall, color = tone.content)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    SectionCard(modifier.fillMaxWidth(), padding = 32.dp) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(28.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) {
            Spacer(Modifier.size(4.dp))
            action()
        }
    }
}

@Composable
fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

fun toneOf(userId: String, homies: List<Homie>): Tone =
    homies.indexOfFirst { it.userId == userId }.takeIf { it >= 0 }?.let { HomieTones[it % HomieTones.size] } ?: NeutralTone

fun toneOf(assignee: Assignee, homies: List<Homie>): Tone = when (assignee.kind) {
    AssigneeKind.PEOPLE -> assignee.userIds.firstOrNull()?.let { toneOf(it, homies) } ?: NeutralTone
    AssigneeKind.EVERYONE -> HomieTones.last()
    AssigneeKind.UNASSIGNED -> NeutralTone
}
