package com.auraplayer.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import com.auraplayer.ui.screens.nowplaying.DynamicColors
import com.auraplayer.ui.theme.DeepBlack
import org.intellij.lang.annotations.Language

/**
 * AGSL Liquid Water & Ambient Shader.
 * Simulates gentle water ripples and liquid caustic waves.
 * - If a song is active: bleeds song palette into the water waves.
 * - If idle: relaxes into deep oceanic midnight water ripples.
 */
@Language("AGSL")
private const val WATER_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float4 color1;
    uniform float4 color2;
    uniform float4 colorBg;

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float t = iTime * 0.5;

        // Water ripple frequency & caustic turbulence
        float2 p = uv * 3.0 - float2(1.5);
        float len = length(p);
        
        // Concentric ripples + lateral cross-waves
        float ripple1 = sin(len * 8.0 - t * 2.5);
        float ripple2 = cos(p.x * 5.0 + t + sin(p.y * 4.0 - t * 0.7));
        float wave = (ripple1 + ripple2) * 0.5;
        
        // Liquid highlight threshold
        float specular = pow(clamp(wave * 0.5 + 0.5, 0.0, 1.0), 3.0) * 0.35;
        float blend = clamp((wave * 0.5 + 0.5) + (uv.y * 0.2), 0.0, 1.0);

        float4 col = mix(color1, color2, blend);
        col = mix(col, colorBg, uv.y * 0.75);
        col += float4(specular, specular, specular, 0.0);

        return half4(col);
    }
"""

// Default oceanic midnight water colors when idle
val IdleWaterDeep = Color(0xFF071426)
val IdleWaterWave = Color(0xFF0F3B5C)
val IdleWaterDark = Color(0xFF03070E)

@Composable
fun FluidMeshBackground(
    colors: DynamicColors?,
    modifier: Modifier = Modifier,
    isIdle: Boolean = false
) {
    val targetCol1 = if (isIdle || colors == null) IdleWaterDeep else colors.dominant
    val targetCol2 = if (isIdle || colors == null) IdleWaterWave else colors.vibrant

    val animColor1 by animateColorAsState(
        targetValue = targetCol1,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "water_c1"
    )
    val animColor2 by animateColorAsState(
        targetValue = targetCol2,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "water_c2"
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslWaterBackground(animColor1, animColor2, modifier)
    } else {
        FallbackWaterBackground(animColor1, animColor2, modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslWaterBackground(
    c1: Color,
    c2: Color,
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "water_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 62.83f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "water_timer"
    )

    val shader = remember { RuntimeShader(WATER_SHADER_SRC) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)
                // Use setFloatUniform to guarantee 100% crash-free uniform binding
                shader.setFloatUniform("color1", c1.red, c1.green, c1.blue, 1f)
                shader.setFloatUniform("color2", c2.red, c2.green, c2.blue, 1f)
                shader.setFloatUniform("colorBg", 0.02f, 0.04f, 0.06f, 1f)

                val brush = ShaderBrush(shader)
                onDrawBehind {
                    drawRect(brush)
                }
            }
    )
}

@Composable
private fun FallbackWaterBackground(
    c1: Color,
    c2: Color,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(c2, c1, DeepBlack),
                    radius = 900f
                )
            )
    )
}
