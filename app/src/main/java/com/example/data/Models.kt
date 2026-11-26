package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object DailyExamRules {
    const val TOTAL_QUESTIONS = 50
    const val TIME_LIMIT_MINUTES = 25
    const val TIME_LIMIT_MILLIS = 25L * 60L * 1000L
    const val PASS_MARK = 35.0
    const val CORRECT_MARK = 1.0
    const val WRONG_PENALTY = 0.25

    fun calculateNetScore(correctCount: Int, wrongCount: Int): Double {
        return (correctCount * CORRECT_MARK) - (wrongCount * WRONG_PENALTY)
    }

    fun isPassed(netScore: Double): Boolean = netScore >= PASS_MARK

    /**
     * Rank-based daily reward points:
     * - Fail (< 35 marks): 0 points
     * - Rank 1..10 (Passed): 50 points
     * - Rank 11..20 (Passed): 20 points
     * - Rank 21..30 (Passed): 10 points
     * - Rank 31+ (Passed): 5 points
     */
    fun rewardPointsForRank(rank: Int, passed: Boolean): Long {
        if (!passed) return 0L
        return when {
            rank in 1..10 -> 50L
            rank in 11..20 -> 20L
            rank in 21..30 -> 10L
            rank > 30 -> 5L
            else -> 0L
        }
    }

    fun formatScore(score: Double): String {
        val roundedInt = score.toInt()
        return if (abs(score - roundedInt.toDouble()) < 0.001) {
            roundedInt.toString()
        } else if (abs(score * 10.0 - (score * 10.0).toInt().toDouble()) < 0.001) {
            String.format(Locale.US, "%.1f", score)
        } else {
            String.format(Locale.US, "%.2f", score)
        }
    }

    fun formatCountdown(remainingMillis: Long): String {
        val safe = remainingMillis.coerceAtLeast(0L)
        val totalSeconds = (safe + 999L) / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Keep
@IgnoreExtraProperties
data class StudentProfile(
    @DocumentId val docId: String = "",
    val uid: String = "",
    val displayName: String = "",
    val schoolName: String = "",
    val studentIdNumber: String = "",
    val studentIdHash: String = "",
    val gradeLevel: String = "அனைத்து வயது",
    val verificationStatus: String = "verified",
    val rewardPoints: Long = 0L,
    val totalScore: Long = 0L,
    val tournamentsCompleted: Long = 0L,
    val currentStreak: Long = 0L,
    val bestStreak: Long = 0L,
    val lastPlayedDate: String? = null,
    val adRewardCountToday: Long = 0L,
    val adRewardDate: String? = null,
    val categoryStats: Map<String, Map<String, Long>> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizQuestion(
    val id: String = "",
    val category: String = "பொது அறிவு",
    val difficulty: String = "நடுத்தரம்",
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = ""
)

@Keep
@IgnoreExtraProperties
data class DailyTournament(
    @DocumentId val docId: String = "",
    val dateKey: String = "",
    val title: String = "",
    val theme: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentSubmission(
    @DocumentId val docId: String = "",
    val uid: String = "",
    val dateKey: String = "",
    val monthKey: String = "",
    val studentName: String = "",
    val schoolName: String = "",
    val gradeLevel: String = "",
    val score: Double = 0.0,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val unansweredCount: Long = 0L,
    val totalQuestions: Long = DailyExamRules.TOTAL_QUESTIONS.toLong(),
    val passed: Boolean = false,
    val rank: Long = 1L,
    val durationMillis: Long = 0L,
    val pointsEarned: Long = 0L,
    val visibility: String = "public_leaderboard",
    @ServerTimestamp val submittedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonthlyRanking(
    @DocumentId val docId: String = "",
    val uid: String = "",
    val monthKey: String = "",
    val studentName: String = "",
    val schoolName: String = "",
    val gradeLevel: String = "",
    val monthlyScore: Long = 0L,
    val tournamentsPlayed: Long = 0L,
    val totalDurationMillis: Long = 0L,
    val visibility: String = "public_leaderboard",
    @ServerTimestamp val lastSubmittedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val docId: String = "",
    val dateKey: String = "",
    val score: Double = 0.0,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val unansweredCount: Long = 0L,
    val totalQuestions: Long = DailyExamRules.TOTAL_QUESTIONS.toLong(),
    val passed: Boolean = false,
    val rank: Long = 1L,
    val durationMillis: Long = 0L,
    val pointsEarned: Long = 0L,
    val selectedAnswers: List<Long> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RewardClaim(
    @DocumentId val docId: String = "",
    val uid: String = "",
    val rewardId: String = "",
    val rewardTitle: String = "",
    val rewardCategory: String = "gift_card",
    val pointsCost: Long = 0L,
    val claimCode: String = "",
    val status: String = "confirmed",
    val monthKey: String = "",
    @ServerTimestamp val claimedAt: Timestamp? = null
)

data class StudentIdExtractionResult(
    val appearsToBeStudentId: Boolean,
    val fullName: String,
    val schoolName: String,
    val studentIdNumber: String,
    val gradeLevel: String,
    val confidenceNote: String
)

enum class RewardCategory(val wireValue: String, val label: String) {
    GIFT_CARD("gift_card", "பரிசு அட்டை"),
    TECH_ACCESSORY("tech_accessory", "தொழில்நுட்பம்"),
    WINNER_CERTIFICATE("winner_certificate", "சான்றிதழ்");

    companion object {
        fun fromWire(raw: String): RewardCategory =
            entries.firstOrNull { it.wireValue == raw } ?: GIFT_CARD
    }
}

data class RewardCatalogItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: RewardCategory,
    val pointsCost: Long,
    val badgeText: String,
    val requiresMonthlyTopRank: Boolean = false
)

object LeagueTimeUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val prettyMonthFormat = SimpleDateFormat("MMMM yyyy", Locale("ta", "IN"))
    private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun todayDateKey(now: Date = Date()): String = dateFormat.format(now)

    fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    fun currentMonthKey(now: Date = Date()): String = monthFormat.format(now)

    fun prettyCurrentMonth(now: Date = Date()): String = prettyMonthFormat.format(now)

    fun formatStopwatch(millis: Long): String {
        val safe = millis.coerceAtLeast(0L)
        val totalSeconds = safe / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val tenths = (safe % 1000L) / 100L
        return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
    }

    fun formatSubmittedClock(timestamp: Timestamp?): String {
        if (timestamp == null) return "--:--:--"
        return clockFormat.format(timestamp.toDate())
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
