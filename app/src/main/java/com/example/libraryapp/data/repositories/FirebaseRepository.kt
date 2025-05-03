package com.example.libraryapp.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.example.libraryapp.utils.NotificationHelper

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseRepository"

    suspend fun addBook(book: Map<String, Any>): Boolean {
        return try {
            val result = firestore.collection("books")
                .add(book)
                .await()

            // Send notification about new book
            val bookTitle = book["title"] as? String ?: ""
            NotificationHelper().sendNewBookAvailableNotification(bookTitle)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding book: ${e.message}", e)
            false
        }
    }

    suspend fun borrowBook(bookId: Long, userName: String, days: Int): Boolean {
        return try {
            // Get book details
            val bookDoc = firestore.collection("books")
                .whereEqualTo("id", bookId)
                .get()
                .await()
                .documents
                .firstOrNull()

            if (bookDoc == null) {
                Log.e(TAG, "Book not found: $bookId")
                return false
            }

            val bookName = bookDoc.getString("title") ?: ""
            val returnDate = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)

            // Create lending record
            val lendingData = mapOf(
                "bookId" to bookId,
                "userName" to userName,
                "borrowDate" to System.currentTimeMillis(),
                "returnDate" to returnDate,
                "isReturned" to false,
                "bookName" to bookName
            )

            firestore.collection("bookLendings")
                .add(lendingData)
                .await()

            // Send notification to user
            NotificationHelper().sendBookBorrowedNotification(userName, bookName)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error borrowing book: ${e.message}", e)
            false
        }
    }

    suspend fun returnBook(bookId: Long, userName: String): Boolean {
        return try {
            val lendingSnapshot = firestore.collection("bookLendings")
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("userName", userName)
                .whereEqualTo("isReturned", false)
                .get()
                .await()

            if (lendingSnapshot.isEmpty) {
                Log.e(TAG, "No active lending record found for book $bookId and user $userName")
                return false
            }

            val lendingDoc = lendingSnapshot.documents.first()
            val lendingId = lendingDoc.id

            // Update the lending record
            firestore.collection("bookLendings")
                .document(lendingId)
                .update("isReturned", true)
                .await()

            // Get the book name for the notification
            val bookName = lendingDoc.data?.get("bookName") as? String ?: ""

            // Send notification that the book is now available
            NotificationHelper().sendNewBookAvailableNotification(bookName)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error returning book: ${e.message}", e)
            false
        }
    }
} 