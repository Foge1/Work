package com.loaderapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val role: UserRole,
    val rating: Double = 5.0,
    val birthDate: Long? = null, // timestamp дня рождения
    val avatarInitials: String = "", // инициалы для аватара (авто)
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole { DISPATCHER, LOADER }
