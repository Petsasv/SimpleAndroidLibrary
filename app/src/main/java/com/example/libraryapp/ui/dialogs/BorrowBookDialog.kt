package com.example.libraryapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
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
import com.example.libraryapp.fragments.StatsFragment
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

        borrowBook(borrowerName, days)
    }

    private fun borrowBook(borrowerName: String, daysToBorrow: Int) {
        lifecycleScope.launch {
            try {
                // Calculate return date
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, daysToBorrow)
                val returnDate = calendar.time

                // Create a new lending record in Firestore
                val lendingId = UUID.randomUUID().toString()
                val lendingRecord = BookLending(
                    id = lendingId,
                    userName = borrowerName,
                    bookId = book.bookId,
                    bookName = book.title,
                    borrowDate = Date(),
                    returnDate = returnDate,
                    isReturned = false
                )

                Log.d("BorrowBookDialog", "Creating lending record: $lendingRecord")

                // Add the lending record to Firestore
                firestore.collection("bookLendings")
                    .document(lendingId)
                    .set(lendingRecord)
                    .await()

                Log.d("BorrowBookDialog", "Lending record created successfully")

                // Update the book in local database - only status
                val updatedBook = book.copy(status = "borrowed")
                database.bookDao().updateBook(updatedBook)

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