package com.example.bodyfat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyfat.data.Measurement
import com.example.bodyfat.data.UserProfile
import com.example.bodyfat.viewmodel.BodyFatViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class EntryMode { SKINFOLD, DIRECT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChart: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: BodyFatViewModel = viewModel()
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val lastFive by viewModel.lastFive.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val scope = rememberCoroutineScope()

    var entryMode by remember { mutableStateOf(EntryMode.SKINFOLD) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 86_400_000L
    )
    var chest by remember { mutableStateOf("") }
    var abdomen by remember { mutableStateOf("") }
    var thigh by remember { mutableStateOf("") }
    var bodyFatInput by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf<Double?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var editingMeasurement by remember { mutableStateOf<Measurement?>(null) }
    var deletingMeasurement by remember { mutableStateOf<Measurement?>(null) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    editingMeasurement?.let { m ->
        EditMeasurementDialog(
            measurement = m,
            profile = profile,
            formatter = formatter,
            viewModel = viewModel,
            onDismiss = { editingMeasurement = null },
            onSave = { updated ->
                viewModel.updateMeasurement(updated)
                editingMeasurement = null
            }
        )
    }

    deletingMeasurement?.let { m ->
        DeleteConfirmDialog(
            date = LocalDate.ofEpochDay(m.dateEpochDay).format(formatter),
            onConfirm = {
                viewModel.deleteMeasurement(m)
                deletingMeasurement = null
            },
            onDismiss = { deletingMeasurement = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Körperfett-Tracker") },
                actions = {
                    IconButton(onClick = onNavigateToChart) {
                        Icon(Icons.Default.BarChart, contentDescription = "Verlauf")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Neue Messung", style = MaterialTheme.typography.titleMedium)

                        // Entry mode toggle
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = entryMode == EntryMode.SKINFOLD,
                                onClick = { entryMode = EntryMode.SKINFOLD },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text("3-Falten-Methode") }
                            SegmentedButton(
                                selected = entryMode == EntryMode.DIRECT,
                                onClick = { entryMode = EntryMode.DIRECT },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text("Direkteingabe") }
                        }

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Datum: ${selectedDate.format(formatter)}")
                        }

                        if (entryMode == EntryMode.SKINFOLD) {
                            OutlinedTextField(
                                value = chest,
                                onValueChange = { chest = it.filter { c -> c.isDigit() } },
                                label = { Text("Brust (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = abdomen,
                                onValueChange = { abdomen = it.filter { c -> c.isDigit() } },
                                label = { Text("Bauch (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = thigh,
                                onValueChange = { thigh = it.filter { c -> c.isDigit() } },
                                label = { Text("Oberschenkel (mm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            OutlinedTextField(
                                value = bodyFatInput,
                                onValueChange = { v ->
                                    val filtered = v.filter { it.isDigit() || it == '.' || it == ',' }
                                    if (filtered.count { it == '.' || it == ',' } <= 1) bodyFatInput = filtered
                                },
                                label = { Text("Körperfettanteil (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        if (errorMsg != null) {
                            Text(
                                errorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                errorMsg = null
                                if (entryMode == EntryMode.SKINFOLD) {
                                    val c = chest.toIntOrNull()
                                    val a = abdomen.toIntOrNull()
                                    val t = thigh.toIntOrNull()
                                    if (c == null || a == null || t == null || c <= 0 || a <= 0 || t <= 0) {
                                        errorMsg = "Bitte alle Werte als positive ganze Zahlen eingeben."
                                        return@Button
                                    }
                                    scope.launch {
                                        val result = viewModel.saveMeasurement(selectedDate, c, a, t)
                                        if (result == null) {
                                            errorMsg = "Profil fehlt. Bitte zuerst Geburtsdatum eintragen."
                                        } else {
                                            lastResult = result
                                            chest = ""; abdomen = ""; thigh = ""
                                        }
                                    }
                                } else {
                                    val fat = bodyFatInput.replace(",", ".").toDoubleOrNull()
                                    if (fat == null || fat <= 0.0 || fat >= 100.0) {
                                        errorMsg = "Bitte einen gültigen Wert zwischen 0 und 100 eingeben."
                                        return@Button
                                    }
                                    viewModel.saveMeasurementDirect(selectedDate, fat)
                                    lastResult = fat
                                    bodyFatInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("OK") }

                        lastResult?.let { fat ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Körperfettanteil: ${"%.1f".format(fat)} %",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (lastFive.isNotEmpty()) {
                item {
                    Text("Letzte 5 Messungen", style = MaterialTheme.typography.titleSmall)
                }
                items(lastFive, key = { it.id }) { m ->
                    MeasurementRow(
                        measurement = m,
                        formatter = formatter,
                        onEdit = { editingMeasurement = m },
                        onDelete = { deletingMeasurement = m }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMeasurementDialog(
    measurement: Measurement,
    profile: UserProfile?,
    formatter: DateTimeFormatter,
    viewModel: BodyFatViewModel,
    onDismiss: () -> Unit,
    onSave: (Measurement) -> Unit
) {
    var editMode by remember { mutableStateOf(if (measurement.chest != null) EntryMode.SKINFOLD else EntryMode.DIRECT) }
    var selectedDate by remember { mutableStateOf(LocalDate.ofEpochDay(measurement.dateEpochDay)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 86_400_000L
    )
    var chest by remember { mutableStateOf(measurement.chest?.toString() ?: "") }
    var abdomen by remember { mutableStateOf(measurement.abdomen?.toString() ?: "") }
    var thigh by remember { mutableStateOf(measurement.thigh?.toString() ?: "") }
    var bodyFatInput by remember { mutableStateOf("%.1f".format(measurement.bodyFatPercent)) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Messung bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = editMode == EntryMode.SKINFOLD,
                        onClick = { editMode = EntryMode.SKINFOLD },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("3-Falten") }
                    SegmentedButton(
                        selected = editMode == EntryMode.DIRECT,
                        onClick = { editMode = EntryMode.DIRECT },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Direkt") }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Datum: ${selectedDate.format(formatter)}") }

                if (editMode == EntryMode.SKINFOLD) {
                    OutlinedTextField(
                        value = chest,
                        onValueChange = { chest = it.filter { c -> c.isDigit() } },
                        label = { Text("Brust (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = abdomen,
                        onValueChange = { abdomen = it.filter { c -> c.isDigit() } },
                        label = { Text("Bauch (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = thigh,
                        onValueChange = { thigh = it.filter { c -> c.isDigit() } },
                        label = { Text("Oberschenkel (mm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = bodyFatInput,
                        onValueChange = { v ->
                            val filtered = v.filter { it.isDigit() || it == '.' || it == ',' }
                            if (filtered.count { it == '.' || it == ',' } <= 1) bodyFatInput = filtered
                        },
                        label = { Text("Körperfettanteil (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                errorMsg = null
                if (editMode == EntryMode.SKINFOLD) {
                    val c = chest.toIntOrNull()
                    val a = abdomen.toIntOrNull()
                    val t = thigh.toIntOrNull()
                    if (c == null || a == null || t == null || c <= 0 || a <= 0 || t <= 0) {
                        errorMsg = "Bitte alle Werte als positive ganze Zahlen eingeben."
                        return@TextButton
                    }
                    if (profile == null) { errorMsg = "Profil nicht gefunden."; return@TextButton }
                    val fat = viewModel.calculateBodyFat(selectedDate, c, a, t, profile)
                    onSave(measurement.copy(dateEpochDay = selectedDate.toEpochDay(), chest = c, abdomen = a, thigh = t, bodyFatPercent = fat))
                } else {
                    val fat = bodyFatInput.replace(",", ".").toDoubleOrNull()
                    if (fat == null || fat <= 0.0 || fat >= 100.0) {
                        errorMsg = "Bitte einen gültigen Wert zwischen 0 und 100 eingeben."
                        return@TextButton
                    }
                    onSave(measurement.copy(dateEpochDay = selectedDate.toEpochDay(), chest = null, abdomen = null, thigh = null, bodyFatPercent = fat))
                }
            }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    date: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag löschen") },
        text = { Text("Den Eintrag vom $date wirklich löschen?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Löschen", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun MeasurementRow(
    measurement: Measurement,
    formatter: DateTimeFormatter,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val date = LocalDate.ofEpochDay(measurement.dateEpochDay)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(date.format(formatter), style = MaterialTheme.typography.bodyMedium)
                if (measurement.chest != null) {
                    Text(
                        "B: ${measurement.chest} | Ba: ${measurement.abdomen} | O: ${measurement.thigh} mm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Direkteingabe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${"%.1f".format(measurement.bodyFatPercent)} %",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Löschen", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
