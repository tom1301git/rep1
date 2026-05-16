package com.example.bodyfat.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bodyfat.viewmodel.BodyFatViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: BodyFatViewModel = viewModel()
) {
    val allMeasurements by viewModel.allMeasurements.collectAsState()
    val labelFormatter = DateTimeFormatter.ofPattern("dd.MM.")
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verlauf") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (allMeasurements.size < 2) {
                Text(
                    "Mindestens 2 Messungen erforderlich,\num einen Verlauf anzuzeigen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val fatValues = allMeasurements.map { it.bodyFatPercent }
                val minFat = fatValues.min()
                val maxFat = fatValues.max()
                val fatRange = (maxFat - minFat).coerceAtLeast(1.0)

                // Time range for proportional x-axis
                val xMinDay = allMeasurements.first().dateEpochDay
                val xMaxDay = allMeasurements.last().dateEpochDay
                val dayRange = (xMaxDay - xMinDay).coerceAtLeast(1L)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val padV = fatRange * 0.1
                    val yMin = minFat - padV
                    val yMax = maxFat + padV
                    val yRange = yMax - yMin

                    // x proportional to actual date, 1 pixel = dayRange / w days
                    fun xOf(epochDay: Long): Float = (epochDay - xMinDay).toFloat() / dayRange * w
                    fun yOf(v: Double): Float = (h * (1.0 - (v - yMin) / yRange)).toFloat()

                    val gridPaint = android.graphics.Paint().apply {
                        color = gridColor.toArgb()
                        strokeWidth = 1f
                    }
                    val textPaint = android.graphics.Paint().apply {
                        color = onSurface.toArgb()
                        textSize = 28f
                        isAntiAlias = true
                    }

                    // Horizontal grid lines with y-axis labels
                    repeat(5) { i ->
                        val v = yMin + yRange * i / 4.0
                        val y = yOf(v)
                        drawContext.canvas.nativeCanvas.drawLine(-48f, y, w, y, gridPaint)
                        drawContext.canvas.nativeCanvas.drawText(
                            "${"%.1f".format(v)}%", -48f, y + 8f, textPaint
                        )
                    }

                    // Line connecting measurement points
                    val path = Path()
                    allMeasurements.forEachIndexed { i, m ->
                        val x = xOf(m.dateEpochDay)
                        val y = yOf(m.bodyFatPercent)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = primaryColor, style = Stroke(width = 4f))

                    // Measurement dots
                    allMeasurements.forEach { m ->
                        val x = xOf(m.dateEpochDay)
                        val y = yOf(m.bodyFatPercent)
                        drawCircle(color = primaryColor, radius = 8f, center = Offset(x, y))
                        drawCircle(color = Color.White, radius = 4f, center = Offset(x, y))
                    }

                    // X-axis date labels: up to 5 evenly spaced across the time range
                    val labelCount = minOf(5, allMeasurements.size)
                    repeat(labelCount) { i ->
                        val epochDay = xMinDay + (dayRange * i / (labelCount - 1).coerceAtLeast(1))
                        val x = xOf(epochDay)
                        val date = LocalDate.ofEpochDay(epochDay)
                        drawContext.canvas.nativeCanvas.drawText(
                            date.format(labelFormatter), x - 28f, h + 32f, textPaint
                        )
                    }
                }
            }
        }
    }
}
