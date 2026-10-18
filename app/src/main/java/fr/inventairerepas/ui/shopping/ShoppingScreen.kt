package fr.inventairerepas.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.inventairerepas.ui.common.AppScaffold
import fr.inventairerepas.ui.common.WeekSelector
import fr.inventairerepas.ui.common.dayLabel
import fr.inventairerepas.ui.common.label

@Composable
fun ShoppingScreen(viewModel: ShoppingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScaffold(
        title = "Listes de courses",
        message = state.message,
        onMessageShown = viewModel::messageShown,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
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
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                        FilledTonalButton(onClick = viewModel::generate) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.lists.isEmpty()) "Générer les listes" else "Régénérer")
                        }
                    }
                }
            }
            if (state.lists.isEmpty() && !state.loading) {
                item {
                    Text(
                        if (state.editable) {
                            "Aucune liste pour cette semaine. Génère-les à partir du menu et du stock."
                        } else {
                            "Aucune liste pour cette semaine."
                        },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            state.lists.forEach { list ->
                item(key = "header-${list.blockIndex}") {
                    Text(
                        "Courses du ${list.shoppingDate.dayLabel()}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                if (list.items.isEmpty()) {
                    item(key = "empty-${list.blockIndex}") {
                        Text("Rien à acheter : le stock couvre tout.", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                items(list.items, key = { it.id }) { item ->
                    val toggle = { if (state.editable) viewModel.toggle(item.id, !item.checked) }
                    ListItem(
                        modifier = Modifier.clickable(enabled = state.editable) { toggle() },
                        leadingContent = {
                            Checkbox(
                                checked = item.checked,
                                onCheckedChange = { viewModel.toggle(item.id, it) },
                                enabled = state.editable,
                            )
                        },
                        headlineContent = {
                            Text(
                                item.ingredient.name,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            )
                        },
                        trailingContent = { Text(item.quantity.label()) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
