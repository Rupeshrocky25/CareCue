package com.example.carecue.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carecue.data.model.ActionType
import com.example.carecue.data.model.ProposedAction
import com.example.carecue.engine.CareCueSmartEngine
import com.example.ui.theme.*
import java.util.TimeZone

@Composable
fun ActionReviewCard(
    proposedActions: List<ProposedAction>,
    isConfirmed: Boolean,
    isCancelled: Boolean,
    executionResult: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val smartEngine = CareCueSmartEngine()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CharcoalSurfaceElevated)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("action_review_card")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AmberOrangePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Review Action",
                            tint = AmberOrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Proposed Action Review",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SoftWhite
                        )
                    )
                }

                // Status Badge
                when {
                    isConfirmed -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Saved",
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Saved",
                                    color = StatusSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    isCancelled -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Cancelled",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberOrangePrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Requires Confirmation",
                                color = AmberOrangePrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Items List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                proposedActions.forEach { action ->
                    ActionItemRow(action = action, smartEngine = smartEngine)
                }
            }

            // Truthful Execution Result banner
            if (executionResult != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isConfirmed) StatusSuccess.copy(alpha = 0.1f) else CharcoalDarkest)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = executionResult,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isConfirmed) StatusSuccess else MutedText,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Confirmation Controls
            if (!isConfirmed && !isCancelled) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("action_cancel_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MutedText
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("action_confirm_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberOrangePrimary,
                            contentColor = CharcoalDarkest
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (proposedActions.size > 1) "Confirm & Save (${proposedActions.size})" else "Confirm & Save",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionItemRow(
    action: ProposedAction,
    smartEngine: CareCueSmartEngine
) {
    val tz = try {
        TimeZone.getTimeZone(action.timeZoneId)
    } catch (_: Exception) {
        TimeZone.getDefault()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CharcoalSurface)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            val icon = when (action.actionType) {
                ActionType.CREATE_REMINDER -> Icons.Default.Notifications
                ActionType.ADD_TO_SHOPPING_LIST -> Icons.Default.ShoppingBag
                ActionType.RESCHEDULE_TASK -> Icons.Default.Event
                ActionType.COMPLETE_TASK -> Icons.Default.Check
                ActionType.DELETE_TASK -> Icons.Default.Close
                ActionType.QUERY_TASKS -> Icons.Default.ListAlt
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmberOrangePrimary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = SoftWhite
                    )
                )

                if (action.actionType == ActionType.ADD_TO_SHOPPING_LIST && action.itemsToAdd.isNotEmpty()) {
                    Text(
                        text = "Items: ${action.itemsToAdd.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                    )
                }

                if (action.dueTimestamp != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Due time",
                            tint = AmberOrangePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = smartEngine.formatDueTime(action.dueTimestamp, tz),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AmberOrangePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                if (action.details != null) {
                    Text(
                        text = action.details,
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedText)
                    )
                }
            }
        }
    }
}
