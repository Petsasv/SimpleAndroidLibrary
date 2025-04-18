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
import com.example.libraryapp.R
import com.example.libraryapp.data.remote.models.User
import com.example.libraryapp.databinding.DialogAddUserBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class AddUserDialog(
    private val user: User? = null,
    private val onUserAdded: () -> Unit
) : DialogFragment() {

    private var _binding: DialogAddUserBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUserTypeDropdown()
        setupDepartmentDropdown()
        setupButtons()
        if (user != null) {
            populateFields(user)
        }
    }

    private fun setupUserTypeDropdown() {
        val userTypes = resources.getStringArray(R.array.user_types)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, userTypes)
        binding.actvUserType.setAdapter(adapter)
    }

    private fun setupDepartmentDropdown() {
        val departments = resources.getStringArray(R.array.departments)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, departments)
        binding.actvDepartment.setAdapter(adapter)
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                if (user != null) {
                    updateUser()
                } else {
                    saveUser()
                }
            }
        }
    }

    private fun populateFields(user: User) {
        binding.apply {
            etName.setText(user.name)
            etEmail.setText(user.email)
            actvUserType.setText(user.userType, false)
            actvDepartment.setText(user.department, false)
            etContactNumber.setText(user.contactNumber)
        }
    }

    private fun validateInput(): Boolean {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val userType = binding.actvUserType.text.toString().trim()
        val department = binding.actvDepartment.text.toString().trim()
        val contactNumber = binding.etContactNumber.text.toString().trim()

        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.error_name_required)
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = getString(R.string.error_email_required)
            return false
        }

        if (userType.isEmpty()) {
            binding.tilUserType.error = getString(R.string.error_user_type_required)
            return false
        }

        if (department.isEmpty()) {
            binding.tilDepartment.error = getString(R.string.error_department_required)
            return false
        }

        if (contactNumber.isEmpty()) {
            binding.etContactNumber.error = getString(R.string.error_contact_number_required)
            return false
        }

        return true
    }

    private fun saveUser() {
        val user = User(
            userId = UUID.randomUUID().toString(),
            name = binding.etName.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            userType = binding.actvUserType.text.toString().trim(),
            department = binding.actvDepartment.text.toString().trim(),
            contactNumber = binding.etContactNumber.text.toString().trim()
        )

        firestore.collection("users")
            .document(user.userId)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "User added successfully", Toast.LENGTH_SHORT).show()
                onUserAdded()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUser() {
        val updatedUser = user!!.copy(
            name = binding.etName.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            userType = binding.actvUserType.text.toString().trim(),
            department = binding.actvDepartment.text.toString().trim(),
            contactNumber = binding.etContactNumber.text.toString().trim()
        )

        firestore.collection("users")
            .document(updatedUser.userId)
            .set(updatedUser)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "User updated successfully", Toast.LENGTH_SHORT).show()
                onUserAdded()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 