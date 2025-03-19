package com.example.fragments_test

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    private lateinit var frameLayout: FrameLayout
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        frameLayout = findViewById(R.id.framelayout)
        tabLayout = findViewById(R.id.tablayout)

        // Set up tabs if they don't already exist
        if (tabLayout.tabCount == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("First"))
            tabLayout.addTab(tabLayout.newTab().setText("Second"))
            tabLayout.addTab(tabLayout.newTab().setText("Third"))
        }

        // Only set the initial fragment if this is the first creation
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.framelayout, FirstFragment())
                .commit()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                var fragment: Fragment? = null
                when (tab?.position) {
                    0 -> fragment = FirstFragment()
                    1 -> fragment = SecondFragment()
                    2 -> fragment = ThirdFragment()
                }

                fragment?.let {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.framelayout, it)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not used, but must be implemented
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not used, but must be implemented
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}