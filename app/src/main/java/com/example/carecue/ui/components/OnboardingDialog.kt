package com.example.carecue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDarkest
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SunsetOrangeContainer
import com.example.ui.theme.SunsetOrangePrimary
import java.util.TimeZone

@Composable
fun OnboardingDialog(
    initialTimezoneId: String,
    onComplete: (userName: String, focus: String, timeZoneId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTimezone by remember { mutableStateOf(initialTimezoneId) }
    var tzExpanded by remember { mutableStateOf(false) }
    var selectedFocus by remember { mutableStateOf("Family & Household Care") }
    var nameError by remember { mutableStateOf<String?>(null) }

    val focusOptions = listOf(
        "Family & Household Care",
        "Medication & Health",
        "Groceries & Errands",
        "Personal Daily Cues"
    )

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

    Dialog(
        onDismissRequest = {}, // Non-cancellable until user completes setup
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ObsidianSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header with glowing icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SunsetOrangeContainer)
                            .border(1.dp, SunsetOrangePrimary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WavingHand,
                            contentDescription = null,
                            tint = SunsetOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Welcome to CareCue",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Text(
                            text = "Let's set up your personal assistant",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Question 1: User / Household Name
                Text(
                    text = "1. What should CareCue call you?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    placeholder = { Text("Your name (e.g. Alex, Sarah, Mom)...", color = SlateMuted, fontSize = 13.sp) },
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(nameError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = SunsetOrangePrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = ObsidianDarkest,
                        unfocusedContainerColor = ObsidianDarkest
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Question 2: Timezone
                Text(
                    text = "2. Confirm your timezone for reminders",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedButton(
                        onClick = { tzExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_timezone_selector"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = SunsetOrangePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedTimezone, fontSize = 13.sp)
                            }
                            Text("Change", fontSize = 11.sp, color = SunsetOrangePrimary)
                        }
                    }

                    DropdownMenu(
                        expanded = tzExpanded,
                        onDismissRequest = { tzExpanded = false },
                        modifier = Modifier.background(ObsidianSurface)
                    ) {
                        commonTimezones.forEach { tz ->
                            DropdownMenuItem(
                                text = { Text(tz, color = PureWhite) },
                                onClick = {
                                    selectedTimezone = tz
                                    tzExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Question 3: Primary Care Focus
                Text(
                    text = "3. Primary care priority",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    focusOptions.forEach { opt ->
                        val isSelected = selectedFocus == opt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SunsetOrangePrimary.copy(alpha = 0.15f) else ObsidianSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelected) SunsetOrangePrimary else ObsidianBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedFocus = opt }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = opt,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) SunsetOrangePrimary else PureWhite
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SunsetOrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Finish Setup Button
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isEmpty()) {
                            nameError = "Please enter your name"
                            return@Button
                        }
                        onComplete(trimmedName, selectedFocus, selectedTimezone)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_complete_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SunsetOrangePrimary,
                        contentColor = ObsidianDarkest
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Get Started with CareCue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
