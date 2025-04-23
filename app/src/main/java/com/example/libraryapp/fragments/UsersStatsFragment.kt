package com.example.libraryapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.libraryapp.R
import com.example.libraryapp.databinding.FragmentUsersStatsBinding
import com.example.libraryapp.databinding.ItemTopUserBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class UsersStatsFragment : Fragment() {
    private var _binding: FragmentUsersStatsBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private var isDataLoaded = false
    private val TAG = "UsersStatsFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInfoButton()
        if (!isDataLoaded) {
            loadUserStatistics()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isDataLoaded) {
            loadUserStatistics()
        }
    }

    private fun setupInfoButton() {
        binding.ivInfo.setOnClickListener {
            showUserInfoDialog()
        }
    }

    private fun showUserInfoDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val thirtyDaysAgo = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000)
                
                // Get all users from the users collection
                val allUsers = firestore.collection("users")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.getString("name") }
                    .toSet()
                
                Log.d(TAG, "Total users found: ${allUsers.size}")
                Log.d(TAG, "All user names: $allUsers")
                
                // Get all lending records from the last 30 days
                val recentLendings = firestore.collection("bookLendings")
                    .get()
                    .await()
                    .documents
                    .filter { doc ->
                        val borrowDate = doc.getTimestamp("borrowDate")?.toDate()
                        val returnDate = doc.getTimestamp("returnDate")?.toDate()
                        val isReturned = doc.getBoolean("isReturned") ?: false
                        
                        // Include if:
                        // 1. Borrowed in last 30 days
                        // 2. Returned in last 30 days
                        // 3. Currently borrowed (not returned)
                        (borrowDate != null && borrowDate.after(thirtyDaysAgo)) ||
                        (isReturned && returnDate != null && returnDate.after(thirtyDaysAgo)) ||
                        (!isReturned && borrowDate != null)
                    }
                
                // Get active users (those with activity in last 30 days)
                val activeUsers = recentLendings
                    .mapNotNull { it.getString("userName") }
                    .toSet()
                
                Log.d(TAG, "Active users found: ${activeUsers.size}")
                Log.d(TAG, "Active user names: $activeUsers")
                
                // Inactive users are those who haven't had any activity in the last 30 days
                val inactiveUsers = allUsers - activeUsers
                
                Log.d(TAG, "Inactive users found: ${inactiveUsers.size}")
                Log.d(TAG, "Inactive user names: $inactiveUsers")
                
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("User Activity (Last 30 Days)")
                    .setMessage("""
                        Active Users: ${activeUsers.size}
                        Inactive Users: ${inactiveUsers.size}
                        
                        Note: Active users are those who have:
                        - Borrowed a book in the last 30 days
                        - Returned a book in the last 30 days
                        - Currently have a book borrowed
                    """.trimIndent())
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user activity", e)
                Toast.makeText(requireContext(), "Error loading user activity: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadUserStatistics() {
        if (!isAdded) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load total users
                val totalUsers = firestore.collection("users").get().await().size()
                binding.tvTotalUsers.text = totalUsers.toString()

                // Load top users (including both current and past borrowings)
                val bookLendings = firestore.collection("bookLendings")
                    .get()
                    .await()
                    .documents

                val topUsers = bookLendings
                    .groupBy { it.getString("userName") ?: "" }
                    .filter { it.key.isNotEmpty() }
                    .map { it.key to it.value.size }
                    .sortedByDescending { it.second }
                    .take(3)  // Show top 3 users instead of 2

                binding.llTopUsers.removeAllViews()
                if (topUsers.isEmpty()) {
                    val itemBinding = ItemTopUserBinding.inflate(layoutInflater)
                    itemBinding.tvUserName.text = "No borrowing history"
                    itemBinding.tvBorrowCount.text = "0"
                    binding.llTopUsers.addView(itemBinding.root)
                } else {
                    topUsers.forEach { (userName, count) ->
                        val itemBinding = ItemTopUserBinding.inflate(layoutInflater)
                        itemBinding.tvUserName.text = userName
                        itemBinding.tvBorrowCount.text = count.toString()
                        binding.llTopUsers.addView(itemBinding.root)
                    }
                }

                // Load user type distribution
                val userTypes = firestore.collection("users")
                    .get()
                    .await()
                    .documents
                    .groupBy { it.getString("userType") ?: "Unknown" }
                    .map { it.key to it.value.size }
                    .sortedBy { it.first }

                binding.llUserTypes.removeAllViews()
                userTypes.forEach { (type, count) ->
                    val view = layoutInflater.inflate(R.layout.item_top_user, binding.llUserTypes, false)
                    view.findViewById<TextView>(R.id.tvUserName).text = type
                    view.findViewById<TextView>(R.id.tvBorrowCount).text = count.toString()
                    binding.llUserTypes.addView(view)
                }

                isDataLoaded = true
            } catch (e: Exception) {
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