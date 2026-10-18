package fr.inventairerepas.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.repository.ShoppingRepository
import fr.inventairerepas.domain.usecase.GenerateShoppingListsUseCase
import fr.inventairerepas.domain.usecase.ToggleShoppingItemUseCase
import fr.inventairerepas.domain.week.WeekStructure
import fr.inventairerepas.ui.common.SelectedWeek
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ShoppingUiState(
    val loading: Boolean = true,
    val weekStart: LocalDate? = null,
    val offset: Int = 0,
    val editable: Boolean = false,
    val lists: List<ShoppingList> = emptyList(),
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val shopping: ShoppingRepository,
    private val selectedWeek: SelectedWeek,
    private val generateLists: GenerateShoppingListsUseCase,
    private val toggleItem: ToggleShoppingItemUseCase,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    private val week = combine(settingsRepository.settings, selectedWeek.offset) { settings, offset ->
        val today = LocalDate.now()
        val start = WeekStructure.weekStartFor(today, settings.firstShoppingDay).plusWeeks(offset.toLong())
        Triple(start, offset, WeekStructure.isEditable(start, today, settings.firstShoppingDay))
    }.distinctUntilChanged()

    val state: StateFlow<ShoppingUiState> = combine(
        week.flatMapLatest { w -> shopping.observeWeek(w.first).map { w to it } },
        message,
    ) { (w, lists), msg ->
        val (start, offset, editable) = w
        ShoppingUiState(
            loading = false,
            weekStart = start,
            offset = offset,
            editable = editable,
            lists = lists,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    fun previousWeek() = selectedWeek.previous()

    fun nextWeek() = selectedWeek.next()

    /** Calcule les deux listes à partir du menu et du stock actuels, et remplace les précédentes. */
    fun generate() {
        val weekStart = state.value.weekStart ?: return
        viewModelScope.launch {
            runCatching { generateLists(weekStart) }
                .onSuccess { message.value = "Listes de courses générées" }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun toggle(itemId: Long, checked: Boolean) {
        viewModelScope.launch {
            runCatching { toggleItem(itemId, checked) }.onFailure { message.value = it.userMessage() }
        }
    }

    fun messageShown() {
        message.value = null
    }
}
