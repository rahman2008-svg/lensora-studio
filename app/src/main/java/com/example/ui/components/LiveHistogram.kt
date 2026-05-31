package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EditParams
import kotlin.math.exp
import kotlin.math.sin

@Composable
fun LiveHistogram(
    params: EditParams,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF333333))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Calculate shift offsets from parameters
            val exposureVal = params.exposure // -4 to +4
            val contrastVal = params.contrast // -1 to +1
            val tempVal = params.temperature // -1 to +1
            val tintVal = params.tint // -1 to +1
            val shadowsVal = params.shadows // -1 to +1
            val highlightsVal = params.highlights // -1 to +1

            // Helper to generate a realistic histogram distribution curve (Sum of Gaussians)
            fun generateChannelPoints(
                chShift: Float,
                chStretch: Float,
                chShadowCompression: Float,
                chHighlightCompression: Float
            ): Path {
                val path = Path()
                path.moveTo(0f, h)

                val points = 60
                val midX = w / 2f

                for (i in 0..points) {
                    val x = (i.toFloat() / points) * w
                    
                    // Normalized dynamic position 0..1
                    val normX = i.toFloat() / points 

                    // Base distribution peaks (Midtones + Shadows peak)
                    val peak1 = 0.35f
                    val peak2 = 0.65f
                    
                    // Apply exposure shift
                    val shiftValue = chShift * 0.18f
                    val stretchValue = 1.0f + chStretch * 0.4f

                    // Map x value into gaussian centers
                    val g1 = (normX - peak1 - shiftValue) * stretchValue
                    val g2 = (normX - peak2 - shiftValue) * stretchValue

                    // Gaussian math
                    var yVal = 0.62f * exp(-((g1 * g1) / 0.035f)) + 0.48f * exp(-((g2 * g2) / 0.045f))

                    // Shadows adjustments (curves on left tail)
                    if (normX < 0.25f) {
                        yVal += (chShadowCompression * 0.38f) * (1.0f - normX / 0.25f)
                    }

                    // Highlights adjustments (curves on right tail)
                    if (normX > 0.75f) {
                        yVal += (chHighlightCompression * 0.38f) * ((normX - 0.75f) / 0.25f)
                    }

                    // Limit range
                    val yCoerced = (yVal.coerceIn(0.01f, 0.95f)) * h
                    val finalY = h - yCoerced

                    path.lineTo(x, finalY)
                }

                path.lineTo(w, h)
                path.close()
                return path
            }

            // Generate Paths for Red, Green, and Blue Channels
            // Temperature pulls Red right, Blue left.
            // Tint pulls Red and Blue right (looks purple/magenta), Green left.
            // Exposure shifts everything right.
            // Contrast stretches everything away from center.
            val redShift = exposureVal + tempVal * 1.5f + tintVal * 0.8f
            val greenShift = exposureVal - tintVal * 1.5f
            val blueShift = exposureVal - tempVal * 1.5f + tintVal * 0.8f

            val redPath = generateChannelPoints(redShift, contrastVal, shadowsVal, highlightsVal)
            val greenPath = generateChannelPoints(greenShift, contrastVal, shadowsVal, highlightsVal)
            val bluePath = generateChannelPoints(blueShift, contrastVal, shadowsVal, highlightsVal)

            // Draw RGB Channels overlapping
            drawPath(
                path = redPath,
                brush = Brush.verticalGradient(listOf(Color(0x55FF5252), Color.Transparent)),
                alpha = 0.82f
            )
            drawPath(
                path = redPath,
                color = Color(0xFFFF5252),
                alpha = 0.9f,
                style = Stroke(width = 1.dp.toPx())
            )

            drawPath(
                path = greenPath,
                brush = Brush.verticalGradient(listOf(Color(0x4469F0AE), Color.Transparent)),
                alpha = 0.82f
            )
            drawPath(
                path = greenPath,
                color = Color(0xFF69F0AE),
                alpha = 0.9f,
                style = Stroke(width = 1.dp.toPx())
            )

            drawPath(
                path = bluePath,
                brush = Brush.verticalGradient(listOf(Color(0x44448AFF), Color.Transparent)),
                alpha = 0.82f
            )
            drawPath(
                path = bluePath,
                color = Color(0xFF448AFF),
                alpha = 0.9f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Dynamic grid indicators for histogram channels
            for (j in 1..3) {
                val gridX = w * (j * 0.25f)
                drawLine(
                    color = Color(0x22FFFFFF),
                    start = Offset(gridX, 0f),
                    end = Offset(gridX, h),
                    strokeWidth = 1.dp.toPx()
        )
            }
        }

        // Tiny indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .background(Color(0x80000000)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Shadows", color = Color.Gray, fontSize = 9.sp)
            Text("Midtones", color = Color.Gray, fontSize = 9.sp)
            Text("Highlights", color = Color.Gray, fontSize = 9.sp)
        }
    }
}
