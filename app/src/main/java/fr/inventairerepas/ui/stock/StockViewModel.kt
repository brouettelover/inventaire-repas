package fr.inventairerepas.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.StockLine
import fr.inventairerepas.domain.repository.IngredientRepository
import fr.inventairerepas.domain.repository.StockRepository
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.usecase.AdjustStockUseCase
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockUiState(
    val loading: Boolean = true,
    val lines: List<StockLine> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class StockViewModel @Inject constructor(
    stock: StockRepository,
    private val ingredients: IngredientRepository,
    private val adjustStock: AdjustStockUseCase,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<StockUiState> = combine(
        stock.observeAll(),
        ingredients.observeAll(),
        message,
    ) { lines, catalog, msg ->
        StockUiState(loading = false, lines = lines, ingredients = catalog, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockUiState())

    /** Fixe la quantité en stock d'un ingrédient, en le créant dans le catalogue au besoin. */
    fun setStock(ingredientName: String, quantity: Quantity) {
        if (ingredientName.isBlank()) {
            message.value = "Indique le nom de l'ingrédient"
            return
        }
        viewModelScope.launch {
            runCatching {
                val ingredient = ingredients.getOrCreate(Ingredient(name = ingredientName.trim()))
                adjustStock(ingredient.id, quantity)
            }.onFailure { message.value = it.userMessage() }
        }
    }

    fun remove(line: StockLine) {
        viewModelScope.launch {
            runCatching { adjustStock.clear(line.ingredient.id, line.family) }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun setAlwaysInStock(ingredient: Ingredient, alwaysInStock: Boolean) {
        viewModelScope.launch {
            runCatching { ingredients.setAlwaysInStock(ingredient.id, alwaysInStock) }
                .onFailure { message.value = it.userMessage() }
        }
    }

    fun messageShown() {
        message.value = null
    }
}
