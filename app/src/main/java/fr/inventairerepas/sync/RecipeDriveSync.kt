package fr.inventairerepas.sync

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.inventairerepas.data.drive.GoogleDriveRecipeRemote
import fr.inventairerepas.data.export.NetworkStatus
import fr.inventairerepas.data.repository.RoomDriveLinkRepository
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.usecase.DriveSyncReport
import fr.inventairerepas.domain.usecase.SyncRecipesWithDriveUseCase
import fr.inventairerepas.ui.export.GoogleAuth
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class DriveSyncState(
    val running: Boolean = false,
    /** Heure de la dernière synchronisation réussie (millisecondes). */
    val lastSyncAt: Long? = null,
    val lastReport: DriveSyncReport? = null,
    val error: String? = null,
    /** Google demande un consentement (première fois, ou nouvel accès) : l'écran doit lancer cet intent. */
    val consent: PendingIntent? = null,
    /** Message à afficher une fois après une synchronisation demandée par l'utilisateur. */
    val message: String? = null,
)

/**
 * Synchronisation des recettes avec le dossier Drive « recettes ».
 * Lancée à l'ouverture de l'app, régulièrement tant qu'elle est ouverte, après chaque modification
 * d'une recette, et avec le bouton ⟳. Sans compte Google ou sans réseau, elle ne fait rien.
 */
@Singleton
class RecipeDriveSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val network: NetworkStatus,
    private val links: RoomDriveLinkRepository,
    private val syncRecipes: SyncRecipesWithDriveUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(DriveSyncState())
    val state: StateFlow<DriveSyncState> = _state.asStateFlow()

    /** [manual] : demandé avec le bouton ⟳, le résultat est alors annoncé même s'il n'y a rien de neuf. */
    fun requestSync(manual: Boolean = false) {
        scope.launch {
            val account = settings.settings.first().googleAccountEmail
            when {
                account == null -> if (manual) report("Connecte-toi à Google dans les réglages pour synchroniser avec Drive.")
                !network.isOnline() -> if (manual) report("Pas de connexion internet.")
                else -> authorizeAndSync(account, manual)
            }
        }
    }

    /** Retour de l'écran de consentement Google lancé avec [DriveSyncState.consent]. */
    fun onConsentResult(data: Intent?) {
        _state.update { it.copy(consent = null) }
        scope.launch {
            runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data) }
                .onSuccess { result -> withToken(result, manual = true) }
                .onFailure { error -> fail(GoogleAuth.errorMessage(error, cancelled = true), manual = true) }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    /** Après un changement de compte : les liens et le dossier mémorisés concernaient l'ancien Drive. */
    suspend fun reset() {
        mutex.withLock {
            links.clear()
            settings.update { it.copy(driveFolderId = null) }
            _state.value = DriveSyncState()
        }
    }

    /** Oublie le dossier mémorisé, puis le recherche à nouveau (utile si le dossier partagé a changé). */
    fun findFolderAgain() {
        scope.launch {
            reset()
            requestSync(manual = true)
        }
    }

    private suspend fun authorizeAndSync(account: String, manual: Boolean) {
        val result = runCatching {
            Identity.getAuthorizationClient(context).authorize(GoogleAuth.authorizationRequest(account)).await()
        }.getOrElse { error ->
            fail(GoogleAuth.errorMessage(error, cancelled = false), manual)
            return
        }
        val pending = result.pendingIntent
        if (result.hasResolution() && pending != null) {
            _state.update { it.copy(consent = pending, error = "Autorise l'accès à ton dossier Drive « recettes ».") }
            return
        }
        withToken(result, manual)
    }

    private suspend fun withToken(result: AuthorizationResult, manual: Boolean) {
        val token = result.accessToken ?: return fail("Google n'a pas fourni d'accès à Drive.", manual)
        if (!mutex.tryLock()) return // une synchronisation est déjà en cours
        try {
            _state.update { it.copy(running = true, error = null) }
            val knownFolder = settings.settings.first().driveFolderId
            val folderId = GoogleDriveRecipeRemote.resolveFolder(token, knownFolder)
            if (folderId != knownFolder) {
                // Nouveau dossier (première fois, ou l'ancien a été supprimé) : les anciens liens ne veulent plus rien dire.
                links.clear()
                settings.update { it.copy(driveFolderId = folderId) }
            }
            val report = syncRecipes(GoogleDriveRecipeRemote(token, folderId))
            _state.update {
                it.copy(
                    running = false,
                    lastSyncAt = System.currentTimeMillis(),
                    lastReport = report,
                    error = report.errors.takeIf(List<String>::isNotEmpty)?.let(::errorsMessage),
                    message = if (manual || report.changed || report.errors.isNotEmpty()) summary(report) else it.message,
                )
            }
        } catch (error: Exception) {
            _state.update { it.copy(running = false) }
            fail("Synchronisation Drive impossible : ${error.userMessage()}", manual)
        } finally {
            mutex.unlock()
        }
    }

    private fun fail(message: String, manual: Boolean) {
        _state.update { it.copy(error = message, message = if (manual) message else it.message) }
    }

    private fun report(message: String) = _state.update { it.copy(message = message) }

    private fun errorsMessage(errors: List<String>) =
        "${errors.size} fichier${if (errors.size > 1) "s" else ""} non synchronisé${if (errors.size > 1) "s" else ""} : " +
            errors.joinToString(" ; ")

    private fun summary(report: DriveSyncReport): String {
        val parts = buildList {
            if (report.downloaded > 0) add("${report.downloaded} reçue${plural(report.downloaded)}")
            if (report.uploaded > 0) add("${report.uploaded} envoyée${plural(report.uploaded)}")
            val deleted = report.deletedLocally + report.trashedRemotely
            if (deleted > 0) add("$deleted supprimée${plural(deleted)}")
        }
        val base = if (parts.isEmpty()) "Recettes à jour avec Drive." else "Drive : " + parts.joinToString(", ") + "."
        return if (report.errors.isEmpty()) base else "$base ${errorsMessage(report.errors)}"
    }

    private fun plural(count: Int) = if (count > 1) "s" else ""
}
