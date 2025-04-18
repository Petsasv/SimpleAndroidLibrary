package com.example.libraryapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "branch_books",
    primaryKeys = ["branchId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = Branch::class,
            parentColumns = ["branchId"],
            childColumns = ["branchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Book::class,
            parentColumns = ["bookId"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("branchId"), Index("bookId")]
)
data class BranchBook(
    val branchId: Long,
    val bookId: Long,
    val quantity: Int,
    val availableQuantity: Int
) 