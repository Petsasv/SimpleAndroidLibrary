package com.example.libraryapp.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseRepository"

    suspend fun returnBook(bookId: String, userId: String, lendingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to return book: $bookId for user: $userId with lending ID: $lendingId")
            
            // Get the document reference directly using the lendingId
            val lendingRef = firestore.collection("bookLendings").document(lendingId)
            
            // Verify the document exists
            val docSnapshot = lendingRef.get().await()
            if (!docSnapshot.exists()) {
                Log.e(TAG, "Lending record $lendingId does not exist")
                return@withContext Result.failure(Exception("Lending record not found"))
            }

            // Verify it's the correct book and user
            val recordBookId = docSnapshot.getLong("bookId")?.toString()
            val recordUserName = docSnapshot.getString("userName")
            
            if (recordBookId != bookId || recordUserName != userId) {
                Log.e(TAG, "Lending record $lendingId does not match the provided book and user")
                return@withContext Result.failure(Exception("Lending record does not match"))
            }

            // Update the lending record to mark it as returned
            lendingRef.update(
                mapOf(
                    "isReturned" to true,
                    "returnDate" to FieldValue.serverTimestamp()
                )
            ).await()
            
            Log.d(TAG, "Successfully updated lending record as returned")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error returning book: ${e.message}", e)
            Result.failure(e)
        }
    }
} 