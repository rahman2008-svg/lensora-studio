package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.data.model.EditParams
import com.example.data.model.MediaAsset
import com.example.data.model.MediaType
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MediaRenderer(
    asset: MediaAsset,
    editParams: EditParams,
    currentTimeSeconds: Float,
    modifier: Modifier = Modifier,
    compareOriginal: Boolean = false
) {
    // If compareOriginal is set, use default unedited settings
    val params = if (compareOriginal) EditParams() else editParams

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .clipToBounds()
    ) {
        // Continuous animation state for live simulation of grains, video waves, etc.
        val infiniteTransition = rememberInfiniteTransition(label = "grain")
        val grainSeed by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "grainSeed"
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            if (canvasWidth == 0f || canvasHeight == 0f) return@Canvas

            // Apply Lens Correction / Distortion simulation
            // Crop geometry, and standard rotation
            val rotationAngle = params.cropAngle
            val scaleFactor = 1.0f + (kotlin.math.abs(rotationAngle) / 45f) * 0.2f

            withTransform({
                // Center rotation
                rotate(rotationAngle, pivot = center)
                // Center scale for rotating without border gaps
                scale(scaleFactor, scaleFactor, pivot = center)
                // Simulated Barrel Distortion when Lens Correction is turned off
                if (!params.lensCorrection) {
                    scale(1.05f, 0.98f, pivot = center)
                }
            }) {
                // 1. Draw base photo context
                when (asset.uri) {
                    "demo_iceland_glacier" -> drawIcelandGlacier(params)
                    "demo_tokyo_neon" -> drawTokyoNeon(params)
                    "demo_serengeti" -> drawSerengeti(params)
                    "demo_highway_cruise" -> drawHighwayCruise(params, currentTimeSeconds)
                    "demo_rainy_cafe" -> drawRainyCafe(params, currentTimeSeconds)
                    else -> drawPlaceholderImage(params, asset)
                }

                // 2. Chromatic Aberration simulation
                if (!params.chromaticAberration && asset.mediaType == MediaType.RAW) {
                    drawChromaticAberrationOverlay(canvasWidth, canvasHeight)
                }

                // 3. Color temperature & green/magenta tint simulation
                drawColorTemperatureAndTint(params, canvasWidth, canvasHeight)

                // 4. Highlight & Shadows / Exposure simulation
                drawExposureAndLightingCurves(params, canvasWidth, canvasHeight)

                // 5. Professional 3-Way Color Grading Overlay
                drawColorGradingOverlay(params, canvasWidth, canvasHeight)

                // 6. Vignette overlay
                if (params.vignette != 0.0f) {
                    drawVignette(params, canvasWidth, canvasHeight)
                }

                // 7. Detail / Grain simulation (dependent on noise reduction & sharpness)
                drawFilmGrain(params, grainSeed, canvasWidth, canvasHeight)
            }
        }
    }
}

