package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.BannerAdSlot
import com.example.data.DailyExamRules
import com.example.data.LeagueTimeUtils
import com.example.data.QuizAttempt
import com.example.data.RewardCatalog
import com.example.data.StudentProfile
import com.example.data.TournamentSubmission
import com.example.ui.theme.ArenaNavyDeep

@Composable
fun ArenaScreen(
    student: StudentProfile,
    todayDateKey: String,
    todayAttempt: QuizAttempt?,
    dailyLeaderboard: List<TournamentSubmission>,
    onStartDailyTournament: () -> Unit,
    onReviewDailyResults: () -> Unit,
    onStartPracticeCategory: (String) -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val myRank = dailyLeaderboard.indexOfFirst { it.uid == student.uid }
        .takeIf { it >= 0 }
        ?.plus(1)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Verified Student Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "வணக்கம், ${student.displayName.substringBefore(" ")}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "சரிபார்க்கப்பட்ட மாணவர்",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Text(
                            text = "${student.schoolName} • ID ${student.studentIdNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("arena_points_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "புள்ளிகள்",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${student.rewardPoints} புள்ளி",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Hero Daily Test Card (50 Questions • 25 Mins • Negative Marking • Pass 35)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaNavyDeep),
                    border = BorderStroke(1.dp, ArenaNavyDeep),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_tournament_hero_card")
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_arena_hero_bw),
                            contentDescription = "தினசரி தமிழ் GK தேர்வு",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(274.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(274.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ArenaNavyDeep.copy(alpha = 0.45f),
                                            ArenaNavyDeep.copy(alpha = 0.95f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "தினசரி தேர்வு • $todayDateKey",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ArenaNavyDeep,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${dailyLeaderboard.size} பேர் எழுதினர்",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "இன்றைய 50 வினா நேரத் தேர்வு",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "50 வினாக்கள் • 25 நிமிடம் • தேர்ச்சி மதிப்பெண்: 35 • தவறான விடைக்கு -0.25 எதிர்மறை மதிப்பெண்",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (todayAttempt != null) {
                                val attemptPassed = todayAttempt.passed || DailyExamRules.isPassed(todayAttempt.score)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                            Column {
                                                Text(
                                                    text = "${DailyExamRules.formatScore(todayAttempt.score)}/${todayAttempt.totalQuestions} • ${if (attemptPassed) "தேர்ச்சி" else "தோல்வி"}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${myRank?.let { "இடம் #$it • " } ?: ""}+${todayAttempt.pointsEarned} புள்ளி",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = onReviewDailyResults,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = ArenaNavyDeep
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 50.dp)
                                            .testTag("review_daily_results_button")
                                    ) {
                                        Text("விடைகள்", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = onStartDailyTournament,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = ArenaNavyDeep
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 54.dp)
                                        .testTag("start_daily_tournament_button")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.arena_start_tournament),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rank-Based Daily Reward Tiers Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_reward_tiers_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "தினசரி தரவரிசைப் பரிசுப் புள்ளிகள்",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "50 வினா • 25 நிமிடம் • தேர்ச்சி: 35 • தவறான விடை: -0.25",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RewardTierRow(rankLabel = "இடம் 1 – 10 (தேர்ச்சி)", pointsLabel = "50 புள்ளிகள்", highlighted = true)
                        RewardTierRow(rankLabel = "இடம் 11 – 20 (தேர்ச்சி)", pointsLabel = "20 புள்ளிகள்")
                        RewardTierRow(rankLabel = "இடம் 21 – 30 (தேர்ச்சி)", pointsLabel = "10 புள்ளிகள்")
                        RewardTierRow(rankLabel = "பிற தேர்ச்சியாளர்கள் (31+)", pointsLabel = "5 புள்ளிகள்")
                        RewardTierRow(rankLabel = "தோல்வி (35-க்கு கீழ்)", pointsLabel = "0 புள்ளிகள்", isFail = true)
                    }
                }
            }

            // Quick Pulse Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ArenaStatPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.LocalFireDepartment,
                        label = "தொடர் நாட்கள்",
                        value = "${student.currentStreak} நாள்"
                    )
                    ArenaStatPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Timer,
                        label = "இன்றைய இடம்",
                        value = myRank?.let { "#$it" } ?: "—"
                    )
                    ArenaStatPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.School,
                        label = "தேர்வுகள்",
                        value = "${student.tournamentsCompleted}"
                    )
                }
            }

            // Today's Top Rankers Snapshot
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "இன்றைய முன்னணி பட்டியல்",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedButton(
                                onClick = onOpenLeaderboard,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("arena_view_full_board_button")
                            ) {
                                Text("முழு பட்டியல்", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (dailyLeaderboard.isEmpty()) {
                            Text(
                                text = "இன்று இன்னும் யாரும் சமர்ப்பிக்கவில்லை — 35+ மதிப்பெண் எடுத்து முதல் 10 இடத்தில் 50 புள்ளிகள் வெல்லுங்கள்!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            dailyLeaderboard.take(3).forEachIndexed { idx, entry ->
                                val passed = entry.passed || DailyExamRules.isPassed(entry.score)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = if (idx == 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceContainer,
                                            shape = CircleShape,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${idx + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (idx == 0) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = entry.studentName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${if (passed) "தேர்ச்சி" else "தோல்வி"} • ${LeagueTimeUtils.formatStopwatch(entry.durationMillis)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (passed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${DailyExamRules.formatScore(entry.score)}/${entry.totalQuestions}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Practice Sprints Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.arena_practice_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "புதிய Gemini AI தமிழ் வினாக்களுடன் எந்த நேரமும் பயிற்சி செய்யுங்கள் (எதிர்மறை மதிப்பெண் இல்லை).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(RewardCatalog.practiceCategories) { idx, category ->
                            Card(
                                onClick = { onStartPracticeCategory(category) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .width(184.dp)
                                    .testTag("practice_card_$idx")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "6 வினா • AI பயிற்சி",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = "தொடங்குக →",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Student-Safe Banner Ad Slot
            item {
                BannerAdSlot()
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun RewardTierRow(
    rankLabel: String,
    pointsLabel: String,
    highlighted: Boolean = false,
    isFail: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rankLabel,
            style = MaterialTheme.typography.bodySmall,
            color = if (isFail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Surface(
            color = when {
                highlighted -> ArenaNavyDeep
                isFail -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceContainer
            },
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = pointsLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    highlighted -> Color.White
                    isFail -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun ArenaStatPill(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
