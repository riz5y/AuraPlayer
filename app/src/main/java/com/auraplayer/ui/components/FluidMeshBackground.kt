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

@Language("AGSL")
private const val FLUID_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform vec4 color1;
    uniform vec4 color2;
    uniform vec4 colorBg;

    half4 main(in float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float t = iTime * 0.4;
        
        // Liquid distortion coordinates
        float wave1 = sin(uv.x * 3.0 + t + cos(uv.y * 2.0 + t * 0.5));
        float wave2 = cos(uv.y * 4.0 - t * 0.8 + sin(uv.x * 2.5));
        float blend = clamp((wave1 + wave2) * 0.5 + 0.5, 0.0, 1.0);
        
        vec4 col = mix(color1, color2, blend);
        col = mix(col, colorBg, uv.y * 0.8);
        return half4(col);
    }
"""

@Composable
fun FluidMeshBackground(
    colors: DynamicColors,
    modifier: Modifier = Modifier
) {
    val animDominant by animateColorAsState(
        targetValue = colors.dominant,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "mesh_dom"
    )
    val animVibrant by animateColorAsState(
        targetValue = colors.vibrant,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "mesh_vib"
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslFluidBackground(animDominant, animVibrant, modifier)
    } else {
        FallbackMeshBackground(animDominant, animVibrant, modifier)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslFluidBackground(
    color1: Color,
    color2: Color,
    modifier: Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shader_time"
    )

    val shader = remember { RuntimeShader(FLUID_SHADER_SRC) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                shader.setFloatUniform("iResolution", size.width, size.height)
                shader.setFloatUniform("iTime", time)
                shader.setColorUniform(
                    "color1",
                    android.graphics.Color.valueOf(color1.red, color1.green, color1.blue, 1f)
                )
                shader.setColorUniform(
                    "color2",
                    android.graphics.Color.valueOf(color2.red, color2.green, color2.blue, 1f)
                )
                shader.setColorUniform(
                    "colorBg",
                    android.graphics.Color.valueOf(0.04f, 0.04f, 0.04f, 1f)
                )
                val brush = ShaderBrush(shader)
                onDrawBehind {
                    drawRect(brush)
                }
            }
    )
}

@Composable
private fun FallbackMeshBackground(
    color1: Color,
    color2: Color,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        color1,
                        color2.copy(alpha = 0.7f),
                        DeepBlack
                    )
                )
            )
    )
}
