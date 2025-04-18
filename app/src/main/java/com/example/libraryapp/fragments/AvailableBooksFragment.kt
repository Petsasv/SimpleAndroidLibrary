package com.example.libraryapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.R
import com.example.libraryapp.adapters.AvailableBooksAdapter
import com.example.libraryapp.data.local.LibraryDatabase
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.databinding.FragmentAvailableBooksBinding
import com.example.libraryapp.ui.dialogs.BorrowBookDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AvailableBooksFragment : Fragment() {
    private var _binding: FragmentAvailableBooksBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AvailableBooksAdapter
    private lateinit var database: LibraryDatabase
    private val TAG = "AvailableBooksFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LibraryDatabase.getDatabase(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvailableBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadBooks()
    }

    private fun setupRecyclerView() {
        adapter = AvailableBooksAdapter { book ->
            showBorrowDialog(book)
        }
        binding.recyclerViewAvailableBooks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AvailableBooksFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun showBorrowDialog(book: Book) {
        BorrowBookDialog(book) {
            // Refresh the list when a book is borrowed
            loadBooks()
        }.show(childFragmentManager, "borrow_dialog")
    }

    private fun loadBooks() {
        Log.d(TAG, "Loading books from Room...")
        viewLifecycleOwner.lifecycleScope.launch {
            database.bookDao().getAvailableBooks().collectLatest { books ->
                Log.d(TAG, "Loaded ${books.size} books")
                adapter.submitList(books)
                binding.emptyState.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 