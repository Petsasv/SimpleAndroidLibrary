package com.example.libraryapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.libraryapp.R
import com.example.libraryapp.data.local.LibraryDatabase
import com.example.libraryapp.databinding.FragmentBooksStatsBinding
import com.example.libraryapp.databinding.ItemTopBookBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.TimeUnit

class BooksStatsFragment : Fragment() {
    private var _binding: FragmentBooksStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: LibraryDatabase
    private val firestore = FirebaseFirestore.getInstance()
    private var isDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LibraryDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBooksStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isDataLoaded) {
            loadBookStatistics()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDataLoaded) {
            loadBookStatistics()
        }
    }

    fun loadBookStatistics() {
        if (!isAdded) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load overview statistics
                val allBooks = database.bookDao().getAllBooks().first()
                val availableBooks = allBooks.count { it.status == "available" }
                val borrowedBooks = allBooks.count { it.status == "borrowed" }

                binding.tvTotalBooks.text = allBooks.size.toString()
                binding.tvAvailableBooks.text = availableBooks.toString()
                binding.tvBorrowedBooks.text = borrowedBooks.toString()

                // Load top borrowed books (including both current and past borrowings)
                val bookLendings = firestore.collection("bookLendings")
                    .get()
                    .await()
                    .documents

                val topBooks = bookLendings
                    .groupBy { it.getLong("bookId") ?: 0L }
                    .map { (bookId, lendings) ->
                        val book = database.bookDao().getBookById(bookId)
                        Triple(bookId, book?.title ?: "Unknown Book", lendings.size)
                    }
                    .sortedByDescending { it.third }
                    .take(3)  // Show top 3 books instead of 2

                binding.llTopBooks.removeAllViews()
                if (topBooks.isEmpty()) {
                    val itemBinding = ItemTopBookBinding.inflate(layoutInflater)
                    itemBinding.tvBookTitle.text = "No borrowing history"
                    itemBinding.tvBorrowCount.text = "0"
                    binding.llTopBooks.addView(itemBinding.root)
                } else {
                    topBooks.forEach { (_, title, count) ->
                        val itemBinding = ItemTopBookBinding.inflate(layoutInflater)
                        itemBinding.tvBookTitle.text = title
                        itemBinding.tvBorrowCount.text = count.toString()
                        binding.llTopBooks.addView(itemBinding.root)
                    }
                }

                // Calculate total value of currently borrowed books
                val allBorrowings = firestore.collection("bookLendings")
                    .get()
                    .await()
                    .documents

                Log.d("BooksStats", "Found ${allBorrowings.size} total borrowings")
                
                // Filter current borrowings
                val currentBorrowings = allBorrowings.filter { doc ->
                    val isReturned = doc.getBoolean("isReturned") ?: false
                    Log.d("BooksStats", "Borrowing: bookId=${doc.getLong("bookId")}, isReturned=$isReturned")
                    !isReturned
                }

                Log.d("BooksStats", "Filtered to ${currentBorrowings.size} current borrowings")

                val totalValue = if (currentBorrowings.isEmpty()) {
                    0.0
                } else {
                    currentBorrowings.sumOf { lending ->
                        val bookId = lending.getLong("bookId") ?: 0L
                        val book = database.bookDao().getBookById(bookId)
                        if (book != null) {
                            val borrowDate = lending.getTimestamp("borrowDate")?.toDate()
                            val returnDate = lending.getTimestamp("returnDate")?.toDate()
                            
                            if (borrowDate != null && returnDate != null) {
                                val days = TimeUnit.MILLISECONDS.toDays(returnDate.time - borrowDate.time)
                                Log.d("BooksStats", "Book found: id=$bookId, title=${book.title}, price=${book.price}, days=$days")
                                book.price * days
                            } else {
                                Log.d("BooksStats", "Book found but dates are null: id=$bookId, title=${book.title}, price=${book.price}")
                                book.price
                            }
                        } else {
                            Log.d("BooksStats", "Book not found for id: $bookId")
                            0.0
                        }
                    }
                }

                Log.d("BooksStats", "Total value calculated: $totalValue")
                binding.tvTotalValue.text = String.format("%.2f €", totalValue)
                isDataLoaded = true
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading statistics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isDataLoaded = false
    }
} 