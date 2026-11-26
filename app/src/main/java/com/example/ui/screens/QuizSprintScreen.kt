package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DailyExamRules
import com.example.data.LeagueTimeUtils
import com.example.ui.theme.ArenaNavyDeep
import com.example.viewmodel.QuizSessionState

@Composable
fun QuizSprintScreen(
    session: QuizSessionState,
    onSelectAnswer: (questionIndex: Int, optionIndex: Int) -> Unit,
    onClearAnswer: (questionIndex: Int) -> Unit,
    onNavigateQuestion: (Int) -> Unit,
    onSubmit: () -> Unit,
    onRetryLoad: () -> Unit,
    onClose: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    BackHandler(onBack = onClose)

    // நேரத் தேர்வின் நேர்மையைக் காக்க விளம்பரமில்லா முழுத்திரை அமைப்பு
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            session.loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.5.dp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = if (session.practiceMode) "AI பயிற்சி வினாக்கள் உருவாகின்றன…"
                        else "இன்றைய 50 வினா தமிழ் GK தேர்வு தயாராகிறது…",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (session.practiceMode) {
                            "உங்கள் திறனை மேம்படுத்த 6 தமிழ் பயிற்சி வினாக்களை Gemini AI உருவாக்குகிறது."
                        } else {
                            "50 வினாக்கள் • 25 நிமிட நேரம் • தேர்ச்சி மதிப்பெண் 35 • தவறான விடைக்கு -0.25 எதிர்மறை மதிப்பெண்."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            session.errorMessage != null && session.tournament == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 520.dp)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "வினாத்தாளை ஏற்ற முடியவில்லை",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = session.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onClose,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.testTag("quiz_error_back_button")
                        ) {
                            Text("போட்டிப் பக்கம்")
                        }
                        Button(
                            onClick = onRetryLoad,
                            modifier = Modifier.testTag("quiz_error_retry_button")
                        ) {
                            Text("மீண்டும் முயற்சி")
                        }
                    }
                }
            }

            session.submittedSummary != null && session.tournament != null -> {
                QuizResultsAndExplanations(
                    session = session,
                    onClose = onClose,
                    onOpenLeaderboard = onOpenLeaderboard
                )
            }

            session.tournament != null -> {
                ActiveQuizSprintContent(
                    session = session,
                    onSelectAnswer = onSelectAnswer,
                    onClearAnswer = onClearAnswer,
                    onNavigateQuestion = onNavigateQuestion,
                    onSubmit = onSubmit,
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun ActiveQuizSprintContent(
    session: QuizSessionState,
    onSelectAnswer: (Int, Int) -> Unit,
    onClearAnswer: (Int) -> Unit,
    onNavigateQuestion: (Int) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit
) {
    val tournament = session.tournament ?: return
    val questions = tournament.questions
    val currentQuestion = questions.getOrNull(session.currentIndex) ?: return
    val totalCount = questions.size
    val answeredCount = session.selectedAnswers.size
    val skippedCount = (totalCount - answeredCount).coerceAtLeast(0)
    val progress = if (totalCount > 0) (session.currentIndex + 1).toFloat() / totalCount.toFloat() else 0f

    var showQuestionPalette by remember { mutableStateOf(false) }
    var showSubmitConfirm by remember { mutableStateOf(false) }

    val isLowTime = !session.practiceMode && session.remainingMillis < 180_000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // மேல் பட்டி: வெளியேறு + தலைப்பு + 25:00 நேரக் கடிகாரம்
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("quiz_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "தேர்விலிருந்து வெளியேறு",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = if (session.practiceMode) "AI பயிற்சி வினாக்கள்" else "தினசரி நேரத் தேர்வு",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "வினா ${session.currentIndex + 1} / $totalCount • $answeredCount பதிலளித்தவை",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = if (isLowTime) MaterialTheme.colorScheme.error else ArenaNavyDeep,
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("quiz_stopwatch_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "மீதமுள்ள நேரம்",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (session.practiceMode) {
                            LeagueTimeUtils.formatStopwatch(session.elapsedMillis)
                        } else {
                            DailyExamRules.formatCountdown(session.remainingMillis)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
        )

        // எதிர்மறை மதிப்பெண் & தேர்ச்சி விதிமுறைப் பட்டை
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Rule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (session.practiceMode) {
                        "AI பயிற்சி — எதிர்மறை மதிப்பெண் இல்லை • சுதந்திரமாகக் கற்றுக்கொள்ளுங்கள்!"
                    } else {
                        "சரி: +1 • தவறு: -0.25 • தவிர்த்தல்: 0 • தேர்ச்சி மதிப்பெண்: 35 / $totalCount"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // வினா எண் பட்டியல் (1-50) & முன்கூட்டிய சமர்ப்பிப்பு
        if (!session.practiceMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showQuestionPalette = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .testTag("quiz_palette_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "வினா பட்டியல் ($answeredCount/$totalCount)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                OutlinedButton(
                    onClick = { showSubmitConfirm = true },
                    enabled = !session.submitting,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .testTag("quiz_early_submit_button")
                ) {
                    Text(
                        text = "சமர்ப்பி",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // வினா மற்றும் விருப்பத் தேர்வுகள்
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = ArenaNavyDeep,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentQuestion.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = currentQuestion.difficulty,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${session.currentIndex + 1}. ${currentQuestion.prompt}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(18.dp)
                )
            }

            val selectedIdx = session.selectedAnswers[session.currentIndex]
            val optionLetters = listOf("அ", "ஆ", "இ", "ஈ")

            currentQuestion.options.forEachIndexed { optIdx, optionText ->
                val isSelected = selectedIdx == optIdx
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.surface,
                    animationSpec = spring(),
                    label = "optionBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = spring(),
                    label = "optionBorder"
                )

                Surface(
                    onClick = { onSelectAnswer(session.currentIndex, optIdx) },
                    shape = RoundedCornerShape(16.dp),
                    color = bgColor,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("quiz_option_$optIdx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = if (isSelected) ArenaNavyDeep
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = optionLetters.getOrElse(optIdx) { "${optIdx + 1}" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) Color.White
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (selectedIdx != null && !session.practiceMode) {
                TextButton(
                    onClick = { onClearAnswer(session.currentIndex) },
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("quiz_clear_answer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RemoveCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "விடையை நீக்கு (தவிர்த்தல் — 0 மதிப்பெண்)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (session.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = session.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // கீழ் வழிசெலுத்தல் & சமர்ப்பிப்பு பொத்தான்கள்
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onNavigateQuestion(session.currentIndex - 1) },
                enabled = session.currentIndex > 0 && !session.submitting,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .testTag("quiz_prev_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "முந்தைய வினா")
            }

            if (session.currentIndex < totalCount - 1) {
                Button(
                    onClick = { onNavigateQuestion(session.currentIndex + 1) },
                    enabled = !session.submitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArenaNavyDeep,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("quiz_next_button")
                ) {
                    Text("அடுத்த வினா", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            } else {
                Button(
                    onClick = {
                        if (session.practiceMode) onSubmit()
                        else showSubmitConfirm = true
                    },
                    enabled = !session.submitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArenaNavyDeep,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("quiz_submit_button")
                ) {
                    if (session.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("தேர்வு பதிவாகிறது…")
                    } else {
                        Icon(Icons.Filled.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (session.practiceMode) "பயிற்சியை முடிக்கவும்"
                            else "தேர்வைச் சமர்ப்பிக்கவும்",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    // 50 வினா எண் பட்டியல் சாளரம்
    if (showQuestionPalette) {
        AlertDialog(
            onDismissRequest = { showQuestionPalette = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("வினா எண் பட்டியல் (1 – $totalCount)")
                    Text(
                        text = "பதிலளித்தவை: $answeredCount • தவிர்த்தவை: $skippedCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(totalCount) { idx ->
                        val isAnswered = session.selectedAnswers.containsKey(idx)
                        val isCurrent = idx == session.currentIndex
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isAnswered -> ArenaNavyDeep
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    onNavigateQuestion(idx)
                                    showQuestionPalette = false
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isCurrent || isAnswered) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAnswered) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuestionPalette = false }) {
                    Text("மூடு")
                }
            }
        )
    }

    // சமர்ப்பிப்பு உறுதிப்படுத்தல் சாளரம்
    if (showSubmitConfirm) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("தேர்வைச் சமர்ப்பிக்கவா?") },
            text = {
                Text(
                    text = "பதிலளித்த வினாக்கள்: $answeredCount / $totalCount\n" +
                        "தவிர்த்த வினாக்கள்: $skippedCount (0 மதிப்பெண்)\n\n" +
                        "சரி: +1 • தவறு: -0.25 • தேர்ச்சி: 35 மதிப்பெண். சமர்ப்பித்த பின் விடைகளை மாற்ற முடியாது."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirm = false
                        onSubmit()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArenaNavyDeep,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_submit_exam_button")
                ) {
                    Text("ஆம், சமர்ப்பி")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirm = false }) {
                    Text("தொடர்ந்து எழுது")
                }
            }
        )
    }
}

@Composable
private fun QuizResultsAndExplanations(
    session: QuizSessionState,
    onClose: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val summary = session.submittedSummary ?: return
    val tournament = session.tournament ?: return
    val penaltyText = DailyExamRules.formatScore(summary.wrongCount * DailyExamRules.WRONG_PENALTY)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaNavyDeep),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_results_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )

                    if (!session.practiceMode) {
                        Surface(
                            color = if (summary.passed) Color.White else MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (summary.passed) "தேர்ச்சி (PASS • ≥ 35)"
                                else "தோல்வி (FAIL • தேர்ச்சி: 35)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.passed) ArenaNavyDeep else Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = if (session.practiceMode) "பயிற்சி நிறைவடைந்தது!"
                        else if (summary.timedOut) "நேரம் முடிந்து தேர்வு பதிவானது!"
                        else "தினசரி தேர்வு பதிவானது!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "${DailyExamRules.formatScore(summary.score)} / ${summary.totalQuestions}",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )

                    // சரி / தவறு / தவிர்த்தவை சுருக்கம்
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "சரி: ${summary.correctCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (session.practiceMode) "தவறு: ${summary.wrongCount}"
                                else "தவறு: ${summary.wrongCount} (-$penaltyText)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "தவிர்த்தவை: ${summary.unansweredCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "நேரம்: ${LeagueTimeUtils.formatStopwatch(summary.durationMillis)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        if (!session.practiceMode) {
                            Surface(
                                color = if (summary.passed) Color.White else Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (summary.passed) "இடம் #${summary.rank} • +${summary.pointsEarned} புள்ளி"
                                    else "0 புள்ளி (தோல்வி)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (summary.passed) ArenaNavyDeep else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("results_back_arena_button")
                        ) {
                            Text("போட்டிப் பக்கம்")
                        }
                        if (!session.practiceMode) {
                            Button(
                                onClick = {
                                    onClose()
                                    onOpenLeaderboard()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = ArenaNavyDeep
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("results_view_leaderboard_button")
                            ) {
                                Text("தரவரிசைப் பட்டியல்", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "விடைகள் மற்றும் விளக்கங்கள் (${tournament.questions.size} வினாக்கள்)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        itemsIndexed(tournament.questions) { idx, question ->
            val chosen = session.selectedAnswers[idx] ?: -1
            val isSkipped = chosen < 0
            val isRight = chosen == question.correctIndex
            val markTag = when {
                isSkipped -> "தவிர்த்தது (0)"
                isRight -> "சரி (+1)"
                session.practiceMode -> "தவறு"
                else -> "தவறு (-0.25)"
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "வினா ${idx + 1} • ${question.category}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = markTag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isRight -> MaterialTheme.colorScheme.primary
                                    isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            Icon(
                                imageVector = when {
                                    isRight -> Icons.Filled.CheckCircle
                                    isSkipped -> Icons.Filled.RemoveCircleOutline
                                    else -> Icons.Filled.Cancel
                                },
                                contentDescription = markTag,
                                tint = when {
                                    isRight -> MaterialTheme.colorScheme.primary
                                    isSkipped -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = question.prompt,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isSkipped && !isRight && chosen in question.options.indices) {
                        Text(
                            text = "உங்கள் விடை: ${question.options[chosen]}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "சரியான விடை: ${question.options.getOrElse(question.correctIndex) { "" }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (question.explanation.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
