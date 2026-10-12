package com.auraplayer.core.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-fidelity hardware audio effects manager.
 * Connects to the system audio session to drive a real 5-band equalizer,
 * hardware bass booster, and 3D spatial virtualizer.
 */
class EqualizerManager(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null

    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    // Band levels in dB (-10 to +10 dB)
    private val _bandLevels = MutableStateFlow(listOf(4, 2, -1, 3, 5))
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()

    // 0 to 100
    private val _bassBoostStrength = MutableStateFlow(60)
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    // 0 to 100
    private val _virtualizerStrength = MutableStateFlow(40)
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _currentPreset = MutableStateFlow("Club & Bass")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    fun bindAudioSession(sessionId: Int) {
        if (sessionId <= 0) return
        release()
        runCatching {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _isEnabled.value
            }
            bassBoostEffect = BassBoost(0, sessionId).apply {
                enabled = _isEnabled.value
                setStrength((_bassBoostStrength.value * 10).toShort())
            }
            virtualizerEffect = Virtualizer(0, sessionId).apply {
                enabled = _isEnabled.value
                setStrength((_virtualizerStrength.value * 10).toShort())
            }
            applyLevelsToHardware(_bandLevels.value)
        }.onFailure {
            Log.w("EqualizerManager", "AudioEffect hardware initialization notice: ${it.message}")
        }
    }

    fun setBandLevel(bandIndex: Int, levelDb: Int) {
        val current = _bandLevels.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = levelDb.coerceIn(-10, 10)
            _bandLevels.value = current
            _currentPreset.value = "Custom"
            applyLevelsToHardware(current)
        }
    }

    fun setBassBoost(strengthPercent: Int) {
        val clamped = strengthPercent.coerceIn(0, 100)
        _bassBoostStrength.value = clamped
        runCatching {
            bassBoostEffect?.setStrength((clamped * 10).toShort())
        }
    }

    fun setVirtualizer(strengthPercent: Int) {
        val clamped = strengthPercent.coerceIn(0, 100)
        _virtualizerStrength.value = clamped
        runCatching {
            virtualizerEffect?.setStrength((clamped * 10).toShort())
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        runCatching {
            equalizer?.enabled = enabled
            bassBoostEffect?.enabled = enabled
            virtualizerEffect?.enabled = enabled
        }
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        val levels = when (presetName) {
            "Club & Bass" -> listOf(6, 4, -1, 3, 5)
            "Vocal Clarity" -> listOf(-2, 1, 5, 4, 2)
            "Electronic" -> listOf(5, 3, 0, 4, 6)
            "Rock" -> listOf(4, 2, -1, 2, 5)
            "Acoustic Warmth" -> listOf(3, 2, 1, 2, 3)
            "Bass Booster" -> listOf(8, 6, 1, 0, 1)
            "Studio Flat" -> listOf(0, 0, 0, 0, 0)
            else -> _bandLevels.value
        }
        _bandLevels.value = levels
        applyLevelsToHardware(levels)
    }

    private fun applyLevelsToHardware(levels: List<Int>) {
        val eq = equalizer ?: return
        runCatching {
            val range = eq.bandLevelRange
            val minMb = range[0]
            val maxMb = range[1]

            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until minOf(numBands, levels.size)) {
                // levels: -10..+10 dB -> mB
                val mB = (levels[i] * 100).coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                eq.setBandLevel(i.toShort(), mB)
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoostEffect?.release() }
        runCatching { virtualizerEffect?.release() }
        equalizer = null
        bassBoostEffect = null
        virtualizerEffect = null
    }
}
