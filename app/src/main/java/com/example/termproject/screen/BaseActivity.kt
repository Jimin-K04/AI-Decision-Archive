package com.example.termproject.screen

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.termproject.R

open class BaseActivity : AppCompatActivity() {

        companion object {
                const val NAV_RECORD = "record"
                const val NAV_TIME_CAPSULE = "time_capsule"
                const val NAV_REPORT = "report"
        }

        protected fun setBaseContent(layoutResId: Int, selectedNav: String) {
                setContentView(R.layout.base_activity)

                val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)
                layoutInflater.inflate(layoutResId, contentFrame, true)

                setupBottomNav(selectedNav)
        }

        private fun setupBottomNav(selectedNav: String) {
                val navRecord = findViewById<LinearLayout>(R.id.navRecord)
                val navTimeCapsule = findViewById<LinearLayout>(R.id.navTimeCapsule)
                val navReport = findViewById<LinearLayout>(R.id.navReport)

                navRecord.setOnClickListener {
                        if (this !is HomeActivity) {
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                                overridePendingTransition(0, 0)
                        }
                }

                navTimeCapsule.setOnClickListener {
                        if (this !is Activity3) {
                                startActivity(Intent(this, Activity3::class.java))
                                finish()
                                overridePendingTransition(0, 0)
                        }
                }

                navReport.setOnClickListener {
                        if (this !is Activity4) {
                                startActivity(Intent(this, Activity4::class.java))
                                finish()
                                overridePendingTransition(0, 0)
                        }
                }

                updateNavColor(selectedNav)
        }

        private fun updateNavColor(selectedNav: String) {
                val orange = Color.parseColor("#E05A3C")
                val brown = Color.parseColor("#7E6A5D")

                val navRecordIcon = findViewById<ImageView>(R.id.navRecordIcon)
                val navRecordText = findViewById<TextView>(R.id.navRecordText)

                val navTimeCapsuleIcon = findViewById<ImageView>(R.id.navTimeCapsuleIcon)
                val navTimeCapsuleText = findViewById<TextView>(R.id.navTimeCapsuleText)

                val navReportIcon = findViewById<ImageView>(R.id.navReportIcon)
                val navReportText = findViewById<TextView>(R.id.navReportText)

                navRecordIcon.setColorFilter(if (selectedNav == NAV_RECORD) orange else brown)
                navRecordText.setTextColor(if (selectedNav == NAV_RECORD) orange else brown)

                navTimeCapsuleIcon.setColorFilter(if (selectedNav == NAV_TIME_CAPSULE) orange else brown)
                navTimeCapsuleText.setTextColor(if (selectedNav == NAV_TIME_CAPSULE) orange else brown)

                navReportIcon.setColorFilter(if (selectedNav == NAV_REPORT) orange else brown)
                navReportText.setTextColor(if (selectedNav == NAV_REPORT) orange else brown)

                navRecordText.setTypeface(
                        null,
                        if (selectedNav == NAV_RECORD) android.graphics.Typeface.BOLD
                        else android.graphics.Typeface.NORMAL
                )

                navTimeCapsuleText.setTypeface(
                        null,
                        if (selectedNav == NAV_TIME_CAPSULE) android.graphics.Typeface.BOLD
                        else android.graphics.Typeface.NORMAL
                )

                navReportText.setTypeface(
                        null,
                        if (selectedNav == NAV_REPORT) android.graphics.Typeface.BOLD
                        else android.graphics.Typeface.NORMAL
                )
        }
}