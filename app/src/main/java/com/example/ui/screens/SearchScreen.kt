package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CyberViewModel

@Composable
fun SearchScreen(
    viewModel: CyberViewModel,
    onNavigateToNoteDetail: (String) -> Unit
) {
    val searchResults by viewModel.filteredNotes.collectAsState()
    val rawQuery by viewModel.searchQuery.collectAsState()

    var activeInputQuery by remember { mutableStateOf(rawQuery) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // --- Header Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Scan", tint = NeonBlue)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "🔍 NEURAL VECTOR SCANNER //",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Global Search",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // --- Fast Input Field area ---
        OutlinedTextField(
            value = activeInputQuery,
            onValueChange = {
                activeInputQuery = it
                viewModel.searchQuery.value = it
            },
            placeholder = { Text("Search title, tags, block contents...", color = TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface
            ),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Scan", tint = TextSecondary) },
            trailingIcon = {
                if (activeInputQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            activeInputQuery = ""
                            viewModel.searchQuery.value = ""
                        }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- Results Feed list ---
        Text(
            text = "⚡ SCAN RESULTS // SIZE: ${searchResults.size}",
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Terminal, contentDescription = "Scan void", tint = TextSecondary, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Query completed. No matching notes.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(searchResults) { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectNote(note.id)
                                onNavigateToNoteDetail(note.id)
                            }
                            .background(CyberSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorder.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
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
                                    modifier = Modifier.size(14.dp)
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
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = "Open note", tint = TextSecondary)
                    }
                }
            }
        }
    }
}
