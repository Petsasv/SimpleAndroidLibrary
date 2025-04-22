package com.example.libraryapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.databinding.ItemAvailableBookBinding

class AvailableBooksAdapter(
    private val onBorrowClick: (Book) -> Unit
) : ListAdapter<Book, AvailableBooksAdapter.AvailableBookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailableBookViewHolder {
        val binding = ItemAvailableBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AvailableBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvailableBookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AvailableBookViewHolder(
        private val binding: ItemAvailableBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.apply {
                tvTitle.text = book.title
                tvAuthor.text = book.author
                tvQuantity.text = "Quantity: ${book.quantity}"
                tvPrice.text = String.format("Price: %.2f €", book.price)
                tvStatus.text = "Available" // Always show as available since this adapter only shows available books

                btnBorrow.setOnClickListener {
                    onBorrowClick(book)
                }
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