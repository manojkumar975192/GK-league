package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ads.BannerAdSlot
import com.example.data.DailyExamRules
import com.example.data.LeagueTimeUtils
import com.example.data.MonthlyRanking
import com.example.data.TournamentSubmission
import com.example.ui.theme.ArenaNavyDeep
import com.example.ui.theme.PodiumBronze
import com.example.ui.theme.PodiumSilver
import com.example.viewmodel.UiState

@Composable
fun LeaderboardScreen(
    currentUid: String,
    todayDateKey: String,
    dailyState: UiState<List<TournamentSubmission>>,
    monthlyState: UiState<List<MonthlyRanking>>
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val monthTitle = LeagueTimeUtils.prettyCurrentMonth()

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "தரவரிசைப் பட்டியல்",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (selectedTab == 0) "தினசரி 50 வினா தேர்வு • $todayDateKey • தேர்ச்சி: 35"
                        else "$monthTitle சாம்பியன்ஷிப் • முதல் 3 இடங்களுக்கு வெற்றியாளர் சான்றிதழ்",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                val segColors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ArenaNavyDeep,
                    activeContentColor = Color.White,
                    activeBorderColor = ArenaNavyDeep,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        colors = segColors,
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("leaderboard_tab_daily")
                    ) {
                        Text(
                            text = stringResource(R.string.leaderboard_daily),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        colors = segColors,
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("leaderboard_tab_monthly")
                    ) {
                        Text(
                            text = stringResource(R.string.leaderboard_monthly),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Bolt else Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (selectedTab == 0) {
                                "பரிசு விதி (தேர்ச்சி ≥ 35): 1–10 இடம் = 50 புள்ளி • 11–20 = 20 புள்ளி • 21–30 = 10 புள்ளி • பிற தேர்ச்சி = 5 புள்ளி • தோல்வி = 0 புள்ளி."
                            } else {
                                "மாதாந்திர வெற்றியாளர்கள்: $monthTitle மாதத்தில் தினசரி தரவரிசைப் புள்ளிகளில் முதல் 3 இடங்களைப் பிடிக்கும் மாணவர்கள் வெற்றியாளர் சான்றிதழ் பெறுவர்!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                when (dailyState) {
                    is UiState.Loading -> {
                        item { LeaderboardLoadingPlaceholder() }
                    }
                    is UiState.Error -> {
                        item { LeaderboardErrorCard(dailyState.message) }
                    }
                    is UiState.Success -> {
                        val entries = dailyState.data
                        if (entries.isEmpty()) {
                            item {
                                LeaderboardEmptyState(
                                    title = "இன்று இதுவரை சமர்ப்பிப்புகள் இல்லை",
                                    subtitle = "போட்டிப் பக்கத்தில் 50 வினா நேரத் தேர்வை எழுதி 35+ மதிப்பெண் பெற்று 50 புள்ளிகள் வெல்லுங்கள்!"
                                )
                            }
                        } else {
                            item {
                                PodiumRow(
                                    first = entries.getOrNull(0)?.let {
                                        PodiumEntry(
                                            it.studentName,
                                            "${DailyExamRules.formatScore(it.score)}/${it.totalQuestions}",
                                            "+${DailyExamRules.rewardPointsForRank(1, it.passed || DailyExamRules.isPassed(it.score))} புள்ளி"
                                        )
                                    },
                                    second = entries.getOrNull(1)?.let {
                                        PodiumEntry(
                                            it.studentName,
                                            "${DailyExamRules.formatScore(it.score)}/${it.totalQuestions}",
                                            "+${DailyExamRules.rewardPointsForRank(2, it.passed || DailyExamRules.isPassed(it.score))} புள்ளி"
                                        )
                                    },
                                    third = entries.getOrNull(2)?.let {
                                        PodiumEntry(
                                            it.studentName,
                                            "${DailyExamRules.formatScore(it.score)}/${it.totalQuestions}",
                                            "+${DailyExamRules.rewardPointsForRank(3, it.passed || DailyExamRules.isPassed(it.score))} புள்ளி"
                                        )
                                    }
                                )
                            }

                            itemsIndexed(entries, key = { _, item -> item.docId.ifBlank { item.uid } }) { idx, item ->
                                val rank = idx + 1
                                val isMe = item.uid == currentUid
                                val passed = item.passed || DailyExamRules.isPassed(item.score)
                                val tierPoints = DailyExamRules.rewardPointsForRank(rank, passed)
                                LeaderboardRowCard(
                                    rank = rank,
                                    name = item.studentName,
                                    school = item.schoolName,
                                    metaLine = "${if (passed) "தேர்ச்சி" else "தோல்வி"} • நேரம் ${LeagueTimeUtils.formatStopwatch(item.durationMillis)}",
                                    primaryMetric = "${DailyExamRules.formatScore(item.score)}/${item.totalQuestions}",
                                    secondaryMetric = if (passed) "+$tierPoints புள்ளி" else "0 புள்ளி",
                                    isCurrentUser = isMe,
                                    isFail = !passed
                                )
                            }
                        }
                    }
                }
            } else {
                when (monthlyState) {
                    is UiState.Loading -> {
                        item { LeaderboardLoadingPlaceholder() }
                    }
                    is UiState.Error -> {
                        item { LeaderboardErrorCard(monthlyState.message) }
                    }
                    is UiState.Success -> {
                        val entries = monthlyState.data
                        if (entries.isEmpty()) {
                            item {
                                LeaderboardEmptyState(
                                    title = "மாதாந்திர போட்டி இப்போது தொடங்கியது",
                                    subtitle = "$monthTitle மாதம் முழுவதும் தினசரி தேர்வுகளில் தேர்ச்சி பெற்று மாதாந்திர வெற்றியாளர் மேடையில் இடம் பிடியுங்கள்!"
                                )
                            }
                        } else {
                            item {
                                PodiumRow(
                                    first = entries.getOrNull(0)?.let {
                                        PodiumEntry(it.studentName, "${it.monthlyScore} புள்ளி", "${it.tournamentsPlayed} தேர்வு")
                                    },
                                    second = entries.getOrNull(1)?.let {
                                        PodiumEntry(it.studentName, "${it.monthlyScore} புள்ளி", "${it.tournamentsPlayed} தேர்வு")
                                    },
                                    third = entries.getOrNull(2)?.let {
                                        PodiumEntry(it.studentName, "${it.monthlyScore} புள்ளி", "${it.tournamentsPlayed} தேர்வு")
                                    }
                                )
                            }

                            itemsIndexed(entries, key = { _, item -> item.docId.ifBlank { item.uid } }) { idx, item ->
                                val isMe = item.uid == currentUid
                                LeaderboardRowCard(
                                    rank = idx + 1,
                                    name = item.studentName,
                                    school = item.schoolName,
                                    metaLine = "${item.tournamentsPlayed} தேர்வுகள் • ${item.gradeLevel}",
                                    primaryMetric = "${item.monthlyScore} புள்ளி",
                                    secondaryMetric = if (idx < 3) "வெற்றியாளர்" else "போட்டியாளர்",
                                    isCurrentUser = isMe
                                )
                            }
                        }
                    }
                }
            }

            item {
                BannerAdSlot()
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

private data class PodiumEntry(
    val name: String,
    val primary: String,
    val secondary: String
)

@Composable
private fun PodiumRow(
    first: PodiumEntry?,
    second: PodiumEntry?,
    third: PodiumEntry?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumColumn(
            modifier = Modifier.weight(1f),
            rank = 2,
            entry = second,
            heightDp = 114
        )
        PodiumColumn(
            modifier = Modifier.weight(1.12f),
            rank = 1,
            entry = first,
            heightDp = 138
        )
        PodiumColumn(
            modifier = Modifier.weight(1f),
            rank = 3,
            entry = third,
            heightDp = 100
        )
    }
}

@Composable
private fun PodiumColumn(
    modifier: Modifier,
    rank: Int,
    entry: PodiumEntry?,
    heightDp: Int
) {
    val isFirst = rank == 1
    val badgeBg = when (rank) {
        1 -> Color.White
        2 -> PodiumSilver
        else -> PodiumBronze
    }
    val badgeFg = if (isFirst) ArenaNavyDeep else Color.White

    Card(
        modifier = modifier.height(heightDp.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFirst) ArenaNavyDeep else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isFirst) 1.5.dp else 1.dp,
            color = if (isFirst) ArenaNavyDeep else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = badgeBg,
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = entry?.name?.substringBefore(" ") ?: "காலி",
                style = MaterialTheme.typography.titleSmall,
                color = if (isFirst) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = entry?.primary ?: "—",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isFirst) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = entry?.secondary ?: "காத்திருக்கிறது",
                style = MaterialTheme.typography.labelSmall,
                color = if (isFirst) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    rank: Int,
    name: String,
    school: String,
    metaLine: String,
    primaryMetric: String,
    secondaryMetric: String,
    isCurrentUser: Boolean,
    isFail: Boolean = false
) {
    val rankBg = when {
        isFail -> MaterialTheme.colorScheme.errorContainer
        rank == 1 -> ArenaNavyDeep
        rank == 2 -> PodiumSilver
        rank == 3 -> PodiumBronze
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val rankFg = when {
        isFail -> MaterialTheme.colorScheme.onErrorContainer
        rank <= 3 -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isCurrentUser) 1.5.dp else 1.dp,
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(rankBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = rankFg,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Surface(
                            color = ArenaNavyDeep,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "நீங்கள்",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = school,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = primaryMetric,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = secondaryMetric,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFail) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LeaderboardLoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LeaderboardErrorCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun LeaderboardEmptyState(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Leaderboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
