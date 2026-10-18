package fr.inventairerepas.ui.export

import android.accounts.Account
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import fr.inventairerepas.BuildConfig

/**
 * Authentification Google :
 * - connexion au compte avec Credential Manager (bouton « Se connecter avec Google ») ;
 * - autorisation d'accès à Google Docs avec l'Authorization API, au moment de l'export.
 */
object GoogleAuth {

    /** Portée minimale : l'application ne voit que les fichiers qu'elle crée dans le Drive. */
    const val DOCS_SCOPE = "https://www.googleapis.com/auth/drive.file"

    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Ouvre le sélecteur de compte Google et renvoie l'adresse du compte choisi. [context] doit être une Activity. */
    suspend fun signIn(context: Context): String {
        check(isConfigured) {
            "Identifiant client Google manquant : ajoute google.webClientId dans local.properties"
        }
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Type d'identifiant inattendu : ${credential.type}"
        }
        return GoogleIdTokenCredential.createFrom(credential.data).id
    }

    suspend fun signOut(context: Context) {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }

    fun authorizationRequest(email: String): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DOCS_SCOPE)))
            .setAccount(Account(email, "com.google"))
            .build()
}
