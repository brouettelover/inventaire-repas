package fr.inventairerepas.ui.export

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Section « Compte Google » des réglages : choix du compte et autorisation Google Docs en une étape. */
@Composable
fun GoogleAccountSection(email: String?, onAccountChange: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun finish(result: AuthorizationResult) {
        busy = false
        val account = GoogleAuth.email(result)
        if (account != null) onAccountChange(account) else error = "Google n'a pas transmis l'adresse du compte."
    }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            busy = false
            return@rememberLauncherForActivityResult
        }
        runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data) }
            .onSuccess(::finish)
            .onFailure {
                busy = false
                error = it.message
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (email == null) {
            Text("Non connecté. Sans compte, l'export propose un partage en texte.")
            Button(
                enabled = !busy,
                onClick = {
                    error = null
                    busy = true
                    scope.launch {
                        runCatching { Identity.getAuthorizationClient(context).authorize(GoogleAuth.signInRequest()).await() }
                            .onSuccess { result ->
                                val pending = result.pendingIntent
                                if (result.hasResolution() && pending != null) {
                                    consentLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                                } else {
                                    finish(result)
                                }
                            }
                            .onFailure {
                                busy = false
                                error = "Connexion impossible : ${it.message}. Vérifie l'identifiant Android (package et SHA-1) dans la Cloud Console."
                            }
                    }
                },
            ) { Text("Se connecter avec Google") }
        } else {
            Text("Connecté : $email")
            OutlinedButton(onClick = { onAccountChange(null) }) { Text("Se déconnecter") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
