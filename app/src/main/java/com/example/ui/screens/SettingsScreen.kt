package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CyberViewModel

@Composable
fun SettingsScreen(viewModel: CyberViewModel) {
    val secPinEnabled by viewModel.securityPinEnabled.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val voiceTranscript by viewModel.voiceTranscript.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var pinSetupCode by remember { mutableStateOf("") }
    var rawBackupCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 90.dp)
    ) {
        // --- Header Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NeonBlue)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "⚙️ NODE CONFIGURATOR //",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "System Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        // --- Cyber Security Center (Lock PIN) ---
        Text(
            text = "🛡️ SECURE CRYPTO SHELL",
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Terminal Biosecurity Pin", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Require 4-digit decrypt code to view vault pages.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = secPinEnabled,
                        onCheckedChange = { toggle ->
                            if (!toggle) {
                                viewModel.securityPinEnabled.value = false
                                Toast.makeText(context, "Lock system offline.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Enter experimental PIN below to toggle.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonBlue, checkedTrackColor = NeonBlue.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pinSetupCode,
                    onValueChange = { if (it.length <= 4) pinSetupCode = it },
                    placeholder = { Text("Set 4-Digit Security Code", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonBlue,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = CyberSurfaceVariant,
                        unfocusedContainerColor = CyberSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (pinSetupCode.length == 4) {
                            viewModel.setPinCode(pinSetupCode)
                            Toast.makeText(context, "Crypto Shield Locked with PIN: $pinSetupCode", Toast.LENGTH_SHORT).show()
                            pinSetupCode = ""
                        } else {
                            Toast.makeText(context, "Code must be exactly 4 digits.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("LOCK SHIELD", color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Voice Transcription Module ---
        Text(
            text = "🎙️ SYNAPTIC VOICE DICTATION",
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Ambient Dictation Engine", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = "Formulate draft notes hands-free. Scans auditory spectrum for custom links.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.runVoiceTranscription() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecordingVoice) NeonPink else NeonPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isRecordingVoice) Icons.Default.PauseCircleFilled else Icons.Default.Mic,
                        contentDescription = "Mic"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecordingVoice) "RECORDING SPECTRUM..." else "START DICTATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                if (voiceTranscript.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(voiceTranscript, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Archive recovery Backup System ---
        Text(
            text = "⚡ SECURE ROOM BACKUP CORE",
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Export & Import Systems", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = "Export the entire room schema string or paste backups code to recover assets.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val backupStr = viewModel.exportBackupJson()
                            clipboardManager.setText(AnnotatedString(backupStr))
                            Toast.makeText(context, "Vault Schema Copied to Clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SYS_EXPORT", color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            if (rawBackupCode.isNotBlank()) {
                                val success = viewModel.restoreBackupImport(rawBackupCode)
                                if (success) {
                                    Toast.makeText(context, "Room restored successfully.", Toast.LENGTH_SHORT).show()
                                    rawBackupCode = ""
                                } else {
                                    Toast.makeText(context, "Restoration failure. Key mismatched.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Paste string backup below first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Import")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SYS_RESTORE", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pasta Area
                OutlinedTextField(
                    value = rawBackupCode,
                    onValueChange = { rawBackupCode = it },
                    placeholder = { Text("Paste recovery string payload directly here to restore archives...", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = CyberSurfaceVariant,
                        unfocusedContainerColor = CyberSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System specs card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CYBERNOTE v1.2026 // RETINA INTEGRAL",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                Text(
                    text = "SECURE LOCAL STORAGE ENCRYPTED",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = NeonGreen
                )
            }
        }

    }
}
