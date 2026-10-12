package com.auraplayer.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(
    private val context: Context
) {
    companion object {
        const val SPLASH_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        const val SESSION_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        const val REWARDED_VIDEO_ID = "ca-app-pub-3940256099942544/5224354917"

        private const val MIN_INTERSTITIAL_INTERVAL_MS = 3 * 60 * 1000L // 3 min
        private const val SKIP_THRESHOLD = 3
    }

    private var splashAd: InterstitialAd? = null
    private var sessionAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var lastInterstitialShownAt = 0L
    private var skipCount = 0
    private var splashAdShownThisSession = false
    var isProUser: () -> Boolean = { false }

    fun initialize() {
        if (isProUser()) return
        MobileAds.initialize(context) {}
        preloadSplashAd()
        preloadSessionAd()
        preloadRewardedAd()
    }

    // ── Splash Interstitial ───────────────────────────────────────────────────

    fun preloadSplashAd() {
        if (isProUser()) return
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, SPLASH_INTERSTITIAL_ID, request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    splashAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("AdManager", "Splash ad failed: ${error.message}")
                    splashAd = null
                }
            })
    }

    /**
     * Waits for the splash ad to load if not ready yet (up to timeoutMs),
     * and shows it automatically before entering the app.
     */
    fun loadAndShowSplashAd(activity: Activity, timeoutMs: Long = 5000L, onDone: () -> Unit) {
        if (isProUser()) {
            onDone(); return
        }

        Log.i("AdManager", "loadAndShowSplashAd called. Existing ad available: ${splashAd != null}")
        var completed = false
        val mainHandler = Handler(Looper.getMainLooper())

        val timeoutRunnable = Runnable {
            if (!completed) {
                Log.w("AdManager", "Splash ad timed out after ${timeoutMs}ms, continuing to app")
                completed = true
                onDone()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, timeoutMs)

        fun presentAd(ad: InterstitialAd) {
            Log.i("AdManager", "Presenting splash interstitial ad on activity")
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.i("AdManager", "Splash ad dismissed by user")
                    lastInterstitialShownAt = System.currentTimeMillis()
                    splashAd = null
                    preloadSplashAd()
                    if (!completed) {
                        completed = true
                        mainHandler.removeCallbacks(timeoutRunnable)
                        onDone()
                    }
                }
                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    Log.w("AdManager", "Splash ad failed to show: ${e.message}")
                    if (!completed) {
                        completed = true
                        mainHandler.removeCallbacks(timeoutRunnable)
                        onDone()
                    }
                }
            }
            ad.show(activity)
        }

        val existingAd = splashAd
        if (existingAd != null) {
            presentAd(existingAd)
        } else {
            val request = AdRequest.Builder().build()
            InterstitialAd.load(activity, SPLASH_INTERSTITIAL_ID, request,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.i("AdManager", "Splash interstitial ad loaded from network successfully")
                        splashAd = ad
                        if (!completed) {
                            presentAd(ad)
                        }
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w("AdManager", "Splash ad network load failed: ${error.message} (code ${error.code})")
                        if (!completed) {
                            completed = true
                            mainHandler.removeCallbacks(timeoutRunnable)
                            onDone()
                        }
                    }
                })
        }
    }

    // ── Rewarded Video Ad ────────────────────────────────────────────────────

    private fun preloadRewardedAd() {
        if (isProUser()) return
        val request = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_VIDEO_ID, request, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w("AdManager", "Rewarded video failed to load: ${error.message}")
                rewardedAd = null
            }
        })
    }

    /**
     * Plays a pop-up rewarded video ad on button press.
     * Rewarded callback invoked upon successful video watch completion.
     */
    fun showRewardedVideo(
        activity: Activity,
        onRewarded: () -> Unit,
        onClosed: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            // Load and show on-demand
            val request = AdRequest.Builder().build()
            RewardedAd.load(context, REWARDED_VIDEO_ID, request, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedAd) {
                    presentRewarded(activity, loadedAd, onRewarded, onClosed)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w("AdManager", "Rewarded ad failed: ${error.message}")
                    onClosed()
                }
            })
            return
        }
        presentRewarded(activity, ad, onRewarded, onClosed)
    }

    private fun presentRewarded(
        activity: Activity,
        ad: RewardedAd,
        onRewarded: () -> Unit,
        onClosed: () -> Unit
    ) {
        var rewardGranted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preloadRewardedAd()
                if (rewardGranted) onRewarded()
                onClosed()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                rewardedAd = null
                preloadRewardedAd()
                onClosed()
            }
        }
        ad.show(activity) { _ ->
            rewardGranted = true
        }
    }

    // ── Session Interstitial ──────────────────────────────────────────────────

    private fun preloadSessionAd() {
        if (isProUser()) return
        val request = AdRequest.Builder().build()
        InterstitialAd.load(context, SESSION_INTERSTITIAL_ID, request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { sessionAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { sessionAd = null }
            })
    }

    fun onSongSkipped(activity: Activity, isPlaying: Boolean) {
        if (isProUser()) return
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
                preloadSessionAd()
            }
        }
        ad.show(activity)
    }
}
