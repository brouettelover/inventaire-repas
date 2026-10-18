package fr.inventairerepas.ui.recipe.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.repository.RecipeRepository
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.usecase.SetCookedUseCase
import fr.inventairerepas.domain.week.WeekStructure
import fr.inventairerepas.ui.navigation.NO_DATE
import fr.inventairerepas.ui.navigation.RecipeDetailRoute
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class RecipeDetailUiState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
    /** Jour du menu d'où l'on vient, s'il sert bien cette recette. */
    val day: DayPlan? = null,
    val servings: Int = 2,
    /** « Cuisinée » n'est proposé que depuis un jour du menu d'une semaine modifiable. */
    val canMarkCooked: Boolean = false,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    recipes: RecipeRepository,
    private val plans: PlanRepository,
    private val settingsRepository: SettingsRepository,
    private val setCooked: SetCookedUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<RecipeDetailRoute>()
    private val date: LocalDate? = route.dateEpochDay.takeIf { it != NO_DATE }?.let(LocalDate::ofEpochDay)

    /** Portions choisies sur l'écran quand on n'arrive pas depuis le menu. */
    private val localServings = MutableStateFlow<Int?>(null)
    private val message = MutableStateFlow<String?>(null)

    private val dayFlow: Flow<DayPlan?> = if (date == null) {
        flowOf(null)
    } else {
        settingsRepository.settings
            .map { WeekStructure.weekStartFor(date, it.firstShoppingDay) }
            .distinctUntilChanged()
            .flatMapLatest { weekStart -> plans.observeWeek(weekStart) }
            .map { days -> days.firstOrNull { it.date == date } }
    }

    val state: StateFlow<RecipeDetailUiState> = combine(
        recipes.observe(route.recipeId),
        dayFlow,
        settingsRepository.settings,
        localServings,
        message,
    ) { recipe, day, settings, chosen, msg ->
        val menuDay = day?.takeIf { it.recipe?.id == route.recipeId && !it.noMeal }
        val editableWeek = menuDay != null && WeekStructure.isEditable(
            WeekStructure.weekStartFor(menuDay.date, settings.firstShoppingDay),
            LocalDate.now(),
            settings.firstShoppingDay,
        )
        RecipeDetailUiState(
            loading = false,
            recipe = recipe,
            day = menuDay,
            servings = menuDay?.servings ?: chosen ?: settings.defaultServings,
            canMarkCooked = editableWeek,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeDetailUiState())

    fun setLocalServings(servings: Int) {
        localServings.value = servings
    }

    fun toggleCooked() {
        val day = state.value.day ?: return
        viewModelScope.launch {
            runCatching {
                val settings = settingsRepository.settings.first()
                setCooked(WeekStructure.weekStartFor(day.date, settings.firstShoppingDay), day.date, !day.cooked)
            }.onSuccess {
                message.value = if (day.cooked) "Ingrédients remis en stock" else "Ingrédients déduits du stock"
            }.onFailure { message.value = it.userMessage() }
        }
    }

    fun messageShown() {
        message.value = null
    }
}
