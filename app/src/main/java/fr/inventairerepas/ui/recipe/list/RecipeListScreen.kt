package fr.inventairerepas.ui.recipe.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.ui.common.AppScaffold
import fr.inventairerepas.ui.common.dayLabel
import fr.inventairerepas.ui.common.formatMinutes
import fr.inventairerepas.ui.common.label

/**
 * Liste des recettes avec filtres. Si [onBack] est fourni, l'écran est ouvert depuis le menu
 * pour choisir la recette d'un jour : toucher une recette la sélectionne et revient au menu.
 */
@Composable
fun RecipeListScreen(
    onOpenRecipe: (Long) -> Unit,
    onCreateRecipe: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: RecipeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickDate = state.pickDate

    LaunchedEffect(state.picked) {
        if (state.picked) onBack?.invoke()
    }

    AppScaffold(
        title = if (pickDate != null) "Choisir pour ${pickDate.dayLabel()}" else "Recettes",
        onBack = onBack,
        message = state.message,
        onMessageShown = viewModel::messageShown,
        floatingActionButton = {
            if (pickDate == null) {
                FloatingActionButton(onClick = onCreateRecipe) {
                    Icon(Icons.Filled.Add, contentDescription = "Nouvelle recette")
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Filters(
                    state = state,
                    onQuery = viewModel::setQuery,
                    onMaxMinutes = viewModel::setMaxMinutes,
                    onSeasonOnly = viewModel::setSeasonOnly,
                    onFreshness = viewModel::setFreshness,
                )
            }
            if (state.rows.isEmpty() && !state.loading) {
                item {
                    Text(
                        "Aucune recette ne correspond aux filtres.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.rows, key = { it.recipe.id }) { row ->
                val recipe = row.recipe
                ListItem(
                    modifier = Modifier.clickable {
                        if (pickDate != null) viewModel.choose(recipe) else onOpenRecipe(recipe.id)
                    },
                    headlineContent = { Text(recipe.name) },
                    supportingContent = {
                        Text(
                            buildString {
                                append(recipe.freshness.label)
                                append(" · ")
                                append(formatMinutes(recipe.totalMinutes))
                                append(" · ")
                                append(if (row.inSeason) recipe.seasonality.label() else "Hors saison")
                            },
                            color = if (row.inSeason) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    },
                    trailingContent = {
                        if (pickDate != null) {
                            IconButton(onClick = { onOpenRecipe(recipe.id) }) {
                                Icon(Icons.Filled.Info, contentDescription = "Voir la recette")
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Filters(
    state: RecipeListUiState,
    onQuery: (String) -> Unit,
    onMaxMinutes: (Int?) -> Unit,
    onSeasonOnly: (Boolean) -> Unit,
    onFreshness: (Freshness?) -> Unit,
) {
    val filters = state.filters
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = filters.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Rechercher une recette") },
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filters.maxMinutes == null,
                onClick = { onMaxMinutes(null) },
                label = { Text("Toutes durées") },
            )
            state.durationChoices.forEach { minutes ->
                FilterChip(
                    selected = filters.maxMinutes == minutes,
                    onClick = { onMaxMinutes(minutes) },
                    label = { Text("≤ ${formatMinutes(minutes)}") },
                )
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filters.seasonOnly,
                onClick = { onSeasonOnly(!filters.seasonOnly) },
                label = { Text("De saison") },
            )
            FilterChip(
                selected = filters.freshness == null,
                onClick = { onFreshness(null) },
                label = { Text("Toutes listes") },
            )
            Freshness.entries.forEach { freshness ->
                FilterChip(
                    selected = filters.freshness == freshness,
                    onClick = { onFreshness(freshness) },
                    label = { Text(freshness.label) },
                )
            }
        }
    }
}
