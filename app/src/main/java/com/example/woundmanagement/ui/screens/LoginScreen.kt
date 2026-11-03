package com.example.woundmanagement.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.woundmanagement.security.BiometricCryptoManager
import com.example.woundmanagement.security.RootDetector
import com.example.woundmanagement.ui.strings.S
import com.example.woundmanagement.ui.strings.loc4
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.woundmanagement.ui.components.brandGradient
import com.example.woundmanagement.ui.theme.BrandIndigo

// Token arbitrario cifrato al login per esercitare la chiave Keystore auth-bound (l'output
// viene scartato: conta solo che l'operazione riesca, prova che la chiave è stata sbloccata).
private val LOGIN_TOKEN = "wounddocs-login".toByteArray()

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val activity = context as FragmentActivity

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var showRootWarning by remember { mutableStateOf(false) }

    // Pre-resolve strings needed in non-composable callbacks
    val strNoEnrolled = S.loginNoEnrolled
    val strNoHardware = S.loginNoHardware
    val strNotRecognized = S.loginNotRecognized
    val strLockout = S.loginErrorLockout
    val strGeneric = S.loginErrorGeneric
    val strBiometricTitle = S.loginBiometricTitle
    val strBiometricSubtitle = S.loginBiometricSubtitle

    fun launchBiometric() {
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        val biometricManager = BiometricManager.from(context)

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                errorMessage = strNoEnrolled
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                errorMessage = strNoHardware
                return
            }
        }

        isAuthenticating = true
        errorMessage = null

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                isAuthenticating = false
                // Esercita realmente la chiave Keystore auth-bound: il CryptoObject viene
                // sbloccato dall'autenticazione e doFinal deve riuscire. Se la chiave non è
                // stata sbloccata (es. hook/bypass del flusso), doFinal lancia e il login NON
                // prosegue. Su API < 30 il cipher è null e si procede come prima.
                val cipherUnlocked = try {
                    result.cryptoObject?.cipher?.doFinal(LOGIN_TOKEN)
                    true
                } catch (_: Exception) {
                    false
                }
                if (!cipherUnlocked) {
                    errorMessage = strGeneric
                    return
                }
                if (RootDetector.isDeviceCompromised(context)) {
                    showRootWarning = true
                } else {
                    onLoginSuccess()
                }
            }
            override fun onAuthenticationFailed() {
                errorMessage = strNotRecognized
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                isAuthenticating = false
                if (com.example.woundmanagement.BuildConfig.DEBUG) {
                    android.util.Log.d("LoginBiometric", "auth error code=$errorCode: $errString")
                }
                // Mappa i codici principali a messaggi localizzati (mai il testo di sistema).
                errorMessage = when (errorCode) {
                    // Annullato dall'utente: nessun errore rumoroso, stato pulito.
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> null
                    // Troppi tentativi.
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> strLockout
                    // Nessuna biometria/credenziale registrata.
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> strNoEnrolled
                    // Hardware non disponibile o assente.
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> strNoHardware
                    // Fallback generico localizzato.
                    else -> strGeneric
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(strBiometricTitle)
            .setSubtitle(strBiometricSubtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        val cryptoCipher = BiometricCryptoManager.getCipher()
        if (cryptoCipher != null) {
            BiometricPrompt(activity, executor, callback)
                .authenticate(promptInfo, BiometricPrompt.CryptoObject(cryptoCipher))
        } else {
            BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
        }
    }

    // Lancia la biometria quando la schermata di blocco è in primo piano e la RILANCIA a ogni
    // ritorno in foreground. Fix: con LaunchedEffect(Unit) il prompt partiva una sola volta,
    // perciò al rientro dal background (o dopo che il prompt era stato chiuso/annullato) non
    // ripartiva il riconoscimento. addObserver, se l'app è già RESUMED, dispatcia subito ON_RESUME.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // Allo stop/pausa azzera lo stato: se il prompt è stato chiuso senza callback,
                // al rientro (ON_RESUME) il rilancio è sempre possibile (niente stato "bloccato").
                Lifecycle.Event.ON_PAUSE -> isAuthenticating = false
                Lifecycle.Event.ON_RESUME ->
                    if (!isAuthenticating && !showRootWarning) launchBiometric()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFE7E9FB), MaterialTheme.colorScheme.background),
                    radius = 1400f
                )
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo: riquadro a gradiente con l'impronta bianca (coerente con l'icona dell'app)
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(brandGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            S.loginTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            S.loginSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Pulsante a gradiente (Box cliccabile: consente il riempimento con brush)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brandGradient)
                .alpha(if (isAuthenticating) 0.6f else 1f)
                .clickable(enabled = !isAuthenticating) { launchBiometric() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(S.loginButton, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            loc4(
                it = "Solo documentazione · non è un dispositivo medico",
                en = "Documentation only · not a medical device",
                fr = "Documentation uniquement · pas un dispositif médical",
                es = "Solo documentación · no es un dispositivo médico"
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    if (showRootWarning) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(loc4(it = "Avviso di sicurezza", en = "Security Warning", fr = "Avertissement de sécurité", es = "Aviso de seguridad"))
            },
            text = {
                Text(
                    loc4(
                        it = "Questo dispositivo risulta rootato o compromesso. " +
                            "La sicurezza dei dati non può essere garantita. " +
                            "Procedere con cautela.",
                        en = "This device appears to be rooted or compromised. " +
                            "Data security cannot be guaranteed. Proceed with caution.",
                        fr = "Cet appareil semble être rooté ou compromis. " +
                            "La sécurité des données ne peut être garantie. " +
                            "Procédez avec prudence.",
                        es = "Este dispositivo parece estar rooteado o comprometido. " +
                            "No se puede garantizar la seguridad de los datos. " +
                            "Proceda con precaución."
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    showRootWarning = false
                    onLoginSuccess()
                }) {
                    Text(loc4(it = "Procedi", en = "Proceed", fr = "Continuer", es = "Continuar"))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { activity.finish() }) {
                    Text(loc4(it = "Esci", en = "Exit", fr = "Quitter", es = "Salir"))
                }
            }
        )
    }
}
