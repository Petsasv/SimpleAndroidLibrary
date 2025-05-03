package com.example.libraryapp.data.local.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val bookId: Long = 0,
    val title: String,
    val author: String,
    val isbn: String,
    val category: String,
    val quantity: Int = 1,  // Default quantity is 1
    val price: Double = 0.0,  // Default price is 0.0
    val status: String = "available" // "available" or "borrowed"
) : Parcelable 