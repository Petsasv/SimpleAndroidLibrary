package com.example.libraryapp

import android.app.Application
import com.google.firebase.FirebaseApp

class LibraryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
} 