package com.example.bodyfat.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyfat.viewmodel.BodyFatViewModel
import com.example.bodyfat.viewmodel.ImportResult
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BodyFatViewModel = viewModel()
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val profile by viewModel.userProfile.collectAsState()
    val allMeasurements by viewModel.allMeasurements.collectAsState()
    val scope = rememberCoroutineScope()

    val currentBirthDate = profile?.let { LocalDate.ofEpochDay(it.birthDateEpochDay) }
    var pendingBirthDate by remember(currentBirthDate) { mutableStateOf(currentBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var exportError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = pendingBirthDate?.toEpochDay()?.times(86_400_000L)
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            importResult = null
            scope.launch {
                importResult = viewModel.importFromCsv(context, it)
            }
        }
    }

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
            DatePicker(
                state = datePickerState,
                title = { Text("Geburtsdatum wählen", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) }
            )
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
            // ── Profil ────────────────────────────────────────────────────
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
                        Text("Gespeichertes Geburtsdatum")
                        Text(
                            text = currentBirthDate?.format(formatter) ?: "Nicht gesetzt",
                            color = if (currentBirthDate != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(pendingBirthDate?.format(formatter) ?: "Neues Datum auswählen")
                    }

                    val isChanged = pendingBirthDate != null && pendingBirthDate != currentBirthDate
                    Button(
                        onClick = {
                            if (pendingBirthDate == null) return@Button
                            val birthdateChanged = currentBirthDate != null && isChanged
                            if (birthdateChanged && allMeasurements.isNotEmpty()) {
                                showConfirmDialog = true
                            } else {
                                viewModel.saveProfile(pendingBirthDate!!)
                                onNavigateBack()
                            }
                        },
                        enabled = pendingBirthDate != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Speichern") }
                }
            }

            // ── Datensicherung ────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Datensicherung", style = MaterialTheme.typography.titleMedium)

                    Text(
                        "Die App sichert Daten automatisch über Android Auto Backup " +
                        "(Google Drive). Zusätzlich können Daten manuell als CSV exportiert " +
                        "und importiert werden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            exportError = false
                            scope.launch {
                                val uri = viewModel.exportToCsv(context)
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Körperfett-Export")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export teilen"))
                                } else {
                                    exportError = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Daten exportieren (CSV)") }

                    if (exportError) {
                        Text("Export fehlgeschlagen.", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = {
                            importResult = null
                            importLauncher.launch(arrayOf("text/csv", "text/plain", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Daten importieren (CSV)") }

                    importResult?.let { result ->
                        val msg = when {
                            result.imported == 0 && result.skipped == 0 ->
                                "Import fehlgeschlagen oder Datei leer."
                            result.skipped > 0 ->
                                "${result.imported} Einträge importiert, ${result.skipped} übersprungen (Datum bereits vorhanden)."
                            else -> "${result.imported} Einträge erfolgreich importiert."
                        }
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result.imported > 0)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
