package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "GKLeagueAds"

object AdUnitIds {
    // Official Google AdMob Test Ad Unit IDs — safe for testing and Play Policy compliant.
    // Replace with your live AdMob Ad Unit IDs before publishing to Google Play.
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
}

object AdMobInitializer {
    @Volatile
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        try {
            val requestConfig = RequestConfiguration.Builder()
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
                .build()
            MobileAds.setRequestConfiguration(requestConfig)
            MobileAds.initialize(context.applicationContext) {
                Log.d(TAG, "Google Mobile Ads SDK initialized with student-safe G rating.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds SDK", e)
        }
    }
}

sealed interface RewardedAdStatus {
    data object Loading : RewardedAdStatus
    data object Ready : RewardedAdStatus
    data class Unavailable(val reason: String) : RewardedAdStatus
}

class RewardedAdController(private val context: Context) {
    private var rewardedAd: RewardedAd? = null

    private val _status = MutableStateFlow<RewardedAdStatus>(RewardedAdStatus.Loading)
    val status: StateFlow<RewardedAdStatus> = _status.asStateFlow()

    fun preload() {
        if (rewardedAd != null) {
            _status.value = RewardedAdStatus.Ready
            return
        }
        _status.value = RewardedAdStatus.Loading
        try {
            RewardedAd.load(
                context.applicationContext,
                AdUnitIds.REWARDED,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        _status.value = RewardedAdStatus.Ready
                        Log.d(TAG, "Rewarded ad preloaded and ready.")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        _status.value = RewardedAdStatus.Unavailable(
                            "விளம்பரம் இப்போது கிடைக்கவில்லை. சிறிது நேரத்தில் மீண்டும் முயற்சிக்கவும்."
                        )
                        Log.w(TAG, "Rewarded ad failed to load: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            rewardedAd = null
            _status.value = RewardedAdStatus.Unavailable("இந்தச் சாதனத்தில் விளம்பரத்தை ஏற்ற முடியவில்லை.")
            Log.e(TAG, "Error preloading rewarded ad", e)
        }
    }

    fun show(
        activity: Activity?,
        onRewardEarned: () -> Unit,
        onError: (String) -> Unit
    ) {
        val ad = rewardedAd
        if (activity == null) {
            onError("இப்போது விளம்பரத்தைக் காட்ட முடியவில்லை.")
            return
        }
        if (ad == null) {
            onError("விளம்பரம் இன்னும் ஏற்றப்படுகிறது. சிறிது நேரம் கழித்து மீண்டும் முயற்சிக்கவும்.")
            preload()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                preload()
                onError("விளம்பரத்தைக் காட்ட முடியவில்லை: ${adError.message}")
            }
        }
        ad.show(activity) { _ ->
            onRewardEarned()
        }
    }
}

@Composable
fun BannerAdSlot(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var adLoadFailed by remember { mutableStateOf(false) }

    val adView = remember(context) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdUnitIds.BANNER
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    adLoadFailed = true
                    Log.w(TAG, "Banner ad failed to load: ${error.message}")
                }

                override fun onAdLoaded() {
                    adLoadFailed = false
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("banner_ad_slot"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.ad_label_sponsored),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (adLoadFailed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "மாணவர் பாதுகாப்பான விளம்பர இடம் (ஆன்லைனில் ஏற்றப்படும்)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    factory = { adView }
                )
            }
        }
    }
}
