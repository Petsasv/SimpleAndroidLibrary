package com.example.libraryapp.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.libraryapp.R
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.data.local.LibraryDatabase
import com.example.libraryapp.databinding.DialogAddBookBinding
import kotlinx.coroutines.launch

class AddBookDialog : DialogFragment() {
    private var _binding: DialogAddBookBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: LibraryDatabase
    private var existingBook: Book? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LibraryDatabase.getDatabase(requireContext())
        existingBook = arguments?.getParcelable(ARG_BOOK)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        setupButtons()
        prefillFields()
        updateDialogTitle()
    }

    private fun updateDialogTitle() {
        if (existingBook != null) {
            dialog?.setTitle("Edit Book")
            binding.btnSave.text = "Update"
        } else {
            dialog?.setTitle("Add New Book")
            binding.btnSave.text = "Save"
        }
    }

    private fun prefillFields() {
        existingBook?.let { book ->
            with(binding) {
                etTitle.setText(book.title)
                etAuthor.setText(book.author)
                etIsbn.setText(book.isbn)
                actvCategory.setText(book.category, false)
                actvYear.setText(book.publicationYear.toString(), false)
                etDescription.setText(book.description)
            }
        }
    }

    private fun setupDropdowns() {
        // Setup category dropdown
        val categories = resources.getStringArray(R.array.book_categories)
        val categoryAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.actvCategory.setAdapter(categoryAdapter)

        // Setup year dropdown
        val years = resources.getStringArray(R.array.publication_years)
        val yearAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, years)
        binding.actvYear.setAdapter(yearAdapter)
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveBook()
            }
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        with(binding) {
            if (etTitle.text.isNullOrBlank()) {
                tilTitle.error = getString(R.string.error_title_required)
                isValid = false
            } else {
                tilTitle.error = null
            }

            if (etAuthor.text.isNullOrBlank()) {
                tilAuthor.error = getString(R.string.error_author_required)
                isValid = false
            } else {
                tilAuthor.error = null
            }

            if (etIsbn.text.isNullOrBlank()) {
                tilIsbn.error = getString(R.string.error_isbn_required)
                isValid = false
            } else {
                tilIsbn.error = null
            }

            if (actvCategory.text.isNullOrBlank()) {
                tilCategory.error = getString(R.string.error_category_required)
                isValid = false
            } else {
                tilCategory.error = null
            }

            if (actvYear.text.isNullOrBlank()) {
                tilYear.error = getString(R.string.error_year_required)
                isValid = false
            } else {
                tilYear.error = null
            }
        }

        return isValid
    }

    private fun saveBook() {
        val book = Book(
            bookId = existingBook?.bookId ?: 0,
            title = binding.etTitle.text.toString(),
            author = binding.etAuthor.text.toString(),
            isbn = binding.etIsbn.text.toString(),
            publicationYear = binding.actvYear.text.toString().toInt(),
            category = binding.actvCategory.text.toString(),
            description = binding.etDescription.text.toString()
        )

        lifecycleScope.launch {
            try {
                if (existingBook != null) {
                    database.bookDao().updateBook(book)
                    Toast.makeText(requireContext(), "Book updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    database.bookDao().insertBook(book)
                    Toast.makeText(requireContext(), R.string.book_added_success, Toast.LENGTH_SHORT).show()
                }
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_adding_book, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_BOOK = "book"

        fun newInstance(book: Book? = null) = AddBookDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_BOOK, book)
            }
        }
    }
} 