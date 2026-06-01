package com.example.termproject.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.termproject.R
import com.example.termproject.data.weather.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class Activity1 : AppCompatActivity() {
    private lateinit var categoryButtons: List<Button>

    private val weatherRepository = WeatherRepository()
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity1_main)
        val recordButton =
            findViewById<Button>(R.id.saveButton)

        recordButton.setOnClickListener {
            requestEnvironmentAndSave()
        }

        setupDate()
        setupEmotion()
        setupCategories()
        setupGoAnalysisButton()
    }

    private fun getSelectedCategory(): String {
        val selectedButton = categoryButtons.find { it.isSelected }
        return selectedButton?.text.toString()
    }

    private fun getEmotionScore(): Int {
        val seekBar = findViewById<SeekBar>(R.id.emotionSeekBar)
        return seekBar.progress + 1
    }

    private fun setupGoAnalysisButton() {
        val btnGoAnalysis =
            findViewById<Button>(R.id.btnGoAnalysis)
        btnGoAnalysis.setOnClickListener {
            val intent =
                Intent(this, AiAnalysisActivity::class.java).apply {
                    putExtra("title", "오늘의 결정")
                    putExtra(
                        "category",
                        getSelectedCategory()
                    )

                    putExtra(
                        "selectedOption",
                        "친구에게 먼저 연락했다"
                    )

                    putExtra(
                        "reason",
                        "계속 어색한 상태로 두기 싫어서 먼저 연락했다"
                    )

                    putExtra(
                        "expectedResult",
                        "관계가 조금이라도 회복됐으면 좋겠다"
                    )

                    putExtra(
                        "emotionScore",
                        getEmotionScore()
                    )

                    putExtra(
                        "createdTime",
                        System.currentTimeMillis()
                    )
                }

            startActivity(intent)
        }
    }

    private fun setupDate() {
        val dateText =
            findViewById<TextView>(R.id.dateText)

        val formatter = SimpleDateFormat(
            "yyyy.MM.dd HH:mm",
            Locale.KOREA
        )

        formatter.timeZone =
            TimeZone.getTimeZone("Asia/Seoul")

        val currentTime =
            formatter.format(Date())
        dateText.text = currentTime
    }

    private fun setupEmotion() {
        val emotionText =
            findViewById<TextView>(R.id.emotionText)

        val seekBar =
            findViewById<SeekBar>(R.id.emotionSeekBar)

        val emojis = listOf(
            findViewById<TextView>(R.id.emoji0),
            findViewById<TextView>(R.id.emoji1),
            findViewById<TextView>(R.id.emoji2),
            findViewById<TextView>(R.id.emoji3),
            findViewById<TextView>(R.id.emoji4),
            findViewById<TextView>(R.id.emoji5),
            findViewById<TextView>(R.id.emoji6)
        )

        fun updateEmojiUI(selected: Int) {
            for (i in emojis.indices) {
                if (i == selected) {
                    emojis[i].scaleX = 1.25f
                    emojis[i].scaleY = 1.25f
                    emojis[i].alpha = 1f

                } else {
                    emojis[i].scaleX = 0.9f
                    emojis[i].scaleY = 0.9f
                    emojis[i].alpha = 0.4f
                }
            }

            emotionText.text =
                "감정 점수 : ${selected + 1} / 7"
        }

        updateEmojiUI(3)
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    updateEmojiUI(progress)
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )
    }

    private fun setupCategories() {
        categoryButtons = listOf(
            findViewById(R.id.categoryCareer),
            findViewById(R.id.categoryLove),
            findViewById(R.id.categoryRelation),
            findViewById(R.id.categoryConsume),
            findViewById(R.id.categoryStudy),
            findViewById(R.id.categoryWork),
            findViewById(R.id.categoryFood),
            findViewById(R.id.categoryDaily)
        )

        selectCategory(
            findViewById(R.id.categoryDaily)
        )

        categoryButtons.forEach { button ->
            button.setOnClickListener {
                selectCategory(button)
            }
        }
    }

    private fun selectCategory(
        selectedButton: Button
    ) {
        categoryButtons.forEach { button ->
            button.isSelected = false
        }
        selectedButton.isSelected = true
    }

    private fun requestEnvironmentAndSave() {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            android.util.Log.d( "LOCATION", "lat=${location.latitude}, lon=${location.longitude}" )
            if (location == null) {
                Toast.makeText(
                    this,
                    "현재 위치를 가져오지 못했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }

            lifecycleScope.launch {
                try {
                    val environmentData =
                        weatherRepository.getEnvironmentData(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )

                    val record = hashMapOf(
                        "weather" to environmentData.weatherText,
                        "discomfort" to environmentData.discomfortText,
                        "temperature" to environmentData.temperature,
                        "humidity" to environmentData.humidity,
                        "category" to getSelectedCategory(),
                        "emotionScore" to getEmotionScore(),
                        "createdAt" to Timestamp.now()                    )

                    db.collection("records")
                        .add(record)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@Activity1,
                                "Firestore 저장 성공",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .addOnFailureListener { e ->

                            Toast.makeText(
                                this@Activity1,
                                "저장 실패 : ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@Activity1,
                        "날씨 정보를 가져오지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )
        if (
            requestCode ==
            LOCATION_PERMISSION_REQUEST_CODE
        ) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {
                requestEnvironmentAndSave()
            } else {
                Toast.makeText(
                    this,
                    "위치 권한이 필요합니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}