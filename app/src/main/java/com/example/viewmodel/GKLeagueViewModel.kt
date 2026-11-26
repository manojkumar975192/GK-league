package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailyExamRules
import com.example.data.DailyTournament
import com.example.data.GKLeagueRepository
import com.example.data.GeminiAiRepository
import com.example.data.LeagueTimeUtils
import com.example.data.MonthlyRanking
import com.example.data.QuizAttempt
import com.example.data.RewardCatalogItem
import com.example.data.RewardClaim
import com.example.data.StudentIdExtractionResult
import com.example.data.StudentProfile
import com.example.data.TournamentSubmission
import com.example.data.TournamentSubmitSummary
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface IdScanUiState {
    data object Idle : IdScanUiState
    data object Scanning : IdScanUiState
    data class Extracted(val result: StudentIdExtractionResult) : IdScanUiState
    data object Saving : IdScanUiState
    data class Error(val message: String) : IdScanUiState
}

sealed interface CoachUiState {
    data object Idle : CoachUiState
    data object Loading : CoachUiState
    data class Streaming(val text: String) : CoachUiState
    data class Success(val text: String) : CoachUiState
    data class Error(val message: String) : CoachUiState
}

data class QuizSessionState(
    val active: Boolean = false,
    val practiceMode: Boolean = false,
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val tournament: DailyTournament? = null,
    val currentIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(),
    val elapsedMillis: Long = 0L,
    val remainingMillis: Long = DailyExamRules.TIME_LIMIT_MILLIS,
    val submitting: Boolean = false,
    val submittedSummary: TournamentSubmitSummary? = null
)

data class RewardFeedback(
    val message: String,
    val isError: Boolean = false,
    val recentClaim: RewardClaim? = null
)

