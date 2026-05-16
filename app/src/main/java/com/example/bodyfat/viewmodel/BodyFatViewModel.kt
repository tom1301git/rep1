package com.example.bodyfat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bodyfat.data.AppDatabase
import com.example.bodyfat.data.Measurement
import com.example.bodyfat.data.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import kotlin.math.pow

class BodyFatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val measurementDao = db.measurementDao()
    private val profileDao = db.userProfileDao()

    val lastFive = measurementDao.getLastFive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
