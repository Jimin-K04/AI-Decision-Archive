package com.example.termproject.screen

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.termproject.R
import com.google.android.material.card.MaterialCardView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeActivity : BaseActivity() {
    private val db = FirebaseFirestore.getInstance()

    private lateinit var totalDecisionText: TextView
    private lateinit var todayDecisionText: TextView
    private lateinit var averageSatisfactionText: TextView
    private lateinit var recentDecisionContainer: LinearLayout
    private lateinit var viewAllRecentText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBaseContent(R.layout.activity_home, NAV_RECORD)

        totalDecisionText = findViewById(R.id.totalDecisionText)
        todayDecisionText = findViewById(R.id.todayDecisionText)
        averageSatisfactionText = findViewById(R.id.averageSatisfactionText)
        recentDecisionContainer = findViewById(R.id.recentDecisionContainer)
        viewAllRecentText = findViewById(R.id.viewAllRecentText)

        findViewById<View>(R.id.newRecordButton).setOnClickListener {
            startActivity(Intent(this, Activity1::class.java))
        }

        viewAllRecentText.setOnClickListener {
            startActivity(Intent(this, Activity3::class.java))
        }

        loadHomeData()
    }

    private fun loadHomeData() {
        db.collection("records")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    Toast.makeText(this, "홈 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val docs = snapshot.documents

                totalDecisionText.text = "${docs.size}개"

                val todayCount = docs.count { doc ->
                    val createdAt = doc.getTimestamp("createdAt")
                    createdAt != null && isToday(createdAt)
                }

                todayDecisionText.text = "${todayCount}개"
                showRecentDecisionCards(docs.take(3))
            }
        ReportRepository.loadRecords(
            onSuccess = { records ->
                val monthlyRecords = ReportAnalyzer.getThisMonthRecords(records)
                val report = ReportAnalyzer.analyze(monthlyRecords)

                averageSatisfactionText.text =
                    if (monthlyRecords.isNotEmpty()) {
                        String.format("%.1f점", report.avgSatisfaction)
                    } else {
                        "-"
                    }
            },
            onFailure = {
                averageSatisfactionText.text = "-"
            }
        )
    }

    private fun showRecentDecisionCards(docs: List<DocumentSnapshot>) {
        recentDecisionContainer.removeAllViews()

        if (docs.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "아직 기록이 없어요.\n새 기록 버튼으로 시작해보세요!"
                textSize = 18f
                setTextColor(Color.parseColor("#6F5E55"))
                gravity = Gravity.CENTER
            }

            recentDecisionContainer.addView(
                emptyText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            return
        }

        for (doc in docs) {
            val title =
                doc.getString("title")
                    ?: doc.getString("content")
                    ?: doc.getString("decision")
                    ?: doc.getString("memo")
                    ?: doc.getString("category")
                    ?: "결정 기록"

            val category = doc.getString("category") ?: "카테고리 없음"
            val weather = doc.getString("weather") ?: "날씨 정보 없음"
            val createdAt = doc.getTimestamp("createdAt")

            val dateText = if (createdAt != null) {
                SimpleDateFormat("MM.dd HH:mm", Locale.KOREA).format(createdAt.toDate())
            } else {
                ""
            }

            val satisfaction =
                getNumber(doc, "reviewSatisfaction")
                    ?: getNumber(doc, "satisfaction")
                    ?: getNumber(doc, "satisfactionScore")

            val card = MaterialCardView(this).apply {
                radius = dp(18).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(Color.WHITE)
                strokeColor = Color.parseColor("#E8D8CC")
                strokeWidth = dp(1)
                useCompatPadding = true
            }

            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(14), dp(18), dp(14))
            }

            val titleText = TextView(this).apply {
                text = title
                textSize = 17f
                setTextColor(Color.parseColor("#1F120D"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val subText = TextView(this).apply {
                text = if (satisfaction != null) {
                    "$category · 만족도 ${String.format("%.1f", satisfaction)}점 · $dateText"
                } else {
                    "$category · $dateText"
                }
                textSize = 14f
                setTextColor(Color.parseColor("#6F5E55"))
                setPadding(0, dp(6), 0, 0)
            }

            val weatherText = TextView(this).apply {
                text = weather
                textSize = 13f
                setTextColor(Color.parseColor("#8A766B"))
                setPadding(0, dp(4), 0, 0)
            }

            box.addView(titleText)
            box.addView(subText)
            box.addView(weatherText)
            card.addView(box)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }

            recentDecisionContainer.addView(card, params)
        }
    }

    private fun getNumber(doc: DocumentSnapshot, field: String): Double? {
        return when (val value = doc.get(field)) {
            is Long -> value.toDouble()
            is Int -> value.toDouble()
            is Double -> value
            is Float -> value.toDouble()
            else -> null
        }
    }

    private fun isToday(timestamp: Timestamp): Boolean {
        val target = Calendar.getInstance().apply {
            time = timestamp.toDate()
        }

        val today = Calendar.getInstance()

        return target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}