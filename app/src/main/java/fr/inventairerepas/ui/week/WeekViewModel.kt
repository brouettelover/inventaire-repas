package fr.inventairerepas.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.domain.model.DayMode
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Settings
import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.usecase.EnsureWeekUseCase
import fr.inventairerepas.domain.usecase.RedrawDayUseCase
import fr.inventairerepas.domain.usecase.RedrawWeekUseCase
import fr.inventairerepas.domain.usecase.UpdateDayUseCase
import fr.inventairerepas.domain.week.DaySlot
import fr.inventairerepas.domain.week.WeekStructure
import fr.inventairerepas.ui.common.SelectedWeek
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

data class DayUi(val slot: DaySlot, val plan: DayPlan?)

data class WeekUiState(
    val loading: Boolean = true,
    val weekStart: LocalDate? = null,
    val offset: Int = 0,
    val editable: Boolean = false,
    val days: List<DayUi> = emptyList(),
    val settings: Settings = Settings(),
    val message: String? = null,
)

private data class WeekKey(val start: LocalDate, val offset: Int, val settings: Settings, val editable: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WeekViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val plans: PlanRepository,
    private val selectedWeek: SelectedWeek,
    private val ensureWeek: EnsureWeekUseCase,
    private val redrawWeekUseCase: RedrawWeekUseCase,
    private val redrawDayUseCase: RedrawDayUseCase,
    private val updateDay: UpdateDayUseCase,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    private val key = combine(settingsRepository.settings, selectedWeek.offset) { settings, offset ->
        val today = LocalDate.now()
        val start = WeekStructure.weekStartFor(today, settings.firstShoppingDay).plusWeeks(offset.toLong())
        WeekKey(start, offset, settings, WeekStructure.isEditable(start, today, settings.firstShoppingDay))
    }.distinctUntilChanged()

    val state: StateFlow<WeekUiState> = combine(
        key.flatMapLatest { k ->
            // La semaine en cours et la suivante sont créées et tirées à la première ouverture.
            if (k.editable) runCatching { ensureWeek(k.start) }.onFailure { message.value = it.userMessage() }
            plans.observeWeek(k.start).map { k to it }
        },
        message,
    ) { (k, days), msg ->
        val byDate = days.associateBy { it.date }
        WeekUiState(
            loading = false,
            weekStart = k.start,
            offset = k.offset,
            editable = k.editable,
            days = WeekStructure.slots(k.start).map { DayUi(it, byDate[it.date]) },
            settings = k.settings,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekUiState())

    fun previousWeek() = selectedWeek.previous()

    fun nextWeek() = selectedWeek.next()

    fun messageShown() {
        message.value = null
    }

    fun redrawWeek() = launch { redrawWeekUseCase(it) }

    fun redrawDay(date: LocalDate) = launch { redrawDayUseCase(it, date) }

    fun setMode(date: LocalDate, mode: DayMode) = launch { updateDay.setMode(it, date, mode) }

    fun setServings(date: LocalDate, servings: Int) = launch { updateDay.setServings(it, date, servings) }

    fun setNoMeal(date: LocalDate, noMeal: Boolean) = launch { updateDay.setNoMeal(it, date, noMeal) }

    private fun launch(action: suspend (weekStart: LocalDate) -> Unit) {
        val weekStart = state.value.weekStart ?: return
        viewModelScope.launch {
            runCatching { action(weekStart) }.onFailure { message.value = it.userMessage() }
        }
    }
}

fun Throwable.userMessage(): String = message ?: "Une erreur est survenue"
