package com.example.libraryapp.data.local

import com.example.libraryapp.data.local.entities.Branch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object DefaultData {
    private val defaultBranches = listOf(
        Branch(
            name = "Central Library",
            location = "123 Main Street, City Center",
            contactInfo = "central@library.com"
        ),
        Branch(
            name = "North Branch",
            location = "456 North Avenue, North District",
            contactInfo = "north@library.com"
        ),
        Branch(
            name = "South Branch",
            location = "789 South Road, South District",
            contactInfo = "south@library.com"
        )
    )

    fun initializeDefaultData(database: LibraryDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingBranches = database.branchDao().getAllBranches()
            if (existingBranches.first().isEmpty()) {
                defaultBranches.forEach { branch ->
                    database.branchDao().insertBranch(branch)
                }
            }
        }
    }
} 