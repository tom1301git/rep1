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

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 40.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val n = allMeasurements.size
                    val padV = fatRange * 0.1
                    val yMin = minFat - padV
                    val yMax = maxFat + padV
                    val yRange = yMax - yMin

                    fun xOf(i: Int) = if (n == 1) w / 2f else i.toFloat() / (n - 1) * w
                    fun yOf(v: Double) = (h * (1.0 - (v - yMin) / yRange)).toFloat()

                    val gridPaint = android.graphics.Paint().apply {
                        color = gridColor.toArgb()
                        strokeWidth = 1f
                    }
                    val textPaint = android.graphics.Paint().apply {
                        color = onSurface.toArgb()
                        textSize = 28f
                        isAntiAlias = true
                    }

                    repeat(5) { i ->
                        val v = yMin + yRange * i / 4.0
                        val y = yOf(v)
                        drawContext.canvas.nativeCanvas.drawLine(-48f, y, w, y, gridPaint)
                        drawContext.canvas.nativeCanvas.drawText(
                            "${"%.1f".format(v)}%", -48f, y + 8f, textPaint
                        )
                    }

                    val path = Path()
                    allMeasurements.forEachIndexed { i, m ->
                        val x = xOf(i)
                        val y = yOf(m.bodyFatPercent)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = primaryColor, style = Stroke(width = 4f))

                    val maxLabels = 5
                    val step = ((n - 1).toFloat() / (maxLabels - 1).coerceAtLeast(1)).coerceAtLeast(1f)
                    allMeasurements.forEachIndexed { i, m ->
                        val x = xOf(i)
                        val y = yOf(m.bodyFatPercent)
                        drawCircle(color = primaryColor, radius = 8f, center = Offset(x, y))
                        drawCircle(color = Color.White, radius = 4f, center = Offset(x, y))

                        val showLabel = n <= maxLabels || i == 0 || i == n - 1 ||
                            (i % step.toInt() == 0)
                        if (showLabel) {
                            val date = LocalDate.ofEpochDay(m.dateEpochDay)
                            drawContext.canvas.nativeCanvas.drawText(
                                date.format(labelFormatter), x - 28f, h + 32f, textPaint
                            )
                        }
                    }
                }
            }
        }
    }
}
