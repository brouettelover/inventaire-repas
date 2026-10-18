package fr.inventairerepas.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.data.export.GoogleDocsClient
import fr.inventairerepas.data.export.NetworkStatus
import fr.inventairerepas.domain.export.ExportDocument
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.usecase.BuildWeekExportUseCase
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ShareContent(val title: String, val text: String)

data class ExportUiState(
    val dialogOpen: Boolean = false,
    val includeDetails: Boolean = false,
    val busy: Boolean = false,
    /** Compte à autoriser pour Google Docs : l'écran lance alors l'Authorization API. */
    val authorizeAccount: String? = null,
    /** Raison pour laquelle l'export Google Docs n'est pas possible ; le partage texte est proposé. */
    val fallbackReason: String? = null,
    val documentUrl: String? = null,
    val share: ShareContent? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val buildExport: BuildWeekExportUseCase,
    private val settings: SettingsRepository,
    private val network: NetworkStatus,
    private val docs: GoogleDocsClient,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state: StateFlow<ExportUiState> = _state.asStateFlow()

    private var weekStart: LocalDate? = null

    fun open(weekStart: LocalDate) {
        this.weekStart = weekStart
        _state.update { it.copy(dialogOpen = true, fallbackReason = null, documentUrl = null) }
    }

    fun dismiss() = _state.update { ExportUiState(includeDetails = it.includeDetails) }

    fun setIncludeDetails(include: Boolean) = _state.update { it.copy(includeDetails = include) }

    /** Vérifie la connexion et le compte, puis demande à l'écran d'obtenir l'autorisation Google Docs. */
    fun exportToGoogleDocs() {
        viewModelScope.launch {
            val account = settings.settings.first().googleAccountEmail
            when {
                !GoogleAuth.isConfigured -> fallback("L'export Google Docs n'est pas encore configuré (identifiant client manquant).")
                !network.isOnline() -> fallback("Pas de connexion internet.")
                account == null -> fallback("Aucun compte Google connecté. Tu peux te connecter dans les réglages.")
                else -> _state.update { it.copy(busy = true, authorizeAccount = account, fallbackReason = null) }
            }
        }
    }

    fun onAccessToken(token: String) {
        val start = weekStart ?: return
        _state.update { it.copy(authorizeAccount = null, busy = true) }
        viewModelScope.launch {
            runCatching { docs.create(token, document(start)) }
                .onSuccess { url -> _state.update { it.copy(busy = false, documentUrl = url) } }
                .onFailure { error -> fallback("Échec de l'export : ${error.userMessage()}") }
        }
    }

    fun onAuthorizationFailed(error: Throwable?) {
        fallback(
            if (error == null) "Autorisation Google refusée." else "Autorisation Google impossible : ${error.userMessage()}",
        )
    }

    fun prepareShare() {
        val start = weekStart ?: return
        viewModelScope.launch {
            runCatching { document(start) }
                .onSuccess { doc -> _state.update { it.copy(share = ShareContent(doc.title, doc.toPlainText())) } }
                .onFailure { error -> _state.update { it.copy(fallbackReason = error.userMessage()) } }
        }
    }

    fun shareLaunched() = _state.update { ExportUiState(includeDetails = it.includeDetails) }

    private suspend fun document(start: LocalDate): ExportDocument = buildExport(start, _state.value.includeDetails)

    private fun fallback(reason: String) =
        _state.update { it.copy(busy = false, authorizeAccount = null, fallbackReason = reason) }
}
