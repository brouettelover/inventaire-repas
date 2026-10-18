package fr.inventairerepas.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Barre de navigation entre semaines : passée, en cours, suivante. */
@Composable
fun WeekSelector(
    weekStart: LocalDate?,
    offset: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Semaine précédente")
        }
        val title = when {
            offset == 0 -> "Cette semaine"
            offset == 1 -> "Semaine prochaine"
            offset == -1 -> "Semaine dernière"
            else -> "Il y a ${-offset} semaines"
        }
        val range = weekStart?.let { "du ${it.shortLabel()} au ${it.plusDays(6).shortLabel()}" }.orEmpty()
        Text(
            text = "$title\n$range",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = offset < SelectedWeek.MAX_OFFSET) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Semaine suivante")
        }
    }
}

/** Sélecteur − n + pour un nombre de personnes. */
@Composable
fun ServingsStepper(
    servings: Int,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(servings - 1) }, enabled = enabled && servings > 1) {
            Icon(Icons.Filled.Remove, contentDescription = "Une personne de moins")
        }
        Text(servingsLabel(servings), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { onChange(servings + 1) }, enabled = enabled && servings < 20) {
            Icon(Icons.Filled.Add, contentDescription = "Une personne de plus")
        }
    }
}
