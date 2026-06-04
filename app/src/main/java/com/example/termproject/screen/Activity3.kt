package com.example.termproject.screen

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.termproject.R
import com.example.termproject.timeCapsule.ReviewInput
import com.example.termproject.timeCapsule.TimeCapsuleAdapter
import com.example.termproject.timeCapsule.TimeCapsuleItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Activity3 : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var adapter: TimeCapsuleAdapter

    private val db = FirebaseFirestore.getInstance()

    private val COLLECTION_NAME = "records"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBaseContent(R.layout.activity3_main, NAV_TIME_CAPSULE)

        recyclerView = findViewById(R.id.timeCapsuleRecyclerView)
        emptyText = findViewById(R.id.emptyText)

        adapter = TimeCapsuleAdapter(
            onSaveReview = { item, input ->
                saveReview(item, input)
            },
            onAiAnalyze = { item ->
                Toast.makeText(
                    this,
                    "${item.category} AI 재분석",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadTimeCapsulesFromDb()
    }

    private fun loadTimeCapsulesFromDb() {
        db.collection(COLLECTION_NAME)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.mapNotNull { doc ->
                    val category = doc.getString("category") ?: ""

                    if (category.isBlank()) {
                        return@mapNotNull null
                    }

                    val createdAt = doc.getTimestamp("createdAt")
                    val dateText = if (createdAt != null) {
                        formatDate(createdAt.toDate())
                    } else {
                        ""
                    }

                    TimeCapsuleItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        choiceOptions = doc.getString("choiceOptions") ?: "",
                        selectedOption = doc.getString("selectedOption") ?: "",
                        reason = doc.getString("reason") ?: "",

                        category = doc.getString("category") ?: "",
                        dateText = dateText,
                        discomfort = doc.getString("discomfort") ?: "",
                        emotionScore = doc.getLong("emotionScore")?.toInt() ?: 0,
                        humidity = doc.getLong("humidity")?.toInt() ?: 0,
                        temperature = doc.getDouble("temperature") ?: 0.0,
                        weather = doc.getString("weather") ?: "",
                        reviewCompleted = doc.getBoolean("reviewCompleted") ?: false,
                        previousReviewText = doc.getString("previousReviewText") ?: ""
                    )
                }

                adapter.submitList(items)

                if (items.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveReview(item: TimeCapsuleItem, input: ReviewInput) {
        val reviewData = hashMapOf(
            "satisfaction" to input.satisfaction,
            "result" to input.result,
            "regret" to input.regret,
            "retryChoice" to input.retryChoice,
            "memo" to input.memo,
            "createdAt" to Timestamp.now()
        )

        db.collection(COLLECTION_NAME)
            .document(item.id)
            .collection("reviews")
            .add(reviewData)
            .addOnSuccessListener {
                db.collection(COLLECTION_NAME)
                    .document(item.id)
                    .update(
                        mapOf(
                            "reviewCompleted" to true,
                            "previousReviewText" to "만족도 ${input.satisfaction}/5 · ${input.result}"
                        )
                    )
                    .addOnSuccessListener {
                        Toast.makeText(this, "회고가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        loadTimeCapsulesFromDb()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "회고 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("yyyy. M. d.", Locale.KOREA)
        return formatter.format(date)
    }
}