private fun DrawScope.drawIcelandGlacier(params: EditParams) {
    val w = size.width
    val h = size.height

    // Cold deep sky background (Gradient)
    val skyColor = Color(0xFF0F1E36)
    val horizonColor = Color(0xFF28546C)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyColor, horizonColor),
            startY = 0f,
            endY = h * 0.5f
        ),
        size = Size(w, h)
    )

    // Animated glow Aurora Borealis
    val auroraColor = Color(0x334CAF50)
    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.1f)
            cubicTo(w * 0.25f, h * 0.02f, w * 0.6f, h * 0.25f, w, h * 0.08f)
            lineTo(w, h * 0.22f)
            cubicTo(w * 0.6f, h * 0.35f, w * 0.25f, h * 0.15f, 0f, h * 0.25f)
            close()
        },
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, auroraColor, Color(0x6600E676), auroraColor, Color.Transparent)
        )
    )

    // Mountain 1 (Back, darker blue-grey)
    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.55f)
            lineTo(w * 0.35f, h * 0.28f)
            lineTo(w * 0.7f, h * 0.55f)
            close()
        },
        color = Color(0xFF162536)
    )

    // Mountain 2 (Front, snowy details)
    val mountainPath = Path().apply {
        moveTo(w * 0.2f, h * 0.55f)
        lineTo(w * 0.6f, h * 0.22f)
        lineTo(w * 0.95f, h * 0.55f)
        close()
    }
    drawPath(mountainPath, color = Color(0xFF1E324D))

    // Snow peak capping (Highlights)
    drawPath(
        path = Path().apply {
            moveTo(w * 0.53f, h * 0.28f)
            lineTo(w * 0.6f, h * 0.22f)
            lineTo(w * 0.68f, h * 0.31f)
            lineTo(w * 0.62f, h * 0.34f)
            lineTo(w * 0.58f, h * 0.3f)
            close()
        },
        color = Color(0xFFE3F2FD) // Snowy ice caps
    )

    // Glacier Lake in the lower half (Reflective)
    val lakeTop = h * 0.55f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2E5B70), Color(0xFF0D1E2D)),
            startY = lakeTop,
            endY = h
        ),
        topLeft = Offset(0f, lakeTop),
        size = Size(w, h - lakeTop)
    )

    // Reflection ripples
    for (i in 1..4) {
        val y = lakeTop + (h - lakeTop) * (i * 0.2f)
        val widthScale = 1.0f - (i * 0.15f)
        drawLine(
            color = Color(0x40E3F2FD),
            start = Offset(w * (0.5f - 0.35f * widthScale), y),
            end = Offset(w * (0.5f + 0.35f * widthScale), y),
            strokeWidth = 2.dp.toPx()
        )
    }

    // Icebergs floating
    drawPath(
        path = Path().apply {
            moveTo(w * 0.15f, h * 0.7f)
            lineTo(w * 0.25f, h * 0.66f)
            lineTo(w * 0.32f, h * 0.72f)
            lineTo(w * 0.22f, h * 0.74f)
            close()
        },
        color = Color(0xFFB0BEC5)
    )
}

