package com.ismael.daybyday.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ismael.daybyday.data.Prefs
import com.ismael.daybyday.ui.theme.Brand

@Composable
fun LockScreen(prefs: Prefs, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val biometricAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(Unit) {
        if (prefs.biometricEnabled && biometricAvailable) {
            showBiometricPrompt(context, onUnlocked)
        }
    }

    // C'est le premier ecran de la journee : il doit accueillir, pas barrer la
    // route. Un fond vivant, un cadenas dans un rond de la couleur de
    // l'application, et le reste dans une carte blanche posee dessus.
    ScreenBackground(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .brandShadow(elevation = 20.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(Brand.gradient)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White,
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = androidx.compose.ui.text.buildAnnotatedString {
                append("Ton ")
                withStyle(
                    androidx.compose.ui.text.SpanStyle(
                        fontFamily = com.ismael.daybyday.ui.theme.Serif,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    )
                ) {
                    append("journal")
                }
            },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Il t'attend, bien fermé.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(26.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { input ->
                pin = input.filter { it.isDigit() }.take(8)
                error = false
            },
            label = { Text("Code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            isError = error,
        )

        if (error) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Code incorrect.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (prefs.checkPin(pin)) {
                    pin = ""
                    onUnlocked()
                } else {
                    error = true
                }
            },
            enabled = pin.length >= 4,
        ) {
            Text("Déverrouiller")
        }

        if (prefs.biometricEnabled && biometricAvailable) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { showBiometricPrompt(context, onUnlocked) }) {
                Text("Utiliser l'empreinte")
            }
        }
    }
}
}

private fun showBiometricPrompt(context: Context, onSuccess: () -> Unit) {
    val activity = context.findActivity() as? FragmentActivity ?: return
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(context),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("DayByDay")
        .setSubtitle("Déverrouille ton journal")
        .setNegativeButtonText("Utiliser le code")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
}
