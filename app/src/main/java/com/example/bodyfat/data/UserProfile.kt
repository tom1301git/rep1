package com.example.bodyfat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val birthDateEpochDay: Long,
    val targetLower: Double = 8.0,
    val targetUpper: Double = 10.0
)
