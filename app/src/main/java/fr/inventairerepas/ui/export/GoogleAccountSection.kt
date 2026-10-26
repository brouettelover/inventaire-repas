package fr.inventairerepas.ui.export

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    var success by remember { mutableStateOf<String?>(null) }

    fun finish(result: AuthorizationResult) {
        scope.launch {
            val account = GoogleAuth.email(result)
            busy = false
            if (account != null) {
                onAccountChange(account)
                success = "Connexion réussie. L'export Google Docs est prêt."
            } else {
                error = "Google n'a pas transmis l'adresse du compte. Vérifie ta connexion internet et réessaie."
            }
        }
    }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        // Même en cas d'échec, l'intent de retour contient le code d'erreur de Google : on le lit pour l'afficher.
        runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data) }
            .onSuccess(::finish)
            .onFailure {
                busy = false
                error = GoogleAuth.errorMessage(it, cancelled = result.resultCode != Activity.RESULT_OK)
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ConnectionStatus(email = email, busy = busy)
        if (email == null) {
            Text("Sans compte, l'export propose un partage en texte.", style = MaterialTheme.typography.bodySmall)
            Button(
                enabled = !busy,
                onClick = {
                    error = null
                    success = null
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
                                error = GoogleAuth.errorMessage(it, cancelled = false)
                            }
                    }
                },
            ) { Text("Se connecter avec Google") }
        } else {
            OutlinedButton(onClick = {
                success = null
                error = null
                onAccountChange(null)
            }) { Text("Se déconnecter") }
        }
        success?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

/** État de connexion : pastille colorée et texte. */
@Composable
private fun ConnectionStatus(email: String?, busy: Boolean) {
    val (label, color) = when {
        busy -> "Connexion en cours…" to MaterialTheme.colorScheme.tertiary
        email != null -> "Connecté : $email" to Color(0xFF2E7D32)
        else -> "Non connecté" to MaterialTheme.colorScheme.outline
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
        } else {
            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
