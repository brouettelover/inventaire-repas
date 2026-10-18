package fr.inventairerepas.ui.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.inventairerepas.domain.model.DayMode
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.ui.common.AppScaffold
import fr.inventairerepas.ui.common.ServingsStepper
import fr.inventairerepas.ui.common.WeekSelector
import fr.inventairerepas.ui.common.dayLabel
import fr.inventairerepas.ui.common.formatMinutes
import fr.inventairerepas.ui.common.servingsLabel
import java.time.LocalDate

@Composable
fun WeekScreen(
    onOpenRecipe: (recipeId: Long, date: LocalDate) -> Unit,
    onPickRecipe: (weekStart: LocalDate, date: LocalDate) -> Unit,
    onOpenShopping: () -> Unit,
    onOpenSettings: () -> Unit,
    exportAction: @Composable () -> Unit = {},
    viewModel: WeekViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScaffold(
        title = "Menu de la semaine",
        message = state.message,
        onMessageShown = viewModel::messageShown,
        actions = {
            IconButton(onClick = onOpenShopping) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = "Listes de courses")
            }
            exportAction()
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Réglages")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                WeekSelector(
                    weekStart = state.weekStart,
                    offset = state.offset,
                    onPrevious = viewModel::previousWeek,
                    onNext = viewModel::nextWeek,
                )
            }
            if (state.editable) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        FilledTonalButton(onClick = viewModel::redrawWeek) {
                            Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Relancer la semaine")
                        }
                    }
                }
            } else if (!state.loading) {
                item {
                    Text(
                        "Semaine passée : consultation seule.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            items(state.days, key = { it.slot.date.toEpochDay() }) { day ->
                DayCard(
                    day = day,
                    editable = state.editable,
                    onOpenRecipe = { recipeId -> onOpenRecipe(recipeId, day.slot.date) },
                    onPick = { state.weekStart?.let { onPickRecipe(it, day.slot.date) } },
                    onRedraw = { viewModel.redrawDay(day.slot.date) },
                    onModeChange = { viewModel.setMode(day.slot.date, it) },
                    onServingsChange = { viewModel.setServings(day.slot.date, it) },
                    onNoMealChange = { viewModel.setNoMeal(day.slot.date, it) },
                )
            }
            item { Spacer(Modifier.size(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCard(
    day: DayUi,
    editable: Boolean,
    onOpenRecipe: (Long) -> Unit,
    onPick: () -> Unit,
    onRedraw: () -> Unit,
    onModeChange: (DayMode) -> Unit,
    onServingsChange: (Int) -> Unit,
    onNoMealChange: (Boolean) -> Unit,
) {
    val plan = day.plan
    val locked = !editable || plan?.cooked == true
    val colors = if (day.slot.isShoppingDay) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = colors) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(day.slot.date.dayLabel(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = buildString {
                            append(day.slot.offsetLabel)
                            if (day.slot.isShoppingDay) append(" · Jour de courses")
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (plan?.cooked == true) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Cuisinée",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (!locked && plan != null) {
                    DayMenu(noMeal = plan.noMeal, onNoMealChange = onNoMealChange)
                }
            }

            DayContent(plan = plan, freshnessLabel = day.slot.freshness.label, onOpenRecipe = onOpenRecipe)

            if (plan != null && !plan.noMeal) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ServingsStepper(
                        servings = plan.servings,
                        onChange = onServingsChange,
                        enabled = !locked,
                        modifier = Modifier.weight(1f),
                    )
                    if (!locked) {
                        IconButton(onClick = onPick) {
                            Icon(Icons.Filled.Search, contentDescription = "Choisir la recette")
                        }
                        IconButton(onClick = onRedraw) {
                            Icon(Icons.Filled.Casino, contentDescription = "Relancer le tirage du jour")
                        }
                    }
                }
                if (!locked) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        DayMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = plan.mode == mode,
                                onClick = { onModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, DayMode.entries.size),
                            ) {
                                Text(if (mode == DayMode.MANUAL) "Choix" else "Aléatoire")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayContent(plan: DayPlan?, freshnessLabel: String, onOpenRecipe: (Long) -> Unit) {
    val recipe = plan?.recipe
    when {
        plan == null -> Text("Pas de menu enregistré pour ce jour.", style = MaterialTheme.typography.bodyMedium)
        plan.noMeal -> Text("Sans repas", style = MaterialTheme.typography.bodyLarge)
        recipe == null -> Text(
            if (plan.mode == DayMode.RANDOM) {
                "Aucune recette disponible dans la liste $freshnessLabel."
            } else {
                "Aucune recette choisie."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        else -> Column(Modifier.fillMaxWidth().clickable { onOpenRecipe(recipe.id) }) {
            Text(recipe.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${formatMinutes(recipe.totalMinutes)} · ${servingsLabel(plan.servings)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    if (plan != null && plan.relaxed.isNotEmpty()) {
        AssistChip(
            onClick = {},
            label = { Text("Contrainte relâchée : " + plan.relaxed.joinToString(", ") { it.label }) },
            leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp)) },
        )
    }
}

@Composable
private fun DayMenu(noMeal: Boolean, onNoMealChange: (Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "Options du jour")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(if (noMeal) "Prévoir un repas" else "Sans repas ce jour-là") },
            onClick = {
                expanded = false
                onNoMealChange(!noMeal)
            },
        )
    }
}
