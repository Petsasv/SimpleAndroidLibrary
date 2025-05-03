package com.example.libraryapp

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.libraryapp.data.remote.models.User
import com.example.libraryapp.databinding.ActivityUserDetailsBinding
import com.google.firebase.firestore.FirebaseFirestore

class UserDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserDetailsBinding
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Get user ID from intent
        val userId = intent.getStringExtra("userId")
        if (userId == null) {
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserData(userId)
    }

    private fun loadUserData(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        displayUserDetails(user)
                    } else {
                        Toast.makeText(this, "Error: Could not load user data", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading user: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayUserDetails(user: User) {
        binding.apply {
            tvName.text = user.name
            tvEmail.text = user.email
            tvPhone.text = user.contactNumber
            tvRole.text = user.userType
            tvDepartment.text = user.department
            tvStatus.text = "Active" // Default status since we removed the status check
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