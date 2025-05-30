package com.example.libraryapp.data.remote.models

import java.util.*

data class BookLending(
    val id: String = "",  // Firebase generated ID
    val userName: String = "",  // Name of the user borrowing the book
    val bookId: Long = 0,  // ID of the book from Room database
    val bookName: String = "",  // Name of the book
    val borrowDate: Date = Date(),  // When the book was borrowed
    val returnDate: Date,  // When the book is expected to be returned
    val isReturned: Boolean = false  // Whether the book has been returned
) 