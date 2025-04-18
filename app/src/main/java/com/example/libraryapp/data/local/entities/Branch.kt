package com.example.libraryapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "branches")
data class Branch(
    @PrimaryKey(autoGenerate = true)
    val branchId: Long = 0,
    val name: String,
    val location: String,
    val contactInfo: String
) 