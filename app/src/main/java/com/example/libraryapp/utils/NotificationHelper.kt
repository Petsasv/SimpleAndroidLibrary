package com.example.libraryapp.utils


import android.os.Build
import android.util.Log
import com.example.libraryapp.MainActivity
import com.example.libraryapp.services.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationHelper {
    private val TAG = "NotificationHelper"
    private val notificationService = NotificationService()

    suspend fun sendBookBorrowedNotification(userName: String, bookTitle: String) {
        try {
            Log.d(TAG, "Sending borrow notification for user: $userName, book: $bookTitle")
            showLocalNotification(
                title = "Book Borrowed",
                message = "You have successfully borrowed '$bookTitle'"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending borrow notification: ${e.message}", e)
        }
    }

    suspend fun sendBookReturnReminder(userName: String, bookTitle: String, daysLeft: Int) {
        try {
            Log.d(TAG, "Sending return reminder for user: $userName, book: $bookTitle")
            showLocalNotification(
                title = "Book Return Reminder",
                message = "Please return '$bookTitle' in $daysLeft days"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending return reminder: ${e.message}", e)
        }
    }

    suspend fun sendNewBookAvailableNotification(bookTitle: String) {
        try {
            Log.d(TAG, "Sending new book notification for: $bookTitle")
            showLocalNotification(
                title = "New Book Available",
                message = "'$bookTitle' is now available in the library"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending new book notification: ${e.message}", e)
        }
    }

    private suspend fun showLocalNotification(title: String, message: String): Unit = withContext(Dispatchers.Main) {
        try {
            val context = MainActivity.instance
            
            // Check if notifications are enabled in app settings
            if (!AppSettings.getInstance(context).areNotificationsEnabled()) {
                Log.d(TAG, "Notifications are disabled in app settings")
                return@withContext
            }

            // Check if notification permission is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Notification permission not granted")
                    return@withContext
                }
            }

            // Show the notification
            notificationService.showNotification(context, title, message)
            Log.d(TAG, "Local notification sent successfully: $title - $message")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing local notification: ${e.message}", e)
        }
    }
}