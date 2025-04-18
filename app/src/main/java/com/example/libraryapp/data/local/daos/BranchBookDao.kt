package com.example.libraryapp.data.local.daos

import androidx.room.*
import com.example.libraryapp.data.local.entities.BranchBook
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranchBook(branchBook: BranchBook): Long

    @Update
    suspend fun updateBranchBook(branchBook: BranchBook)

    @Delete
    suspend fun deleteBranchBook(branchBook: BranchBook)

    @Query("SELECT * FROM branch_books")
    fun getAllBranchBooks(): Flow<List<BranchBook>>

    @Query("SELECT * FROM branch_books WHERE branchId = :branchId")
    fun getBooksByBranch(branchId: Long): Flow<List<BranchBook>>

    @Query("SELECT * FROM branch_books WHERE bookId = :bookId")
    fun getBranchesByBook(bookId: Long): Flow<List<BranchBook>>

    @Query("SELECT * FROM branch_books WHERE branchId = :branchId AND bookId = :bookId")
    suspend fun getBranchBook(branchId: Long, bookId: Long): BranchBook?

    @Query("SELECT * FROM branch_books WHERE availableQuantity > 0")
    fun getAvailableBooks(): Flow<List<BranchBook>>

    @Query("SELECT * FROM branch_books WHERE bookId = :bookId LIMIT 1")
    suspend fun getBranchBookByBookId(bookId: Long): BranchBook?
} 