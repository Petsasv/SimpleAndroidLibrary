package com.example.libraryapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.libraryapp.utils.AppSettings
import com.example.libraryapp.utils.LocaleHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import android.widget.ImageView
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.view.Menu
import android.widget.RadioGroup
import android.widget.Button
import android.widget.TextView
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import android.widget.Toast

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private var _instance: MainActivity? = null
        val instance: MainActivity
            get() = _instance ?: throw IllegalStateException("MainActivity not initialized")
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var imgSettings: ImageView
    private lateinit var appSettings: AppSettings

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _instance = this
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        // Create notification channel
        createNotificationChannel()

        // Initialize AppSettings
        appSettings = AppSettings.getInstance(this)
        
        // Apply saved theme and language
        AppCompatDelegate.setDefaultNightMode(appSettings.getThemeMode())
        LocaleHelper.setLocale(this, appSettings.getLanguage())

        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        bottomNav = findViewById(R.id.bottom_nav)
        imgSettings = findViewById(R.id.imgSettings)

        // Set up the toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)  // Disable the default navigation icon
        supportActionBar?.setHomeButtonEnabled(false)      // Disable the home button

        // Initialize navigation components
        initializeNavigation()

        // Set up settings icon click listener
        imgSettings.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Update menu item subtitles
        updateMenuSubtitles()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "library_app_channel",
                "Library Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for library activities"
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted")
            } else {
                Log.d("MainActivity", "Notification permission denied")
            }
        }
    }

    private fun initializeNavigation() {
        // Get NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Configure app bar without drawer layout to prevent hamburger icon
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_books,
                R.id.navigation_users,
                R.id.navigation_stats
            )
        )

        // Set up navigation components
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNav.setupWithNavController(navController)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_theme -> {
                toggleTheme()
                return true
            }
            R.id.nav_language -> {
                showLanguageSelectionDialog()
                return true
            }
            R.id.nav_notifications -> {
                toggleNotifications()
                return true
            }
            else -> return false
        }
    }

    private fun toggleNotifications() {
        val currentState = appSettings.areNotificationsEnabled()
        appSettings.setNotificationsEnabled(!currentState)
        updateMenuSubtitles()
        
        // Show feedback to user
        val message = if (!currentState) {
            "Notifications enabled"
        } else {
            "Notifications disabled"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateMenuSubtitles() {
        val themeItem = navView.menu.findItem(R.id.nav_theme)
        val languageItem = navView.menu.findItem(R.id.nav_language)
        val notificationsItem = navView.menu.findItem(R.id.nav_notifications)

        // Update theme title with subtitle and chevron
        val themeView = themeItem.actionView as? TextView
        themeView?.apply {
            text = when (AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_YES -> "Dark"
                AppCompatDelegate.MODE_NIGHT_NO -> "Light"
                else -> "Light"
            }
            gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0)
            compoundDrawablePadding = 8
            setPadding(0, 0, 16, 0)
        }

        // Update language title with subtitle and chevron
        val languageView = languageItem.actionView as? TextView
        languageView?.apply {
            text = when (appSettings.getLanguage()) {
                "en" -> "English"
                "el" -> "Greek"
                else -> "English"
            }
            gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0)
            compoundDrawablePadding = 8
            setPadding(0, 0, 16, 0)
        }

        // Update notifications title with subtitle and chevron
        val notificationsView = notificationsItem.actionView as? TextView
        notificationsView?.apply {
            text = if (appSettings.areNotificationsEnabled()) "On" else "Off"
            gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0)
            compoundDrawablePadding = 8
            setPadding(0, 0, 16, 0)
        }
    }

    private fun toggleTheme() {
        val newMode = if (isDarkTheme()) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(newMode)
        appSettings.setThemeMode(newMode)
        updateMenuSubtitles()
    }

    private fun showLanguageSelectionDialog() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_language, null)
        dialog.setContentView(view)

        val radioGroup = view.findViewById<RadioGroup>(R.id.languageRadioGroup)
        val btnApply = view.findViewById<Button>(R.id.btnApplyLanguage)

        // Set initial selection
        when (appSettings.getLanguage()) {
            "el" -> radioGroup.check(R.id.radioGreek)
            else -> radioGroup.check(R.id.radioEnglish)
        }

        btnApply.setOnClickListener {
            val selectedLanguage = when (radioGroup.checkedRadioButtonId) {
                R.id.radioEnglish -> "en"
                R.id.radioGreek -> "el"
                else -> "en"
            }
            appSettings.setLanguage(selectedLanguage)
            LocaleHelper.setLocale(this, selectedLanguage)
            dialog.dismiss()
            updateMenuSubtitles()
            
            // Recreate the activity to apply language changes to all UI elements
            val intent = intent
            finish()
            startActivity(intent)
        }

        dialog.show()
    }

    private fun isDarkTheme(): Boolean {
        return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Override onCreateOptionsMenu to prevent automatic menu inflation
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true  // Return true but don't inflate any menu
    }

    override fun onResume() {
        super.onResume()
        updateMenuSubtitles()
    }

    override fun onPause() {
        super.onPause()
        // Settings are automatically saved when changed
    }

    override fun onStop() {
        super.onStop()
        // Clean up resources that are not needed when app is in background
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_instance === this) {
            _instance = null
        }
    }
}
