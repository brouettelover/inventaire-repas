package fr.inventairerepas.ui.recipe.edit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.ui.common.AppScaffold
import fr.inventairerepas.ui.common.shortLabel
import java.time.Month

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeEditScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: RecipeEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) { if (state.done) onBack() }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    AppScaffold(
        title = if (state.isNew) "Nouvelle recette" else "Modifier la recette",
        onBack = onBack,
        message = state.error,
        onMessageShown = viewModel::errorShown,
        actions = {
            if (!state.isNew) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer la recette")
                }
            }
            IconButton(onClick = viewModel::save, enabled = !state.loading) {
                Icon(Icons.Filled.Check, contentDescription = "Enregistrer")
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding))
            return@AppScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Nom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Portions", state.baseServings, viewModel::setBaseServings, Modifier.weight(1f))
                NumberField("Préparation (min)", state.prepMinutes, viewModel::setPrepMinutes, Modifier.weight(1f))
                NumberField("Cuisson (min)", state.cookMinutes, viewModel::setCookMinutes, Modifier.weight(1f))
            }

            Text("Liste de fraîcheur", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Freshness.entries.forEach { freshness ->
                    FilterChip(
                        selected = state.freshness == freshness,
                        onClick = { viewModel.setFreshness(freshness) },
                        label = { Text(freshness.label) },
                    )
                }
            }

            Text("Saison", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.months.size == Month.entries.size,
                    onCheckedChange = viewModel::setAllYear,
                )
                Text("Toute l'année")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Month.entries.forEach { month ->
                    FilterChip(
                        selected = month in state.months,
                        onClick = { viewModel.toggleMonth(month) },
                        label = { Text(month.shortLabel()) },
                    )
                }
            }

            HorizontalDivider()
            Text("Ingrédients (pour ${state.baseServings.ifEmpty { "?" }} pers.)", style = MaterialTheme.typography.titleSmall)
            state.ingredients.forEach { row ->
                IngredientEditor(
                    row = row,
                    suggestions = if (state.suggestionsRowKey == row.key) state.suggestions else emptyList(),
                    onName = { viewModel.setIngredientName(row.key, it) },
                    onSuggestion = { viewModel.chooseSuggestion(row.key, it) },
                    onAmount = { viewModel.setIngredientAmount(row.key, it) },
                    onUnit = { viewModel.setIngredientUnit(row.key, it) },
                    onRemove = { viewModel.removeIngredient(row.key) },
                )
            }
            OutlinedButton(onClick = viewModel::addIngredient) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Ajouter un ingrédient")
            }

            HorizontalDivider()
            Text("Étapes", style = MaterialTheme.typography.titleSmall)
            state.steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = step.text,
                        onValueChange = { viewModel.setStep(step.key, it) },
                        label = { Text("Étape ${index + 1}") },
                        modifier = Modifier.weight(1f),
                    )
                    Column {
                        IconButton(onClick = { viewModel.moveStep(step.key, -1) }, enabled = index > 0) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Monter")
                        }
                        IconButton(onClick = { viewModel.moveStep(step.key, 1) }, enabled = index < state.steps.lastIndex) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Descendre")
                        }
                    }
                    IconButton(onClick = { viewModel.removeStep(step.key) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Supprimer l'étape")
                    }
                }
            }
            OutlinedButton(onClick = viewModel::addStep) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Ajouter une étape")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer cette recette ?") },
            text = { Text("Les jours du menu qui la servent n'auront plus de recette.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun IngredientEditor(
    row: IngredientRow,
    suggestions: List<String>,
    onName: (String) -> Unit,
    onSuggestion: (String) -> Unit,
    onAmount: (String) -> Unit,
    onUnit: (MeasureUnit) -> Unit,
    onRemove: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = row.name,
                onValueChange = onName,
                label = { Text("Ingrédient") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = row.amount,
                onValueChange = onAmount,
                label = { Text("Qté") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(80.dp),
            )
            UnitPicker(row.unit, onUnit)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Retirer l'ingrédient")
            }
        }
        if (suggestions.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                suggestions.forEach { name ->
                    SuggestionChip(onClick = { onSuggestion(name) }, label = { Text(name) })
                }
            }
        }
    }
}

@Composable
fun UnitPicker(unit: MeasureUnit, onUnit: (MeasureUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text(unit.symbol) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MeasureUnit.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.symbol) },
                    onClick = {
                        expanded = false
                        onUnit(option)
                    },
                )
            }
        }
    }
}
