package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.ui.theme.*
import com.example.viewmodel.CyberViewModel

@Composable
fun TasksScreen(viewModel: CyberViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }
    var selectedNoteId by remember { mutableStateOf("") }
    var selectedColumnId by remember { mutableStateOf("To Do") }

    // Seed note selection default
    LaunchedEffect(notes) {
        if (notes.isNotEmpty() && selectedNoteId.isEmpty()) {
            selectedNoteId = notes.first().id
        }
    }

    val columns = listOf("To Do", "In Progress", "Done")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp) // Leave bottom navigation spacing
    ) {
        // --- Header Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "👾 DYNAMIC PROTOCOL BOARD",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = NeonPink,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Sprint Board",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Button(
                onClick = { showAddTaskDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ALLOCATE", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        // --- Column Boards Scroll ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            columns.forEach { colName ->
                val colTasks = tasks.filter { it.columnId == colName }
                val colColor = when(colName) {
                    "To Do" -> NeonBlue
                    "In Progress" -> NeonPurple
                    else -> NeonGreen
                }

                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Column Title Area
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(colColor, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = colName.uppercase(),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                            // Total count glow indicator
                            Box(
                                modifier = Modifier
                                    .background(colColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = colTasks.size.toString(),
                                    color = colColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Tasks list
                        if (colTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No operational protocols.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(colTasks) { task ->
                                    KanbanTaskCard(
                                        task = task,
                                        colColor = colColor,
                                        associatedNoteTitle = notes.find { it.id == task.noteId }?.title ?: "Unlinked Node",
                                        onToggleComplete = { viewModel.toggleTaskComplete(task.id) },
                                        onMoveCol = { targetCol -> viewModel.updateTaskColumn(task.id, targetCol) },
                                        onDelete = { viewModel.deleteTask(task.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Task Creation Modal Dialog ---
    if (showAddTaskDialog) {
        val notesList = notes
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = {
                Text(
                    "FABRICATE PROTOCOL",
                    fontFamily = FontFamily.Monospace,
                    color = NeonPink,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        label = { Text("Task Directive") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("SELECT BOARD DOMAIN:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                    
                    // Simple drop-down representation/selection helper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        notesList.forEach { note ->
                            val isSelected = selectedNoteId == note.id
                            OutlinedButton(
                                onClick = { selectedNoteId = note.id },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) NeonPink.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) NeonPink else GlassBorder),
                            ) {
                                Text(note.title, color = if (isSelected) Color.White else TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("INITIAL STATUS CONFIG:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        columns.forEach { col ->
                            val isSelected = selectedColumnId == col
                            OutlinedButton(
                                onClick = { selectedColumnId = col },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) NeonBlue.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) NeonBlue else GlassBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(col, color = if (isSelected) Color.White else TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskText.isNotBlank() && selectedNoteId.isNotEmpty()) {
                            viewModel.addTask(newTaskText, selectedNoteId, selectedColumnId)
                            showAddTaskDialog = false
                            newTaskText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Text("ALLOCATE", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("ABORT", color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = CyberSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun KanbanTaskCard(
    task: TaskEntity,
    colColor: Color,
    associatedNoteTitle: String,
    onToggleComplete: () -> Unit,
    onMoveCol: (String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Task text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleComplete() },
                        colors = CheckboxDefaults.colors(checkedColor = colColor, uncheckedColor = TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (task.isCompleted) TextSecondary else Color.White,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Erase", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Associated node label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(colColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = associatedNoteTitle,
                        color = colColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Column transfer arrow keys
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (task.columnId != "To Do") {
                        IconButton(
                            onClick = {
                                val prevCol = if (task.columnId == "Done") "In Progress" else "To Do"
                                onMoveCol(prevCol)
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (task.columnId != "Done") {
                        IconButton(
                            onClick = {
                                val nextCol = if (task.columnId == "To Do") "In Progress" else "Done"
                                onMoveCol(nextCol)
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", tint = colColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