private fun DrawScope.drawTokyoNeon(params: EditParams) {
    val w = size.width
    val h = size.height

    // Tokyo night sky and buildings silhouettes
    drawRect(color = Color(0xFF030712), size = Size(w, h))

    // Vertical building towers from base up to varied heights
    drawRect(color = Color(0xFF0B0F19), topLeft = Offset(w * 0.05f, h * 0.15f), size = Size(w * 0.22f, h * 0.85f))
    drawRect(color = Color(0xFF0F1424), topLeft = Offset(w * 0.3f, h * 0.35f), size = Size(w * 0.2f, h * 0.65f))
    drawRect(color = Color(0xFF070B14), topLeft = Offset(w * 0.55f, h * 0.2f), size = Size(w * 0.18f, h * 0.8f))
    drawRect(color = Color(0xFF0E1320), topLeft = Offset(w * 0.76f, h * 0.28f), size = Size(w * 0.2f, h * 0.72f))

    // Glowing Neon Signs
    // Cyber blue sign on building left
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0x3300E5FF))),
        topLeft = Offset(w * 0.12f, h * 0.22f),
        size = Size(w * 0.02f, h * 0.35f)
    )

    // Vibrant Magenta glowing bar center-right
    drawRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFFFF007F), Color(0x22FF007F))),
        topLeft = Offset(w * 0.62f, h * 0.32f),
        size = Size(w * 0.06f, h * 0.25f)
    )

    // Glowing window dots (Neon warm orange/yellow highlights)
    val orangeLight = Color(0xFFFFA726)
    drawCircle(color = orangeLight, center = Offset(w * 0.35f, h * 0.45f), radius = 4.dp.toPx())
    drawCircle(color = orangeLight, center = Offset(w * 0.42f, h * 0.42f), radius = 4.dp.toPx())
    drawCircle(color = Color(0xFF26A69A), center = Offset(w * 0.35f, h * 0.52f), radius = 5.dp.toPx())
    drawCircle(color = orangeLight, center = Offset(w * 0.82f, h * 0.38f), radius = 4.dp.toPx())
    drawCircle(color = orangeLight, center = Offset(w * 0.88f, h * 0.44f), radius = 4.dp.toPx())

    // Wet asphalt floor reflection in bottom 30% of height
    val reflexTop = h * 0.7f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0B1120), Color(0xFF02040A)),
            startY = reflexTop,
            endY = h
        ),
        topLeft = Offset(0f, reflexTop),
        size = Size(w, h - reflexTop)
    )

    // Neon reflection streaks on wet street
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xCC00E5FF), Color.Transparent),
            center = Offset(w * 0.15f, h * 0.8f),
            radius = w * 0.12f
        ),
        topLeft = Offset(w * 0.02f, h * 0.72f),
        size = Size(w * 0.26f, h * 0.25f)
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xCCFF007F), Color.Transparent),
            center = Offset(w * 0.65f, h * 0.82f),
            radius = w * 0.15f
        ),
        topLeft = Offset(w * 0.48f, h * 0.72f),
        size = Size(w * 0.34f, h * 0.25f)
    )

    // Silhouette walking man with umbrella
    val shadowColor = Color(0xD801040D)
    drawCircle(color = shadowColor, center = Offset(w * 0.52f, h * 0.64f), radius = 9.dp.toPx()) // head
    drawLine(color = shadowColor, start = Offset(w * 0.52f, h * 0.65f), end = Offset(w * 0.53f, h * 0.78f), strokeWidth = 12.dp.toPx()) // torso
    drawLine(color = shadowColor, start = Offset(w * 0.53f, h * 0.78f), end = Offset(w * 0.50f, h * 0.90f), strokeWidth = 5.dp.toPx()) // leg
    drawLine(color = shadowColor, start = Offset(w * 0.53f, h * 0.78f), end = Offset(w * 0.56f, h * 0.89f), strokeWidth = 5.dp.toPx()) // leg

    // Umbrella dome
    drawPath(
        path = Path().apply {
            moveTo(w * 0.44f, h * 0.62f)
            cubicTo(w * 0.46f, h * 0.55f, w * 0.58f, h * 0.55f, w * 0.6f, h * 0.62f)
            close()
        },
        color = Color(0xF41A1C23)
    )
    drawLine(color = Color.Black, start = Offset(w * 0.52f, h * 0.58f), end = Offset(w * 0.52f, h * 0.65f), strokeWidth = 2.dp.toPx())
}

private fun DrawScope.drawSerengeti(params: EditParams) {
    val w = size.width
    val h = size.height

    // Warm high-contrast golden horizon gradient
    val skyAmber = Color(0xFFFF6F00)
    val skyGold = Color(0xFFFFB300)
    val groundWarm = Color(0xFF2A1501)

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(skyAmber, skyGold, Color(0xFFFFCC80)),
            startY = 0f,
            endY = h * 0.72f
        ),
        size = Size(w, h)
    )

    // Huge glowing yellow Solar disk (setting sun)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0x22FFF59D), Color.Transparent),
            center = Offset(w * 0.55f, h * 0.55f),
            radius = w * 0.28f
        ),
        center = Offset(w * 0.55f, h * 0.55f),
        radius = w * 0.28f
    )

    // Ground silhouetted flatland
    drawRect(
        color = groundWarm,
        topLeft = Offset(0f, h * 0.72f),
        size = Size(w, h - h * 0.72f)
    )

    // Majestic Acacia Tree geometry
    val trunkBase = Offset(w * 0.28f, h * 0.72f)
    val pathTrunk = Path().apply {
        moveTo(w * 0.24f, h * 0.72f)
        quadraticTo(w * 0.27f, h * 0.6f, w * 0.28f, h * 0.45f)
        lineTo(w * 0.32f, h * 0.45f)
        quadraticTo(w * 0.31f, h * 0.62f, w * 0.34f, h * 0.72f)
        close()
    }
    drawPath(pathTrunk, color = groundWarm)

    // Broad flat-topped leaf canopies (Acacia)
    drawOval(color = groundWarm, topLeft = Offset(w * 0.12f, h * 0.41f), size = Size(w * 0.22f, h * 0.05f))
    drawOval(color = groundWarm, topLeft = Offset(w * 0.24f, h * 0.38f), size = Size(w * 0.25f, h * 0.06f))
    drawOval(color = groundWarm, topLeft = Offset(w * 0.16f, h * 0.35f), size = Size(w * 0.28f, h * 0.06f))

    // Giraffe silhouette
    val gL = w * 0.72f
    val gT = h * 0.5f

    // Legs
    drawLine(color = groundWarm, start = Offset(gL + 12.dp.toPx(), gT + 75.dp.toPx()), end = Offset(gL + 10.dp.toPx(), h * 0.73f), strokeWidth = 3.dp.toPx())
    drawLine(color = groundWarm, start = Offset(gL + 20.dp.toPx(), gT + 75.dp.toPx()), end = Offset(gL + 21.dp.toPx(), h * 0.73f), strokeWidth = 3.dp.toPx())
    drawLine(color = groundWarm, start = Offset(gL + 34.dp.toPx(), gT + 75.dp.toPx()), end = Offset(gL + 32.dp.toPx(), h * 0.73f), strokeWidth = 3.dp.toPx())
    drawLine(color = groundWarm, start = Offset(gL + 40.dp.toPx(), gT + 75.dp.toPx()), end = Offset(gL + 43.dp.toPx(), h * 0.73f), strokeWidth = 3.dp.toPx())

    // Torso (oval)
    drawOval(color = groundWarm, topLeft = Offset(gL + 8.dp.toPx(), gT + 42.dp.toPx()), size = Size(w * 0.08f, h * 0.08f))

    // Long Neck
    drawLine(color = groundWarm, start = Offset(gL + 12.dp.toPx(), gT + 46.dp.toPx()), end = Offset(gL, gT), strokeWidth = 6.dp.toPx())

    // Head
    drawOval(color = groundWarm, topLeft = Offset(gL - 6.dp.toPx(), gT - 4.dp.toPx()), size = Size(w * 0.03f, h * 0.02f))
}

