package com.example.carecue.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carecue.notifications.CareCueNotificationHelper
import com.example.carecue.ui.viewmodel.CareCueViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun SettingsScreen(
    viewModel: CareCueViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTz by remember(settings.timeZoneId) { mutableStateOf(settings.timeZoneId) }
    var tzMenuExpanded by remember { mutableStateOf(false) }

    var apiKeyInput by remember(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var keyVisible by remember { mutableStateOf(false) }

    var providerMenuExpanded by remember { mutableStateOf(false) }

    val commonTimezones = listOf(
        TimeZone.getDefault().id,
        "Asia/Kolkata",
        "America/New_York",
        "America/Los_Angeles",
        "Europe/London",
        "Europe/Berlin",
        "Asia/Tokyo",
        "UTC"
    ).distinct()

    val currentTz = try {
        TimeZone.getTimeZone(selectedTz)
    } catch (_: Exception) {
        TimeZone.getDefault()
    }

    val timeFormat = SimpleDateFormat("h:mm:ss a, z", Locale.getDefault()).apply {
        timeZone = currentTz
    }
    val currentLocalTimeStr = timeFormat.format(Date())

    val aiProviders = listOf(
        "Local Smart Engine (Offline/Deterministic - Recommended)",
        "Google Gemini API",
        "OpenAI API",
        "Anthropic Claude API"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalDarkest)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Controls",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Timezone, honest notifications, and AI provider",
                style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
            )
        }

        // 1. Timezone Settings
        item {
            SettingsCard(
                title = "Timezone & Clock",
                icon = Icons.Default.Schedule
            ) {
                Text(
                    text = "Relative dates (such as “7 PM tomorrow”) are calculated strictly in this timezone to prevent shifting reminders.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CharcoalDarkest)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Current Local Time in ${currentTz.id}:",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                        Text(
                            text = currentLocalTimeStr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberOrangePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box {
                    OutlinedButton(
                        onClick = { tzMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("timezone_selector_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Selected Timezone: $selectedTz")
                    }

                    DropdownMenu(
                        expanded = tzMenuExpanded,
                        onDismissRequest = { tzMenuExpanded = false },
                        modifier = Modifier.background(CharcoalSurface)
                    ) {
                        commonTimezones.forEach { tzId ->
                            DropdownMenuItem(
                                text = { Text(tzId, color = SoftWhite) },
                                onClick = {
                                    selectedTz = tzId
                                    viewModel.updateTimeZone(tzId)
                                    tzMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Real Notifications System
        item {
            SettingsCard(
                title = "Reminders & Notifications",
                icon = Icons.Default.Notifications
            ) {
                val hasPerm = CareCueNotificationHelper.hasNotificationPermission(context)

                Text(
                    text = "CareCue delivers in-app alerts and Android system notifications when your reminders are due.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Notification Delivery",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = SoftWhite
                            )
                        )
                        Text(
                            text = if (hasPerm) "System notifications permitted" else "Permission required for system notifications",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (hasPerm) StatusSuccess else AmberOrangePrimary
                            )
                        )
                    }

                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { viewModel.updateNotificationsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CharcoalDarkest,
                            checkedTrackColor = AmberOrangePrimary,
                            uncheckedTrackColor = CharcoalBorder
                        ),
                        modifier = Modifier.testTag("notification_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Notification Action
                Button(
                    onClick = {
                        val sent = viewModel.sendTestNotification(context)
                        if (sent) {
                            Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Notification permission needed. Check system settings.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("send_test_notification_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberOrangePrimary.copy(alpha = 0.2f),
                        contentColor = AmberOrangePrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Test Notification Now", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 3. AI & Natural Language Provider
        item {
            SettingsCard(
                title = "Assistant Intelligence Provider",
                icon = Icons.Default.SmartToy
            ) {
                Text(
                    text = "Choose your preferred intent extraction provider. API keys are strictly configured at runtime and never hardcoded in source code or committed files.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Provider Dropdown
                Box {
                    OutlinedButton(
                        onClick = { providerMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("ai_provider_dropdown"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Provider: ${settings.aiProvider}")
                    }

                    DropdownMenu(
                        expanded = providerMenuExpanded,
                        onDismissRequest = { providerMenuExpanded = false },
                        modifier = Modifier.background(CharcoalSurface)
                    ) {
                        aiProviders.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider, color = SoftWhite) },
                                onClick = {
                                    viewModel.updateAiProvider(provider)
                                    providerMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Trade-offs summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CharcoalDarkest)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "• Local Smart Engine: 0ms latency, zero API keys required, works 100% offline, handles compound requests.\n• Gemini / OpenAI / Anthropic: Optional external LLMs for open-domain rephrasing.",
                        fontSize = 11.sp,
                        color = MutedText,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secure Runtime API Key Input
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key (Runtime Input Only)") },
                    placeholder = { Text("YOUR_API_KEY_HERE", color = MutedText) },
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                imageVector = if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle visibility",
                                tint = MutedText
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("runtime_api_key_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        focusedBorderColor = AmberOrangePrimary,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedLabelColor = AmberOrangePrimary,
                        unfocusedLabelColor = MutedText
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.updateApiKey(apiKeyInput)
                        Toast.makeText(context, "API Key saved securely in runtime storage", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_api_key_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberOrangePrimary,
                        contentColor = CharcoalDarkest
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Runtime Key", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Data & Privacy Controls
        item {
            SettingsCard(
                title = "Data & Privacy Controls",
                icon = Icons.Default.Refresh
            ) {
                Text(
                    text = "All reminders, care cues, and checklists are strictly stored locally on your device. There is zero sample or dummy data.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.clearAllUserData()
                        Toast.makeText(context, "All data cleared successfully.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("clear_all_data_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SunsetOrangePrimary,
                        contentColor = ObsidianDarkest
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All Data & Start Fresh", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. Hackathon & Submission About
        item {
            SettingsCard(
                title = "About CareCue",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = "CareCue: “Small things remembered. People better cared for.”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AmberOrangePrimary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Created for the Amazon Developer — Build, Ship, Shape Hackathon (Alexa+ simulated experience track).\n\nNotice: CareCue is an independent simulated assistant experience demonstrating conversational intent parsing, explicit action confirmation, and verified persistence.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MutedText,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurface)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AmberOrangePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AmberOrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SoftWhite
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}
