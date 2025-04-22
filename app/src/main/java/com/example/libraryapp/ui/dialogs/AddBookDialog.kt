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
import com.example.libraryapp.data.local.entities.BranchBook
import com.example.libraryapp.data.local.LibraryDatabase
import com.example.libraryapp.databinding.DialogAddBookBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

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
            lifecycleScope.launch {
                binding.etTitle.setText(book.title)
                binding.etAuthor.setText(book.author)
                binding.etIsbn.setText(book.isbn)
                binding.actvCategory.setText(book.category, false)
                binding.etQuantity.setText(book.quantity.toString())
                binding.etPrice.setText(String.format("%.2f", book.price))

                // Get the branch for this book
                val branchBook = database.branchBookDao().getBranchBookByBookId(book.bookId)
                branchBook?.let { bb ->
                    val branch = database.branchDao().getBranchById(bb.branchId)
                    branch?.let { b ->
                        binding.actvBranch.setText(b.name, false)
                    }
                }
            }
        }
    }

    private fun setupDropdowns() {
        // Setup category dropdown
        val categories = resources.getStringArray(R.array.book_categories)
        val categoryAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.actvCategory.setAdapter(categoryAdapter)

        // Setup branch dropdown
        lifecycleScope.launch {
            val branches = database.branchDao().getAllBranches().first()
            val branchNames = branches.map { it.name }
            val branchAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, branchNames)
            binding.actvBranch.setAdapter(branchAdapter)
        }
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

            if (actvBranch.text.isNullOrBlank()) {
                tilBranch.error = getString(R.string.error_branch_required)
                isValid = false
            } else {
                tilBranch.error = null
            }

            if (actvCategory.text.isNullOrBlank()) {
                tilCategory.error = getString(R.string.error_category_required)
                isValid = false
            } else {
                tilCategory.error = null
            }

            if (etQuantity.text.isNullOrBlank()) {
                tilQuantity.error = getString(R.string.error_quantity_required)
                isValid = false
            } else {
                tilQuantity.error = null
            }

            if (etPrice.text.isNullOrBlank()) {
                tilPrice.error = getString(R.string.error_price_required)
                isValid = false
            } else {
                tilPrice.error = null
            }
        }

        return isValid
    }

    private fun saveBook() {
        val title = binding.etTitle.text.toString().trim()
        val author = binding.etAuthor.text.toString().trim()
        val isbn = binding.etIsbn.text.toString().trim()
        val branchName = binding.actvBranch.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()
        val quantity = binding.etQuantity.text.toString().trim()
        val price = binding.etPrice.text.toString().trim()

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty() || 
            branchName.isEmpty() || category.isEmpty() || quantity.isEmpty() || price.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_all_fields_required), Toast.LENGTH_SHORT).show()
            return
        }

        val quantityInt = quantity.toIntOrNull()
        val priceDouble = price.toDoubleOrNull()

        if (quantityInt == null || quantityInt < 1) {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_quantity), Toast.LENGTH_SHORT).show()
            return
        }

        if (priceDouble == null || priceDouble < 0) {
            Toast.makeText(requireContext(), getString(R.string.error_invalid_price), Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // First, get the branch ID
                val branch = database.branchDao().getAllBranches().first().find { it.name == branchName }
                if (branch == null) {
                    Toast.makeText(requireContext(), getString(R.string.error_branch_not_found), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val book = Book(
                    bookId = existingBook?.bookId ?: 0,
                    title = title,
                    author = author,
                    isbn = isbn,
                    category = category,
                    quantity = quantityInt,
                    price = priceDouble,
                    status = existingBook?.status ?: "available"
                )

                if (existingBook != null) {
                    database.bookDao().updateBook(book)
                    // Update branch book entry
                    val branchBook = database.branchBookDao().getBranchBook(branch.branchId, book.bookId)
                    if (branchBook != null) {
                        database.branchBookDao().updateBranchBook(
                            branchBook.copy(
                                quantity = quantityInt,
                                availableQuantity = quantityInt
                            )
                        )
                    }
                    Toast.makeText(requireContext(), getString(R.string.book_updated_successfully), Toast.LENGTH_SHORT).show()
                } else {
                    val bookId = database.bookDao().insertBook(book)
                    // Create branch book entry for selected branch only
                    database.branchBookDao().insertBranchBook(
                        BranchBook(
                            branchId = branch.branchId,
                            bookId = bookId,
                            quantity = quantityInt,
                            availableQuantity = quantityInt
                        )
                    )
                    Toast.makeText(requireContext(), getString(R.string.book_added_successfully), Toast.LENGTH_SHORT).show()
                }
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_adding_book), Toast.LENGTH_SHORT).show()
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