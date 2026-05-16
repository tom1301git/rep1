package com.example.bodyfat.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(measurement: Measurement)

    @Update
    suspend fun update(measurement: Measurement)

    @Delete
    suspend fun delete(measurement: Measurement)

    @Query("SELECT * FROM measurements ORDER BY dateEpochDay DESC LIMIT 5")
    fun getLastFive(): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements ORDER BY dateEpochDay ASC")
    fun getAll(): Flow<List<Measurement>>

    @Query("SELECT * FROM measurements ORDER BY dateEpochDay ASC")
    suspend fun getAllOnce(): List<Measurement>
}
