package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EditParams

@Composable
fun ToneCurveEditor(
    params: EditParams,
    onParamsChange: (EditParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
            .border(1.dp, Color(0xFF333333))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tone Curve (RGB)", color = Color.White, fontSize = 13.sp)
            Text("Adjust tonal highlights & shadows", color = Color.Gray, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Curve Graph Canvas
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(160.dp)
                .background(Color(0xFF121212))
                .border(1.dp, Color(0xFF444444))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw Grid lines
                val gridColor = Color(0x33888888)
                for (i in 1..3) {
                    val pos = w * (i * 0.25f)
                    drawLine(gridColor, start = Offset(pos, 0f), end = Offset(pos, h), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, start = Offset(0f, pos), end = Offset(w, pos), strokeWidth = 1.dp.toPx())
                }

                // Reference Linear diagonal
                drawLine(
                    color = Color(0x55888888),
                    start = Offset(0f, h),
                    end = Offset(w, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                // Draw curve using edit parameters
                // (0,0) mapped to (0, h)
                // Shadows control: point at normalized x = 0.25f, y = params.curveShadowPoint
                // Midtones control: point at normalized x = 0.50f, y = params.curveMidPoint
                // Highlights control: point at normalized x = 0.75f, y = params.curveHighlightPoint
                // (1,1) mapped to (w, 0)
                val ptShadow = Offset(w * 0.25f, h * (1.0f - params.curveShadowPoint))
                val ptMid = Offset(w * 0.50f, h * (1.0f - params.curveMidPoint))
                val ptHighlight = Offset(w * 0.75f, h * (1.0f - params.curveHighlightPoint))

                val curvePath = Path().apply {
                    moveTo(0f, h)
                    // Smooth cubic Bezier interpolation through points
                    cubicTo(
                        w * 0.12f, h * (1.0f - (params.curveShadowPoint * 0.5f).coerceIn(0f, 1f)),
                        w * 0.20f, h * (1.0f - params.curveShadowPoint),
                        ptShadow.x, ptShadow.y
                    )
                    cubicTo(
                        w * 0.33f, h * (1.0f - (params.curveShadowPoint + (params.curveMidPoint - params.curveShadowPoint) * 0.4f).coerceIn(0f, 1f)),
                        w * 0.42f, h * (1.0f - (params.curveMidPoint - (params.curveMidPoint - params.curveShadowPoint) * 0.2f).coerceIn(0f, 1f)),
                        ptMid.x, ptMid.y
                    )
                    cubicTo(
                        w * 0.58f, h * (1.0f - (params.curveMidPoint + (params.curveHighlightPoint - params.curveMidPoint) * 0.2f).coerceIn(0f, 1f)),
                        w * 0.67f, h * (1.0f - (params.curveHighlightPoint - (params.curveHighlightPoint - params.curveMidPoint) * 0.4f).coerceIn(0f, 1f)),
                        ptHighlight.x, ptHighlight.y
                    )
                    cubicTo(
                        w * 0.82f, h * (1.0f - params.curveHighlightPoint),
                        w * 0.90f, h * (1.0f - (1.0f - (1.0f - params.curveHighlightPoint) * 0.5f).coerceIn(0f, 1f)),
                        w, 0f
                    )
                }

                drawPath(
                    path = curvePath,
                    color = Color(0xFFE0E0E0),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Highlight interactive nodes on graph
                drawCircle(color = Color(0xFF448AFF), center = ptShadow, radius = 4.dp.toPx())
                drawCircle(color = Color(0xFF69F0AE), center = ptMid, radius = 4.dp.toPx())
                drawCircle(color = Color(0xFFFF5252), center = ptHighlight, radius = 4.dp.toPx())
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Curve Sliders below graph
        CurveSliderRow(
            label = "Shadows (Left node)",
            value = params.curveShadowPoint,
            color = Color(0xFF448AFF),
            onValueChange = { onParamsChange(params.copy(curveShadowPoint = it)) }
        )

        CurveSliderRow(
            label = "Midtones (Center node)",
            value = params.curveMidPoint,
            color = Color(0xFF69F0AE),
            onValueChange = { onParamsChange(params.copy(curveMidPoint = it)) }
        )

        CurveSliderRow(
            label = "Highlights (Right node)",
            value = params.curveHighlightPoint,
            color = Color(0xFFFF5252),
            onValueChange = { onParamsChange(params.copy(curveHighlightPoint = it)) }
        )
    }
}

@Composable
private fun CurveSliderRow(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, fontSize = 10.sp)
            Text(String.format("%.2f", value), color = color, fontSize = 10.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.5f),
                inactiveTrackColor = Color(0xFF333333)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
