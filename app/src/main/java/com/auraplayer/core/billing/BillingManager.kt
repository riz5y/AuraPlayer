package com.auraplayer.core.billing

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Play In-App Billing state and Pro entitlements.
 * Provides a persistent reactive stream of Pro status to suppress ads and unlock VIP features.
 */
class BillingManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_billing_prefs", Context.MODE_PRIVATE)

    private val _isProUser = MutableStateFlow(prefs.getBoolean(KEY_IS_PRO, false))
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    fun unlockProLifetime() {
        prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
        _isProUser.value = true
    }

    fun restorePurchases(): Boolean {
        val isPro = prefs.getBoolean(KEY_IS_PRO, false)
        _isProUser.value = isPro
        return isPro
    }

    companion object {
        private const val KEY_IS_PRO = "key_aura_pro_lifetime"
        const val PRO_PRODUCT_ID = "aura_pro_lifetime_299"
        const val PRO_PRICE_LABEL = "$2.99"
    }
}
