package fr.inventairerepas.ui.export

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/** Bouton d'export de la semaine (Google Docs, ou texte via le menu de partage) et ses dialogues. */
@Composable
fun ExportAction(weekStart: LocalDate?, viewModel: ExportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            viewModel.onAuthorizationFailed(null)
            return@rememberLauncherForActivityResult
        }
        runCatching {
            Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data)
        }.onSuccess { authorization ->
            authorization.accessToken?.let(viewModel::onAccessToken) ?: viewModel.onAuthorizationFailed(null)
        }.onFailure(viewModel::onAuthorizationFailed)
    }

    // Autorisation d'accès à Google Docs : directe si déjà accordée, sinon écran de consentement.
    LaunchedEffect(state.authorizeAccount) {
        val account = state.authorizeAccount ?: return@LaunchedEffect
        runCatching {
            Identity.getAuthorizationClient(context).authorize(GoogleAuth.authorizationRequest(account)).await()
        }.onSuccess { authorization ->
            val pending = authorization.pendingIntent
            val token = authorization.accessToken
            when {
                authorization.hasResolution() && pending != null ->
                    consentLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                token != null -> viewModel.onAccessToken(token)
                else -> viewModel.onAuthorizationFailed(null)
            }
        }.onFailure(viewModel::onAuthorizationFailed)
    }

    LaunchedEffect(state.share) {
        val share = state.share ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, share.title)
            putExtra(Intent.EXTRA_TEXT, share.text)
        }
        context.startActivity(Intent.createChooser(send, share.title))
        viewModel.shareLaunched()
    }

    IconButton(onClick = { weekStart?.let(viewModel::open) }, enabled = weekStart != null) {
        Icon(Icons.Filled.Share, contentDescription = "Exporter la semaine")
    }

    if (state.dialogOpen) {
        ExportDialog(
            state = state,
            onIncludeDetails = viewModel::setIncludeDetails,
            onGoogleDocs = viewModel::exportToGoogleDocs,
            onShareText = viewModel::prepareShare,
            onOpenDocument = { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                viewModel.dismiss()
            },
            onDismiss = viewModel::dismiss,
        )
    }
}

@Composable
private fun ExportDialog(
    state: ExportUiState,
    onIncludeDetails: (Boolean) -> Unit,
    onGoogleDocs: () -> Unit,
    onShareText: () -> Unit,
    onOpenDocument: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val url = state.documentUrl
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (url != null) "Document créé" else "Exporter la semaine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    url != null -> Text("Le menu et les listes de courses sont dans un nouveau Google Doc de ton Drive.")
                    state.busy -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Text("  Export en cours…")
                    }
                    else -> {
                        Text("Menu de la semaine et les deux listes de courses.")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = state.includeDetails, onCheckedChange = onIncludeDetails)
                            Text("Inclure le détail des recettes")
                        }
                        state.fallbackReason?.let {
                            Text(it)
                            Text("Tu peux partager le même contenu en texte.")
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                url != null -> TextButton(onClick = { onOpenDocument(url) }) { Text("Ouvrir") }
                state.busy -> Unit
                state.fallbackReason != null -> TextButton(onClick = onShareText) { Text("Partager en texte") }
                else -> TextButton(onClick = onGoogleDocs) { Text("Google Docs") }
            }
        },
        dismissButton = {
            Row {
                if (url == null && !state.busy && state.fallbackReason == null) {
                    TextButton(onClick = onShareText) { Text("Texte") }
                }
                TextButton(onClick = onDismiss) { Text(if (url != null) "Fermer" else "Annuler") }
            }
        },
    )
}
