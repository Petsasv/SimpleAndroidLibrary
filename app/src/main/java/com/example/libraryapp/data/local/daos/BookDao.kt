package com.example.libraryapp.data.local.daos

import androidx.room.*
import com.example.libraryapp.data.local.entities.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE bookId = :bookId")
    suspend fun getBookById(bookId: Long): Book?

    @Query("""
        SELECT * FROM books 
        WHERE title LIKE '%' || :query || '%' 
        OR author LIKE '%' || :query || '%'
        OR isbn LIKE '%' || :query || '%'
    """)
    fun searchBooks(query: String): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE category = :category")
    fun getBooksByCategory(category: String): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE bookId IN (:bookIds)")
    fun getBooksByIds(bookIds: Set<Long>): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'available'")
    fun getAvailableBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = 'borrowed'")
    fun getBorrowedBooks(): Flow<List<Book>>
} 