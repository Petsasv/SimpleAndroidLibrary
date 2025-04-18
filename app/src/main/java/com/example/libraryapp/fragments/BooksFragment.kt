package com.example.libraryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.R
import com.example.libraryapp.adapters.BookAdapter
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.data.local.LibraryDatabase
import com.example.libraryapp.databinding.FragmentBooksBinding
import com.example.libraryapp.ui.dialogs.AddBookDialog
import com.example.libraryapp.ui.dialogs.BorrowBookDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BooksFragment : Fragment() {
    private var _binding: FragmentBooksBinding? = null
    private val binding get() = _binding!!
    private lateinit var bookAdapter: BookAdapter
    private lateinit var database: LibraryDatabase
    private var currentSortOrder = SortOrder.TITLE_ASC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LibraryDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupAddButton()
        setupFilterButton()
        loadBooks()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter(
            onEditClick = { book ->
                showAddBookDialog(book)
            },
            onDeleteClick = { book ->
                showDeleteConfirmation(book)
            }
        )

        binding.rvBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookAdapter
        }
    }

    private fun setupAddButton() {
        binding.fabAddBook.setOnClickListener {
            showAddBookDialog()
        }
    }

    private fun setupFilterButton() {
        // TODO: Add filter button functionality
    }

    private fun showAddBookDialog(book: Book? = null) {
        AddBookDialog.newInstance(book).show(childFragmentManager, "AddBookDialog")
    }

    private fun showSortOptionsDialog() {
        val sortOptions = arrayOf(
            "Title (A-Z)",
            "Title (Z-A)",
            "Author (A-Z)",
            "Author (Z-A)",
            "Category (A-Z)",
            "Category (Z-A)",
            "Publication Year (Newest)",
            "Publication Year (Oldest)"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Books")
            .setItems(sortOptions) { _, which ->
                currentSortOrder = when (which) {
                    0 -> SortOrder.TITLE_ASC
                    1 -> SortOrder.TITLE_DESC
                    2 -> SortOrder.AUTHOR_ASC
                    3 -> SortOrder.AUTHOR_DESC
                    4 -> SortOrder.CATEGORY_ASC
                    5 -> SortOrder.CATEGORY_DESC
                    6 -> SortOrder.DATE_DESC
                    7 -> SortOrder.DATE_ASC
                    else -> SortOrder.TITLE_ASC
                }
                loadBooks()
            }
            .show()
    }

    private fun loadBooks() {
        viewLifecycleOwner.lifecycleScope.launch {
            database.bookDao().getAllBooks().collectLatest { books ->
                val sortedBooks = when (currentSortOrder) {
                    SortOrder.TITLE_ASC -> books.sortedBy { it.title }
                    SortOrder.TITLE_DESC -> books.sortedByDescending { it.title }
                    SortOrder.AUTHOR_ASC -> books.sortedBy { it.author }
                    SortOrder.AUTHOR_DESC -> books.sortedByDescending { it.author }
                    SortOrder.CATEGORY_ASC -> books.sortedBy { it.category }
                    SortOrder.CATEGORY_DESC -> books.sortedByDescending { it.category }
                    SortOrder.DATE_DESC -> books.sortedByDescending { it.publicationYear }
                    SortOrder.DATE_ASC -> books.sortedBy { it.publicationYear }
                }
                bookAdapter.submitList(sortedBooks)
                binding.emptyState.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showDeleteConfirmation(book: Book) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Book")
            .setMessage("Are you sure you want to delete '${book.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.bookDao().deleteBook(book)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBorrowBookDialog(book: Book) {
        BorrowBookDialog(book) {
            // Refresh the book list when a book is borrowed
            loadBooks()
        }.show(childFragmentManager, "BorrowBookDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class SortOrder {
        TITLE_ASC, TITLE_DESC,
        AUTHOR_ASC, AUTHOR_DESC,
        CATEGORY_ASC, CATEGORY_DESC,
        DATE_ASC, DATE_DESC
    }
}