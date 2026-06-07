package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as CanvasStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CyberViewModel
import com.example.viewmodel.Stroke

@Composable
fun WhiteboardScreen(viewModel: CyberViewModel) {
    val strokes = viewModel.currentStrokes
    
    var activeColorHex by remember { mutableStateOf("#00E5FF") } // Default Blue
    var strokeWidth by remember { mutableStateOf(6f) }
    
    // In-bounds temporary path storage
    var tempPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    val colorsMap = listOf(
        "#00E5FF" to NeonBlue,
        "#C700FF" to NeonPurple,
        "#FFFF007A" to NeonPink,
        "#39FF14" to NeonGreen
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(bottom = 80.dp) // Leave navigation space
    ) {
        // --- Custom Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🌌 COSMIC CANVAS // MODULE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Whiteboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Shape drop triggers
                IconButton(
                    onClick = {
                        // Quick-drop Rectangle
                        val rectPoints = listOf(
                            200f to 300f,
                            500f to 300f,
                            500f to 500f,
                            200f to 500f,
                            200f to 300f
                        )
                        viewModel.addStroke(Stroke(rectPoints, activeColorHex, strokeWidth, "rect"))
                    }
                ) {
                    Icon(Icons.Default.CropSquare, contentDescription = "Add Rect", tint = Color.White)
                }

                IconButton(
                    onClick = {
                        // Quick-drop Circle
                        val circPoints = listOf(
                            350f to 450f,
                            450f to 450f // Used to measure radius
                        )
                        viewModel.addStroke(Stroke(circPoints, activeColorHex, strokeWidth, "circle"))
                    }
                ) {
                    Icon(Icons.Default.Circle, contentDescription = "Add Circle", tint = Color.White)
                }

                Button(
                    onClick = { viewModel.clearWhiteboard() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Purge Board")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PURGE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // --- Toolbar controls: Palette selection & weights ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorsMap.forEach { (hex, tint) ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(tint, CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (activeColorHex == hex) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { activeColorHex = hex }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WEIGHT: ", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                listOf(4f to "Thin", 8f to "Med", 16f to "Thick").forEach { (weight, label) ->
                    val isSel = strokeWidth == weight
                    Text(
                        text = label.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = if (isSel) NeonBlue else TextSecondary,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { strokeWidth = weight }
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // --- Infinite Canvas Drawing field ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .background(CyberSurface.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            tempPoints.clear()
                            tempPoints.add(startOffset.x to startOffset.y)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextPt = tempPoints.lastOrNull()?.let { (x, y) ->
                                (x + dragAmount.x) to (y + dragAmount.y)
                            } ?: (change.position.x to change.position.y)
                            tempPoints.add(nextPt)
                        },
                        onDragEnd = {
                            if (tempPoints.isNotEmpty()) {
                                viewModel.addStroke(
                                    Stroke(
                                        points = tempPoints.toList(),
                                        colorHex = activeColorHex,
                                        width = strokeWidth,
                                        type = "freehand"
                                    )
                                )
                                tempPoints.clear()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Blueprint grids inside Whiteboard Canvas
                val spacing = 40.dp.toPx()
                for (i in 0..(size.width / spacing).toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.02f),
                        start = Offset(i * spacing, 0f),
                        end = Offset(i * spacing, size.height)
                    )
                }
                for (j in 0..(size.height / spacing).toInt()) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.02f),
                        start = Offset(0f, j * spacing),
                        end = Offset(size.width, j * spacing)
                    )
                }

                // Render existing strokes
                strokes.forEach { stroke ->
                    val color = Color(android.graphics.Color.parseColor(stroke.colorHex))
                    when (stroke.type) {
                        "freehand" -> {
                            if (stroke.points.size > 1) {
                                val path = Path().apply {
                                    val first = stroke.points.first()
                                    moveTo(first.first, first.second)
                                    for (i in 1 until stroke.points.size) {
                                        val pt = stroke.points[i]
                                        lineTo(pt.first, pt.second)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = color,
                                    style = CanvasStroke(width = stroke.width, cap = StrokeCap.Round)
                                )
                            }
                        }
                        "rect" -> {
                            if (stroke.points.size >= 4) {
                                val p1 = stroke.points[0]
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(p1.first, p1.second),
                                    size = Size(300f, 200f),
                                    cornerRadius = CornerRadius(10f, 10f),
                                    style = CanvasStroke(width = stroke.width)
                                )
                            }
                        }
                        "circle" -> {
                            if (stroke.points.size >= 2) {
                                val p1 = stroke.points[0]
                                drawCircle(
                                    color = color,
                                    radius = 100f,
                                    center = Offset(p1.first, p1.second),
                                    style = CanvasStroke(width = stroke.width)
                                )
                            }
                        }
                    }
                }

                // Render current dragging stroke
                if (tempPoints.size > 1) {
                    val activeColor = Color(android.graphics.Color.parseColor(activeColorHex))
                    val activePath = Path().apply {
                        val first = tempPoints.first()
                        moveTo(first.first, first.second)
                        for (i in 1 until tempPoints.size) {
                            val pt = tempPoints[i]
                            lineTo(pt.first, pt.second)
                        }
                    }
                    drawPath(
                        path = activePath,
                        color = activeColor,
                        style = CanvasStroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
