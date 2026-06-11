package com.example.termproject.ml

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class DecisionMlPredictor(
    private val context: Context
) {
    private val interpreter: Interpreter by lazy {
        Interpreter(loadModelFile())
    }

    private val modelInfo: JSONObject by lazy {
        val jsonText = context.assets
            .open("decision_model_info.json")
            .bufferedReader()
            .use { it.readText() }

        JSONObject(jsonText)
    }

    fun predict(rawFeatures: FloatArray): DecisionMlResult {
        val normalizedFeatures = normalize(rawFeatures)

        val input = ByteBuffer
            .allocateDirect(4 * normalizedFeatures.size)
            .order(ByteOrder.nativeOrder())

        normalizedFeatures.forEach { value ->
            input.putFloat(value)
        }

        input.rewind()

        val output = Array(1) { FloatArray(3) }

        interpreter.run(input, output)

        val probabilities = output[0]
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 1
        val confidence = probabilities[maxIndex]

        val label = getLabel(maxIndex)

        return DecisionMlResult(
            regretLabel = label,
            confidence = confidence,
            probabilities = probabilities
        )
    }

    private fun normalize(rawFeatures: FloatArray): FloatArray {
        val meanArray = modelInfo.getJSONArray("mean")
        val scaleArray = modelInfo.getJSONArray("scale")

        val normalized = FloatArray(rawFeatures.size)

        for (i in rawFeatures.indices) {
            val mean = meanArray.getDouble(i).toFloat()
            val scale = scaleArray.getDouble(i).toFloat()
            val safeScale = max(scale, 0.0001f)

            normalized[i] = (rawFeatures[i] - mean) / safeScale
        }

        return normalized
    }

    private fun getLabel(index: Int): String {
        val labels = modelInfo.getJSONArray("labels")
        return labels.optString(index, "보통")
    }

    private fun loadModelFile(): ByteBuffer {
        val inputStream = context.assets.open("decision_regret_model.tflite")
        val outputStream = ByteArrayOutputStream()

        val buffer = ByteArray(1024)
        var read: Int

        while (true) {
            read = inputStream.read(buffer)
            if (read == -1) break
            outputStream.write(buffer, 0, read)
        }

        inputStream.close()

        val modelBytes = outputStream.toByteArray()

        val byteBuffer = ByteBuffer
            .allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())

        byteBuffer.put(modelBytes)
        byteBuffer.rewind()

        return byteBuffer
    }
}