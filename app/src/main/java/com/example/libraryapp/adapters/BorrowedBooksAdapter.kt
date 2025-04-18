package com.example.libraryapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.databinding.ItemBorrowedBookBinding
import com.example.libraryapp.ui.dialogs.ConfirmReturnDialog

class BorrowedBooksAdapter(
    private val onReturnClick: (Book) -> Unit
) : ListAdapter<Book, BorrowedBooksAdapter.BorrowedBookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BorrowedBookViewHolder {
        val binding = ItemBorrowedBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BorrowedBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BorrowedBookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BorrowedBookViewHolder(
        private val binding: ItemBorrowedBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.apply {
                tvTitle.text = book.title
                tvAuthor.text = book.author
                tvStatus.text = "Borrowed"
                tvStatus.setTextColor(root.context.getColor(android.R.color.holo_red_dark))
                
                btnReturn.setOnClickListener {
                    showReturnConfirmationDialog(book)
                }
            }
        }

        private fun showReturnConfirmationDialog(book: Book) {
            val activity = binding.root.context as? FragmentActivity
            activity?.let {
                ConfirmReturnDialog {
                    onReturnClick(book)
                }.show(it.supportFragmentManager, "ConfirmReturnDialog")
            }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.bookId == newItem.bookId
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
} 