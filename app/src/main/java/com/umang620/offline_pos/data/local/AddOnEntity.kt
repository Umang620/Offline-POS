package com.umang620.offline_pos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "add_ons")
data class AddOnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double = 0.0,
    val category: String = "General",
    val isActive: Boolean = true
)
