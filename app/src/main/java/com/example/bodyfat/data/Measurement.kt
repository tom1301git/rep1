package com.example.bodyfat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateEpochDay: Long,
    val chest: Int?,
    val abdomen: Int?,
    val thigh: Int?,
    val bodyFatPercent: Double
)
