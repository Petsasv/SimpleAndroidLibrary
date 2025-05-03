package com.example.libraryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.adapters.BorrowedBooksAdapter
import com.example.libraryapp.data.local.LibraryDatabase
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.data.repositories.FirebaseRepository
import com.example.libraryapp.databinding.FragmentBorrowedBooksBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import java.util.*

class BorrowedBooksFragment : Fragment() {
    private var _binding: FragmentBorrowedBooksBinding? = null
    private val binding get() = _binding!!
    private lateinit var borrowedBooksAdapter: BorrowedBooksAdapter
    private lateinit var database: LibraryDatabase
    private val firebaseRepository = FirebaseRepository()
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LibraryDatabase.getDatabase(requireContext())
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBorrowedBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        borrowedBooksAdapter = BorrowedBooksAdapter(
            onReturnClick = { book ->
                returnBook(book)
            }
        )

        binding.rvBorrowedBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = borrowedBooksAdapter
        }

        // Observe borrowed books
        viewLifecycleOwner.lifecycleScope.launch {
            database.bookDao().getBorrowedBooks().collectLatest { books ->
                borrowedBooksAdapter.submitList(books)
                binding.emptyState.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun returnBook(book: Book) {
        // Capture fragment reference before coroutine
        val fragment = this
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Update book status in local database
                val updatedBook = book.copy(status = "available")
                database.bookDao().updateBook(updatedBook)

                // Find the active lending record in Firestore
                val lendingSnapshot = firestore.collection("bookLendings")
                    .whereEqualTo("bookId", book.bookId)
                    .whereEqualTo("isReturned", false)  // Only get non-returned records
                    .get()
                    .await()

                if (!lendingSnapshot.isEmpty) {
                    val lendingDoc = lendingSnapshot.documents.first()
                    val lendingId = lendingDoc.id
                    val userName = lendingDoc.getString("userName") ?: "Unknown User"

                    // Use the repository to return the book
                    val result = firebaseRepository.returnBook(
                        bookId = book.bookId,
                        userName = userName
                    )

                    if (result) {
                        Toast.makeText(requireContext(), "Book returned successfully", Toast.LENGTH_SHORT).show()
                        
                        // Refresh stats using the companion object method
                        StatsFragment.refreshStatsFromAnyFragment(this@BorrowedBooksFragment)
                    } else {
                        Toast.makeText(requireContext(), "Error returning book", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No active lending record found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error returning book: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 