package com.example.libraryapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.libraryapp.R
import com.example.libraryapp.data.local.LibraryDatabase
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.data.remote.models.BookLending
import com.example.libraryapp.databinding.DialogBorrowBookBinding
import com.example.libraryapp.fragments.StatsFragment
import com.example.libraryapp.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BorrowBookDialog(
    private val book: Book,
    private val onBorrowSuccess: () -> Unit
) : DialogFragment(), LifecycleOwner {

    private var _binding: DialogBorrowBookBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var database: LibraryDatabase
    private val TAG = "BorrowBookDialog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LibraryDatabase.getDatabase(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBorrowBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUserSearch()
        setupButtons()
    }

    private fun setupUserSearch() {
        val autoCompleteTextView = binding.etBorrowerName
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf())
        autoCompleteTextView.setAdapter(adapter)

        // Load users from Firestore
        lifecycleScope.launch {
            try {
                val usersSnapshot = firestore.collection("users")
                    .get()
                    .await()

                val userNames = usersSnapshot.documents
                    .mapNotNull { it.getString("name") }
                    .sorted()

                adapter.clear()
                adapter.addAll(userNames)
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users: ${e.message}", e)
                Toast.makeText(requireContext(), "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Filter users as user types
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = adapter.getItem(position)
            if (selectedUser != null) {
                autoCompleteTextView.setText(selectedUser)
            }
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnBorrow.setOnClickListener {
            if (validateInput()) {
                borrowBook()
            }
        }
    }

    private fun validateInput(): Boolean {
        val borrowerName = binding.etBorrowerName.text.toString().trim()
        val days = binding.etDays.text.toString()

        if (borrowerName.isEmpty()) {
            binding.tilBorrowerName.error = getString(R.string.error_borrower_name_required)
            return false
        }

        if (days.isEmpty()) {
            binding.tilDays.error = getString(R.string.error_days_required)
            return false
        }

        val daysInt = days.toIntOrNull()
        if (daysInt == null || daysInt < 1) {
            binding.tilDays.error = getString(R.string.error_days_required)
            return false
        }

        return true
    }

    private fun borrowBook() {
        val borrowerName = binding.etBorrowerName.text.toString().trim()
        val days = binding.etDays.text.toString().toInt()

        borrowBook(borrowerName, days)
    }

    private fun borrowBook(borrowerName: String, daysToBorrow: Int) {
        lifecycleScope.launch {
            try {
                // Verify user exists
                val userSnapshot = firestore.collection("users")
                    .whereEqualTo("name", borrowerName)
                    .get()
                    .await()

                if (userSnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Calculate return date
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, daysToBorrow)
                val returnDate = calendar.time

                // Create a new lending record in Firestore
                val lendingId = UUID.randomUUID().toString()
                
                // Add the lending record to Firestore with explicit field names
                firestore.collection("bookLendings")
                    .document(lendingId)
                    .set(mapOf(
                        "id" to lendingId,
                        "userName" to borrowerName,
                        "bookId" to book.bookId,
                        "bookName" to book.title,
                        "borrowDate" to Date(),
                        "returnDate" to returnDate,
                        "isReturned" to false
                    ))
                    .await()

                Log.d(TAG, "Lending record created successfully")

                // Update the book in local database - only status
                val updatedBook = book.copy(status = "borrowed")
                database.bookDao().updateBook(updatedBook)

                // Send notification
                lifecycleScope.launch {
                    NotificationHelper().sendBookBorrowedNotification(borrowerName, book.title)
                }

                Toast.makeText(requireContext(), getString(R.string.book_borrowed_successfully), Toast.LENGTH_SHORT).show()
                
                // Refresh stats
                (parentFragment as? StatsFragment)?.refreshStats()
                
                onBorrowSuccess()
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_borrowing_book, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 