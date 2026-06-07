package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.GlassBg
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CyberViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: CyberViewModel = viewModel()
        val isLocked by viewModel.isLocked.collectAsState()
        
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          if (isLocked) {
            CryptographicLockScreen(viewModel)
          } else {
            MainAppLayout(viewModel)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppLayout(viewModel: CyberViewModel) {
    var activeTab by remember { mutableStateOf("dashboard") }
    val activeNoteId by viewModel.activeNoteId.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switching animations
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() with fadeOut()
                }
            ) { targetTab ->
                when (targetTab) {
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToNotes = { activeTab = "notes" },
                        onNavigateToGraph = { activeTab = "graph" },
                        onNavigateToNoteDetail = { noteId ->
                            viewModel.selectNote(noteId)
                            activeTab = "notes"
                        }
                    )
                    "notes" -> NotesScreen(
                        viewModel = viewModel,
                        onNavigateToGraph = { activeTab = "graph" },
                        onEditNote = { /* Covered internally */ },
                        activeNoteId = activeNoteId
                    )
                    "graph" -> GraphScreen(
                        viewModel = viewModel,
                        onNavigateToNoteDetail = { noteId ->
                            viewModel.selectNote(noteId)
                            activeTab = "notes"
                        }
                    )
                    "calendar" -> CalendarScreen(
                        viewModel = viewModel,
                        onNavigateToNoteDetail = { noteId ->
                            viewModel.selectNote(noteId)
                            activeTab = "notes"
                        }
                    )
                    "tasks" -> TasksScreen(viewModel = viewModel)
                    "whiteboard" -> WhiteboardScreen(viewModel = viewModel)
                    "search" -> SearchScreen(
                        viewModel = viewModel,
                        onNavigateToNoteDetail = { noteId ->
                            viewModel.selectNote(noteId)
                            activeTab = "notes"
                        }
                    )
                    "settings" -> SettingsScreen(viewModel = viewModel)
                }
            }

            // --- Floating Dark Neon Navigation Bar ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = GlassBg),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabNavItem(
                            icon = Icons.Default.SpaceDashboard,
                            label = "Core",
                            isSelected = activeTab == "dashboard",
                            onClick = {
                                activeTab = "dashboard"
                                viewModel.selectNote(null) // Unselect pages Detail
                            }
                        )
                        TabNavItem(
                            icon = Icons.Default.Folder,
                            label = "Vault",
                            isSelected = activeTab == "notes",
                            onClick = { activeTab = "notes" }
                        )
                        TabNavItem(
                            icon = Icons.Default.Hub,
                            label = "Graph",
                            isSelected = activeTab == "graph",
                            onClick = {
                                activeTab = "graph"
                                viewModel.selectNote(null)
                            }
                        )
                        TabNavItem(
                            icon = Icons.Default.ViewKanban,
                            label = "Sprint",
                            isSelected = activeTab == "tasks",
                            onClick = {
                                activeTab = "tasks"
                                viewModel.selectNote(null)
                            }
                        )
                        TabNavItem(
                            icon = Icons.Default.Palette,
                            label = "Board",
                            isSelected = activeTab == "whiteboard",
                            onClick = {
                                activeTab = "whiteboard"
                                viewModel.selectNote(null)
                            }
                        )
                        TabNavItem(
                            icon = Icons.Default.CalendarMonth,
                            label = "Chrono",
                            isSelected = activeTab == "calendar",
                            onClick = {
                                activeTab = "calendar"
                                viewModel.selectNote(null)
                            }
                        )
                        TabNavItem(
                            icon = Icons.Default.Search,
                            label = "Scan",
                            isSelected = activeTab == "search",
                            onClick = {
                                activeTab = "search"
                                viewModel.selectNote(null)
                            }
                        )
                        TabNavItem(
                            icon = Icons.Default.Settings,
                            label = "Config",
                            isSelected = activeTab == "settings",
                            onClick = {
                                activeTab = "settings"
                                viewModel.selectNote(null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) NeonBlue else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) NeonBlue else TextSecondary
        )
    }
}

// --- Cryptographic Biometric Keypad Lock Screen ---

@Composable
fun CryptographicLockScreen(viewModel: CyberViewModel) {
    var pinValue by remember { mutableStateOf("") }
    val authError by viewModel.authError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF07080B), Color(0xFF131520))
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Lock",
            tint = NeonPink,
            modifier = Modifier
                .size(72.dp)
                .border(2.dp, NeonPink, CircleShape)
                .padding(14.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "CYBERNOTE SECURITY GATEWAY",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = NeonPink,
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )

        Text(
            text = "SECURE PROTOCOL TERMINATED",
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Dots Code Row Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            for (i in 0..3) {
                val hasChar = pinValue.length > i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (hasChar) NeonPink else Color.Transparent,
                            CircleShape
                        )
                        .border(2.dp, NeonPink, CircleShape)
                )
            }
        }

        if (authError.isNotEmpty()) {
            Text(
                text = authError,
                color = NeonPink,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Grid Keypad
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(280.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "🔓")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { k ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GlassBg)
                                .border(1.dp, GlassBorder, CircleShape)
                                .clickable {
                                    when (k) {
                                        "C" -> pinValue = ""
                                        "🔓" -> {
                                            viewModel.requestPinRelease(pinValue)
                                            pinValue = ""
                                        }
                                        else -> {
                                            if (pinValue.length < 4) {
                                                pinValue += k
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = k,
                                color = if (k == "🔓") NeonGreen else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
