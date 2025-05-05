package com.example.libraryapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.libraryapp.data.local.daos.BookDao
import com.example.libraryapp.data.local.daos.BranchBookDao
import com.example.libraryapp.data.local.daos.BranchDao
import com.example.libraryapp.data.local.entities.Book
import com.example.libraryapp.data.local.entities.Branch
import com.example.libraryapp.data.local.entities.BranchBook

@Database(
    entities = [
        Branch::class,
        Book::class,
        BranchBook::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun branchDao(): BranchDao
    abstract fun bookDao(): BookDao
    abstract fun branchBookDao(): BranchBookDao

    companion object {
        @Volatile
        private var INSTANCE: LibraryDatabase? = null

        fun getDatabase(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "library_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                // Initialize default data
                DefaultData.initializeDefaultData(instance)
                instance
            }
        }
    }
} 