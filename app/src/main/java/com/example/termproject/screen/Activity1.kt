package com.example.termproject.screen

import com.example.termproject.R
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.termproject.data.weather.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class Activity1 : AppCompatActivity() {

    private lateinit var categoryButtons: List<Button>
    private val weatherRepository = WeatherRepository()
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

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
    }

    private fun setupDate() {
        val dateText = findViewById<TextView>(R.id.dateText)

        val formatter = SimpleDateFormat(
            "yyyy.MM.dd HH:mm",
            Locale.KOREA
        )

        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val currentTime = formatter.format(Date())

        dateText.text = currentTime
    }

    private fun setupEmotion() {
        val emotionText = findViewById<TextView>(R.id.emotionText)
        val seekBar = findViewById<SeekBar>(R.id.emotionSeekBar)

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

            emotionText.text = "감정 점수 : ${selected + 1} / 7"
        }

        updateEmojiUI(3)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                updateEmojiUI(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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

        selectCategory(findViewById(R.id.categoryDaily))

        categoryButtons.forEach { button ->
            button.setOnClickListener {
                selectCategory(button)
            }
        }
    }

    private fun selectCategory(selectedButton: Button) {
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

            lifecycleScope.launch {
                try {
                    val environmentData =
                        weatherRepository.getEnvironmentData(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )

                    Toast.makeText(
                        this@Activity1,
                        "날씨 저장 완료: ${environmentData.weatherText}, ${environmentData.discomfortText}",
                        Toast.LENGTH_LONG
                    ).show()

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

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
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