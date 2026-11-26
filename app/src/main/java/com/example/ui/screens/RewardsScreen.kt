package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ads.BannerAdSlot
import com.example.ads.RewardedAdController
import com.example.ads.RewardedAdStatus
import com.example.data.LeagueTimeUtils
import com.example.data.MonthlyRanking
import com.example.data.RewardCatalog
import com.example.data.RewardCatalogItem
import com.example.data.RewardCategory
import com.example.data.RewardClaim
import com.example.data.StudentProfile
import com.example.ui.theme.ArenaCobaltDeep
import com.example.ui.theme.ArenaNavyDeep
import com.example.viewmodel.RewardFeedback
import com.example.viewmodel.UiState

@Composable
fun RewardsScreen(
    student: StudentProfile,
    todayDateKey: String,
    monthlyLeaderboard: List<MonthlyRanking>,
    claimsState: UiState<List<RewardClaim>>,
    claimingRewardId: String?,
    rewardFeedback: RewardFeedback?,
    rewardedAdController: RewardedAdController,
    onRewardedAdEarned: () -> Unit,
    onClaimReward: (RewardCatalogItem) -> Unit,
    onDismissFeedback: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val adStatus by rewardedAdController.status.collectAsStateWithLifecycle()

    LaunchedEffect(rewardedAdController) {
        rewardedAdController.preload()
    }

    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var pendingConfirmItem by remember { mutableStateOf<RewardCatalogItem?>(null) }
    var showClaimsDialog by remember { mutableStateOf(false) }
    var previewCertificateClaim by remember { mutableStateOf<RewardClaim?>(null) }
    var adErrorMessage by remember { mutableStateOf<String?>(null) }

    val myMonthlyRank = monthlyLeaderboard.indexOfFirst { it.uid == student.uid }
        .takeIf { it >= 0 }
        ?.plus(1)
    val adsWatchedToday = if (student.adRewardDate == todayDateKey) student.adRewardCountToday else 0L
    val claims = (claimsState as? UiState.Success)?.data.orEmpty()

    val filteredCatalog = remember(selectedFilter) {
        if (selectedFilter == "ALL") RewardCatalog.items
        else RewardCatalog.items.filter { it.category.wireValue == selectedFilter }
    }

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
            // புள்ளிகள் கணக்கு அட்டை (Monochrome Black Hero)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rewards_wallet_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(ArenaNavyDeep, ArenaCobaltDeep)))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "லீக் புள்ளிகள் கணக்கு",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                OutlinedButton(
                                    onClick = { showClaimsDialog = true },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("open_my_claims_button")
                                ) {
                                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("எனது பரிசுகள் (${claims.size})", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Text(
                                text = "${student.rewardPoints} புள்ளிகள்",
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White
                            )
                            Text(
                                text = "நீங்கள் ஈட்டிய புள்ளிகளை பரிசு அட்டைகள், தொழில்நுட்ப உபகரணங்கள் மற்றும் சான்றிதழ்களுக்குப் பயன்படுத்தலாம். மாதாந்திர முதல் 3 வெற்றியாளர்களுக்கு இலவச சான்றிதழ்!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                    }
                }
            }

            // அறிவிப்புப் பட்டை
            item {
                AnimatedVisibility(visible = rewardFeedback != null || adErrorMessage != null) {
                    val isError = rewardFeedback?.isError == true || adErrorMessage != null
                    val message = rewardFeedback?.message ?: adErrorMessage.orEmpty()
                    Surface(
                        color = if (isError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = if (isError) null else BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    adErrorMessage = null
                                    onDismissFeedback()
                                }
                            ) {
                                Text("சரி")
                            }
                        }
                    }
                }
            }

            // விருப்ப விளம்பரப் போனஸ் அட்டை (+25 புள்ளிகள், தினமும் 3 முறை)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rewarded_ad_bonus_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(ArenaNavyDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.rewards_watch_ad_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "கூடுதல் பரிசுப் புள்ளிகள் ($adsWatchedToday/3 இன்று) • தரவரிசையைப் பாதிக்காது",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                adErrorMessage = null
                                if (adStatus is RewardedAdStatus.Unavailable) {
                                    rewardedAdController.preload()
                                } else {
                                    rewardedAdController.show(
                                        activity = activity,
                                        onRewardEarned = { onRewardedAdEarned() },
                                        onError = { err -> adErrorMessage = err }
                                    )
                                }
                            },
                            enabled = adsWatchedToday < 3L && adStatus !is RewardedAdStatus.Loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArenaNavyDeep,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("watch_rewarded_ad_button")
                        ) {
                            val label = when {
                                adsWatchedToday >= 3L -> "முடிந்தது"
                                adStatus is RewardedAdStatus.Loading -> "ஏற்றுகிறது"
                                adStatus is RewardedAdStatus.Unavailable -> "மீண்டும்"
                                else -> "+25 புள்ளி"
                            }
                            Text(label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // மாதாந்திர வெற்றியாளர் நிலை அட்டை (Monochrome Laurel Crest)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_winner_medal_bw),
                            contentDescription = "மாதாந்திர வெற்றியாளர் பதக்கம்",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "${LeagueTimeUtils.prettyCurrentMonth()} வெற்றியாளர்கள்",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    myMonthlyRank != null && myMonthlyRank <= 3 ->
                                        "நீங்கள் தற்போது #$myMonthlyRank இடத்தில் உள்ளீர்கள் — உங்கள் மாதாந்திர வெற்றியாளர் சான்றிதழைக் கீழே பெறலாம்!"
                                    myMonthlyRank != null ->
                                        "உங்கள் தற்போதைய மாதாந்திர இடம் #$myMonthlyRank. முதல் 3 இடங்களுக்கு வந்து இலவச சான்றிதழ் பெறுங்கள்!"
                                    else ->
                                        "தினசரி போட்டிகளில் பங்கேற்று மாதாந்திர முதல் 3 இடங்களைப் பிடித்து வெற்றியாளர் சான்றிதழ் பெறுங்கள்!"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // பிரிவு வடிகட்டி சிப்புகள் (Monochrome B&W)
            item {
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ArenaNavyDeep,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            colors = chipColors,
                            label = { Text("அனைத்தும்") },
                            modifier = Modifier.testTag("reward_filter_all")
                        )
                    }
                    items(RewardCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedFilter == cat.wireValue,
                            onClick = { selectedFilter = cat.wireValue },
                            colors = chipColors,
                            label = { Text(cat.label) },
                            modifier = Modifier.testTag("reward_filter_${cat.wireValue}")
                        )
                    }
                }
            }

            // பரிசுப் பட்டியல்
            items(filteredCatalog, key = { it.id }) { item ->
                val isClaiming = claimingRewardId == item.id
                val meetsRank = !item.requiresMonthlyTopRank || (myMonthlyRank != null && myMonthlyRank <= 3)
                val canAfford = student.rewardPoints >= item.pointsCost && meetsRank
                val icon = when (item.category) {
                    RewardCategory.GIFT_CARD -> Icons.Filled.CardGiftcard
                    RewardCategory.TECH_ACCESSORY -> Icons.Filled.Headphones
                    RewardCategory.WINNER_CERTIFICATE -> Icons.Filled.WorkspacePremium
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reward_card_${item.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = if (item.pointsCost == 0L) "மாதாந்திர முதல் 3" else "${item.pointsCost} புள்ளிகள்",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { pendingConfirmItem = item },
                            enabled = canAfford && claimingRewardId == null,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ArenaNavyDeep,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("claim_reward_button_${item.id}")
                        ) {
                            if (isClaiming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (item.pointsCost == 0L) "வழங்கு" else "பெறுக",
                                    style = MaterialTheme.typography.labelLarge
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

    // பரிசு பெறுவதை உறுதிப்படுத்தும் சாளரம்
    pendingConfirmItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingConfirmItem = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("பரிசு பெறுவதை உறுதிசெய்க") },
            text = {
                Text(
                    text = if (item.pointsCost == 0L) {
                        "மாணவர் அடையாள எண் ${student.studentIdNumber} உடன் சரிபார்க்கப்பட்ட உங்களின் \"${item.title}\" சான்றிதழைப் பெறவா?"
                    } else {
                        "\"${item.title}\" பரிசிற்காக ${item.pointsCost} லீக் புள்ளிகளைப் பயன்படுத்தவா? தனித்துவமான பரிசுக் குறியீடு 'எனது பரிசுகள்' பகுதியில் சேமிக்கப்படும்."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfirmItem = null
                        onClaimReward(item)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArenaNavyDeep,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_claim_dialog_button")
                ) {
                    Text(stringResource(R.string.rewards_claim_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmItem = null }) {
                    Text("ரத்து")
                }
            }
        )
    }

    // எனது பரிசுகள் & சான்றிதழ்கள் சாளரம்
    if (showClaimsDialog) {
        AlertDialog(
            onDismissRequest = { showClaimsDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.rewards_my_claims)) },
            text = {
                if (claims.isEmpty()) {
                    Text(
                        text = "நீங்கள் இதுவரை எந்தப் பரிசையும் பெறவில்லை. தினசரி போட்டிகள் அல்லது விருப்ப விளம்பரங்கள் மூலம் புள்ளிகள் ஈட்டி முதல் பரிசைப் பெறுங்கள்!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        claims.take(8).forEach { claim ->
                            val isCert = RewardCategory.fromWire(claim.rewardCategory) == RewardCategory.WINNER_CERTIFICATE
                            val statusTamil = if (claim.status == "issued") "வழங்கப்பட்டது" else "உறுதிசெய்யப்பட்டது"
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = claim.rewardTitle, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "குறியீடு: ${claim.claimCode} • $statusTamil",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isCert) {
                                        TextButton(
                                            onClick = {
                                                showClaimsDialog = false
                                                previewCertificateClaim = claim
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("அதிகாரப்பூர்வ சான்றிதழைப் பார் →")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showClaimsDialog = false }) {
                    Text("மூடு")
                }
            }
        )
    }

    // அதிகாரப்பூர்வ வெற்றியாளர் சான்றிதழ் முன்னோட்டம் (Monochrome B&W)
    val certClaim = previewCertificateClaim ?: rewardFeedback?.recentClaim?.takeIf {
        RewardCategory.fromWire(it.rewardCategory) == RewardCategory.WINNER_CERTIFICATE
    }
    if (certClaim != null) {
        AlertDialog(
            onDismissRequest = {
                previewCertificateClaim = null
                onDismissFeedback()
            },
            containerColor = ArenaNavyDeep,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_winner_medal_bw),
                        contentDescription = "GK League தமிழ் வெற்றியாளர் முத்திரை",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "அதிகாரப்பூர்வ சான்றிதழ்",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = certClaim.rewardTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "வழங்கப்படுபவர்",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Text(
                        text = student.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${student.schoolName} • ID ${student.studentIdNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "சரிபார்ப்பு எண்: ${certClaim.claimCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        previewCertificateClaim = null
                        onDismissFeedback()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = ArenaNavyDeep
                    )
                ) {
                    Text("முடிந்தது", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
