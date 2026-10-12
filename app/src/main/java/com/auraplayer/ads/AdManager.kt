package com.auraplayer.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all AdMob ad loading and showing.
 *
 * Strategy:
 *  1. Splash Interstitial  — shown once per cold start (after splash screen)
 *  2. Session Interstitial — shown after N skips/20 min, never during playback
 *  3. Banner              — shown in LibraryScreen, hidden in full NowPlaying
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // ⚠️ Replace with real ad unit IDs before release
        // Use test IDs during development:
        const val SPLASH_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712" // TEST
        const val SESSION_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712" // TEST
        const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111" // TEST

        private const val MIN_INTERSTITIAL_INTERVAL_MS = 5 * 60 * 1000L // 5 min
        private const val SKIP_THRESHOLD = 3
    }

    private var splashAd: InterstitialAd? = null
    private var sessionAd: InterstitialAd? = null
    private var lastInterstitialShownAt = 0L
    private var skipCount = 0
    private var splashAdShownThisSession = false

    fun initialize() {
        MobileAds.initialize(context) {}
        preloadSplashAd()
        preloadSessionAd()
    }

    // ── Splash Interstitial ───────────────────────────────────────────────────

    private fun preloadSplashAd() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, SPLASH_INTERSTITIAL_ID, request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { splashAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("AdManager", "Splash ad failed: ${error.message}")
                    splashAd = null
                }
            })
    }

    /** Call this after SplashScreen finishes — shows once per cold start. */
    fun showSplashAdIfReady(activity: Activity, onDone: () -> Unit) {
        if (splashAdShownThisSession || splashAd == null) {
            onDone(); return
        }
        splashAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                splashAdShownThisSession = true
                lastInterstitialShownAt = System.currentTimeMillis()
                splashAd = null
                onDone()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { onDone() }
        }
        splashAd?.show(activity)
    }

    // ── Session Interstitial ──────────────────────────────────────────────────

    private fun preloadSessionAd() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, SESSION_INTERSTITIAL_ID, request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { sessionAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { sessionAd = null }
            })
    }

    /**
     * Call on every song skip.
     * Shows interstitial only when:
     *  - player is NOT playing (natural break)
     *  - skip threshold reached
     *  - cooldown passed
     */
    fun onSongSkipped(activity: Activity, isPlaying: Boolean) {
        skipCount++
        val cooldownPassed =
            System.currentTimeMillis() - lastInterstitialShownAt > MIN_INTERSTITIAL_INTERVAL_MS

        if (!isPlaying && skipCount >= SKIP_THRESHOLD && cooldownPassed) {
            showSessionAd(activity)
        }
    }

    private fun showSessionAd(activity: Activity) {
        val ad = sessionAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                skipCount = 0
                lastInterstitialShownAt = System.currentTimeMillis()
                sessionAd = null
                preloadSessionAd() // preload next immediately
            }
        }
        ad.show(activity)
    }
}
