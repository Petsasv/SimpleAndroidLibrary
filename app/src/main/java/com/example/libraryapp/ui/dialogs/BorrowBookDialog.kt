package com.example.libraryapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.libraryapp.R
import com.example.libraryapp.data.local.LibraryDatabase
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.data.remote.models.BookLending
import com.example.libraryapp.databinding.DialogBorrowBookBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlinx.coroutines.launch

class BorrowBookDialog(
    private val book: Book,
    private val onBorrowSuccess: () -> Unit
) : DialogFragment(), LifecycleOwner {

    private var _binding: DialogBorrowBookBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var database: LibraryDatabase

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
        setupButtons()
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
            binding.etBorrowerName.error = getString(R.string.error_borrower_name_required)
            return false
        }

        if (days.isEmpty()) {
            binding.etDays.error = getString(R.string.error_days_required)
            return false
        }

        val daysInt = days.toIntOrNull()
        if (daysInt == null || daysInt < 1) {
            binding.etDays.error = getString(R.string.error_days_required)
            return false
        }

        return true
    }

    private fun borrowBook() {
        val borrowerName = binding.etBorrowerName.text.toString().trim()
        val days = binding.etDays.text.toString().toInt()

        // First, verify the user exists in Firestore
        firestore.collection("users")
            .whereEqualTo("name", borrowerName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // User exists, proceed with borrowing
                val userDoc = documents.documents[0]
                val userId = userDoc.id

                // Calculate return date
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, days)
                val returnDate = calendar.time

                // Create lending record
                val bookLending = BookLending(
                    id = UUID.randomUUID().toString(),
                    userName = borrowerName,
                    bookId = book.bookId,
                    bookName = book.title,
                    borrowDate = Date(),
                    returnDate = returnDate
                )

                // Update book status to borrowed
                val updatedBook = book.copy(status = "borrowed")
                android.util.Log.d("BorrowBookDialog", "Updating book status to borrowed: ${updatedBook}")

                // Save to Firestore
                firestore.collection("bookLendings")
                    .document(bookLending.id)
                    .set(bookLending)
                    .addOnSuccessListener {
                        // Update local database
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                database.bookDao().updateBook(updatedBook)
                                android.util.Log.d("BorrowBookDialog", "Book status updated in local database")
                                Toast.makeText(requireContext(), "Book borrowed successfully", Toast.LENGTH_SHORT).show()
                                onBorrowSuccess()
                                dismiss()
                            } catch (e: Exception) {
                                android.util.Log.e("BorrowBookDialog", "Error updating local database: ${e.message}")
                                Toast.makeText(requireContext(), "Error updating local database: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("BorrowBookDialog", "Error borrowing book: ${e.message}")
                        Toast.makeText(requireContext(), "Error borrowing book: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("BorrowBookDialog", "Error verifying user: ${e.message}")
                Toast.makeText(requireContext(), "Error verifying user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 