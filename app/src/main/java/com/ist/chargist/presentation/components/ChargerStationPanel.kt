package com.ist.chargist.presentation.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ist.chargist.R
import com.ist.chargist.domain.model.ChargeSpeed
import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.domain.model.Connector
import com.ist.chargist.presentation.map.MapViewModel
import com.ist.chargist.utils.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargerStationPanel(
    station: ChargerStation,
    viewModel: MapViewModel,
    isAnonymous: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    val slotsState by viewModel.slotLocation
    val pendingSlotUpdates = remember { mutableStateMapOf<String, ChargerSlot>() }
    var editingSlotId by remember { mutableStateOf<String?>(null) }
    val newSlots = remember { mutableStateListOf<ChargerSlot>() }

    var selectedSlot by remember { mutableStateOf<ChargerSlot?>(null) }

    fun updateSlot(updated: ChargerSlot) {
        val newIndex = newSlots.indexOfFirst { it.slotId == updated.slotId }
        if (newIndex != -1) {
            newSlots[newIndex] = updated
        }
        pendingSlotUpdates[updated.slotId] = updated
    }



    LaunchedEffect(station.id) {
        viewModel.getSlotsForStation(station.id)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (pendingSlotUpdates.isNotEmpty()) {
                viewModel.updateSlots(pendingSlotUpdates.values.toList())
            }
            onDismiss()
        }
    }

    Card(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val context = LocalContext.current
            val isUnmetered = remember { isUnmeteredConnection(context) }
            var loadImage by remember { mutableStateOf(isUnmetered) }

            if (!station.imageUri.isNullOrEmpty()) {
                if (loadImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(station.imageUri.toUri())
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image of ${station.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clickable { loadImage = true }
                            .background(Color.Gray) // Optional placeholder color
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tap to load image", color = Color.White)
                    }
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_default_bg_charger),
                    contentDescription = "Default Charger Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!isAnonymous) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "Unmark favorite" else "Mark as favorite",
                            tint = if (isFavorite) Color.Yellow else Color.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Lat: ${station.lat}, Lon: ${station.lon}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text("Prices:", style = MaterialTheme.typography.bodyMedium)
            station.fastPrice?.let {
                Text(
                    "Fast: ${it}€",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            station.mediumPrice?.let {
                Text(
                    "Medium: ${it}€",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            station.slowPrice?.let {
                Text(
                    "Slow: ${it}€",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Payment Methods:", style = MaterialTheme.typography.bodyMedium)
            Row {
                station.payment.forEach { method ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = method,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isAnonymous) {
                Button(onClick = {
                    val newSlotId = UUID.randomUUID().toString()
                    val newSlot = ChargerSlot(
                        slotId = newSlotId,
                        stationId = station.id,
                        speed = ChargeSpeed.F,
                        connector = Connector.CCS2,
                        available = true
                    )
                    newSlots += newSlot
                    pendingSlotUpdates[newSlotId] = newSlot
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add More Slots", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Available Slots:", style = MaterialTheme.typography.bodyMedium)

            when (slotsState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                is UiState.Error -> Text("Failed to load slots.", color = Color.Red)
                is UiState.Success -> {
                    ((slotsState as UiState.Success).data as? List<ChargerSlot>)?.forEachIndexed { index, originalSlot ->

                        val slot = pendingSlotUpdates[originalSlot.slotId] ?: originalSlot
                        var currentSpeed by remember { mutableStateOf(slot.speed) }
                        var currentConnector by remember { mutableStateOf(slot.connector) }
                        val isEditing = editingSlotId == slot.slotId
                        val backgroundColor = if (slot.available)
                            Color(0xFFE8F5E9) // light green for available
                        else
                            Color(0xFFFFEBEE) // light red for unavailable

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = backgroundColor,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedSlot = slot
                                    viewModel.checkDamageReport(slot.slotId)}, // Open popup on click
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Charger ${index + 1}",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        // Set slot availability to false locally
                                        pendingSlotUpdates[slot.slotId] = slot.copy(available = false)

                                        // Trigger ViewModel logic
                                        viewModel.reportProblems(slot.slotId)
                                    }) {
                                        Icon(Icons.Default.Warning, contentDescription = "Report")
                                    }
                                    IconButton(onClick = {
                                        if (isEditing) {
                                            pendingSlotUpdates[slot.slotId] = slot.copy(
                                                speed = currentSpeed,
                                                connector = currentConnector
                                            )
                                        }
                                        editingSlotId = if (isEditing) null else slot.slotId
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = if (isEditing) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                        )
                                    }
                                }

                                if (isEditing) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        // Speed dropdown
                                        var expandedSpeed by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(
                                            expanded = expandedSpeed,
                                            onExpandedChange = { expandedSpeed = !expandedSpeed }
                                        ) {
                                            TextField(
                                                readOnly = true,
                                                value = currentSpeed.name,
                                                onValueChange = {},
                                                label = { Text("Speed") },
                                                modifier = Modifier.menuAnchor().fillMaxWidth()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expandedSpeed,
                                                onDismissRequest = { expandedSpeed = false }
                                            ) {
                                                ChargeSpeed.entries.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option.name) },
                                                        onClick = {
                                                            currentSpeed = option
                                                            expandedSpeed = false
                                                            val updated = slot.copy(
                                                                speed = currentSpeed,
                                                                connector = currentConnector
                                                            )
                                                            updateSlot(updated)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Connector dropdown
                                        var expandedConnector by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(
                                            expanded = expandedConnector,
                                            onExpandedChange = {
                                                expandedConnector = !expandedConnector
                                            }
                                        ) {
                                            TextField(
                                                readOnly = true,
                                                value = currentConnector.name,
                                                onValueChange = {},
                                                label = { Text("Connector") },
                                                modifier = Modifier.menuAnchor().fillMaxWidth()
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expandedConnector,
                                                onDismissRequest = { expandedConnector = false }
                                            ) {
                                                Connector.entries.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option.name) },
                                                        onClick = {
                                                            currentConnector = option
                                                            expandedConnector = false
                                                            val updated = slot.copy(
                                                                speed = currentSpeed,
                                                                connector = currentConnector
                                                            )
                                                            updateSlot(updated)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Render new slots (unsaved additions)
                    newSlots.forEachIndexed { index, slot ->
                        var currentSpeed by remember { mutableStateOf(slot.speed) }
                        var currentConnector by remember { mutableStateOf(slot.connector) }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF9C4), // light yellow,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedSlot = slot }, // Open popup on click
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(
                                    "New Charger ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Speed dropdown
                                var expandedSpeed by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = expandedSpeed,
                                    onExpandedChange = { expandedSpeed = !expandedSpeed }
                                ) {
                                    TextField(
                                        readOnly = true,
                                        value = currentSpeed.name,
                                        onValueChange = {},
                                        label = { Text("Speed") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedSpeed,
                                        onDismissRequest = { expandedSpeed = false }
                                    ) {
                                        ChargeSpeed.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.name) },
                                                onClick = {
                                                    currentSpeed = option
                                                    expandedSpeed = false
                                                    val updated = slot.copy(
                                                        speed = currentSpeed,
                                                        connector = currentConnector
                                                    )
                                                    updateSlot(updated)
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

// Connector dropdown
                                var expandedConnector by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = expandedConnector,
                                    onExpandedChange = { expandedConnector = !expandedConnector }
                                ) {
                                    TextField(
                                        readOnly = true,
                                        value = currentConnector.name,
                                        onValueChange = {},
                                        label = { Text("Connector") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedConnector,
                                        onDismissRequest = { expandedConnector = false }
                                    ) {
                                        Connector.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.name) },
                                                onClick = {
                                                    currentConnector = option
                                                    expandedConnector = false
                                                    val updated = slot.copy(
                                                        speed = currentSpeed,
                                                        connector = currentConnector
                                                    )
                                                    updateSlot(updated)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is UiState.Fail -> Unit
                UiState.Idle -> Unit
            }
        }
    }
// Dialog to show slot details
    selectedSlot?.let { dialogSlot ->
        val currentSlot = pendingSlotUpdates[dialogSlot.slotId] ?: dialogSlot
        val reportTimestamp = viewModel.damageReports[currentSlot.slotId]


        AlertDialog(
            onDismissRequest = { selectedSlot = null },
            title = { Text("Slot Details") },
            text = {
                Column {
                    Text("Speed: ${currentSlot.speed.name}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Connector: ${currentSlot.connector.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusText = when {
                            currentSlot.available -> "Available"
                            reportTimestamp != null -> "For maintenance"
                            else -> "Unavailable"
                        }

                        val statusColor = when {
                            currentSlot.available -> Color.Green
                            reportTimestamp != null -> Color(0xFFFFC107)
                            else -> Color.Red
                        }

                        Text(
                            "Status: $statusText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            modifier = Modifier.weight(1f)
                        )

                        val statusIcon = when {
                            currentSlot.available -> Icons.Default.Check
                            reportTimestamp != null -> Icons.Default.Build
                            else -> Icons.Default.Close
                        }

                        val iconTint = when {
                            currentSlot.available -> Color.Green
                            reportTimestamp != null -> Color(0xFFFFC107) // Yellow
                            else -> Color.Red
                        }

                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Toggle Availability",
                            tint = iconTint
                        )

                    }
                    if (!currentSlot.available && reportTimestamp != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedDate = remember(reportTimestamp) {
                            val date = Date(reportTimestamp)
                            val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            format.format(date)
                        }
                        Text(
                            "⚠️ Damages have been reported. Last report: $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black
                        )
                    }

                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { selectedSlot = null }
                    ) {
                        Text("Close")
                    }
                    if (!isAnonymous) {
                        TextButton(
                            onClick = {
                                val updated = currentSlot.copy(available = !currentSlot.available)
                                updateSlot(updated)
                                selectedSlot = updated
                                if (reportTimestamp != null) {
                                    viewModel.fixSlot(updated.slotId)
                                }
                            }
                        ) {
                            if (reportTimestamp != null) {
                                Text("Set as fixed?")
                            } else {
                                Text("Toggle Status")
                            }
                        }
                    }
                }
            }
        )
    }
}

fun isUnmeteredConnection(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return !cm.isActiveNetworkMetered && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}


