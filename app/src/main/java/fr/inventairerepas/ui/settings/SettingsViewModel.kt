package fr.inventairerepas.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.domain.model.Settings
import fr.inventairerepas.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class SettingsUiState(val loading: Boolean = true, val settings: Settings = Settings())

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = repository.settings
        .map { SettingsUiState(loading = false, settings = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setFirstShoppingDay(day: DayOfWeek) = update { it.copy(firstShoppingDay = day) }

    fun setDefaultServings(servings: Int) = update { it.copy(defaultServings = servings.coerceIn(1, 20)) }

    fun setMaxMinutes(day: DayOfWeek, minutes: Int?) =
        update { it.copy(maxMinutesByDay = it.maxMinutesByDay + (day to minutes)) }

    fun setGoogleAccount(email: String?) = update { it.copy(googleAccountEmail = email) }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { repository.update(transform) }
    }
}