private fun DrawScope.drawHighwayCruise(params: EditParams, currentTime: Float) {
    val w = size.width
    val h = size.height

    // Deep blue Pacific Ocean on left 50%, Coast highway on right
    // Animated waves based on speed/frequency cycle
    val waveOffset = sin(currentTime * 3.5f) * 12f

    // Ocean
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D47A1), Color(0xFF006064)),
            startY = 0f,
            endY = h
        ),
        size = Size(w * 0.52f, h)
    )

    // Rolling ocean white foam waves
    drawPath(
        path = Path().apply {
            moveTo(0f, h * 0.3f + waveOffset)
            cubicTo(w * 0.15f, h * 0.28f - waveOffset, w * 0.35f, h * 0.36f + waveOffset, w * 0.52f, h * 0.32f)
            lineTo(w * 0.52f, h * 0.38f)
            cubicTo(w * 0.35f, h * 0.42f + waveOffset, w * 0.15f, h * 0.34f - waveOffset, 0f, h * 0.37f + waveOffset)
            close()
        },
        color = Color(0x66B2EBF2)
    )

    // Golden Coastal sand coast bar splits road and sea
    drawRect(
        color = Color(0xFFD7CCC8),
        topLeft = Offset(w * 0.52f, 0f),
        size = Size(w * 0.04f, h)
    )

    // Mountainous cliffs highway right
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF4E342E), Color(0xFF3E2723)),
            startY = 0f,
            endY = h
        ),
        topLeft = Offset(w * 0.56f, 0f),
        size = Size(w * 0.44f, h)
    )

    // Two-lane dark asphalt road
    drawRect(
        color = Color(0xFF212121),
        topLeft = Offset(w * 0.6f, 0f),
        size = Size(w * 0.24f, h)
    )

    // Yellow Dashed Dividers (Dashing flows downwards representing car speed movement)
    val yellowDivider = Color(0xFFFFEB3B)
    val flowMultiplier = (currentTime * 400.0f) % 240.0f
    for (i in -1..6) {
        val yBase = i * 140.0f + flowMultiplier
        drawRect(
            color = yellowDivider,
            topLeft = Offset(w * 0.71f, yBase),
            size = Size(w * 0.01f, 60f)
        )
    }

    // Vintage dynamic red sports car driving along the road
    // Car y-coordinate shifts slightly responding to driving physics vibrations
    val carY = h * 0.58f + sin(currentTime * 16f) * 3f
    val carX = w * 0.63f + sin(currentTime * 0.8f) * 12f

    // Red car chassis
    drawRoundRect(
        color = Color(0xFFD50000),
        topLeft = Offset(carX, carY),
        size = Size(w * 0.07f, h * 0.12f),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // Windshield glass window
    drawRoundRect(
        color = Color(0xFFB3E5FC),
        topLeft = Offset(carX + 4.dp.toPx(), carY + 12.dp.toPx()),
        size = Size(w * 0.05f, h * 0.03f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Headlights (beaming downward warm yellow highlights in the direction of travel)
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0x99FFEB3B), Color.Transparent)),
        topLeft = Offset(carX + 4.dp.toPx(), carY - 60f),
        size = Size(w * 0.05f, 60f)
    )
}

