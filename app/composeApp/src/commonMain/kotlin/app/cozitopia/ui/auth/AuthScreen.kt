package app.cozitopia.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.cozitopia.i18n.AppLanguage
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.ui.LabeledTextField
import app.cozitopia.ui.LanguageMenu
import app.cozitopia.ui.LocalWideLayout
import app.cozitopia.ui.PrimaryButton
import app.cozitopia.ui.SectionCard
import app.cozitopia.ui.TertiaryButton
import app.cozitopia.ui.Wordmark
import cozitopia.composeapp.generated.resources.Res
import cozitopia.composeapp.generated.resources.cozy_cloud_sofa
import org.jetbrains.compose.resources.painterResource

@Composable
fun AuthScreen(viewModel: AuthViewModel, language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    val wide = LocalWideLayout.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 64.dp else 24.dp, vertical = if (wide) 32.dp else 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Wordmark(Modifier.weight(1f))
            LanguageMenu(language, onLanguageChange)
        }
        Spacer(Modifier.size(if (wide) 48.dp else 24.dp))
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Hero(Modifier.weight(1f))
                SectionCard(Modifier.width(488.dp), padding = 40.dp) { AuthForm(viewModel) }
            }
        } else {
            Hero(Modifier.fillMaxWidth())
            Spacer(Modifier.size(32.dp))
            AuthForm(viewModel)
        }
    }
}

@Composable
private fun Hero(modifier: Modifier) {
    val strings = LocalStrings.current
    val wide = LocalWideLayout.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            strings(StringKey.TAGLINE),
            style = if (wide) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displaySmall,
            modifier = Modifier.widthIn(max = 560.dp),
        )
        Text(
            strings(StringKey.AUTH_HERO_BODY),
            style = if (wide) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (wide) {
            Text(
                strings(StringKey.AUTH_HERO_NOTE),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(Modifier.padding(top = 16.dp)) {
                Image(
                    painter = painterResource(Res.drawable.cozy_cloud_sofa),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .aspectRatio(1344f / 768f)
                        .clip(MaterialTheme.shapes.extraLarge),
                )
            }
        }
    }
}

@Composable
private fun AuthForm(viewModel: AuthViewModel) {
    val strings = LocalStrings.current
    val awaitingConfirmation by viewModel.awaitingEmailConfirmation.collectAsState()
    var signUpMode by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val canSubmit = email.isNotBlank() && password.isNotEmpty() && (!signUpMode || displayName.isNotBlank())

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                strings(if (signUpMode) StringKey.WELCOME_NEW else StringKey.WELCOME_BACK),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                strings(if (signUpMode) StringKey.WELCOME_NEW_BODY else StringKey.WELCOME_BACK_BODY),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (signUpMode) {
            LabeledTextField(strings(StringKey.DISPLAY_NAME), displayName, { displayName = it })
        }
        LabeledTextField(
            label = strings(StringKey.EMAIL),
            value = email,
            onValueChange = { email = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        LabeledTextField(
            label = strings(StringKey.PASSWORD),
            value = password,
            onValueChange = { password = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                TertiaryButton(
                    strings(if (showPassword) StringKey.HIDE_PASSWORD else StringKey.SHOW_PASSWORD),
                    { showPassword = !showPassword },
                )
            },
        )
        if (awaitingConfirmation) {
            Text(
                strings(StringKey.CONFIRM_EMAIL_NOTICE),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        PrimaryButton(
            text = strings(if (signUpMode) StringKey.SIGN_UP else StringKey.LOG_IN),
            onClick = {
                if (signUpMode) viewModel.signUp(email, password, displayName) else viewModel.signIn(email, password)
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        )
        TertiaryButton(
            strings(if (signUpMode) StringKey.SWITCH_TO_LOG_IN else StringKey.SWITCH_TO_SIGN_UP),
            { signUpMode = !signUpMode },
            Modifier.align(Alignment.CenterHorizontally),
        )
    }
}
