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
import android.widget.EditText
import com.example.termproject.data.weather.EnvironmentData

class Activity1 : BaseActivity() {
    private lateinit var categoryButtons: List<Button>

    private val weatherRepository = WeatherRepository()
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBaseContent(R.layout.activity1_main, NAV_RECORD)
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

//    private fun setupGoAnalysisButton() {
//        val btnGoAnalysis =
//            findViewById<Button>(R.id.btnGoAnalysis)
//        btnGoAnalysis.setOnClickListener {
//            val intent =
//                Intent(this, AiAnalysisActivity::class.java).apply {
//                    putExtra("title", "오늘의 결정")
//                    putExtra(
//                        "category",
//                        getSelectedCategory()
//                    )
//
//                    putExtra(
//                        "selectedOption",
//                        "친구에게 먼저 연락했다"
//                    )
//
//                    putExtra(
//                        "reason",
//                        "계속 어색한 상태로 두기 싫어서 먼저 연락했다"
//                    )
//
//                    putExtra(
//                        "expectedResult",
//                        "관계가 조금이라도 회복됐으면 좋겠다"
//                    )
//
//                    putExtra(
//                        "emotionScore",
//                        getEmotionScore()
//                    )
//
//                    putExtra(
//                        "createdTime",
//                        System.currentTimeMillis()
//                    )
//                }
//
//            startActivity(intent)
//        }
//    }
    private fun setupGoAnalysisButton() {
        val btnGoAnalysis = findViewById<Button>(R.id.btnGoAnalysis)

        btnGoAnalysis.setOnClickListener {
            requestEnvironmentAndMoveToAnalysis()
        }
    }
//    // Activity 1에서 입력한 값을 Activity2 로 넘기기
//    private fun moveToAnalysis() {
//        val title = findViewById<EditText>(R.id.titleEdit)
//            .text.toString().trim()
//
//        val choiceOptions = findViewById<EditText>(R.id.choiceEdit)
//            .text.toString().trim()
//
//        val selectedOption = findViewById<EditText>(R.id.finalChoiceEdit)
//            .text.toString().trim()
//
//        val reason = findViewById<EditText>(R.id.reasonEdit)
//            .text.toString().trim()
//
//        if (title.isBlank()) {
//            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (choiceOptions.isBlank()) {
//            Toast.makeText(this, "고민한 선택지를 입력해주세요.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (selectedOption.isBlank()) {
//            Toast.makeText(this, "결국 선택한 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (reason.isBlank()) {
//            Toast.makeText(this, "선택 이유를 입력해주세요.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val intent = Intent(this, AiAnalysisActivity::class.java).apply {
//            putExtra("title", title)
//            putExtra("category", getSelectedCategory())
//            putExtra("choiceOptions", choiceOptions)
//            putExtra("selectedOption", selectedOption)
//            putExtra("reason", reason)
//
//            // Activity1에는 기대 결과 입력칸이 아직 없으므로 일단 빈 값으로 전달
//            putExtra("expectedResult", "")
//
//            putExtra("emotionScore", getEmotionScore())
//            putExtra("createdTime", System.currentTimeMillis())
//        }
//
//        startActivity(intent)
//    }

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

