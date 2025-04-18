package com.example.libraryapp.data.local.daos

import androidx.room.*
import com.example.libraryapp.data.local.entities.Branch
import kotlinx.coroutines.flow.Flow

@Dao
interface BranchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: Branch): Long

    @Update
    suspend fun updateBranch(branch: Branch)

    @Delete
    suspend fun deleteBranch(branch: Branch)

    @Query("SELECT * FROM branches")
    fun getAllBranches(): Flow<List<Branch>>

    @Query("SELECT * FROM branches WHERE branchId = :branchId")
    suspend fun getBranchById(branchId: Long): Branch?

    @Query("SELECT * FROM branches WHERE name LIKE '%' || :query || '%'")
    fun searchBranches(query: String): Flow<List<Branch>>
} 