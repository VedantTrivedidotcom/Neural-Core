package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CyberViewModel
import com.example.viewmodel.GraphNode
import kotlinx.coroutines.delay

@OptIn(ExperimentalTextApi::class)
@Composable
fun GraphScreen(
    viewModel: CyberViewModel,
    onNavigateToNoteDetail: (String) -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val graphNodes = viewModel.graphNodes

    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }
    
    // Spring physics background trigger loop
    LaunchedEffect(Unit) {
        viewModel.startGraphSimulation()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopGraphSimulation()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(bottom = 80.dp) // Leave tab navigation bar padding
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // --- Simulated background particle system & Grid ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val columns = 16
            val rows = 18
            val colSpacing = size.width / columns
            val rowSpacing = size.height / rows

            // Draw Cyber Grid Lines
            for (i in 0..columns) {
                drawLine(
                    color = GlassBorder.copy(alpha = 0.05f),
                    start = Offset(i * colSpacing, 0f),
                    end = Offset(i * colSpacing, size.height),
                    strokeWidth = 1f
                )
            }
            for (j in 0..rows) {
                drawLine(
                    color = GlassBorder.copy(alpha = 0.05f),
                    start = Offset(0f, j * rowSpacing),
                    end = Offset(size.width, j * rowSpacing),
                    strokeWidth = 1f
                )
            }
        }

        // --- Core Physics Linkages and Nodes Vector Layer ---
        val nodesList = graphNodes.values.toList()

        // Touch Input bounds controller
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodesList) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            // Hitbox detection for physics nodes
                            val hitNode = nodesList.find { node ->
                                val dx = startOffset.x - node.x
                                val dy = startOffset.y - node.y
                                (dx * dx + dy * dy) < 1600f // 40dp radius hitbox
                            }
                            if (hitNode != null) {
                                selectedNode = hitNode
                            }
                        },
                        onDrag = { change, dragAmount ->
                            selectedNode?.let { node ->
                                change.consume()
                                val newX = (node.x + dragAmount.x).coerceIn(40f, width - 40f)
                                val newY = (node.y + dragAmount.y).coerceIn(40f, height - 40f)
                                viewModel.updateDragNode(node.id, newX, newY)
                            }
                        },
                        onDragEnd = {
                            // Leave selected state intact so detail pane pops up
                        }
                    )
                }
        ) {
            // Draw Relational Links lines representing backlinks
            notes.forEach { note ->
                val links = note.relationsList.split(",").filter { it.isNotEmpty() }
                val sourceNode = graphNodes[note.id]
                if (sourceNode != null) {
                    links.forEach { targetId ->
                        val targetNode = graphNodes[targetId]
                        if (targetNode != null) {
                            drawLine(
                                color = NeonPurple.copy(alpha = 0.4f),
                                start = Offset(sourceNode.x, sourceNode.y),
                                end = Offset(targetNode.x, targetNode.y),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
            }

            // Draw Nodes Particles
            nodesList.forEach { node ->
                val isHovered = selectedNode?.id == node.id
                val nodeColor = when(node.type) {
                    "document" -> NeonBlue
                    "kanban" -> NeonPink
                    else -> NeonPurple
                }

                // Node Glowing Outer Aura
                drawCircle(
                    color = nodeColor.copy(alpha = if (isHovered) 0.35f else 0.15f),
                    radius = if (isHovered) 32.dp.toPx() else 20.dp.toPx(),
                    center = Offset(node.x, node.y)
                )

                // Solid Core
                drawCircle(
                    color = nodeColor,
                    radius = if (isHovered) 12.dp.toPx() else 8.dp.toPx(),
                    center = Offset(node.x, node.y)
                )
            }
        }

        // Overlay text labels independently to ensure crisp text renders on top
        val density = LocalDensity.current
        nodesList.forEach { node ->
            val isHovered = selectedNode?.id == node.id
            val nodeX = with(density) { node.x.toDp() } - 40.dp
            val nodeY = with(density) { node.y.toDp() } + 12.dp
            Box(
                modifier = Modifier
                    .offset(x = nodeX, y = nodeY)
                    .width(80.dp)
                    .clickable { selectedNode = node }
            ) {
                Text(
                    text = node.title,
                    color = if (isHovered) Color.White else TextSecondary,
                    fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    onTextLayout = {},
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // --- Status Overlay Text ---
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hub, contentDescription = "Engine active", tint = NeonBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "KNOWLEDGE CONNECTOR //",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = NeonBlue,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "Obsidian Physics Sandbox",
                fontSize = 12.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif
            )
        }

        // --- Dynamic Node Inspector glass panel ---
        AnimatedVisibility(
            visible = selectedNode != null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            selectedNode?.let { node ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NODE PARADIGM: ${node.type.uppercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NeonPink,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = node.title,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Links to external memory hubs detected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        
                        Button(
                            onClick = {
                                viewModel.selectNote(node.id)
                                onNavigateToNoteDetail(node.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = "Open", tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SYNAPSE", color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