    private fun requestEnvironmentAndMoveToAnalysis() {
        val title = findViewById<EditText>(R.id.titleEdit).text.toString().trim()
        val choiceOptions = findViewById<EditText>(R.id.choiceEdit).text.toString().trim()
        val selectedOption = findViewById<EditText>(R.id.finalChoiceEdit).text.toString().trim()
        val reason = findViewById<EditText>(R.id.reasonEdit).text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (choiceOptions.isBlank()) {
            Toast.makeText(this, "고민한 선택지를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedOption.isBlank()) {
            Toast.makeText(this, "결국 선택한 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (reason.isBlank()) {
            Toast.makeText(this, "선택 이유를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        Toast.makeText(this, "날씨 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show()

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            if (location == null) {
                startAnalysisActivityWithoutWeather(
                    title,
                    choiceOptions,
                    selectedOption,
                    reason
                )
                return@addOnSuccessListener
            }

            lifecycleScope.launch {
                try {
                    val env = weatherRepository.getEnvironmentData(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    startAnalysisActivityWithWeather(
                        title,
                        choiceOptions,
                        selectedOption,
                        reason,
                        env
                    )

                } catch (e: Exception) {
                    Toast.makeText(
                        this@Activity1,
                        "날씨 정보 실패, 기본 분석으로 이동합니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    startAnalysisActivityWithoutWeather(
                        title,
                        choiceOptions,
                        selectedOption,
                        reason
                    )
                }
            }
        }
    }

    private fun startAnalysisActivityWithWeather(
        title: String,
        choiceOptions: String,
        selectedOption: String,
        reason: String,
        env: EnvironmentData
    ) {
        val intent = Intent(this, AiAnalysisActivity::class.java).apply {
            putExtra("title", title)
            putExtra("category", getSelectedCategory())
            putExtra("choiceOptions", choiceOptions)
            putExtra("selectedOption", selectedOption)
            putExtra("reason", reason)
            putExtra("expectedResult", "")
            putExtra("emotionScore", getEmotionScore())
            putExtra("createdTime", System.currentTimeMillis())

            putExtra("weatherText", env.weatherText)
            putExtra("temperature", env.temperature)
            putExtra("humidity", env.humidity)
            putExtra("precipitation", env.precipitation)
            putExtra("rain", env.rain)
            putExtra("cloudCover", env.cloudCover)
            putExtra("pm10", env.pm10)
            putExtra("pm25", env.pm25)
            putExtra("uvIndex", env.uvIndex)
            putExtra("discomfortIndex", env.discomfortIndex)
            putExtra("discomfortText", env.discomfortText)
        }

        startActivity(intent)
    }

    private fun startAnalysisActivityWithoutWeather(
        title: String,
        choiceOptions: String,
        selectedOption: String,
        reason: String
    ) {
        val intent = Intent(this, AiAnalysisActivity::class.java).apply {
            putExtra("title", title)
            putExtra("category", getSelectedCategory())
            putExtra("choiceOptions", choiceOptions)
            putExtra("selectedOption", selectedOption)
            putExtra("reason", reason)
            putExtra("expectedResult", "")
            putExtra("emotionScore", getEmotionScore())
            putExtra("createdTime", System.currentTimeMillis())

            putExtra("weatherText", "알 수 없음")
            putExtra("temperature", 0.0)
            putExtra("humidity", 0)
            putExtra("precipitation", 0.0)
            putExtra("rain", 0.0)
            putExtra("cloudCover", 0)
            putExtra("pm10", 0.0)
            putExtra("pm25", 0.0)
            putExtra("uvIndex", 0.0)
            putExtra("discomfortIndex", 0.0)
            putExtra("discomfortText", "알 수 없음")
        }

        startActivity(intent)
    }

    private fun requestEnvironmentAndSave() {
        val title = findViewById<EditText>(R.id.titleEdit)
            .text.toString().trim()

        val choiceOptions = findViewById<EditText>(R.id.choiceEdit)
            .text.toString().trim()

        val selectedOption = findViewById<EditText>(R.id.finalChoiceEdit)
            .text.toString().trim()

        val reason = findViewById<EditText>(R.id.reasonEdit)
            .text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(this, "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (choiceOptions.isBlank()) {
            Toast.makeText(this, "고민한 선택지를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedOption.isBlank()) {
            Toast.makeText(this, "결국 선택한 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (reason.isBlank()) {
            Toast.makeText(this, "선택 이유를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
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

            if (location == null) {
                Toast.makeText(
                    this,
                    "현재 위치를 가져오지 못했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addOnSuccessListener
            }

            android.util.Log.d(
                "LOCATION",
                "lat=${location.latitude}, lon=${location.longitude}"
            )

            lifecycleScope.launch {
                android.util.Log.d("SAVE_FLOW", "날씨 정보 요청 시작")

                val environmentFields = try {
                    val environmentData =
                        weatherRepository.getEnvironmentData(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )

                    android.util.Log.d("SAVE_FLOW", "날씨 정보 가져오기 성공")

                    hashMapOf<String, Any>(
                        "weather" to environmentData.weatherText,
                        "discomfort" to environmentData.discomfortText,
                        "temperature" to environmentData.temperature,
                        "humidity" to environmentData.humidity
                    )

                } catch (e: Exception) {
                    android.util.Log.e("WEATHER_ERROR", "날씨 API 실패", e)

                    Toast.makeText(
                        this@Activity1,
                        "날씨 정보 실패: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    hashMapOf<String, Any>(
                        "weather" to "알 수 없음",
                        "discomfort" to "알 수 없음",
                        "temperature" to 0.0,
                        "humidity" to 0
                    )
                }

                val record = hashMapOf<String, Any>(
                    "title" to title,
                    "choiceOptions" to choiceOptions,
                    "selectedOption" to selectedOption,
                    "reason" to reason,

                    "category" to getSelectedCategory(),
                    "emotionScore" to getEmotionScore(),
                    "createdAt" to Timestamp.now(),

                    "reviewCompleted" to false
                )

                record.putAll(environmentFields)

                android.util.Log.d("FIRESTORE_SAVE", "Firestore 저장 시작: $record")

                db.collection("records")
                    .add(record)
                    .addOnSuccessListener { documentRef ->
                        android.util.Log.d("FIRESTORE_SAVE", "저장 성공 id=${documentRef.id}")

                        Toast.makeText(
                            this@Activity1,
                            "Firestore 저장 성공",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FIRESTORE_SAVE", "저장 실패", e)

                        Toast.makeText(
                            this@Activity1,
                            "저장 실패 : ${e.message}",
                            Toast.LENGTH_LONG
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