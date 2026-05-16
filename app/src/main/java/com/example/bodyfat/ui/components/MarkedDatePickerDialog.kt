package com.example.bodyfat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MarkedDatePickerDialog(
    selectedDate: LocalDate,
    markedDates: Set<Long>,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN) }
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var pendingDate by remember { mutableStateOf(selectedDate) }
    val today = remember { LocalDate.now() }

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Vormonat")
                }
                Text(
                    text = currentMonth.format(monthFormatter),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Folgemonat")
                }
            }
        },
        text = {
            Column {
                // Weekday header
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                val firstDay = currentMonth.atDay(1)
                val firstDayOffset = firstDay.dayOfWeek.value - 1 // 0=Mon … 6=Sun
                val daysInMonth = currentMonth.lengthOfMonth()
                val rows = (firstDayOffset + daysInMonth + 6) / 7

                repeat(rows) { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { col ->
                            val dayNumber = row * 7 + col - firstDayOffset + 1
                            if (dayNumber < 1 || dayNumber > daysInMonth) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val date = currentMonth.atDay(dayNumber)
                                val isSelected = date == pendingDate
                                val hasMarker = markedDates.contains(date.toEpochDay())
                                val isToday = date == today

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable { pendingDate = date },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(
                                                color = when {
                                                    isSelected -> primary
                                                    else -> Color.Transparent
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            color = when {
                                                isSelected -> onPrimary
                                                isToday -> primary
                                                else -> onSurface
                                            },
                                            style = if (isToday && !isSelected)
                                                MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                                )
                                            else MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    // Dot indicator for existing measurements
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(
                                                color = if (hasMarker && !isSelected) primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDateSelected(pendingDate); onDismiss() }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
