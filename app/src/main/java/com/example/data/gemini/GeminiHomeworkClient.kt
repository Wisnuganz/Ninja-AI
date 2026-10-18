package com.example.data.gemini

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class HomeworkSolution(
    val subject: String,
    val shortAnswer: String,
    val detailedSteps: String,
    val keyFormulas: String,
    val rawResponse: String
)

class GeminiHomeworkClient(private val context: Context) {

    private val prefs = context.getSharedPreferences("ninja_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        val userCustomKey = prefs.getString("custom_gemini_api_key", "") ?: ""
        if (userCustomKey.isNotBlank()) {
            return userCustomKey
        }
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "DEFAULT_KEY" || key == "MY_GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString("custom_gemini_api_key", apiKey.trim()).apply()
    }

    suspend fun solveHomework(
        questionText: String?,
        imageBitmap: Bitmap?,
        modelName: String = "gemini-2.5-flash"
    ): Result<HomeworkSolution> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("API Key Gemini belum disetel. Masukkan API Key di Pengaturan NinjaTugas.")
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val partsArray = JSONArray()

            val promptText = buildString {
                if (!questionText.isNullOrBlank()) {
                    append("Soal / Pertanyaan:\n").append(questionText.trim())
                } else if (imageBitmap != null) {
                    append("Tolong deteksi soal atau pertanyaan tugas pada gambar/tangkapan layar ini dan selesaikan secara akurat.")
                } else {
                    append("Selesaikan soal berikut.")
                }
            }

            // Text part
            val textPart = JSONObject().apply {
                put("text", promptText)
            }
            partsArray.put(textPart)

            // Image part if provided (live screen or photo)
            if (imageBitmap != null) {
                val outputStream = ByteArrayOutputStream()
                // Resize if too huge to keep latency low & memory safe
                val scaledBitmap = scaleBitmapIfNeeded(imageBitmap, 1280)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                val imagePart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Image)
                    }
                    put("inlineData", inlineData)
                }
                partsArray.put(imagePart)
            }

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", partsArray)
                })
            }

            val systemInstruction = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", """
                            Kamu adalah NinjaTugas: Asisten AI pengerjaan tugas & PR sekolah/kuliah yang 100% HENING dan visual.
                            Tugasmu adalah memecahkan soal (Matematika, Fisika, Kimia, Biologi, Bahasa, Sejarah, Coding, Ujian Pilihan Ganda, dll) secara akurat, cepat, dan terstruktur.
                            
                            WAJIB gunakan format struktur berikut dalam jawabanmu:
                            [SUBJEK]: (Contoh: Matematika / Fisika / Kimia / Bahasa Inggris / dll)
                            [JAWABAN UTAMA]: (Tuliskan jawaban final secara singkat, jelas, langsung to the point. Jika pilihan ganda, sebutkan opsi dan nilainya, misal: 'Opsi C (x = 7)')
                            [LANGKAH PENGERJAAN]: (Tuliskan langkah-langkah penyelesaian nomor urut 1, 2, 3 dengan jelas dan ringkas agar mudah dipahami)
                            [RUMUS/KONSEP]: (Tuliskan rumus atau konsep inti yang digunakan dalam memecahkan soal ini)
                            
                            Jangan tambahkan basa-basi pembuka atau penutup. Output murni dan bersih.
                        """.trimIndent())
                    })
                }
                put("parts", parts)
            }

            val generationConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("topP", 0.95)
            }

            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", systemInstruction)
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception("Gagal menghubungi AI: $errorMsg"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val candidateParts = content?.optJSONArray("parts")
            val answerText = candidateParts?.optJSONObject(0)?.optString("text") ?: ""

            if (answerText.isBlank()) {
                return@withContext Result.failure(Exception("Jawaban AI kosong."))
            }

            val parsedSolution = parseSolution(answerText)
            Result.success(parsedSolution)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSolution(text: String): HomeworkSolution {
        var subject = "Umum"
        var shortAnswer = ""
        var detailedSteps = ""
        var keyFormulas = ""

        val lines = text.lines()
        var currentSection = ""
        val stepsBuilder = StringBuilder()
        val formulasBuilder = StringBuilder()
        val shortAnswerBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[SUBJEK]:", ignoreCase = true) -> {
                    subject = trimmed.substringAfter(":").trim()
                    currentSection = "SUBJEK"
                }
                trimmed.startsWith("[JAWABAN UTAMA]:", ignoreCase = true) -> {
                    shortAnswerBuilder.append(trimmed.substringAfter(":").trim())
                    currentSection = "JAWABAN"
                }
                trimmed.startsWith("[LANGKAH PENGERJAAN]:", ignoreCase = true) -> {
                    val contentAfter = trimmed.substringAfter(":").trim()
                    if (contentAfter.isNotEmpty()) stepsBuilder.append(contentAfter).append("\n")
                    currentSection = "LANGKAH"
                }
                trimmed.startsWith("[RUMUS/KONSEP]:", ignoreCase = true) -> {
                    val contentAfter = trimmed.substringAfter(":").trim()
                    if (contentAfter.isNotEmpty()) formulasBuilder.append(contentAfter).append("\n")
                    currentSection = "RUMUS"
                }
                else -> {
                    when (currentSection) {
                        "JAWABAN" -> {
                            if (shortAnswerBuilder.isNotEmpty()) shortAnswerBuilder.append(" ")
                            shortAnswerBuilder.append(trimmed)
                        }
                        "LANGKAH" -> {
                            stepsBuilder.append(line).append("\n")
                        }
                        "RUMUS" -> {
                            formulasBuilder.append(line).append("\n")
                        }
                        else -> {
                            stepsBuilder.append(line).append("\n")
                        }
                    }
                }
            }
        }

        shortAnswer = shortAnswerBuilder.toString().trim()
        detailedSteps = stepsBuilder.toString().trim()
        keyFormulas = formulasBuilder.toString().trim()

        if (shortAnswer.isEmpty() && detailedSteps.isNotEmpty()) {
            shortAnswer = detailedSteps.lines().firstOrNull() ?: "Solusi ditemukan"
        }
        if (detailedSteps.isEmpty() && text.isNotEmpty()) {
            detailedSteps = text
        }

        return HomeworkSolution(
            subject = if (subject.isNotBlank()) subject else "Umum",
            shortAnswer = if (shortAnswer.isNotBlank()) shortAnswer else "Jawaban siap",
            detailedSteps = detailedSteps,
            keyFormulas = keyFormulas,
            rawResponse = text
        )
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt()
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
