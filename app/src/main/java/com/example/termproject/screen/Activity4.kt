package com.example.termproject.screen

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.example.termproject.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class Activity4 : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBaseContent(R.layout.activity4_report, NAV_REPORT)

        val viewPager = findViewById<ViewPager2>(R.id.reportViewPager)
        val tabLayout = findViewById<TabLayout>(R.id.reportTabLayout)

        viewPager.adapter = ReportPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "요약"
                1 -> "그래프"
                else -> "스타일"
            }
        }.attach()
    }
}