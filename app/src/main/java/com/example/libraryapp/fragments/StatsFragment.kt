package com.example.libraryapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.libraryapp.R
import com.example.libraryapp.adapters.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class StatsFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private var usersStatsFragment: UsersStatsFragment? = null
    private var booksStatsFragment: BooksStatsFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        // Create fragments for each tab
        usersStatsFragment = UsersStatsFragment()
        booksStatsFragment = BooksStatsFragment()
        val fragments = listOf(
            usersStatsFragment!!,
            booksStatsFragment!!
        )

        // Set up ViewPager with adapter
        viewPager.adapter = ViewPagerAdapter(requireActivity(), fragments)

        // Connect TabLayout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Users"
                1 -> "Books"
                else -> null
            }
        }.attach()

        // Load initial data
        refreshStats()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        refreshStats()
    }

    fun refreshStats() {
        if (isAdded) {  // Check if fragment is still attached to activity
            usersStatsFragment?.loadUserStatistics()
            booksStatsFragment?.loadBookStatistics()
        }
    }

    fun refreshBooksStats() {
        if (isAdded) {
            booksStatsFragment?.loadBookStatistics()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersStatsFragment = null
        booksStatsFragment = null
    }

    companion object {
        fun refreshStatsFromAnyFragment(fragment: Fragment) {
            // Find the StatsFragment in the fragment hierarchy
            var parent = fragment.parentFragment
            while (parent != null) {
                if (parent is StatsFragment) {
                    parent.refreshStats()
                    break
                }
                parent = parent.parentFragment
            }
        }
    }
}