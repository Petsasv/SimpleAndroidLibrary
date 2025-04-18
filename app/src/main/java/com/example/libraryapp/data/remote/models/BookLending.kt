package com.example.libraryapp.data.remote.models

import java.util.*

data class BookLending(
    val id: String = "",  // Firebase generated ID
    val userName: String = "",  // Name of the user borrowing the book
    val bookId: Long = 0,  // ID of the book from Room database
    val bookName: String = "",  // Name of the book
    val borrowDate: Date = Date(),  // When the book was borrowed
    val returnDate: Date? = null  // When the book was returned (null if not returned)
) 