class GKLeagueViewModel(
    val currentUid: String,
    private val repository: GKLeagueRepository,
    private val aiRepository: GeminiAiRepository
) : ViewModel() {

    val todayDateKey: String = LeagueTimeUtils.todayDateKey()
    val currentMonthKey: String = LeagueTimeUtils.currentMonthKey()

    val studentProfileState: StateFlow<UiState<StudentProfile?>> = repository
        .observeStudentProfile(currentUid)
        .map<StudentProfile?, UiState<StudentProfile?>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreFlowError(error, "மாணவர் சுயவிவரத்தை ஏற்ற முடியவில்லை")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val dailyLeaderboardState: StateFlow<UiState<List<TournamentSubmission>>> = repository
        .observeDailyLeaderboard(todayDateKey, currentUid)
        .map<List<TournamentSubmission>, UiState<List<TournamentSubmission>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreFlowError(error, "இன்றைய தரவரிசைப் பட்டியலை ஏற்ற முடியவில்லை")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val monthlyLeaderboardState: StateFlow<UiState<List<MonthlyRanking>>> = repository
        .observeMonthlyLeaderboard(currentMonthKey, currentUid)
        .map<List<MonthlyRanking>, UiState<List<MonthlyRanking>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreFlowError(error, "மாதாந்திர தரவரிசையை ஏற்ற முடியவில்லை")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val todayAttemptState: StateFlow<UiState<QuizAttempt?>> = repository
        .observeTodayAttempt(currentUid, todayDateKey)
        .map<QuizAttempt?, UiState<QuizAttempt?>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreFlowError(error, "இன்றைய போட்டி முயற்சியை ஏற்ற முடியவில்லை")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val rewardClaimsState: StateFlow<UiState<List<RewardClaim>>> = repository
        .observeRewardClaims(currentUid)
        .map<List<RewardClaim>, UiState<List<RewardClaim>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreFlowError(error, "பரிசு விவரங்களை ஏற்ற முடியவில்லை")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _idScanState = MutableStateFlow<IdScanUiState>(IdScanUiState.Idle)
    val idScanState: StateFlow<IdScanUiState> = _idScanState.asStateFlow()

    private val _quizSession = MutableStateFlow(QuizSessionState())
    val quizSession: StateFlow<QuizSessionState> = _quizSession.asStateFlow()

    private val _coachState = MutableStateFlow<CoachUiState>(CoachUiState.Idle)
    val coachState: StateFlow<CoachUiState> = _coachState.asStateFlow()

    private val _rewardFeedback = MutableStateFlow<RewardFeedback?>(null)
    val rewardFeedback: StateFlow<RewardFeedback?> = _rewardFeedback.asStateFlow()

    private val _claimingRewardId = MutableStateFlow<String?>(null)
    val claimingRewardId: StateFlow<String?> = _claimingRewardId.asStateFlow()

    private var timerJob: Job? = null
    private var sprintStartMarkMillis: Long = 0L

    // --- STUDENT ID AI VERIFICATION ---

    fun scanStudentIdBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _idScanState.value = IdScanUiState.Scanning
            val result = aiRepository.extractStudentIdCard(bitmap)
            result.fold(
                onSuccess = { extracted ->
                    _idScanState.value = IdScanUiState.Extracted(extracted)
                },
                onFailure = { error ->
                    _idScanState.value = IdScanUiState.Error(
                        error.message ?: "மாணவர் அடையாள அட்டையைப் படிக்க முடியவில்லை. மீண்டும் முயற்சிக்கவும்."
                    )
                }
            )
        }
    }

    fun confirmAndSaveStudentId(
        displayName: String,
        schoolName: String,
        studentIdNumber: String,
        gradeLevel: String,
        isUpdate: Boolean,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _idScanState.value = IdScanUiState.Saving
            val result = repository.saveVerifiedStudentProfile(
                displayName = displayName,
                schoolName = schoolName,
                studentIdNumber = studentIdNumber,
                gradeLevel = gradeLevel,
                isUpdate = isUpdate
            )
            result.fold(
                onSuccess = {
                    _idScanState.value = IdScanUiState.Idle
                    onSaved()
                },
                onFailure = { error ->
                    _idScanState.value = IdScanUiState.Error(
                        error.message ?: "மாணவர் சுயவிவரத்தைச் சேமிக்க முடியவில்லை."
                    )
                }
            )
        }
    }

    fun resetIdScanState() {
        _idScanState.value = IdScanUiState.Idle
    }

    // --- OFFICIAL 50-QUESTION 25-MINUTE DAILY EXAM & AI PRACTICE SPRINTS ---

    fun launchDailyTournament(reviewOnly: Boolean = false) {
        timerJob?.cancel()
        viewModelScope.launch {
            _quizSession.value = QuizSessionState(
                active = true,
                practiceMode = false,
                loading = true
            )
            val result = repository.getOrCreateDailyTournament(todayDateKey, aiRepository)
            result.fold(
                onSuccess = { tournament ->
                    if (reviewOnly) {
                        val existingAttempt = (todayAttemptState.value as? UiState.Success)?.data
                        val answersMap = existingAttempt?.selectedAnswers
                            ?.mapIndexedNotNull { idx, ans ->
                                val intAns = ans.toInt()
                                if (intAns >= 0) idx to intAns else null
                            }
                            ?.toMap()
                            ?: emptyMap()
                        val liveRank = (dailyLeaderboardState.value as? UiState.Success)?.data
                            ?.indexOfFirst { it.uid == currentUid }
                            ?.takeIf { it >= 0 }
                            ?.plus(1)
                            ?: existingAttempt?.rank?.toInt()
                            ?: 1
                        _quizSession.value = QuizSessionState(
                            active = true,
                            practiceMode = false,
                            loading = false,
                            tournament = tournament,
                            selectedAnswers = answersMap,
                            elapsedMillis = existingAttempt?.durationMillis ?: 0L,
                            remainingMillis = (DailyExamRules.TIME_LIMIT_MILLIS - (existingAttempt?.durationMillis ?: 0L)).coerceAtLeast(0L),
                            submittedSummary = existingAttempt?.let {
                                TournamentSubmitSummary(
                                    score = it.score,
                                    correctCount = it.correctCount.toInt(),
                                    wrongCount = it.wrongCount.toInt(),
                                    unansweredCount = it.unansweredCount.toInt(),
                                    totalQuestions = it.totalQuestions.toInt(),
                                    passed = it.passed || DailyExamRules.isPassed(it.score),
                                    rank = liveRank,
                                    durationMillis = it.durationMillis,
                                    pointsEarned = it.pointsEarned,
                                    streakAfter = (studentProfileState.value as? UiState.Success)?.data?.currentStreak ?: 1L
                                )
                            }
                        )
                    } else {
                        _quizSession.value = QuizSessionState(
                            active = true,
                            practiceMode = false,
                            loading = false,
                            tournament = tournament,
                            remainingMillis = DailyExamRules.TIME_LIMIT_MILLIS
                        )
                        startExamTimer(isOfficialCountdown = true)
                    }
                },
                onFailure = { error ->
                    _quizSession.value = QuizSessionState(
                        active = true,
                        practiceMode = false,
                        loading = false,
                        errorMessage = error.message ?: "இன்றைய 50 வினா தமிழ் AI தேர்வை ஏற்ற முடியவில்லை."
                    )
                }
            )
        }
    }

    fun launchPracticeSprint(category: String) {
        val grade = (studentProfileState.value as? UiState.Success)?.data?.gradeLevel ?: "அனைத்து வயது"
        timerJob?.cancel()
        viewModelScope.launch {
            _quizSession.value = QuizSessionState(
                active = true,
                practiceMode = true,
                loading = true
            )
            val result = aiRepository.generatePracticeSprint(category, grade)
            result.fold(
                onSuccess = { quiz ->
                    _quizSession.value = QuizSessionState(
                        active = true,
                        practiceMode = true,
                        loading = false,
                        tournament = quiz
                    )
                    startExamTimer(isOfficialCountdown = false)
                },
                onFailure = { error ->
                    _quizSession.value = QuizSessionState(
                        active = true,
                        practiceMode = true,
                        loading = false,
                        errorMessage = error.message ?: "பயிற்சி வினாக்களை உருவாக்க முடியவில்லை."
                    )
                }
            )
        }
    }

    private fun startExamTimer(isOfficialCountdown: Boolean) {
        timerJob?.cancel()
        sprintStartMarkMillis = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - sprintStartMarkMillis).coerceAtLeast(0L)
                if (isOfficialCountdown) {
                    val remaining = (DailyExamRules.TIME_LIMIT_MILLIS - elapsed).coerceAtLeast(0L)
                    _quizSession.value = _quizSession.value.copy(
                        elapsedMillis = elapsed.coerceAtMost(DailyExamRules.TIME_LIMIT_MILLIS),
                        remainingMillis = remaining
                    )
                    if (remaining == 0L) {
                        submitCurrentQuiz(timedOut = true)
                        break
                    }
                } else {
                    _quizSession.value = _quizSession.value.copy(elapsedMillis = elapsed)
                }
                delay(200L)
            }
        }
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _quizSession.value
        if (current.submittedSummary != null || current.submitting) return
        val updated = current.selectedAnswers.toMutableMap()
        // Tapping an already-selected option clears it so students can skip to avoid negative marks
        if (updated[questionIndex] == optionIndex) {
            updated.remove(questionIndex)
        } else {
            updated[questionIndex] = optionIndex
        }
        _quizSession.value = current.copy(selectedAnswers = updated)
    }

    fun clearAnswer(questionIndex: Int) {
        val current = _quizSession.value
        if (current.submittedSummary != null || current.submitting) return
        if (!current.selectedAnswers.containsKey(questionIndex)) return
        val updated = current.selectedAnswers.toMutableMap()
        updated.remove(questionIndex)
        _quizSession.value = current.copy(selectedAnswers = updated)
    }

    fun navigateQuestion(index: Int) {
        val current = _quizSession.value
        val maxIdx = (current.tournament?.questions?.size ?: 1) - 1
        _quizSession.value = current.copy(currentIndex = index.coerceIn(0, maxIdx.coerceAtLeast(0)))
    }

    fun submitCurrentQuiz(timedOut: Boolean = false) {
        val current = _quizSession.value
        val tournament = current.tournament ?: return
        if (current.submitting || current.submittedSummary != null) return

        timerJob?.cancel()
        val rawElapsed = (System.currentTimeMillis() - sprintStartMarkMillis).coerceAtLeast(1000L)
        val finalDuration = if (current.practiceMode) rawElapsed
        else rawElapsed.coerceAtMost(DailyExamRules.TIME_LIMIT_MILLIS)

        val answersList = tournament.questions.indices.map { idx ->
            current.selectedAnswers[idx] ?: -1
        }

        if (current.practiceMode) {
            var correct = 0
            var wrong = 0
            var unanswered = 0
            tournament.questions.forEachIndexed { idx, q ->
                val chosen = answersList[idx]
                when {
                    chosen < 0 -> unanswered++
                    chosen == q.correctIndex -> correct++
                    else -> wrong++
                }
            }
            _quizSession.value = current.copy(
                elapsedMillis = finalDuration,
                submittedSummary = TournamentSubmitSummary(
                    score = correct.toDouble(),
                    correctCount = correct,
                    wrongCount = wrong,
                    unansweredCount = unanswered,
                    totalQuestions = tournament.questions.size,
                    passed = correct >= (tournament.questions.size * 0.7).toInt(),
                    rank = 1,
                    durationMillis = finalDuration,
                    pointsEarned = 0L,
                    streakAfter = 0L,
                    timedOut = false
                )
            )
            return
        }

        val profile = (studentProfileState.value as? UiState.Success)?.data
        if (profile == null) {
            _quizSession.value = current.copy(
                errorMessage = "மாணவர் சுயவிவரம் ஏற்றப்படவில்லை. முதலில் உங்கள் மாணவர் ID-ஐச் சரிபார்க்கவும்."
            )
            return
        }

        viewModelScope.launch {
            _quizSession.value = current.copy(
                submitting = true,
                elapsedMillis = finalDuration,
                remainingMillis = (DailyExamRules.TIME_LIMIT_MILLIS - finalDuration).coerceAtLeast(0L),
                errorMessage = null
            )
            val result = repository.submitOfficialTournament(
                student = profile,
                tournament = tournament,
                selectedAnswers = answersList,
                durationMillis = finalDuration,
                timedOut = timedOut
            )
            result.fold(
                onSuccess = { summary ->
                    _quizSession.value = _quizSession.value.copy(
                        submitting = false,
                        submittedSummary = summary
                    )
                },
                onFailure = { error ->
                    _quizSession.value = _quizSession.value.copy(
                        submitting = false,
                        errorMessage = error.message ?: "தேர்வு முடிவைச் சமர்ப்பிக்க முடியவில்லை."
                    )
                }
            )
        }
    }

    fun closeQuizSession() {
        timerJob?.cancel()
        _quizSession.value = QuizSessionState()
    }

    // --- AI STUDY COACH (STREAMING) ---

    fun generateAiStudyCoach() {
        val student = (studentProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _coachState.value = CoachUiState.Loading
            var lastText = ""
            try {
                aiRepository.streamStudyCoach(student).collect { text ->
                    lastText = text
                    _coachState.value = CoachUiState.Streaming(text)
                }
                if (lastText.isNotBlank()) {
                    _coachState.value = CoachUiState.Success(lastText)
                } else {
                    _coachState.value = CoachUiState.Error("வழிகாட்டித் தகவல் கிடைக்கவில்லை. மீண்டும் முயற்சிக்கவும்.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _coachState.value = CoachUiState.Error(
                    e.localizedMessage ?: "AI படிப்பு வழிகாட்டியை உருவாக்க முடியவில்லை."
                )
            }
        }
    }

    // --- REWARDS & OPT-IN REWARDED AD POINTS ---

    fun onRewardedAdEarned() {
        viewModelScope.launch {
            val result = repository.creditRewardedAdBonus(todayDateKey)
            result.fold(
                onSuccess = { newBalance ->
                    _rewardFeedback.value = RewardFeedback(
                        message = "+25 போனஸ் புள்ளிகள் சேர்க்கப்பட்டன! தற்போதைய இருப்பு: $newBalance புள்ளிகள்"
                    )
                },
                onFailure = { error ->
                    _rewardFeedback.value = RewardFeedback(
                        message = error.message ?: "விளம்பரப் போனஸ் புள்ளிகளைச் சேர்க்க முடியவில்லை.",
                        isError = true
                    )
                }
            )
        }
    }

    fun claimReward(item: RewardCatalogItem) {
        val monthlyList = (monthlyLeaderboardState.value as? UiState.Success)?.data.orEmpty()
        val myMonthlyRank = monthlyList.indexOfFirst { it.uid == currentUid }
            .takeIf { it >= 0 }
            ?.plus(1)

        viewModelScope.launch {
            _claimingRewardId.value = item.id
            val result = repository.claimCatalogReward(item, currentMonthKey, myMonthlyRank)
            _claimingRewardId.value = null
            result.fold(
                onSuccess = { claim ->
                    _rewardFeedback.value = RewardFeedback(
                        message = "${claim.rewardTitle} வெற்றிகரமாகப் பெறப்பட்டது! குறியீடு: ${claim.claimCode}",
                        recentClaim = claim
                    )
                },
                onFailure = { error ->
                    _rewardFeedback.value = RewardFeedback(
                        message = error.message ?: "பரிசைப் பெற முடியவில்லை.",
                        isError = true
                    )
                }
            )
        }
    }

    fun dismissRewardFeedback() {
        _rewardFeedback.value = null
    }

    private fun <T> unwrapFirestoreFlowError(error: Throwable, fallback: String): UiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        return UiState.Error(rootError.message ?: fallback)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(
        private val context: Context,
        private val uid: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GKLeagueViewModel(
                currentUid = uid,
                repository = GKLeagueRepository(context),
                aiRepository = GeminiAiRepository()
            ) as T
        }
    }
}
