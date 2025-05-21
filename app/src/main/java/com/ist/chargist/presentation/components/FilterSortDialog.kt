package com.ist.chargist.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ist.chargist.domain.model.ChargeSpeed

@Composable
fun FilterSortDialog(
    currentFilters: List<String>,
    selectedSort: String,
    sortAscending: Boolean,
    onDismiss: () -> Unit,
    onApply: (List<String>, String, Boolean) -> Unit
) {
    val filters = remember { mutableStateListOf<String>().apply { addAll(currentFilters) } }
    var sort by remember { mutableStateOf(selectedSort) }
    var ascending by remember { mutableStateOf(sortAscending) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Filter & Sort", style = MaterialTheme.typography.headlineSmall)

                // Filters Section
                Text("Filters:", style = MaterialTheme.typography.bodyLarge)
                listOf(
                    "Available Slots" to "available",
                    "Fast Charging (${ChargeSpeed.F.name})" to "fast",
                    "Medium Charging (${ChargeSpeed.M.name})" to "medium",
                    "Slow Charging (${ChargeSpeed.S.name})" to "slow"
                ).forEach { (label, key) ->
                    FilterChip(
                        selected = filters.contains(key),
                        onClick = {
                            if (filters.contains(key)) filters.remove(key)
                            else filters.add(key)
                        },
                        label = { Text(label) }
                    )
                }

                // Sorting Section
                Text("Sort by:", style = MaterialTheme.typography.bodyLarge)
                listOf(
                    "Price" to "price",
                    "Fastest Available" to "speed",
                    "Distance" to "distance"
                ).forEach { (label, key) ->
                    SortingOption(
                        label = label,
                        selected = sort == key,
                        onClick = { sort = key }
                    )
                }

                // Sort Direction
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Order:")
                    RadioButton(
                        selected = ascending,
                        onClick = { ascending = true }
                    )
                    Text("Ascending")
                    RadioButton(
                        selected = !ascending,
                        onClick = { ascending = false }
                    )
                    Text("Descending")
                }

                // Dialog Actions
                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onApply(filters.toList(), sort, ascending)
                        onDismiss()
                    }) { Text("Apply") }
                }
            }
        }
    }
}

@Composable
private fun SortingOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label)
    }
}