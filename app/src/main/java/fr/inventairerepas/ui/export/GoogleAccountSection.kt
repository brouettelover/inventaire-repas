package fr.inventairerepas.ui.export

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
import androidx.credentials.exceptions.GetCredentialCancellationException
import kotlinx.coroutines.launch

/** Section « Compte Google » des réglages : connexion avec Credential Manager. */
@Composable
fun GoogleAccountSection(email: String?, onAccountChange: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!GoogleAuth.isConfigured) {
            Text(
                "Export Google Docs non configuré : ajoute google.webClientId dans local.properties puis recompile.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (email == null) {
            Text("Non connecté. Sans compte, l'export propose un partage en texte.")
            Button(
                enabled = GoogleAuth.isConfigured,
                onClick = {
                    error = null
                    scope.launch {
                        runCatching { GoogleAuth.signIn(context) }
                            .onSuccess(onAccountChange)
                            .onFailure { if (it !is GetCredentialCancellationException) error = it.message }
                    }
                },
            ) { Text("Se connecter avec Google") }
        } else {
            Text("Connecté : $email")
            OutlinedButton(onClick = {
                scope.launch {
                    runCatching { GoogleAuth.signOut(context) }
                    onAccountChange(null)
                }
            }) { Text("Se déconnecter") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
