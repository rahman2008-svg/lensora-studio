package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EditParams
import kotlin.math.*

@Composable
fun ColorGradingWheels(
    params: EditParams,
    onParamsChange: (EditParams) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Shadows, 1: Midtones, 2: Highlights

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color(0xFF333333))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Color Grading (3-Way)", color = Color.White, fontSize = 13.sp)
            Text("Balance: %.1f".format(params.gradingBalance), color = Color.Gray, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Selector for Shadows, Midtones, Highlights
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Shadows", "Midtones", "Highlights").forEachIndexed { index, name ->
                val isSelected = selectedTab == index
                OutlinedButton(
                    onClick = { selectedTab = index },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Color(0xFF333333) else Color.Transparent,
                        contentColor = if (isSelected) Color.White else Color.Gray
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .height(36.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF444444))
                ) {
                    Text(name, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display current active Grading Wheel
        when (selectedTab) {
            0 -> {
                SingleColorGradingWheel(
                    label = "Shadows Color Wheel",
                    hue = params.shadowsHue,
                    sat = params.shadowsSat,
                    lum = params.shadowsLum,
                    onValueChange = { h, s, l ->
                        onParamsChange(
                            params.copy(
                                shadowsHue = h,
                                shadowsSat = s,
                                shadowsLum = l
                            )
                        )
                    }
                )
            }
            1 -> {
                SingleColorGradingWheel(
                    label = "Midtones Color Wheel",
                    hue = params.midtonesHue,
                    sat = params.midtonesSat,
                    lum = params.midtonesLum,
                    onValueChange = { h, s, l ->
                        onParamsChange(
                            params.copy(
                                midtonesHue = h,
                                midtonesSat = s,
                                midtonesLum = l
                            )
                        )
                    }
                )
            }
            2 -> {
                SingleColorGradingWheel(
                    label = "Highlights Color Wheel",
                    hue = params.highlightsHue,
                    sat = params.highlightsSat,
                    lum = params.highlightsLum,
                    onValueChange = { h, s, l ->
                        onParamsChange(
                            params.copy(
                                highlightsHue = h,
                                highlightsSat = s,
                                highlightsLum = l
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Balance Slider below
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grading Balance", color = Color.Gray, fontSize = 10.sp)
            Text(String.format("%+.2f", params.gradingBalance), color = Color.White, fontSize = 10.sp)
        }
        Slider(
            value = params.gradingBalance,
            onValueChange = { onParamsChange(params.copy(gradingBalance = it)) },
            valueRange = -1.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.LightGray,
                inactiveTrackColor = Color(0xFF333333)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
fun SingleColorGradingWheel(
    label: String,
    hue: Float,
    sat: Float,
    lum: Float,
    onValueChange: (hue: Float, sat: Float, lum: Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.LightGray, fontSize = 11.sp)
            Text("H: ${hue.toInt()}°  S: ${(sat * 100).toInt()}%", color = Color(0xFF69F0AE), fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Circular Wheel canvas
        Box(
            modifier = Modifier
                .size(130.dp)
                .pointerInput(Unit) {
                    fun updateFromOffset(offset: Offset) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        val maxR = size.width / 2f

                        val finalS = (r / maxR).coerceIn(0f, 1f)
                        val theta = atan2(dy, dx)
                        var finalH = Math.toDegrees(theta.toDouble()).toFloat()
                        if (finalH < 0f) finalH += 360f

                        onValueChange(finalH, finalS, lum)
                    }

                    detectTapGestures(onTap = { updateFromOffset(it) })
                }
                .pointerInput(Unit) {
                    fun updateFromOffset(offset: Offset) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val r = sqrt(dx * dx + dy * dy)
                        val maxR = size.width / 2f

                        val finalS = (r / maxR).coerceIn(0f, 1f)
                        val theta = atan2(dy, dx)
                        var finalH = Math.toDegrees(theta.toDouble()).toFloat()
                        if (finalH < 0f) finalH += 360f

                        onValueChange(finalH, finalS, lum)
                    }

                    detectDragGestures { change, _ ->
                        change.consume()
                        updateFromOffset(change.position)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.width / 2f

                // Draw solid background color wheel
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        ),
                        center = Offset(cx, cy)
                    ),
                    radius = radius
                )

                // Overlay Radial white fade to represent Saturation fading to white center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    radius = radius
                )

                // Wheel boundary rim
                drawCircle(
                    color = Color(0xFF444444),
                    radius = radius,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Draw touch selection thumb position
                val rad = Math.toRadians(hue.toDouble())
                val tx = cx + cos(rad).toFloat() * (sat * radius)
                val ty = cy + sin(rad).toFloat() * (sat * radius)

                // Outer border glow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    center = Offset(tx, ty),
                    radius = 9.dp.toPx()
                )
                // White indicator thumb ring
                drawCircle(
                    color = Color.White,
                    center = Offset(tx, ty),
                    radius = 7.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Core center point
                drawCircle(
                    color = Color.Black,
                    center = Offset(tx, ty),
                    radius = 2.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Luminance Slider of the range
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Luminance", color = Color.Gray, fontSize = 10.sp)
            Text(String.format("%+.2f", lum), color = Color.White, fontSize = 10.sp)
        }
        Slider(
            value = lum,
            onValueChange = { onValueChange(hue, sat, it) },
            valueRange = -1.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF69F0AE),
                activeTrackColor = Color(0xFF2E7D32),
                inactiveTrackColor = Color(0xFF333333)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
