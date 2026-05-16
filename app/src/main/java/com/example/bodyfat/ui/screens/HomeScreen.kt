package com.example.bodyfat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyfat.data.Measurement
import com.example.bodyfat.viewmodel.BodyFatViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChart: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: BodyFatViewModel = viewModel()
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val lastFive by viewModel.lastFive.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 86_400_000L
    )
    var chest by remember { mutableStateOf("") }
    var abdomen by remember { mutableStateOf("") }
    var thigh by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf<Double?>(null) }
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

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Datum: ${selectedDate.format(formatter)}")
                        }

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

                        if (errorMsg != null) {
                            Text(
                                errorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                val c = chest.toIntOrNull()
                                val a = abdomen.toIntOrNull()
                                val t = thigh.toIntOrNull()
                                if (c == null || a == null || t == null || c <= 0 || a <= 0 || t <= 0) {
                                    errorMsg = "Bitte alle Werte als positive ganze Zahlen eingeben."
                                    return@Button
                                }
                                errorMsg = null
                                scope.launch {
                                    val result = viewModel.saveMeasurement(selectedDate, c, a, t)
                                    if (result == null) {
                                        errorMsg = "Profil fehlt. Bitte zuerst Geburtsdatum eintragen."
                                    } else {
                                        lastResult = result
                                        chest = ""
                                        abdomen = ""
                                        thigh = ""
                                    }
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
                items(lastFive) { m -> MeasurementRow(m, formatter) }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MeasurementRow(m: Measurement, formatter: DateTimeFormatter) {
    val date = LocalDate.ofEpochDay(m.dateEpochDay)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(date.format(formatter), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "B: ${m.chest} | Ba: ${m.abdomen} | O: ${m.thigh} mm",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${"%.1f".format(m.bodyFatPercent)} %",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
