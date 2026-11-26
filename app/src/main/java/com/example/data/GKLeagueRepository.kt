package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class TournamentSubmitSummary(
    val score: Double,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val totalQuestions: Int,
    val passed: Boolean,
    val rank: Int,
    val durationMillis: Long,
    val pointsEarned: Long,
    val streakAfter: Long,
    val timedOut: Boolean = false
)

class GKLeagueRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("தொடர்வதற்கு முன் Google மூலம் உள்நுழையவும்.")
    }

    fun observeStudentProfile(uid: String): Flow<StudentProfile?> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(null)
            return@flow
        }
        val path = "students/$uid"
        emitAll(
            db.collection("students").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        StudentProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeDailyLeaderboard(dateKey: String, uid: String): Flow<List<TournamentSubmission>> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(emptyList())
            return@flow
        }
        val path = "tournament_submissions"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public_leaderboard")
                .whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TournamentSubmission::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<TournamentSubmission> { it.score }
                            .thenBy { it.submittedAt?.toDate()?.time ?: Long.MAX_VALUE }
                            .thenBy { it.durationMillis }
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeMonthlyLeaderboard(monthKey: String, uid: String): Flow<List<MonthlyRanking>> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(emptyList())
            return@flow
        }
        val path = "monthly_rankings"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public_leaderboard")
                .whereEqualTo("monthKey", monthKey)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MonthlyRanking::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<MonthlyRanking> { it.monthlyScore }
                            .thenBy { it.lastSubmittedAt?.toDate()?.time ?: Long.MAX_VALUE }
                            .thenBy { it.totalDurationMillis }
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    fun observeTodayAttempt(uid: String, dateKey: String): Flow<QuizAttempt?> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(null)
            return@flow
        }
        val path = "students/$uid/attempts/$dateKey"
        emitAll(
            db.collection("students").document(uid)
                .collection("attempts").document(dateKey)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        QuizAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                }
        )
    }

    fun observeRewardClaims(uid: String): Flow<List<RewardClaim>> = flow {
        if (uid.isBlank() || auth.currentUser?.uid != uid) {
            emit(emptyList())
            return@flow
        }
        val path = "students/$uid/reward_claims"
        emitAll(
            db.collection("students").document(uid)
                .collection("reward_claims")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RewardClaim::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.claimedAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                }
        )
    }

    suspend fun saveVerifiedStudentProfile(
        displayName: String,
        schoolName: String,
        studentIdNumber: String,
        gradeLevel: String,
        isUpdate: Boolean
    ): Result<Unit> {
        val cleanName = displayName.trim().take(80)
        val cleanSchool = schoolName.trim().take(120)
        val cleanId = studentIdNumber.trim().take(60)
        val cleanGrade = gradeLevel.trim().take(40).ifBlank { "அனைத்து வயது" }

        if (cleanName.length < 2) return Result.failure(IllegalArgumentException("உங்கள் முழுப் பெயரை உள்ளிடவும் (குறைந்தது 2 எழுத்துகள்)."))
        if (cleanSchool.length < 2) return Result.failure(IllegalArgumentException("உங்கள் பள்ளி அல்லது கல்லூரிப் பெயரை உள்ளிடவும்."))
        if (cleanId.length < 2) return Result.failure(IllegalArgumentException("உங்கள் மாணவர் அடையாள எண்ணை உள்ளிடவும்."))

        var docPath = "students/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("students").document(uid)
            docPath = docRef.path
            val idHash = LeagueTimeUtils.sha256Hex("$uid|$cleanSchool|$cleanId").take(64)

            if (isUpdate) {
                val updateMap = mapOf<String, Any>(
                    "displayName" to cleanName,
                    "schoolName" to cleanSchool,
                    "studentIdNumber" to cleanId,
                    "studentIdHash" to idHash,
                    "gradeLevel" to cleanGrade,
                    "verificationStatus" to "verified",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf<String, Any>(
                    "uid" to uid,
                    "displayName" to cleanName,
                    "schoolName" to cleanSchool,
                    "studentIdNumber" to cleanId,
                    "studentIdHash" to idHash,
                    "gradeLevel" to cleanGrade,
                    "verificationStatus" to "verified",
                    "rewardPoints" to 50L,
                    "totalScore" to 0L,
                    "tournamentsCompleted" to 0L,
                    "currentStreak" to 0L,
                    "bestStreak" to 0L,
                    "adRewardCountToday" to 0L,
                    "categoryStats" to emptyMap<String, Map<String, Long>>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isUpdate) OperationType.UPDATE else OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun getOrCreateDailyTournament(
        dateKey: String,
        aiRepository: GeminiAiRepository
    ): Result<DailyTournament> {
        val docPath = "daily_tournaments/$dateKey"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("daily_tournaments").document(dateKey)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val parsed = existingSnap.toObject(
                    DailyTournament::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                // Only reuse if it is already a full 50-question paper (>= 40 questions)
                if (parsed != null && parsed.questions.size >= 40) {
                    return Result.success(parsed)
                }
            }

            // Generate shared 50-question daily exam with Gemini AI
            val generated = aiRepository.generateDailyTournament(dateKey, uid).getOrElse { err ->
                return Result.failure(err)
            }

            val questionsPayload = generated.questions.map { q ->
                mapOf(
                    "id" to q.id,
                    "category" to q.category,
                    "difficulty" to q.difficulty,
                    "prompt" to q.prompt,
                    "options" to q.options,
                    "correctIndex" to q.correctIndex,
                    "explanation" to q.explanation
                )
            }

            val payload = mapOf<String, Any>(
                "dateKey" to dateKey,
                "title" to generated.title,
                "theme" to generated.theme,
                "questions" to questionsPayload,
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            try {
                docRef.set(payload).await()
                Result.success(generated)
            } catch (writeErr: Exception) {
                if (writeErr is CancellationException) throw writeErr
                // Another student may have just published today's 50-question exam first; read it
                val retrySnap = docRef.get().await()
                val published = retrySnap.toObject(
                    DailyTournament::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (published != null && published.questions.size >= 40) {
                    Result.success(published)
                } else {
                    handleFirestoreError(writeErr, OperationType.CREATE, docPath)
                    Result.failure(writeErr)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun submitOfficialTournament(
        student: StudentProfile,
        tournament: DailyTournament,
        selectedAnswers: List<Int>,
        durationMillis: Long,
        timedOut: Boolean = false
    ): Result<TournamentSubmitSummary> {
        val dateKey = tournament.dateKey
        val monthKey = dateKey.take(7)
        val safeDuration = durationMillis.coerceIn(1000L, DailyExamRules.TIME_LIMIT_MILLIS)

        var currentPath = "tournament_submissions/${dateKey}_${student.uid}"
        return try {
            val uid = requireUserId()
            require(uid == student.uid) { "உள்நுழைந்த கணக்கு பொருந்தவில்லை." }

            val totalQuestions = tournament.questions.size
            var correctCount = 0
            var wrongCount = 0
            var unansweredCount = 0
            val updatedCategoryStats = student.categoryStats.mapValues { (_, counts) ->
                mutableMapOf(
                    "correct" to (counts["correct"] ?: 0L),
                    "total" to (counts["total"] ?: 0L)
                )
            }.toMutableMap()

            tournament.questions.forEachIndexed { idx, q ->
                val chosen = selectedAnswers.getOrNull(idx) ?: -1
                val isRight = chosen == q.correctIndex
                when {
                    chosen < 0 -> unansweredCount++
                    isRight -> correctCount++
                    else -> wrongCount++
                }
                val catKey = q.category.take(24).ifBlank { "பொது அறிவு" }
                if (updatedCategoryStats.size < 12 || updatedCategoryStats.containsKey(catKey)) {
                    val bucket = updatedCategoryStats.getOrPut(catKey) {
                        mutableMapOf("correct" to 0L, "total" to 0L)
                    }
                    bucket["total"] = (bucket["total"] ?: 0L) + 1L
                    if (isRight) {
                        bucket["correct"] = (bucket["correct"] ?: 0L) + 1L
                    }
                }
            }

            // Negative marking calculation: +1 correct, -0.25 wrong, 0 skipped; pass mark = 35
            val netScore = DailyExamRules.calculateNetScore(correctCount, wrongCount)
            val passed = DailyExamRules.isPassed(netScore)

            // Determine live rank on today's leaderboard (higher score first; earlier submission wins ties)
            val existingDailySnap = db.collection("tournament_submissions")
                .whereEqualTo("visibility", "public_leaderboard")
                .whereEqualTo("dateKey", dateKey)
                .get()
                .await()
            val betterOrEarlierTiedCount = existingDailySnap.documents.count { doc ->
                val docUid = doc.getString("uid")
                if (docUid == uid) return@count false
                val otherScore = (doc.get("score") as? Number)?.toDouble() ?: Double.NEGATIVE_INFINITY
                otherScore >= netScore
            }
            val rank = betterOrEarlierTiedCount + 1

            // Rank-tier reward points:
            // Rank 1..10 -> 50 pts | 11..20 -> 20 pts | 21..30 -> 10 pts | 31+ -> 5 pts | Fail (<35) -> 0 pts
            val pointsEarned = DailyExamRules.rewardPointsForRank(rank = rank, passed = passed)

            // Compute daily streak
            val yesterday = LeagueTimeUtils.yesterdayDateKey()
            val newStreak = when (student.lastPlayedDate) {
                dateKey -> student.currentStreak.coerceAtLeast(1L)
                yesterday -> student.currentStreak + 1L
                else -> 1L
            }
            val newBestStreak = maxOf(student.bestStreak, newStreak)

            val submissionRef = db.collection("tournament_submissions").document("${dateKey}_$uid")
            val attemptRef = db.collection("students").document(uid).collection("attempts").document(dateKey)
            val studentRef = db.collection("students").document(uid)
            val monthlyRef = db.collection("monthly_rankings").document("${monthKey}_$uid")

            val existingMonthlySnap = monthlyRef.get().await()

            val batch = db.batch()

            // 1. Immutable public daily leaderboard entry (server timestamp decides first-submit priority!)
            currentPath = submissionRef.path
            batch.set(
                submissionRef,
                mapOf<String, Any>(
                    "uid" to uid,
                    "dateKey" to dateKey,
                    "monthKey" to monthKey,
                    "studentName" to student.displayName,
                    "schoolName" to student.schoolName,
                    "gradeLevel" to student.gradeLevel,
                    "score" to netScore,
                    "correctCount" to correctCount.toLong(),
                    "wrongCount" to wrongCount.toLong(),
                    "unansweredCount" to unansweredCount.toLong(),
                    "totalQuestions" to totalQuestions.toLong(),
                    "passed" to passed,
                    "rank" to rank.toLong(),
                    "durationMillis" to safeDuration,
                    "pointsEarned" to pointsEarned,
                    "visibility" to "public_leaderboard",
                    "submittedAt" to FieldValue.serverTimestamp()
                )
            )

            // 2. Private quiz attempt breakdown
            batch.set(
                attemptRef,
                mapOf<String, Any>(
                    "dateKey" to dateKey,
                    "score" to netScore,
                    "correctCount" to correctCount.toLong(),
                    "wrongCount" to wrongCount.toLong(),
                    "unansweredCount" to unansweredCount.toLong(),
                    "totalQuestions" to totalQuestions.toLong(),
                    "passed" to passed,
                    "rank" to rank.toLong(),
                    "durationMillis" to safeDuration,
                    "pointsEarned" to pointsEarned,
                    "selectedAnswers" to selectedAnswers.map { it.toLong() },
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            // 3. Update private student stats & rank-tier points (Action A)
            batch.update(
                studentRef,
                mapOf<String, Any>(
                    "rewardPoints" to (student.rewardPoints + pointsEarned),
                    "totalScore" to (student.totalScore + correctCount.toLong()),
                    "tournamentsCompleted" to (student.tournamentsCompleted + 1L),
                    "currentStreak" to newStreak,
                    "bestStreak" to newBestStreak,
                    "lastPlayedDate" to dateKey,
                    "categoryStats" to updatedCategoryStats.mapValues { it.value.toMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 4. Create or update monthly top-rankers standing
            if (!existingMonthlySnap.exists()) {
                batch.set(
                    monthlyRef,
                    mapOf<String, Any>(
                        "uid" to uid,
                        "monthKey" to monthKey,
                        "studentName" to student.displayName,
                        "schoolName" to student.schoolName,
                        "gradeLevel" to student.gradeLevel,
                        "monthlyScore" to pointsEarned,
                        "tournamentsPlayed" to 1L,
                        "totalDurationMillis" to safeDuration,
                        "visibility" to "public_leaderboard",
                        "lastSubmittedAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val prevScore = (existingMonthlySnap.get("monthlyScore") as? Number)?.toLong() ?: 0L
                val prevPlayed = (existingMonthlySnap.get("tournamentsPlayed") as? Number)?.toLong() ?: 0L
                val prevDur = (existingMonthlySnap.get("totalDurationMillis") as? Number)?.toLong() ?: 0L
                batch.update(
                    monthlyRef,
                    mapOf<String, Any>(
                        "studentName" to student.displayName,
                        "schoolName" to student.schoolName,
                        "gradeLevel" to student.gradeLevel,
                        "monthlyScore" to (prevScore + pointsEarned),
                        "tournamentsPlayed" to (prevPlayed + 1L),
                        "totalDurationMillis" to (prevDur + safeDuration),
                        "lastSubmittedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()

            Result.success(
                TournamentSubmitSummary(
                    score = netScore,
                    correctCount = correctCount,
                    wrongCount = wrongCount,
                    unansweredCount = unansweredCount,
                    totalQuestions = totalQuestions,
                    passed = passed,
                    rank = rank,
                    durationMillis = safeDuration,
                    pointsEarned = pointsEarned,
                    streakAfter = newStreak,
                    timedOut = timedOut
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            Result.failure(e)
        }
    }

    suspend fun creditRewardedAdBonus(dateKey: String): Result<Long> {
        var docPath = "students/unauthenticated"
        return try {
            val uid = requireUserId()
            val studentRef = db.collection("students").document(uid)
            docPath = studentRef.path

            val newBalance = db.runTransaction { tx ->
                val snap = tx.get(studentRef)
                val currentPts = (snap.get("rewardPoints") as? Number)?.toLong() ?: 0L
                val adDate = snap.getString("adRewardDate")
                val currentCount = if (adDate == dateKey) {
                    (snap.get("adRewardCountToday") as? Number)?.toLong() ?: 0L
                } else {
                    0L
                }
                if (currentCount >= 3L) {
                    throw IllegalStateException("இன்றைய விளம்பரப் போனஸ் வரம்பு முடிந்துவிட்டது (3/3). நாளை மீண்டும் முயற்சிக்கவும்!")
                }
                val updatedPts = currentPts + 25L
                tx.update(
                    studentRef,
                    mapOf<String, Any>(
                        "rewardPoints" to updatedPts,
                        "adRewardCountToday" to (currentCount + 1L),
                        "adRewardDate" to dateKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                updatedPts
            }.await()

            Result.success(newBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun claimCatalogReward(
        item: RewardCatalogItem,
        monthKey: String,
        monthlyRank: Int?
    ): Result<RewardClaim> {
        var docPath = "students/unauthenticated/reward_claims"
        return try {
            val uid = requireUserId()
            if (item.requiresMonthlyTopRank && (monthlyRank == null || monthlyRank > 3)) {
                return Result.failure(
                    IllegalStateException("மாதாந்திர வெற்றியாளர் சான்றிதழைப் பெற இந்த மாதத் தரவரிசையில் முதல் 3 இடங்களுக்குள் வரவும்.")
                )
            }

            val studentRef = db.collection("students").document(uid)
            val claimId = "clm_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val claimRef = studentRef.collection("reward_claims").document(claimId)
            docPath = claimRef.path

            val codePrefix = when (item.category) {
                RewardCategory.GIFT_CARD -> "GKL-GC"
                RewardCategory.TECH_ACCESSORY -> "GKL-TECH"
                RewardCategory.WINNER_CERTIFICATE -> "GKL-CERT"
            }
            val claimCode = "$codePrefix-${monthKey.replace("-", "")}-${claimId.takeLast(6).uppercase()}"
            val status = if (item.category == RewardCategory.WINNER_CERTIFICATE) "issued" else "confirmed"

            db.runTransaction { tx ->
                val studentSnap = tx.get(studentRef)
                val currentPoints = (studentSnap.get("rewardPoints") as? Number)?.toLong() ?: 0L
                if (currentPoints < item.pointsCost) {
                    throw IllegalStateException("இந்தப் பரிசைப் பெற மேலும் ${item.pointsCost - currentPoints} லீக் புள்ளிகள் தேவை.")
                }

                if (item.pointsCost > 0L) {
                    tx.update(
                        studentRef,
                        mapOf<String, Any>(
                            "rewardPoints" to (currentPoints - item.pointsCost),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                tx.set(
                    claimRef,
                    mapOf<String, Any>(
                        "uid" to uid,
                        "rewardId" to item.id,
                        "rewardTitle" to item.title,
                        "rewardCategory" to item.category.wireValue,
                        "pointsCost" to item.pointsCost,
                        "claimCode" to claimCode,
                        "status" to status,
                        "monthKey" to monthKey,
                        "claimedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(
                RewardClaim(
                    docId = claimId,
                    uid = uid,
                    rewardId = item.id,
                    rewardTitle = item.title,
                    rewardCategory = item.category.wireValue,
                    pointsCost = item.pointsCost,
                    claimCode = claimCode,
                    status = status,
                    monthKey = monthKey,
                    claimedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }
}
