package com.example.libraryapp.data.remote.models

data class User(
    val userId: String = "",  // Firebase UID
    val name: String = "",
    val email: String = "",
    val userType: String = "",  // "student", "professor", etc.
    val department: String = "",
    val contactNumber: String = ""
) 