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
    val publicationYear: Int,
    val category: String,
    val description: String,
    val status: String = "available" // "available" or "borrowed"
) : Parcelable 