package com.example.bodyfat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyfat.viewmodel.BodyFatViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BodyFatViewModel = viewModel()
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val profile by viewModel.userProfile.collectAsState()
    val allMeasurements by viewModel.allMeasurements.collectAsState()
    val scope = rememberCoroutineScope()

    val currentBirthDate = profile?.let { LocalDate.ofEpochDay(it.birthDateEpochDay) }
    var pendingBirthDate by remember(currentBirthDate) { mutableStateOf(currentBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = pendingBirthDate?.toEpochDay()?.times(86_400_000L)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        pendingBirthDate = LocalDate.ofEpochDay(millis / 86_400_000L)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState, title = { Text("Geburtsdatum wählen", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Geburtsdatum ändern") },
            text = {
                Text(
                    "Durch das Ändern des Geburtsdatums können bisherige Direkteinträge " +
                    "(ohne Messwerte) ungültig sein. Bei OK werden alle Direkteinträge gelöscht " +
                    "und alle Faltenmessungen mit dem neuen Alter neu berechnet."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    scope.launch {
                        viewModel.updateProfileAndRecalculate(pendingBirthDate!!)
                        onNavigateBack()
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Benutzerprofil", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gespeichertes Geburtsdatum", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = currentBirthDate?.format(formatter) ?: "Nicht gesetzt",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentBirthDate != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    Text("Neues Geburtsdatum", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(pendingBirthDate?.format(formatter) ?: "Datum auswählen")
                    }

                    val isChanged = pendingBirthDate != null && pendingBirthDate != currentBirthDate
                    Button(
                        onClick = {
                            if (pendingBirthDate == null) return@Button
                            val hasExistingMeasurements = allMeasurements.isNotEmpty()
                            val isActualChange = currentBirthDate != null && isChanged
                            if (isActualChange && hasExistingMeasurements) {
                                showConfirmDialog = true
                            } else {
                                viewModel.saveProfile(pendingBirthDate!!)
                                onNavigateBack()
                            }
                        },
                        enabled = pendingBirthDate != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Speichern")
                    }
                }
            }

            if (currentBirthDate != null) {
                Text(
                    "Die Jackson-Pollock-Formel berechnet das Körperfett anhand " +
                    "des Alters zum Zeitpunkt der jeweiligen Messung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
