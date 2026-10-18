package fr.inventairerepas.ui.recipe.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.inventairerepas.domain.portion.PortionScaler
import fr.inventairerepas.ui.common.AppScaffold
import fr.inventairerepas.ui.common.ServingsStepper
import fr.inventairerepas.ui.common.dayLabel
import fr.inventairerepas.ui.common.formatMinutes
import fr.inventairerepas.ui.common.label

@Composable
fun RecipeDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recipe = state.recipe

    AppScaffold(
        title = recipe?.name ?: "Recette",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        actions = {
            if (recipe != null) {
                IconButton(onClick = { onEdit(recipe.id) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifier la recette")
                }
            }
        },
    ) { padding ->
        if (recipe == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.loading) CircularProgressIndicator() else Text("Recette introuvable")
            }
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
            val day = state.day
            if (day != null) {
                Text("Au menu : ${day.date.dayLabel()}", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                "Préparation ${formatMinutes(recipe.prepMinutes)} · Cuisson ${formatMinutes(recipe.cookMinutes)} · " +
                    "Total ${formatMinutes(recipe.totalMinutes)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Saison : ${recipe.seasonality.label()}", style = MaterialTheme.typography.bodyMedium)
            Text("Liste de fraîcheur : ${recipe.freshness.label}", style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pour ", style = MaterialTheme.typography.bodyLarge)
                ServingsStepper(
                    servings = state.servings,
                    onChange = viewModel::setLocalServings,
                    // Depuis le menu, les portions se règlent sur l'écran « Semaine ».
                    enabled = day == null,
                )
            }

            HorizontalDivider()
            Text("Ingrédients", style = MaterialTheme.typography.titleMedium)
            PortionScaler.scale(recipe, state.servings).forEach { line ->
                Row(Modifier.fillMaxWidth()) {
                    Text(line.ingredient.name, modifier = Modifier.weight(1f))
                    Text(
                        if (line.ingredient.alwaysInStock) "${line.quantity.label()} (toujours en stock)" else line.quantity.label(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            HorizontalDivider()
            Text("Préparation", style = MaterialTheme.typography.titleMedium)
            recipe.steps.forEachIndexed { index, step ->
                Text("${index + 1}. $step", style = MaterialTheme.typography.bodyLarge)
            }

            if (day != null && state.canMarkCooked) {
                Spacer(Modifier.size(8.dp))
                if (day.cooked) {
                    OutlinedButton(onClick = viewModel::toggleCooked, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Annuler « cuisinée »")
                    }
                } else {
                    Button(onClick = viewModel::toggleCooked, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cuisinée")
                    }
                }
            }
        }
    }
}
