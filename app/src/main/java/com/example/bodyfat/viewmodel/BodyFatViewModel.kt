package com.example.bodyfat.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodyfat.data.AppDatabase
import com.example.bodyfat.data.Measurement
import com.example.bodyfat.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.Period
import kotlin.math.pow

data class ImportResult(val imported: Int, val skipped: Int)

class BodyFatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val measurementDao = db.measurementDao()
    private val profileDao = db.userProfileDao()

    val allMeasurements = measurementDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile = profileDao.get()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun hasProfile(): Boolean = profileDao.getOnce() != null

    fun saveProfile(birthDate: LocalDate) {
        viewModelScope.launch {
            profileDao.save(UserProfile(birthDateEpochDay = birthDate.toEpochDay()))
        }
    }

    /** Changes birthdate, deletes all direct entries, recalculates all skinfold entries. */
    suspend fun updateProfileAndRecalculate(newBirthDate: LocalDate) {
        val newProfile = UserProfile(birthDateEpochDay = newBirthDate.toEpochDay())
        profileDao.save(newProfile)
        val all = measurementDao.getAllOnce()
        all.filter { it.chest == null }.forEach { measurementDao.delete(it) }
        all.filter { it.chest != null }.forEach { m ->
            val date = LocalDate.ofEpochDay(m.dateEpochDay)
            val fat = calculateBodyFat(date, m.chest!!, m.abdomen!!, m.thigh!!, newProfile)
            measurementDao.update(m.copy(bodyFatPercent = fat))
        }
    }

    suspend fun saveMeasurement(date: LocalDate, chest: Int, abdomen: Int, thigh: Int): Double? {
        val profile = profileDao.getOnce() ?: return null
        val bodyFat = calculateBodyFat(date, chest, abdomen, thigh, profile)
        measurementDao.insert(
            Measurement(dateEpochDay = date.toEpochDay(), chest = chest, abdomen = abdomen, thigh = thigh, bodyFatPercent = bodyFat)
        )
        return bodyFat
    }

    fun saveMeasurementDirect(date: LocalDate, bodyFatPercent: Double) {
        viewModelScope.launch {
            measurementDao.insert(
                Measurement(dateEpochDay = date.toEpochDay(), chest = null, abdomen = null, thigh = null, bodyFatPercent = bodyFatPercent)
            )
        }
    }

    fun updateMeasurement(measurement: Measurement) {
        viewModelScope.launch { measurementDao.update(measurement) }
    }

    fun deleteMeasurement(measurement: Measurement) {
        viewModelScope.launch { measurementDao.delete(measurement) }
    }

    fun calculateBodyFat(date: LocalDate, chest: Int, abdomen: Int, thigh: Int, profile: UserProfile): Double {
        val birthDate = LocalDate.ofEpochDay(profile.birthDateEpochDay)
        val ageYears = Period.between(birthDate, date).years
        val s = (chest + abdomen + thigh).toDouble()
        val bodyDensity = 1.10938 - (0.0008267 * s) + (0.0000016 * s.pow(2)) - (0.0002574 * ageYears)
        return (495.0 / bodyDensity) - 450.0
    }

    // ── Export ──────────────────────────────────────────────────────────────

    suspend fun exportToCsv(context: Context): Uri? = withContext(Dispatchers.IO) {
        try {
            val measurements = measurementDao.getAllOnce()
            val csv = buildString {
                appendLine("date,chest,abdomen,thigh,bodyFatPercent")
                measurements.forEach { m ->
                    val date = LocalDate.ofEpochDay(m.dateEpochDay)
                    appendLine("$date,${m.chest ?: ""},${m.abdomen ?: ""},${m.thigh ?: ""},${"%.4f".format(m.bodyFatPercent)}")
                }
            }
            val file = File(context.cacheDir, "koerperfett_export.csv")
            file.writeText(csv)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    // ── Import ──────────────────────────────────────────────────────────────

    suspend fun importFromCsv(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val rawContent = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText()

            if (rawContent == null) {
                ImportResult(0, 0)
            } else {
                val existingDates = measurementDao.getAllOnce().map { it.dateEpochDay }.toSet()
                var imported = 0
                var skipped = 0

                for (line in rawContent.lines().drop(1).filter { it.isNotBlank() }) {
                    val parts = line.split(",")
                    if (parts.size < 5) continue
                    val date = runCatching { LocalDate.parse(parts[0].trim()) }.getOrNull() ?: continue
                    val bodyFat = parts[4].trim().toDoubleOrNull() ?: continue

                    if (existingDates.contains(date.toEpochDay())) {
                        skipped++
                    } else {
                        measurementDao.insert(
                            Measurement(
                                dateEpochDay = date.toEpochDay(),
                                chest = parts[1].trim().toIntOrNull(),
                                abdomen = parts[2].trim().toIntOrNull(),
                                thigh = parts[3].trim().toIntOrNull(),
                                bodyFatPercent = bodyFat
                            )
                        )
                        imported++
                    }
                }
                ImportResult(imported, skipped)
            }
        } catch (e: Exception) {
            ImportResult(0, 0)
        }
    }
}
