package com.example.libraryapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.UserDetailsActivity
import com.example.libraryapp.adapters.UsersAdapter
import com.example.libraryapp.data.remote.models.User
import com.example.libraryapp.databinding.FragmentUsersBinding
import com.example.libraryapp.ui.dialogs.AddUserDialog
import com.example.libraryapp.ui.dialogs.ConfirmDeleteDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UsersFragment : Fragment() {
    private var _binding: FragmentUsersBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: UsersAdapter

    companion object {
        fun newInstance() = UsersFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = UsersAdapter(
            onUserClick = { user ->
                val intent = Intent(requireContext(), UserDetailsActivity::class.java)
                intent.putExtra("userId", user.userId)
                startActivity(intent)
            },
            onEditClick = { user ->
                showEditUserDialog(user)
            },
            onDeleteClick = { user ->
                showDeleteConfirmationDialog(user)
            }
        )
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UsersFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAddUser.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun showAddUserDialog() {
        AddUserDialog {
            loadUsers() // Reload users after adding a new one
        }.show(childFragmentManager, "AddUserDialog")
    }

    private fun showEditUserDialog(user: User) {
        AddUserDialog(
            user = user,
            onUserAdded = {
                loadUsers() // Reload users after editing
            }
        ).show(childFragmentManager, "EditUserDialog")
    }

    private fun showDeleteConfirmationDialog(user: User) {
        ConfirmDeleteDialog(
            userName = user.name,
            onDeleteConfirmed = {
                deleteUser(user)
            }
        ).show(childFragmentManager, "ConfirmDeleteDialog")
    }

    private fun deleteUser(user: User) {
        firestore.collection("users")
            .document(user.userId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "User deleted successfully", Toast.LENGTH_SHORT).show()
                loadUsers() // Reload users after deletion
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error deleting user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUsers() {
        firestore.collection("users")
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (_binding != null) {
                    val users = documents.mapNotNull { it.toObject(User::class.java) }
                    adapter.submitList(users)
                    binding.emptyState.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                }
            }
            .addOnFailureListener { e ->
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error loading users: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}