private fun DrawScope.drawRainyCafe(params: EditParams, currentTime: Float) {
    val w = size.width
    val h = size.height

    // Indoor cozy dark background
    drawRect(color = Color(0xFF151210), size = Size(w, h))

    // Big rectangular window pane center (Looking out to blurry blue-green rainy outside)
    val winL = w * 0.15f
    val winW = w * 0.7f
    val winH = h * 0.65f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1A3038), Color(0xFF0F1E24)),
            startY = 0f,
            endY = winH
        ),
        topLeft = Offset(winL, 10.dp.toPx()),
        size = Size(winW, winH),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Falling rain streaks sliding down window glass (diagonal strokes moving with currentTime)
    val randomObj = Random(42)
    for (i in 1..25) {
        val startX = winL + randomObj.nextFloat() * winW
        val speedY = 80f + randomObj.nextFloat() * 150f
        val startY = ((randomObj.nextFloat() * winH) + currentTime * speedY) % winH
        drawLine(
            color = Color(0x33B2DFDB),
            start = Offset(startX, startY),
            end = Offset(startX - 10f, startY + 25f),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Warm wooden countertop bar table in foreground
    val barTop = h * 0.62f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF3E2723), Color(0xFF2D1500)),
            startY = barTop,
            endY = h
        ),
        topLeft = Offset(0f, barTop),
        size = Size(w, h - barTop)
    )

    // Steaming tea ceramic cup
    val cupX = w * 0.44f
    val cupY = h * 0.75f
    // Ceramic cup base body
    drawRoundRect(
        color = Color(0xFFECEFF1),
        topLeft = Offset(cupX, cupY),
        size = Size(w * 0.12f, h * 0.11f),
        cornerRadius = CornerRadius(18f, 18f)
    )
    // Cup handle
    drawPath(
        path = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(cupX + w * 0.09f, cupY + 12.dp.toPx(), cupX + w * 0.14f, cupY + h * 0.08f))
        },
        color = Color(0xFFE0E0E0),
        style = Stroke(width = 4.dp.toPx())
    )

    // Steam spirals floating (coiling upward with sinus function)
    for (waveId in 1..3) {
        val steamOffset = waveId * 25.0f
        val xShift = sin((currentTime * 4.5f) + waveId * 3f) * 12f
        drawPath(
            path = Path().apply {
                moveTo(cupX + w * 0.04f + steamOffset - 25f, cupY - 10.dp.toPx())
                cubicTo(
                    cupX + w * 0.04f + xShift, cupY - 40.dp.toPx(),
                    cupX + w * 0.07f - xShift, cupY - 80.dp.toPx(),
                    cupX + w * 0.05f + xShift, cupY - 120.dp.toPx()
                )
            },
            color = Color(0x33FFFFFF),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Beautiful hanging light filaments overhead (warm amber)
    val wireY = 120f
    drawLine(color = Color.DarkGray, start = Offset(w * 0.28f, 0f), end = Offset(w * 0.28f, wireY), strokeWidth = 1.dp.toPx())
    drawCircle(color = Color(0xFFFFD180), center = Offset(w * 0.28f, wireY), radius = 10.dp.toPx())
    drawCircle(color = Color(0x40FFA726), center = Offset(w * 0.28f, wireY), radius = 30.dp.toPx())

    drawLine(color = Color.DarkGray, start = Offset(w * 0.72f, 0f), end = Offset(w * 0.72f, wireY), strokeWidth = 1.dp.toPx())
    drawCircle(color = Color(0xFFFFD180), center = Offset(w * 0.72f, wireY), radius = 10.dp.toPx())
    drawCircle(color = Color(0x40FFA726), center = Offset(w * 0.72f, wireY), radius = 30.dp.toPx())
}

private fun DrawScope.drawPlaceholderImage(params: EditParams, asset: MediaAsset) {
    val w = size.width
    val h = size.height

    // Standard abstract gradient scene
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF311B92), Color(0xFF006064)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        ),
        size = Size(w, h)
    )

    // Geometric rings simulating aperture
    for (i in 1..5) {
        drawCircle(
            color = Color(0xFF00ACC1),
            center = center,
            radius = (w * 0.08f * i),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawChromaticAberrationOverlay(w: Float, h: Float) {
    // Red fringe top-left and Cyan fringe bottom-right on major highlights
    // Rendered via faint offsets overlays
    drawCircle(color = Color(0x55FF0000), center = center + Offset(-6f, -6f), radius = w * 0.081f, style = Stroke(width = 2.dp.toPx()))
    drawCircle(color = Color(0x5500FFFF), center = center + Offset(6f, 6f), radius = w * 0.081f, style = Stroke(width = 2.dp.toPx()))
}

private fun DrawScope.drawColorTemperatureAndTint(
    params: EditParams,
    w: Float,
    h: Float
) {
    // Apply Warm (Amber) / Cool (Blue) simulation
    if (params.temperature > 0f) {
        drawRect(
            color = Color(0xFFFFB74D).copy(alpha = params.temperature * 0.35f),
            size = Size(w, h)
        )
    } else if (params.temperature < 0f) {
        drawRect(
            color = Color(0xFF64B5F6).copy(alpha = -params.temperature * 0.35f),
            size = Size(w, h)
        )
    }

    // Apply Tint Green / Tint Magenta simulation
    if (params.tint > 0f) {
        drawRect(
            color = Color(0xFFE91E63).copy(alpha = params.tint * 0.15f),
            size = Size(w, h)
        )
    } else if (params.tint < 0f) {
        drawRect(
            color = Color(0xFF4CAF50).copy(alpha = -params.tint * 0.15f),
            size = Size(w, h)
        )
    }
}

private fun DrawScope.drawExposureAndLightingCurves(
    params: EditParams,
    w: Float,
    h: Float
) {
    // 1. Exposure logic (Positive: bright white overlay, Negative: dark black overlay)
    if (params.exposure > 0f) {
        val expAlpha = (params.exposure / 4.0f).coerceIn(0f, 0.75f)
        drawRect(
            color = Color.White.copy(alpha = expAlpha),
            size = Size(w, h)
        )
    } else if (params.exposure < 0f) {
        val expAlpha = (-params.exposure / 4.0f).coerceIn(0f, 0.75f)
        drawRect(
            color = Color.Black.copy(alpha = expAlpha),
            size = Size(w, h)
        )
    }

    // 2. Highlights / Shadows & Contrast Simulation using gradient curves
    // Contrasting layers
    if (params.contrast != 0.0f) {
        val contrastFactor = params.contrast.coerceIn(-1f, 1f)
        if (contrastFactor > 0f) {
            // Darken edges, brighten center
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = contrastFactor * 0.12f), Color.Black.copy(alpha = contrastFactor * 0.18f)),
                    center = center,
                    radius = w * 0.7f
                )
            )
        } else {
            // Washout contrast
            val washoutAlpha = -contrastFactor * 0.2f
            drawRect(color = Color(0xFF808080).copy(alpha = washoutAlpha))
        }
    }

    // Highlights adjustments: selectively tints the sky/bright lights
    if (params.highlights != 0f) {
        val hAlpha = (kotlin.math.abs(params.highlights) * 0.2f).coerceIn(0f, 0.5f)
        val hColor = if (params.highlights > 0) Color.White else Color.Gray
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(hColor.copy(alpha = hAlpha), Color.Transparent),
                startY = 0f,
                endY = h * 0.4f
            )
        )
    }

    // Shadows adjustments: selectively brightens or crushes the bottom/shadows
    if (params.shadows != 0f) {
        val sAlpha = (kotlin.math.abs(params.shadows) * 0.22f).coerceIn(0f, 0.5f)
        val sColor = if (params.shadows > 0) Color.White else Color.Black
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, sColor.copy(alpha = sAlpha)),
                startY = h * 0.5f,
                endY = h
            )
        )
    }
}

