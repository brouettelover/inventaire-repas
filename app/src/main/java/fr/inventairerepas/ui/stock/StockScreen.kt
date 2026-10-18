package fr.inventairerepas.ui.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.StockLine
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.ui.common.AppScaffold
import fr.inventairerepas.ui.common.label
import fr.inventairerepas.ui.recipe.edit.UnitPicker
import java.math.BigDecimal

/** Ligne en cours d'édition : [ingredientName] est figé quand on modifie une ligne existante. */
private data class StockEdit(val ingredientName: String, val amount: String, val unit: MeasureUnit, val isNew: Boolean)

@Composable
fun StockScreen(viewModel: StockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<StockEdit?>(null) }

    AppScaffold(
        title = "Stock",
        message = state.message,
        onMessageShown = viewModel::messageShown,
        floatingActionButton = {
            if (tab == 0) {
                FloatingActionButton(onClick = { editing = StockEdit("", "", MeasureUnit.GRAM, isNew = true) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter au stock")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Placard") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Toujours en stock") })
            }
            if (tab == 0) {
                StockList(
                    lines = state.lines,
                    onEdit = { line ->
                        val display = line.toDisplayQuantity()
                        editing = StockEdit(line.ingredient.name, display.amount.editable(), display.unit, isNew = false)
                    },
                    onRemove = viewModel::remove,
                )
            } else {
                AlwaysInStockList(state.ingredients, viewModel::setAlwaysInStock)
            }
        }
    }

    editing?.let { edit ->
        StockDialog(
            initial = edit,
            onDismiss = { editing = null },
            onConfirm = { name, quantity ->
                editing = null
                viewModel.setStock(name, quantity)
            },
        )
    }
}

@Composable
private fun StockList(lines: List<StockLine>, onEdit: (StockLine) -> Unit, onRemove: (StockLine) -> Unit) {
    if (lines.isEmpty()) {
        Text(
            "Le stock est vide. Il se remplit quand tu coches tes courses, ou avec le bouton +.",
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    LazyColumn {
        items(lines, key = { "${it.ingredient.id}-${it.family}" }) { line ->
            ListItem(
                modifier = Modifier.clickable { onEdit(line) },
                headlineContent = { Text(line.ingredient.name) },
                supportingContent = { Text(line.toDisplayQuantity().label()) },
                trailingContent = {
                    IconButton(onClick = { onRemove(line) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Retirer du stock")
                    }
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun AlwaysInStockList(ingredients: List<Ingredient>, onToggle: (Ingredient, Boolean) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val normalized = Ingredient.normalizeName(query)
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Rechercher un ingrédient") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Text(
            "Ces ingrédients n'apparaissent jamais dans les listes de courses.",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn {
            items(ingredients.filter { it.normalizedName.contains(normalized) }, key = { it.id }) { ingredient ->
                ListItem(
                    headlineContent = { Text(ingredient.name) },
                    trailingContent = {
                        Switch(
                            checked = ingredient.alwaysInStock,
                            onCheckedChange = { onToggle(ingredient, it) },
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StockDialog(
    initial: StockEdit,
    onDismiss: () -> Unit,
    onConfirm: (String, Quantity) -> Unit,
) {
    var name by remember { mutableStateOf(initial.ingredientName) }
    var amount by remember { mutableStateOf(initial.amount) }
    var unit by remember { mutableStateOf(initial.unit) }
    val parsed = amount.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.isNew) "Ajouter au stock" else initial.ingredientName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initial.isNew) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ingrédient") },
                        singleLine = true,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { value -> amount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                        label = { Text("Quantité") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    // La famille d'une ligne existante est fixée : on ne propose que ses unités.
                    UnitPicker(unit) { chosen ->
                        if (initial.isNew || chosen.family == initial.unit.family) unit = chosen
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(name, Quantity(it, unit)) } },
                enabled = parsed != null && name.isNotBlank(),
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

private fun Double.editable(): String = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString().replace('.', ',')
