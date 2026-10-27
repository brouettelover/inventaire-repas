package fr.inventairerepas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.domain.model.Settings
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.sync.DriveSyncState
import fr.inventairerepas.sync.RecipeDriveSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class SettingsUiState(
    val loading: Boolean = true,
    val settings: Settings = Settings(),
    val drive: DriveSyncState = DriveSyncState(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val driveSync: RecipeDriveSync,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(repository.settings, driveSync.state) { settings, drive ->
        SettingsUiState(loading = false, settings = settings, drive = drive)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setFirstShoppingDay(day: DayOfWeek) = update { it.copy(firstShoppingDay = day) }

    fun setDefaultServings(servings: Int) = update { it.copy(defaultServings = servings.coerceIn(1, 20)) }

    fun setMaxMinutes(day: DayOfWeek, minutes: Int?) =
        update { it.copy(maxMinutesByDay = it.maxMinutesByDay + (day to minutes)) }

    fun setGoogleAccount(email: String?) {
        viewModelScope.launch {
            val previous = repository.settings.first().googleAccountEmail
            // Un autre compte, c'est un autre Drive : on oublie le dossier et les liens de l'ancien.
            if (previous != null && previous != email) driveSync.reset()
            repository.update { it.copy(googleAccountEmail = email) }
            if (email != null) driveSync.requestSync(manual = true)
        }
    }

    fun syncRecipesNow() = driveSync.requestSync(manual = true)

    fun findDriveFolderAgain() = driveSync.findFolderAgain()

    fun setShareWithEmail(email: String?) = update { it.copy(shareWithEmail = email?.trim()?.takeIf(String::isNotEmpty)) }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { repository.update(transform) }
    }
}