private fun DrawScope.drawColorGradingOverlay(params: EditParams, w: Float, h: Float) {
    // Hue ranges (0..360), apply 3-way color grading: Shadows (bottom half), Midtones (everywhere), Highlights (top half)
    if (params.shadowsSat > 0.01f) {
        val sColor = colorFromHueSat(params.shadowsHue, params.shadowsSat, params.shadowsLum)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, sColor.copy(alpha = params.shadowsSat * 0.35f)),
                startY = h * 0.35f,
                endY = h
            )
        )
    }

    if (params.midtonesSat > 0.01f) {
        val mColor = colorFromHueSat(params.midtonesHue, params.midtonesSat, params.midtonesLum)
        drawRect(
            color = mColor.copy(alpha = params.midtonesSat * 0.18f),
            size = Size(w, h)
        )
    }

    if (params.highlightsSat > 0.01f) {
        val hColor = colorFromHueSat(params.highlightsHue, params.highlightsSat, params.highlightsLum)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(hColor.copy(alpha = params.highlightsSat * 0.35f), Color.Transparent),
                startY = 0f,
                endY = h * 0.65f
            )
        )
    }
}

private fun DrawScope.drawVignette(params: EditParams, w: Float, h: Float) {
    val strength = params.vignette
    val vignetteColor = if (strength < 0f) Color.Black else Color.White
    val fraction = kotlin.math.abs(strength).coerceIn(0f, 1f)

    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.3f to Color.Transparent,
                0.75f to vignetteColor.copy(alpha = fraction * 0.35f),
                1.0f to vignetteColor.copy(alpha = fraction * 0.75f)
            ),
            center = center,
            radius = w * 0.72f
        ),
        size = Size(w, h)
    )
}

