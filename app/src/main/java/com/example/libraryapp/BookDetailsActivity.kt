package com.example.libraryapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.databinding.ActivityBookDetailsBinding

class BookDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBookDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Get the book from intent
        val book = intent.getParcelableExtra<Book>("book")
        if (book == null) {
            Toast.makeText(this, "Error: Book not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayBookDetails(book)
    }

    private fun displayBookDetails(book: Book) {
        binding.apply {
            tvTitle.text = book.title
            tvAuthor.text = book.author
            tvIsbn.text = book.isbn
            tvCategory.text = book.category
            tvQuantity.text = "Available: ${book.quantity}"
            tvPrice.text = String.format("Price: %.2f €", book.price)
            tvStatus.text = book.status
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 