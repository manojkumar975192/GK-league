package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.AdMobInitializer
import com.example.ads.RewardedAdController
import com.example.data.configureAppCheck
import com.example.data.signOutUser
import com.example.ui.screens.ArenaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.IdVerificationScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.QuizSprintScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.GKLeagueTheme
import com.example.viewmodel.GKLeagueViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent BEFORE UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Initialize student-safe Google Mobile Ads SDK
        AdMobInitializer.initialize(this)

        // STEP 3: Full edge-to-edge display
        enableEdgeToEdge()

        setContent {
            GKLeagueTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GKLeagueRoot()
                }
            }
        }
    }
}

private data class NavDestination(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_DESTINATIONS = listOf(
    NavDestination(R.string.nav_arena, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "nav_tab_arena"),
    NavDestination(R.string.nav_ranks, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_ranks"),
    NavDestination(R.string.nav_stats, Icons.Filled.Insights, Icons.Outlined.Insights, "nav_tab_stats"),
    NavDestination(R.string.nav_rewards, Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard, "nav_tab_rewards")
)

@Composable
private fun GKLeagueRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedGKLeagueApp(
                uid = uid,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedGKLeagueApp(
    uid: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val rewardedAdController = remember(context) { RewardedAdController(context) }

    val viewModel: GKLeagueViewModel = viewModel(
        key = uid,
        factory = GKLeagueViewModel.Factory(context, uid)
    )

    val profileState by viewModel.studentProfileState.collectAsStateWithLifecycle()
    val dailyLeaderboardState by viewModel.dailyLeaderboardState.collectAsStateWithLifecycle()
    val monthlyLeaderboardState by viewModel.monthlyLeaderboardState.collectAsStateWithLifecycle()
    val todayAttemptState by viewModel.todayAttemptState.collectAsStateWithLifecycle()
    val rewardClaimsState by viewModel.rewardClaimsState.collectAsStateWithLifecycle()
    val idScanState by viewModel.idScanState.collectAsStateWithLifecycle()
    val quizSession by viewModel.quizSession.collectAsStateWithLifecycle()
    val coachState by viewModel.coachState.collectAsStateWithLifecycle()
    val rewardFeedback by viewModel.rewardFeedback.collectAsStateWithLifecycle()
    val claimingRewardId by viewModel.claimingRewardId.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingStudentId by rememberSaveable { mutableStateOf(false) }

    val triggerSignOut: () -> Unit = {
        signOutUser(
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = onSignedOut
        )
    }

    when (val state = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "உங்கள் மாணவர் சுயவிவரம் ஏற்றப்படுகிறது…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "மாணவர் சுயவிவரத்தை ஏற்ற முடியவில்லை",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = triggerSignOut) {
                        Text("வெளியேறி மீண்டும் முயற்சி")
                    }
                }
            }
        }

        is UiState.Success -> {
            val student = state.data

            when {
                student == null || editingStudentId -> {
                    IdVerificationScreen(
                        existingProfile = student,
                        scanState = idScanState,
                        onScanBitmap = { bmp -> viewModel.scanStudentIdBitmap(bmp) },
                        onConfirmProfile = { name, school, idNum, grade ->
                            viewModel.confirmAndSaveStudentId(
                                displayName = name,
                                schoolName = school,
                                studentIdNumber = idNum,
                                gradeLevel = grade,
                                isUpdate = student != null,
                                onSaved = { editingStudentId = false }
                            )
                        },
                        onBack = if (student != null) {
                            {
                                viewModel.resetIdScanState()
                                editingStudentId = false
                            }
                        } else null,
                        onSignOut = triggerSignOut
                    )
                }

                quizSession.active -> {
                    QuizSprintScreen(
                        session = quizSession,
                        onSelectAnswer = { qIdx, optIdx -> viewModel.selectAnswer(qIdx, optIdx) },
                        onClearAnswer = { qIdx -> viewModel.clearAnswer(qIdx) },
                        onNavigateQuestion = { idx -> viewModel.navigateQuestion(idx) },
                        onSubmit = { viewModel.submitCurrentQuiz() },
                        onRetryLoad = {
                            if (quizSession.practiceMode) {
                                viewModel.launchPracticeSprint("தமிழ் இலக்கியம் & திருக்குறள்")
                            } else {
                                viewModel.launchDailyTournament(reviewOnly = false)
                            }
                        },
                        onClose = { viewModel.closeQuizSession() },
                        onOpenLeaderboard = { selectedTab = 1 }
                    )
                }

                else -> {
                    val dailyList = (dailyLeaderboardState as? UiState.Success)?.data.orEmpty()
                    val monthlyList = (monthlyLeaderboardState as? UiState.Success)?.data.orEmpty()
                    val todayAttempt = (todayAttemptState as? UiState.Success)?.data

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useRail = maxWidth >= 600.dp

                        val tabContent: @Composable (Modifier) -> Unit = { contentModifier ->
                            Box(modifier = contentModifier) {
                                when (selectedTab) {
                                    0 -> ArenaScreen(
                                        student = student,
                                        todayDateKey = viewModel.todayDateKey,
                                        todayAttempt = todayAttempt,
                                        dailyLeaderboard = dailyList,
                                        onStartDailyTournament = { viewModel.launchDailyTournament(reviewOnly = false) },
                                        onReviewDailyResults = { viewModel.launchDailyTournament(reviewOnly = true) },
                                        onStartPracticeCategory = { cat -> viewModel.launchPracticeSprint(cat) },
                                        onOpenLeaderboard = { selectedTab = 1 }
                                    )
                                    1 -> LeaderboardScreen(
                                        currentUid = uid,
                                        todayDateKey = viewModel.todayDateKey,
                                        dailyState = dailyLeaderboardState,
                                        monthlyState = monthlyLeaderboardState
                                    )
                                    2 -> StatsScreen(
                                        student = student,
                                        coachState = coachState,
                                        onRequestAiCoach = { viewModel.generateAiStudyCoach() },
                                        onUpdateStudentId = {
                                            viewModel.resetIdScanState()
                                            editingStudentId = true
                                        },
                                        onSignOut = triggerSignOut
                                    )
                                    3 -> RewardsScreen(
                                        student = student,
                                        todayDateKey = viewModel.todayDateKey,
                                        monthlyLeaderboard = monthlyList,
                                        claimsState = rewardClaimsState,
                                        claimingRewardId = claimingRewardId,
                                        rewardFeedback = rewardFeedback,
                                        rewardedAdController = rewardedAdController,
                                        onRewardedAdEarned = { viewModel.onRewardedAdEarned() },
                                        onClaimReward = { item -> viewModel.claimReward(item) },
                                        onDismissFeedback = { viewModel.dismissRewardFeedback() }
                                    )
                                }
                            }
                        }

                        if (useRail) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                            ) {
                                NavigationRail(
                                    modifier = Modifier.fillMaxHeight(),
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    NAV_DESTINATIONS.forEachIndexed { idx, dest ->
                                        NavigationRailItem(
                                            selected = selectedTab == idx,
                                            onClick = { selectedTab = idx },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selectedTab == idx) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = stringResource(dest.labelRes)
                                                )
                                            },
                                            label = { Text(stringResource(dest.labelRes)) },
                                            modifier = Modifier.testTag(dest.testTag)
                                        )
                                    }
                                }
                                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                tabContent(Modifier.weight(1f).fillMaxHeight())
                            }
                        } else {
                            Scaffold(
                                contentWindowInsets = WindowInsets.safeDrawing,
                                bottomBar = {
                                    Column {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        NavigationBar(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 0.dp
                                        ) {
                                            NAV_DESTINATIONS.forEachIndexed { idx, dest ->
                                                NavigationBarItem(
                                                    selected = selectedTab == idx,
                                                    onClick = { selectedTab = idx },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    icon = {
                                                        Icon(
                                                            imageVector = if (selectedTab == idx) dest.selectedIcon else dest.unselectedIcon,
                                                            contentDescription = stringResource(dest.labelRes)
                                                        )
                                                    },
                                                    label = { Text(stringResource(dest.labelRes)) },
                                                    modifier = Modifier.testTag(dest.testTag)
                                                )
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                tabContent(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
