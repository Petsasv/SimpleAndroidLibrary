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
    private val TAG = "BooksStatsFragment"

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

                // Get current borrowings and calculate their values
                val allBorrowings = firestore.collection("bookLendings")
                    .get()
                    .await()
                    .documents

                Log.d(TAG, "Total borrowings found: ${allBorrowings.size}")

                val currentBorrowings = allBorrowings.filter { doc ->
                    val bookId = doc.getLong("bookId") ?: 0L
                    val isReturned = doc.getBoolean("isReturned") ?: false
                    val borrowDate = doc.getTimestamp("borrowDate")?.toDate()
                    val returnDate = doc.getTimestamp("returnDate")?.toDate()
                    val currentDate = Date()
                    
                    Log.d(TAG, "Checking book ID: $bookId")
                    Log.d(TAG, "isReturned: $isReturned")
                    Log.d(TAG, "borrowDate: $borrowDate")
                    Log.d(TAG, "returnDate: $returnDate")
                    
                    // A book is currently borrowed if:
                    // 1. It's not returned
                    // 2. The borrow date is in the past
                    // 3. The return date is in the future
                    val isCurrentlyBorrowed = !isReturned && 
                        borrowDate != null && 
                        returnDate != null && 
                        borrowDate.before(currentDate) && 
                        returnDate.after(currentDate)
                    
                    Log.d(TAG, "Is currently borrowed: $isCurrentlyBorrowed")
                    isCurrentlyBorrowed
                }

                Log.d(TAG, "Found ${currentBorrowings.size} current borrowings")

                // Clear previous views
                binding.llCurrentBorrowings.removeAllViews()

                if (currentBorrowings.isEmpty()) {
                    val itemBinding = ItemTopBookBinding.inflate(layoutInflater)
                    itemBinding.tvBookTitle.text = "No books currently borrowed"
                    itemBinding.tvBorrowCount.text = "0"
                    binding.llCurrentBorrowings.addView(itemBinding.root)
                } else {
                    currentBorrowings.forEach { lending ->
                        val bookId = lending.getLong("bookId") ?: 0L
                        val book = database.bookDao().getBookById(bookId)
                        val userName = lending.getString("userName") ?: "Unknown User"
                        val borrowDate = lending.getTimestamp("borrowDate")?.toDate()
                        val returnDate = lending.getTimestamp("returnDate")?.toDate()

                        if (book != null && borrowDate != null && returnDate != null) {
                            // Calculate days between borrow and return dates
                            // Add 1 to include both start and end days
                            val days = TimeUnit.MILLISECONDS.toDays(returnDate.time - borrowDate.time) + 1
                            // Simple price calculation: price per day × number of days
                            val totalPrice = book.price * days

                            val itemBinding = ItemTopBookBinding.inflate(layoutInflater)
                            itemBinding.tvBookTitle.text = "${book.title} (${userName})"
                            itemBinding.tvBorrowCount.text = String.format("%.2f €", totalPrice)
                            binding.llCurrentBorrowings.addView(itemBinding.root)

                            Log.d(TAG, "Displaying book: ${book.title}")
                            Log.d(TAG, "User: $userName")
                            Log.d(TAG, "Days: $days")
                            Log.d(TAG, "Price per day: ${book.price}")
                            Log.d(TAG, "Total: $totalPrice")
                        }
                    }
                }

                isDataLoaded = true
            } catch (e: Exception) {
                Log.e(TAG, "Error loading statistics", e)
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