private fun DrawScope.drawFilmGrain(params: EditParams, seed: Float, w: Float, h: Float) {
    // Generate fine film grain points if noise reduction is low or dehaze is high
    // The density and size of grain is relative to sharpening and opposite to noise reduction
    val baseNoiseDensity = 0.45f + (params.sharpening * 0.2f) - (params.noiseReduction * 0.45f)
    if (baseNoiseDensity <= 0.05f) return

    val grainBrushAlpha = (baseNoiseDensity * 0.15f).coerceIn(0.01f, 0.25f)

    // Instead of drawing 1000 individual circles which would crash UI thread,
    // we draw custom tiny geometric grids or lines with very low alpha to simulate realistic high-fidelity grain noise!
    // Using an infinite transition rotation ticks the seed
    val randomObj = Random((seed * 37.12f).toInt())
    
    for (i in 1..10) {
        val lineX = randomObj.nextFloat() * w
        val yOffset = randomObj.nextFloat() * h
        drawLine(
            color = Color.White.copy(alpha = grainBrushAlpha),
            start = Offset(lineX, yOffset),
            end = Offset(lineX + 3.dp.toPx(), yOffset + 3.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.Black.copy(alpha = grainBrushAlpha * 0.8f),
            start = Offset(lineX + 1.dp.toPx(), yOffset + 1.dp.toPx()),
            end = Offset(lineX + 4.dp.toPx(), yOffset + 4.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun colorFromHueSat(hue: Float, sat: Float, lum: Float = 0f): Color {
    val hsv = floatArrayOf(hue, sat, 0.5f + (lum * 0.25f))
    return Color(android.graphics.Color.HSVToColor(hsv))
}
