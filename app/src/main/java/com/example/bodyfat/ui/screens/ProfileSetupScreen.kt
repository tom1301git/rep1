package com.example.bodyfat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyfat.viewmodel.BodyFatViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onProfileSaved: () -> Unit,
    viewModel: BodyFatViewModel = viewModel()
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

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
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profil einrichten") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Bitte Geburtsdatum eingeben",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Das Alter wird für die Jackson-Pollock-Formel benötigt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(selectedDate?.format(formatter) ?: "Datum wählen")
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    selectedDate?.let {
                        viewModel.saveProfile(it)
                        onProfileSaved()
                    }
                },
                enabled = selectedDate != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }
        }
    }
}
