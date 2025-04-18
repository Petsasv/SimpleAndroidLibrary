package com.example.libraryapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = false
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun branchDao(): BranchDao
    abstract fun bookDao(): BookDao
    abstract fun branchBookDao(): BranchBookDao

    companion object {
        @Volatile
        private var INSTANCE: LibraryDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create a temporary table with the new schema
                database.execSQL("""
                    CREATE TABLE books_new (
                        bookId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        isbn TEXT NOT NULL,
                        publicationYear INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        status TEXT NOT NULL,
                        borrowerId TEXT
                    )
                """)

                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO books_new (bookId, title, author, isbn, publicationYear, category, description, status)
                    SELECT bookId, title, author, isbn, publicationYear, category, description, status
                    FROM books
                """)

                // Drop old table
                database.execSQL("DROP TABLE books")

                // Rename new table to original name
                database.execSQL("ALTER TABLE books_new RENAME TO books")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // First, create a backup of the current table
                database.execSQL("""
                    CREATE TABLE books_backup AS 
                    SELECT * FROM books
                """)

                // Create the new table without borrowerId
                database.execSQL("""
                    CREATE TABLE books_new (
                        bookId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        isbn TEXT NOT NULL,
                        publicationYear INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        status TEXT NOT NULL
                    )
                """)

                // Copy all data from backup to new table
                database.execSQL("""
                    INSERT INTO books_new (bookId, title, author, isbn, publicationYear, category, description, status)
                    SELECT bookId, title, author, isbn, publicationYear, category, description, status
                    FROM books_backup
                """)

                // Drop the old table
                database.execSQL("DROP TABLE books")

                // Rename new table to original name
                database.execSQL("ALTER TABLE books_new RENAME TO books")

                // Drop the backup table
                database.execSQL("DROP TABLE books_backup")
            }
        }

        fun getDatabase(context: Context): LibraryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "library_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                // Initialize default data
                DefaultData.initializeDefaultData(instance)
                instance
            }
        }
    }
} 