package app.cozitopia.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey

@Composable
fun AuthScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val awaitingConfirmation by viewModel.awaitingEmailConfirmation.collectAsState()
    var signUpMode by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = strings(if (signUpMode) StringKey.SIGN_UP else StringKey.LOG_IN),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (signUpMode) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(strings(StringKey.DISPLAY_NAME)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(strings(StringKey.EMAIL)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(strings(StringKey.PASSWORD)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (awaitingConfirmation) {
                Text(text = strings(StringKey.CONFIRM_EMAIL_NOTICE), color = MaterialTheme.colorScheme.secondary)
            }
            Button(
                onClick = {
                    if (signUpMode) viewModel.signUp(email, password, displayName)
                    else viewModel.signIn(email, password)
                },
                enabled = email.isNotBlank() && password.isNotEmpty() && (!signUpMode || displayName.isNotBlank()),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings(if (signUpMode) StringKey.SIGN_UP else StringKey.LOG_IN))
            }
            TextButton(onClick = { signUpMode = !signUpMode }, modifier = Modifier.fillMaxWidth()) {
                Text(strings(if (signUpMode) StringKey.SWITCH_TO_LOG_IN else StringKey.SWITCH_TO_SIGN_UP))
            }
        }
    }
}
