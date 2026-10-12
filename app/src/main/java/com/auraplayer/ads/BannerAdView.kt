package com.auraplayer.ads

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*

/**
 * Adaptive Banner Ad composable.
 * Displays official 320x50 AdMob test banner ad.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_ID
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.i("AdManager", "BannerAdView: Banner loaded successfully")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("AdManager", "BannerAdView failed to load: ${error.message} (code: ${error.code})")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
