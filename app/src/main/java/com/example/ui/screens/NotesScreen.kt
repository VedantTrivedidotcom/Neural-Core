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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.ui.theme.*
import com.example.viewmodel.CyberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NotesScreen(
    viewModel: CyberViewModel,
    onNavigateToGraph: () -> Unit,
    activeNoteId: String? = null,
    onEditNote: (String) -> Unit
) {
    val notes by viewModel.filteredNotes.collectAsState()
    val activeSelectionId by viewModel.activeNoteId.collectAsState()
    val allNotes by viewModel.notes.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteType by remember { mutableStateOf("document") } // "document", "whiteboard", "kanban"

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column or Single Panel if screen details aren't active
        if (activeSelectionId == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header & Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🪐 ARCHIVE VAULT",
                        fontFamily = FontFamily.Monospace,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Document", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("FABRICATE", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }

                // Search Bar within Notes Archive
                var localQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = localQuery,
                    onValueChange = {
                        localQuery = it
                        viewModel.searchQuery.value = it
                    },
                    placeholder = { Text("Search title, tags, block contents...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) }
                )

                // Notes List
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Void", tint = TextSecondary, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Vault database empty or filtered", color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notes) { note ->
                            NoteRowItem(
                                note = note,
                                onClick = {
                                    viewModel.selectNote(note.id)
                                    onEditNote(note.id)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(note.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        } else {
            // Note Detail Editor Pane
            NoteDetailEditorPane(viewModel = viewModel, onBack = { viewModel.selectNote(null) })
        }
    }

    // --- Create Document Dialog ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "CONSTRUCT NEON NODE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonBlue
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newNoteTitle,
                        onValueChange = { newNoteTitle = it },
                        label = { Text("Node Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Type Selector
                    Text("NODE PARADIGM:", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("document" to "Doc", "whiteboard" to "Canvas", "kanban" to "Board").forEach { (typeKey, label) ->
                            val isSelected = newNoteType == typeKey
                            OutlinedButton(
                                onClick = { newNoteType = typeKey },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) NeonPurple.copy(alpha = 0.2f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) NeonPurple else GlassBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, color = if (isSelected) Color.White else TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNoteTitle.isNotBlank()) {
                            viewModel.addNote(newNoteType, newNoteTitle)
                            showCreateDialog = false
                            newNoteTitle = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue)
                ) {
                    Text("ASSEMBLE", color = Color.Black, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("ABORT", color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = CyberSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun NoteRowItem(
    note: NoteEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, GlassBorder.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(note.type) {
                            "document" -> Icons.Default.Description
                            "kanban" -> Icons.Default.ViewKanban
                            else -> Icons.Default.Palette
                        },
                        contentDescription = note.type,
                        tint = when(note.type) {
                            "document" -> NeonBlue
                            "kanban" -> NeonPink
                            else -> NeonPurple
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = note.type.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        note.tags.split(",").forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(NeonPurple.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("#$tag", color = NeonPurple, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (note.isFavorite) NeonPink else TextSecondary
                )
            }
        }
    }
}

// --- Note Editor Detail Overlay ---

@Composable
fun NoteDetailEditorPane(
    viewModel: CyberViewModel,
    onBack: () -> Unit
) {
    val activeNote by viewModel.activeNote.collectAsState()
    val allNotes by viewModel.notes.collectAsState()
    val backlinks by viewModel.backlinks.collectAsState()
    val aiResponseState by viewModel.aiResponse.collectAsState()

    var textTitle by remember { mutableStateOf("") }
    var textBody by remember { mutableStateOf("") }
    var textTags by remember { mutableStateOf("") }

    // Suggestion popup for double brace links
    var showLinkSuggestion by remember { mutableStateOf(false) }
    var suggestedNotes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }

    // Map content if note changes loaded
    LaunchedEffect(activeNote) {
        activeNote?.let {
            textTitle = it.title
            textBody = it.content
            textTags = it.tags
        }
    }

    if (activeNote == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 90.dp)
    ) {
        // Navigation Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Return", tint = NeonBlue)
            }

            Row {
                IconButton(
                    onClick = {
                        // AI Prompt to summarize current document
                        viewModel.queryGeminiAssistant("Here is my note regarding '${textTitle}'. Summarize the key action items and structure: \n\n${textBody}")
                    }
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "AI Action", tint = NeonPink)
                }

                IconButton(
                    onClick = { viewModel.deleteActiveNote() }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Erase Node", tint = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title Input
        OutlinedTextField(
            value = textTitle,
            onValueChange = {
                textTitle = it
                viewModel.updateNoteContent(it, textBody, textTags)
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // Tags Input field
        OutlinedTextField(
            value = textTags,
            onValueChange = {
                textTags = it
                viewModel.updateNoteContent(textTitle, textBody, it)
            },
            placeholder = { Text("Tags list (comma-separated)...", color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = NeonPurple
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Divider(
            color = GlassBorder.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Rich Node Content Box with Bidirectional suggestion logic trigger bounds
        OutlinedTextField(
            value = textBody,
            onValueChange = { input ->
                textBody = input
                viewModel.updateNoteContent(textTitle, input, textTags)

                // Detect if user typed double brace link sign: "[[", and extract query
                val lastBraceIdx = input.lastIndexOf("[[")
                if (lastBraceIdx != -1 && lastBraceIdx >= input.length - 15) {
                    val query = input.substring(lastBraceIdx + 2)
                    if (!query.contains("]]")) {
                        suggestedNotes = allNotes.filter {
                            it.title.contains(query, ignoreCase = true) && it.id != activeNote?.id
                        }
                        showLinkSuggestion = suggestedNotes.isNotEmpty()
                    } else {
                        showLinkSuggestion = false
                    }
                } else {
                    showLinkSuggestion = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp),
            placeholder = { Text("Write Markdown or blocks... Use [[NoteName]] to reference other archive nodes.", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Bidirectional Suggested Nodes list popup
        AnimatedVisibility(visible = showLinkSuggestion) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, NeonBlue, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "BI-DIRECTIONAL SECURE REFERENCES //",
                        fontFamily = FontFamily.Monospace,
                        color = NeonBlue,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    suggestedNotes.forEach { sugNote ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Append completed link to editor body
                                    val lastBraceIdx = textBody.lastIndexOf("[[")
                                    val completedBody = textBody.substring(0, lastBraceIdx) + "[[${sugNote.title}]]"
                                    textBody = completedBody
                                    viewModel.updateNoteContent(textTitle, completedBody, textTags)
                                    showLinkSuggestion = false
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Link node", tint = NeonPink, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(sugNote.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Dedicated AI Core Response inside Document Detail editor
        AnimatedVisibility(visible = aiResponseState.first.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, NeonPurple, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = GlassBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "COGNITIVE OUTLINE EXTRACTED //",
                            fontFamily = FontFamily.Monospace,
                            color = NeonPurple,
                            fontSize = 10.sp
                        )
                        IconButton(
                            onClick = { viewModel.queryGeminiAssistant("") } // Reset assistant
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = aiResponseState.first,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Backlinks Analyzer list ---
        Text(
            text = "⚡ SECURE BACKLINKS ANALYZER",
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (backlinks.isEmpty()) {
            Text(
                "No bidirectional linkages found pointing back to this node.",
                color = TextSecondary,
                fontSize = 12.sp,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                backlinks.forEach { linkNote ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectNote(linkNote.id) }
                            .background(CyberSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = "Go", tint = NeonBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = linkNote.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(NeonPink.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LINKED", color = NeonPink, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
