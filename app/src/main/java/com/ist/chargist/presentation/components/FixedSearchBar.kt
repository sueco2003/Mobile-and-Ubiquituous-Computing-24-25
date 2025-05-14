package com.ist.chargist.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedSearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit
) {
    // This avoids expansion by keeping active = false
    val focusManager = LocalFocusManager.current

    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {
            onSearch(query)
            focusManager.clearFocus() // Dismiss keyboard
        },
        active = false, // Prevents expansion
        onActiveChange = {}, // Disable activation
        placeholder = { Text("Search location...") },
        modifier = modifier
    ) {
        // No suggestions shown
    }
}
