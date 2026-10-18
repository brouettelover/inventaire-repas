package fr.inventairerepas.ui.recipe.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.repository.RecipeRepository
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.usecase.UpdateDayUseCase
import fr.inventairerepas.ui.navigation.PickRecipeRoute
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class RecipeFilters(
    val query: String = "",
    /** Durée totale maximale ; null = toutes. */
    val maxMinutes: Int? = null,
    val seasonOnly: Boolean = false,
    val freshness: Freshness? = null,
)

data class RecipeRow(val recipe: Recipe, val inSeason: Boolean)

data class RecipeListUiState(
    val loading: Boolean = true,
    /** Jour du menu à remplir en mode choix ; null en consultation simple. */
    val pickDate: LocalDate? = null,
    val filters: RecipeFilters = RecipeFilters(),
    val durationChoices: List<Int> = DEFAULT_DURATIONS,
    val rows: List<RecipeRow> = emptyList(),
    val picked: Boolean = false,
    val message: String? = null,
)

val DEFAULT_DURATIONS = listOf(20, 30, 45, 60, 90)

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    recipes: RecipeRepository,
    private val settingsRepository: SettingsRepository,
    private val updateDay: UpdateDayUseCase,
) : ViewModel() {

    private val pick: PickRecipeRoute? =
        if (savedStateHandle.contains("weekStartEpochDay")) savedStateHandle.toRoute<PickRecipeRoute>() else null
    private val pickDate = pick?.let { LocalDate.ofEpochDay(it.dateEpochDay) }

    private val filters = MutableStateFlow(RecipeFilters())
    private val picked = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val durationChoices = MutableStateFlow(DEFAULT_DURATIONS)

    init {
        // En mode choix, le filtre de durée est pré-rempli avec la durée maximale du jour.
        if (pickDate != null) {
            viewModelScope.launch {
                val max = settingsRepository.settings.first().maxMinutesFor(pickDate.dayOfWeek)
                if (max != null) {
                    durationChoices.value = (DEFAULT_DURATIONS + max).distinct().sorted()
                    filters.update { it.copy(maxMinutes = max) }
                }
            }
        }
    }

    val state: StateFlow<RecipeListUiState> = combine(
        recipes.observeAll(),
        filters,
        combine(picked, message, durationChoices) { p, m, d -> Triple(p, m, d) },
    ) { all, f, (wasPicked, msg, durations) ->
        val referenceDate = pickDate ?: LocalDate.now()
        val query = Ingredient.normalizeName(f.query)
        val rows = all
            .map { RecipeRow(it, it.seasonality.isInSeason(referenceDate)) }
            .filter { row ->
                (query.isEmpty() || Ingredient.normalizeName(row.recipe.name).contains(query)) &&
                    (f.maxMinutes == null || row.recipe.totalMinutes <= f.maxMinutes) &&
                    (!f.seasonOnly || row.inSeason) &&
                    (f.freshness == null || row.recipe.freshness == f.freshness)
            }
        RecipeListUiState(
            loading = false,
            pickDate = pickDate,
            filters = f,
            durationChoices = durations,
            rows = rows,
            picked = wasPicked,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeListUiState(pickDate = pickDate))

    fun setQuery(query: String) = filters.update { it.copy(query = query) }

    fun setMaxMinutes(maxMinutes: Int?) = filters.update { it.copy(maxMinutes = maxMinutes) }

    fun setSeasonOnly(seasonOnly: Boolean) = filters.update { it.copy(seasonOnly = seasonOnly) }

    fun setFreshness(freshness: Freshness?) = filters.update { it.copy(freshness = freshness) }

    fun choose(recipe: Recipe) {
        val route = pick ?: return
        viewModelScope.launch {
            runCatching {
                updateDay.chooseRecipe(
                    LocalDate.ofEpochDay(route.weekStartEpochDay),
                    LocalDate.ofEpochDay(route.dateEpochDay),
                    recipe,
                )
            }.onSuccess { picked.value = true }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun messageShown() {
        message.value = null
    }
}
