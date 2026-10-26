package fr.inventairerepas.ui.export

import android.accounts.Account
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope

/**
 * Authentification Google par l'Authorization API uniquement : seul l'identifiant client Android
 * (package + SHA-1, déclaré dans la Cloud Console) est nécessaire, aucun identifiant à saisir dans le projet.
 * - Connexion (réglages) : choix du compte et consentement, on garde l'adresse du compte.
 * - Export : jeton d'accès pour ce compte.
 */
object GoogleAuth {

    /** Portée minimale : l'application ne voit que les fichiers qu'elle crée dans le Drive. */
    const val DOCS_SCOPE = "https://www.googleapis.com/auth/drive.file"

    /** Donne accès à l'adresse du compte choisi. */
    private const val EMAIL_SCOPE = "email"

    private val scopes = listOf(Scope(DOCS_SCOPE), Scope(EMAIL_SCOPE))

    /** Sans compte : Google affiche le sélecteur de compte. */
    fun signInRequest(): AuthorizationRequest =
        AuthorizationRequest.builder().setRequestedScopes(scopes).build()

    fun authorizationRequest(email: String): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(scopes)
            .setAccount(Account(email, "com.google"))
            .build()

    /** Message lisible pour un échec de connexion, avec le code d'erreur de Google quand il existe. */
    fun errorMessage(error: Throwable, cancelled: Boolean): String {
        val code = (error as? ApiException)?.statusCode
        return when (code) {
            CommonStatusCodes.DEVELOPER_ERROR ->
                "Connexion refusée par Google (erreur 10) : l'identifiant client Android est absent ou ne correspond pas. " +
                    "Vérifie le package fr.inventairerepas.app et la SHA-1 dans la Cloud Console."
            CommonStatusCodes.CANCELED -> "Connexion annulée."
            CommonStatusCodes.NETWORK_ERROR -> "Pas de connexion internet."
            null -> if (cancelled) "Connexion annulée ou refusée par Google." else "Connexion impossible : ${error.message}"
            else -> "Connexion impossible (code Google $code) : ${error.message}"
        }
    }

    /** Adresse du compte autorisé, ou null si Google ne l'a pas fournie. */
    fun email(result: AuthorizationResult): String? = result.toGoogleSignInAccount()?.email
}
