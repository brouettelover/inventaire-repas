package fr.inventairerepas.ui.recipe.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RecipeIngredient
import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.repository.IngredientRepository
import fr.inventairerepas.domain.repository.RecipeRepository
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.ui.navigation.NEW_RECIPE
import fr.inventairerepas.ui.navigation.RecipeEditRoute
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Month
import javax.inject.Inject

data class IngredientRow(val key: Long, val name: String = "", val amount: String = "", val unit: MeasureUnit = MeasureUnit.GRAM)

data class StepRow(val key: Long, val text: String = "")

data class RecipeEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val baseServings: String = "2",
    val prepMinutes: String = "15",
    val cookMinutes: String = "15",
    val freshness: Freshness = Freshness.D2,
    val months: Set<Month> = Month.entries.toSet(),
    val ingredients: List<IngredientRow> = emptyList(),
    val steps: List<StepRow> = emptyList(),
    /** Suggestions d'ingrédients du catalogue pour la ligne en cours de saisie. */
    val suggestionsRowKey: Long? = null,
    val suggestions: List<String> = emptyList(),
    val error: String? = null,
    val done: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipes: RecipeRepository,
    private val ingredients: IngredientRepository,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<RecipeEditRoute>().recipeId
    private var nextKey = 1L
    private var searchJob: Job? = null

    private val _state = MutableStateFlow(RecipeEditUiState())
    val state: StateFlow<RecipeEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val recipe = if (recipeId == NEW_RECIPE) null else recipes.get(recipeId)
            _state.value = if (recipe == null) {
                RecipeEditUiState(
                    loading = false,
                    ingredients = listOf(IngredientRow(newKey())),
                    steps = listOf(StepRow(newKey())),
                )
            } else {
                RecipeEditUiState(
                    loading = false,
                    isNew = false,
                    name = recipe.name,
                    baseServings = recipe.baseServings.toString(),
                    prepMinutes = recipe.prepMinutes.toString(),
                    cookMinutes = recipe.cookMinutes.toString(),
                    freshness = recipe.freshness,
                    months = recipe.seasonality.months,
                    ingredients = recipe.ingredients.map {
                        IngredientRow(
                            key = newKey(),
                            name = it.ingredient.name,
                            amount = editableAmount(it.quantity.amount),
                            unit = it.quantity.unit,
                        )
                    },
                    steps = recipe.steps.map { StepRow(newKey(), it) },
                )
            }
        }
    }

    private fun newKey() = nextKey++

    private fun editableAmount(amount: Double): String =
        BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString().replace('.', ',')

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setBaseServings(value: String) = _state.update { it.copy(baseServings = value.filter(Char::isDigit)) }
    fun setPrepMinutes(value: String) = _state.update { it.copy(prepMinutes = value.filter(Char::isDigit)) }
    fun setCookMinutes(value: String) = _state.update { it.copy(cookMinutes = value.filter(Char::isDigit)) }
    fun setFreshness(value: Freshness) = _state.update { it.copy(freshness = value) }

    fun toggleMonth(month: Month) = _state.update {
        it.copy(months = if (month in it.months) it.months - month else it.months + month)
    }

    fun setAllYear(allYear: Boolean) = _state.update {
        it.copy(months = if (allYear) Month.entries.toSet() else emptySet())
    }

    fun addIngredient() = _state.update { it.copy(ingredients = it.ingredients + IngredientRow(newKey())) }

    fun removeIngredient(key: Long) = _state.update { s -> s.copy(ingredients = s.ingredients.filterNot { it.key == key }) }

    fun setIngredientName(key: Long, name: String) {
        updateIngredient(key) { it.copy(name = name) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            val found = if (name.isBlank()) {
                emptyList()
            } else {
                ingredients.search(name).map { it.name }.filterNot { it == name }.take(5)
            }
            _state.update { it.copy(suggestionsRowKey = key, suggestions = found) }
        }
    }

    fun chooseSuggestion(key: Long, name: String) {
        updateIngredient(key) { it.copy(name = name) }
        _state.update { it.copy(suggestionsRowKey = null, suggestions = emptyList()) }
    }

    fun setIngredientAmount(key: Long, amount: String) =
        updateIngredient(key) { it.copy(amount = amount.filter { c -> c.isDigit() || c == ',' || c == '.' }) }

    fun setIngredientUnit(key: Long, unit: MeasureUnit) = updateIngredient(key) { it.copy(unit = unit) }

    private fun updateIngredient(key: Long, change: (IngredientRow) -> IngredientRow) = _state.update { s ->
        s.copy(ingredients = s.ingredients.map { if (it.key == key) change(it) else it })
    }

    fun addStep() = _state.update { it.copy(steps = it.steps + StepRow(newKey())) }

    fun removeStep(key: Long) = _state.update { s -> s.copy(steps = s.steps.filterNot { it.key == key }) }

    fun setStep(key: Long, text: String) = _state.update { s ->
        s.copy(steps = s.steps.map { if (it.key == key) it.copy(text = text) else it })
    }

    fun moveStep(key: Long, delta: Int) = _state.update { s ->
        val index = s.steps.indexOfFirst { it.key == key }
        val target = index + delta
        if (index < 0 || target !in s.steps.indices) return@update s
        val steps = s.steps.toMutableList()
        steps.add(target, steps.removeAt(index))
        s.copy(steps = steps)
    }

    fun errorShown() = _state.update { it.copy(error = null) }

    fun save() {
        val s = _state.value
        val recipe = runCatching { buildRecipe(s) }.getOrElse { error ->
            _state.update { it.copy(error = error.userMessage()) }
            return
        }
        viewModelScope.launch {
            runCatching { recipes.save(recipe) }
                .onSuccess { _state.update { it.copy(done = true) } }
                .onFailure { error -> _state.update { it.copy(error = error.userMessage()) } }
        }
    }

    fun delete() {
        if (recipeId == NEW_RECIPE) return
        viewModelScope.launch {
            runCatching { recipes.delete(recipeId) }
                .onSuccess { _state.update { it.copy(deleted = true) } }
                .onFailure { error -> _state.update { it.copy(error = error.userMessage()) } }
        }
    }

    private fun buildRecipe(s: RecipeEditUiState): Recipe {
        require(s.name.isNotBlank()) { "Donne un nom à la recette" }
        val servings = s.baseServings.toIntOrNull()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("Le nombre de portions doit être positif")
        require(s.months.isNotEmpty()) { "Choisis au moins un mois, ou « toute l'année »" }
        val lines = s.ingredients
            .filterNot { it.name.isBlank() && it.amount.isBlank() }
            .map { row ->
                require(row.name.isNotBlank()) { "Un ingrédient n'a pas de nom" }
                val amount = row.amount.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }
                    ?: throw IllegalArgumentException("Quantité invalide pour « ${row.name.trim()} »")
                RecipeIngredient(Ingredient(name = row.name.trim()), Quantity(amount, row.unit))
            }
        return Recipe(
            id = if (recipeId == NEW_RECIPE) 0 else recipeId,
            name = s.name.trim(),
            baseServings = servings,
            prepMinutes = s.prepMinutes.toIntOrNull() ?: 0,
            cookMinutes = s.cookMinutes.toIntOrNull() ?: 0,
            seasonality = Seasonality(s.months),
            freshness = s.freshness,
            ingredients = lines,
            steps = s.steps.map { it.text.trim() }.filter { it.isNotEmpty() },
            updatedAt = System.currentTimeMillis(),
        )
    }
}
