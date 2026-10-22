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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.carecue.data.model.TaskCategory
import com.example.carecue.data.model.TaskItem
import com.example.carecue.data.model.TaskStatus
import com.example.carecue.engine.CareCueSmartEngine
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDarkest
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SunsetOrangePrimary
import java.util.TimeZone

@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val status = task.getStatus()
    val smartEngine = CareCueSmartEngine()
    val tz = try {
        TimeZone.getTimeZone(task.timeZoneId)
    } catch (_: Exception) {
        TimeZone.getDefault()
    }

    val isOverdue = status == TaskStatus.OVERDUE
    val isCompleted = status == TaskStatus.COMPLETED

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurface)
            .border(
                width = 1.dp,
                color = when {
                    isOverdue -> StatusOverdue.copy(alpha = 0.5f)
                    isCompleted -> ObsidianBorder.copy(alpha = 0.3f)
                    else -> ObsidianBorder
                },
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("task_item_${task.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Checkbox toggle (48dp touch target accessible)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> StatusSuccess
                                isOverdue -> StatusOverdue.copy(alpha = 0.15f)
                                else -> ObsidianDarkest
                            }
                        )
                        .border(
                            width = 1.5.dp,
                            color = when {
                                isCompleted -> StatusSuccess
                                isOverdue -> StatusOverdue
                                else -> SunsetOrangePrimary.copy(alpha = 0.6f)
                            },
                            shape = CircleShape
                        )
                        .clickable { onToggleComplete() }
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = ObsidianDarkest,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) SlateMuted else PureWhite,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                            fontSize = 15.sp
                        )
                    )

                    if (!task.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = SlateMuted,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges row: Category & Due Time & Checklist counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CategoryBadge(category = task.category)

                        if (task.dueTimestamp != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isOverdue) StatusOverdue.copy(alpha = 0.15f) else ObsidianDarkest)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.Event,
                                    contentDescription = null,
                                    tint = if (isOverdue) StatusOverdue else SunsetOrangePrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOverdue) "Overdue • ${smartEngine.formatDueTime(task.dueTimestamp, tz)}"
                                    else smartEngine.formatDueTime(task.dueTimestamp, tz),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isOverdue) StatusOverdue else SunsetOrangePrimary
                                )
                            }
                        }

                        if (task.checklistItems.isNotEmpty()) {
                            val doneCount = task.checklistItems.count { it.isCompleted }
                            Text(
                                text = "$doneCount/${task.checklistItems.size} items",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                    }
                }

                // Overflow menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp).testTag("task_menu_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Task options",
                            tint = SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(ObsidianSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Task", color = PureWhite) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = SunsetOrangePrimary)
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = StatusOverdue) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = StatusOverdue)
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: TaskCategory) {
    val (icon, color) = when (category) {
        TaskCategory.FAMILY -> Icons.Default.Favorite to Color(0xFFFF6584)
        TaskCategory.HEALTH -> Icons.Default.MedicalServices to Color(0xFF38BDF8)
        TaskCategory.HOUSEHOLD -> Icons.Default.Home to Color(0xFF10B981)
        TaskCategory.SHOPPING -> Icons.Default.ShoppingBag to SunsetOrangePrimary
        TaskCategory.GENERAL -> Icons.Default.Check to SlateMuted
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = category.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
