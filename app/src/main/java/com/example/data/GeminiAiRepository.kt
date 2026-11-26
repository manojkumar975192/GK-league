package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAiRepo"
private const val MODEL_NAME = "gemini-3.8-flash"

class GeminiAiRepository {

    private val idExtractionSchema = Schema.obj(
        properties = mapOf(
            "appearsToBeStudentId" to Schema.boolean(
                description = "True if the image looks like a student ID card, school pass, or college enrollment card."
            ),
            "fullName" to Schema.string(
                description = "Extracted full name of the student (in Tamil or English as printed), or empty string if unreadable."
            ),
            "schoolName" to Schema.string(
                description = "Extracted school, college, or university name, or empty string if unreadable."
            ),
            "studentIdNumber" to Schema.string(
                description = "Extracted student ID number or roll number, or empty string if unreadable."
            ),
            "gradeLevel" to Schema.string(
                description = "Academic level in Tamil: choose one of தொடக்கப் பள்ளி, நடுநிலைப் பள்ளி, உயர்நிலைப் பள்ளி, கல்லூரி, அனைத்து வயது."
            ),
            "confidenceNote" to Schema.string(
                description = "Brief 1-sentence note in natural Tamil summarizing what was verified on the student ID card."
            )
        )
    )

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Exciting short Tamil title for this GK exam round (max 60 chars)."),
            "theme" to Schema.string(description = "Short Tamil topic theme subtitle (max 80 chars)."),
            "questions" to Schema.array(
                description = "List of multiple-choice general knowledge questions written in clear Tamil.",
                items = Schema.obj(
                    properties = mapOf(
                        "id" to Schema.string(description = "Short ID like q1, q2..."),
                        "category" to Schema.string(
                            description = "One Tamil category: தமிழ் இலக்கியம், வரலாறு, அறிவியல், புவியியல், தொழில்நுட்பம், நடப்பு நிகழ்வுகள்"
                        ),
                        "difficulty" to Schema.string(
                            description = "One of: எளிது, நடுத்தரம், சவால்"
                        ),
                        "prompt" to Schema.string(
                            description = "Clear, concise factual general knowledge question written in natural Tamil for students."
                        ),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 distinct multiple-choice answer options in Tamil."
                        ),
                        "correctIndex" to Schema.integer(
                            description = "0-based index (0..3) of the correct option."
                        ),
                        "explanation" to Schema.string(
                            description = "Concise 1-sentence educational explanation in Tamil teaching the fact."
                        )
                    )
                )
            )
        )
    )

    private val idExtractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = idExtractionSchema
            }
        )
    }

    private val quizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private val coachModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    suspend fun extractStudentIdCard(bitmap: Bitmap): Result<StudentIdExtractionResult> {
        return try {
            val scaled = withContext(Dispatchers.Default) { downscaleBitmap(bitmap, 1280) }
            val response = withTimeout(60_000L) {
                idExtractionModel.generateContent(
                    content {
                        image(scaled)
                        text(
                            "Inspect this uploaded student ID card photo for Tamil GK League registration. " +
                                "The card may contain Tamil or English text (common in Tamil Nadu & Tamil-medium schools/colleges). " +
                                "Determine if it appears to be a student ID card, and extract fullName, schoolName, studentIdNumber, " +
                                "and gradeLevel (choose one of: தொடக்கப் பள்ளி, நடுநிலைப் பள்ளி, உயர்நிலைப் பள்ளி, கல்லூரி, அனைத்து வயது). " +
                                "Write confidenceNote in clear, polite Tamil."
                        )
                    }
                )
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini AI பதில் காலியாக உள்ளது."))
            val obj = JSONObject(rawJson)
            Result.success(
                StudentIdExtractionResult(
                    appearsToBeStudentId = obj.optBoolean("appearsToBeStudentId", false),
                    fullName = obj.optString("fullName", "").trim(),
                    schoolName = obj.optString("schoolName", "").trim(),
                    studentIdNumber = obj.optString("studentIdNumber", "").trim(),
                    gradeLevel = obj.optString("gradeLevel", "உயர்நிலைப் பள்ளி").trim().ifBlank { "உயர்நிலைப் பள்ளி" },
                    confidenceNote = obj.optString("confidenceNote", "Gemini AI மூலம் சரிபார்க்கப்பட்டது.").trim()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Student ID extraction timed out", e)
            Result.failure(Exception("அடையாள அட்டை சரிபார்ப்பு நேரம் முடிந்தது (60 வினாடிகள்). மீண்டும் முயலவும்."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Student ID extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "மாணவர் அடையாள அட்டையைப் படிக்க முடியவில்லை."))
        }
    }

    /**
     * Generates the official 50-question Tamil GK Daily Exam using 2 parallel 25-question Gemini calls
     * for high reliability, rich topic diversity, and fast generation time.
     */
    suspend fun generateDailyTournament(dateKey: String, creatorUid: String): Result<DailyTournament> {
        return try {
            val combined = withTimeout(90_000L) {
                coroutineScope {
                    val part1Deferred = async {
                        val response = quizModel.generateContent(
                            "Generate Part 1 (EXACTLY 25 unique multiple-choice questions) for today's Tamil GK League Daily Test ($dateKey) " +
                                "ENTIRELY IN CLEAR, NATURAL TAMIL (தமிழ்). " +
                                "Focus Part 1 on: தமிழ் இலக்கியம் (திருக்குறள், சங்க இலக்கியம்), வரலாறு (தமிழ்நாடு & இந்திய வரலாறு, சுதந்திரப் போராட்டம்), " +
                                "மற்றும் இந்திய அரசியலமைப்பு & பண்பாடு. " +
                                "Mix easy (எளிது), medium (நடுத்தரம்), and challenging (சவால்) questions suitable for Tamil students. " +
                                "Keep each prompt and explanation concise (1 short sentence) so all 25 questions are crisp and unambiguous. " +
                                "Every question MUST have exactly 4 Tamil options and correctIndex 0..3."
                        )
                        val raw = response.text
                            ?: throw IllegalStateException("பகுதி 1 வினாக்களை Gemini AI உருவாக்கவில்லை.")
                        parseQuizJson(raw, expectedCount = 25, minRequired = 20)
                    }

                    val part2Deferred = async {
                        val response = quizModel.generateContent(
                            "Generate Part 2 (EXACTLY 25 unique multiple-choice questions) for today's Tamil GK League Daily Test ($dateKey) " +
                                "ENTIRELY IN CLEAR, NATURAL TAMIL (தமிழ்). " +
                                "Focus Part 2 on: அறிவியல் (இயற்பியல், வேதியியல், உயிரியல், விண்வெளி), புவியியல் (தமிழ்நாடு, இந்தியா, உலகம்), " +
                                "தொழில்நுட்பம், விளையாட்டு, மற்றும் நடப்பு நிகழ்வுகள். " +
                                "Mix easy (எளிது), medium (நடுத்தரம்), and challenging (சவால்) questions suitable for Tamil students. " +
                                "Keep each prompt and explanation concise (1 short sentence) so all 25 questions are crisp and unambiguous. " +
                                "Every question MUST have exactly 4 Tamil options and correctIndex 0..3."
                        )
                        val raw = response.text
                            ?: throw IllegalStateException("பகுதி 2 வினாக்களை Gemini AI உருவாக்கவில்லை.")
                        parseQuizJson(raw, expectedCount = 25, minRequired = 20)
                    }

                    val p1 = part1Deferred.await()
                    val p2 = part2Deferred.await()

                    val merged = (p1.third + p2.third)
                        .distinctBy { it.prompt }
                        .take(DailyExamRules.TOTAL_QUESTIONS)
                        .mapIndexed { idx, q -> q.copy(id = "q${idx + 1}") }

                    require(merged.size >= 40) {
                        "50 வினாத்தாள் முழுமையாக உருவாகவில்லை (${merged.size} வினாக்கள்). மீண்டும் முயலவும்."
                    }

                    Triple(
                        p1.first.take(90),
                        "50 வினாக்கள் • 25 நிமிடம் • தேர்ச்சி: 35 • தவறான விடை: -0.25",
                        merged
                    )
                }
            }

            Result.success(
                DailyTournament(
                    dateKey = dateKey,
                    title = combined.first,
                    theme = combined.second.take(110),
                    questions = combined.third,
                    createdByUid = creatorUid
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Daily 50-question exam generation timed out", e)
            Result.failure(Exception("50 வினாத்தாள் உருவாக்க நேரம் முடிந்தது. மீண்டும் முயலவும்."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Daily 50-question exam generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "இன்றைய 50 வினா தமிழ் GK தேர்வை உருவாக்க முடியவில்லை."))
        }
    }

    suspend fun generatePracticeSprint(category: String, gradeLevel: String): Result<DailyTournament> {
        return try {
            val response = withTimeout(60_000L) {
                quizModel.generateContent(
                    "Generate a 6-question Tamil GK League AI Practice Sprint ENTIRELY IN CLEAR TAMIL (தமிழ்) on the topic '$category' " +
                        "for a student at level '$gradeLevel'. " +
                        "Include 2 எளிது, 2 நடுத்தரம், and 2 சவால் questions with 4 distinct Tamil options, exact correctIndex (0..3), " +
                        "and a concise Tamil educational explanation for each question."
                )
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini AI பயிற்சி வினாக்களை வழங்கவில்லை."))
            val parsed = parseQuizJson(rawJson, expectedCount = 6, minRequired = 5)
            Result.success(
                DailyTournament(
                    dateKey = LeagueTimeUtils.todayDateKey(),
                    title = parsed.first.take(90),
                    theme = "$category • $gradeLevel",
                    questions = parsed.third,
                    createdByUid = "practice"
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Practice sprint generation timed out", e)
            Result.failure(Exception("பயிற்சி வினா உருவாக்க நேரம் முடிந்தது. மீண்டும் முயலவும்."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Practice sprint generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "AI பயிற்சி வினாக்களை உருவாக்க முடியவில்லை."))
        }
    }

    fun streamStudyCoach(student: StudentProfile): Flow<String> = flow {
        val categorySummary = if (student.categoryStats.isEmpty()) {
            "இன்னும் அதிகாரப்பூர்வ தேர்வுகளில் பங்கேற்கவில்லை — புதிய போட்டியாளர்."
        } else {
            student.categoryStats.entries.joinToString("; ") { (cat, counts) ->
                val correct = counts["correct"] ?: 0L
                val total = counts["total"] ?: 0L
                val pct = if (total > 0) (correct * 100 / total) else 0
                "$cat: $correct/$total ($pct%)"
            }
        }
        val prompt = """
            நீங்கள் தமிழ் GK League மாணவர் ${student.displayName} (${student.gradeLevel}) அவர்களுக்கான AI படிப்பு வழிகாட்டி.
            தினசரி தேர்வு விதிகள்: 50 வினாக்கள், 25 நிமிட நேர வரம்பு, தவறான விடைக்கு -0.25 எதிர்மறை மதிப்பெண், தேர்ச்சி மதிப்பெண் 35/50.
            பரிசுப் புள்ளிகள்: 1–10 இடம் = 50 புள்ளி, 11–20 = 20 புள்ளி, 21–30 = 10 புள்ளி, பிற தேர்ச்சியாளர்கள் = 5 புள்ளி, தோல்வி = 0 புள்ளி.
            மாணவரின் தற்போதைய புள்ளிவிவரங்கள்:
            - முடித்த தேர்வுகள்: ${student.tournamentsCompleted}
            - மொத்த சரியான விடைகள்: ${student.totalScore}
            - தொடர் நாட்கள் (Streak): ${student.currentStreak} (அதிகபட்சம்: ${student.bestStreak})
            - பிரிவு வாரியான திறன்: $categorySummary

            தெளிவான, ஊக்கமளிக்கும் தமிழில் 4 சுருக்கமான புள்ளிகள் (140 வார்த்தைகளுக்குள்) எழுதுங்கள்:
            1. மாணவரின் பலமான பிரிவு அல்லது ஆரம்ப ஊக்கம்.
            2. அடுத்து மேம்படுத்த வேண்டிய 1-2 GK பிரிவுகள்.
            3. 25 நிமிடங்களில் 50 வினாக்களை முடித்து எதிர்மறை மதிப்பெண்ணைத் தவிர்த்து 35+ மதிப்பெண் பெற ஒரு சிறந்த உத்தி.
            4. முதல் 10 இடங்களைப் பிடித்து 50 புள்ளிகள் வெல்ல தினசரி பழக்கம்.
            எளிமையான புள்ளி வடிவத்தில் (•) மட்டும் எழுதுங்கள்.
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            coachModel.generateContentStream(prompt).collect { chunk ->
                val piece = chunk.text.orEmpty()
                if (piece.isNotEmpty()) {
                    accumulated.append(piece)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI வழிகாட்டி பதில் காலியாக உள்ளது.")
        }
    }

    private fun parseQuizJson(
        rawJson: String,
        expectedCount: Int,
        minRequired: Int = 5
    ): Triple<String, String, List<QuizQuestion>> {
        val root = JSONObject(rawJson)
        val title = root.optString("title", "தமிழ் GK தினசரி தேர்வு").trim().ifBlank { "தமிழ் GK தினசரி தேர்வு" }
        val theme = root.optString("theme", "50 வினாக்கள் • 25 நிமிடம்").trim().ifBlank { "50 வினாக்கள் • 25 நிமிடம்" }
        val arr = root.getJSONArray("questions")
        val questions = mutableListOf<QuizQuestion>()
        val limit = arr.length().coerceAtMost(expectedCount.coerceAtLeast(minRequired))
        for (i in 0 until limit) {
            val item = arr.getJSONObject(i)
            val optsArr = item.getJSONArray("options")
            val options = (0 until optsArr.length()).map { optsArr.getString(it).trim() }.take(4)
            if (options.size < 4) continue
            val correctIdx = item.optInt("correctIndex", 0).coerceIn(0, options.size - 1)
            val promptText = item.optString("prompt", "").trim()
            if (promptText.isBlank()) continue
            questions.add(
                QuizQuestion(
                    id = item.optString("id", "q${i + 1}").ifBlank { "q${i + 1}" },
                    category = item.optString("category", "பொது அறிவு").trim().ifBlank { "பொது அறிவு" },
                    difficulty = item.optString("difficulty", "நடுத்தரம்").trim().ifBlank { "நடுத்தரம்" },
                    prompt = promptText,
                    options = options,
                    correctIndex = correctIdx,
                    explanation = item.optString("explanation", "").trim()
                )
            )
        }
        require(questions.size >= minRequired) {
            "AI போதுமான வினாக்களை உருவாக்கவில்லை (${questions.size}). மீண்டும் முயலவும்."
        }
        return Triple(title, theme, questions)
    }

    private fun downscaleBitmap(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source
        val ratio = maxDimension.toFloat() / maxOf(width, height).toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }
}
