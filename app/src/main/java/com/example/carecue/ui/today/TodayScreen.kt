package com.example.carecue.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.carecue.data.model.TaskItem
import com.example.carecue.data.model.TaskStatus
import com.example.carecue.ui.components.EditTaskDialog
import com.example.carecue.ui.components.TaskCard
import com.example.carecue.ui.viewmodel.CareCueViewModel
import com.example.ui.theme.AmberOrangePrimary
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalDarkest
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.MutedText
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun TodayScreen(
    viewModel: CareCueViewModel,
    onNavigateToAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val tz = try {
        TimeZone.getTimeZone(settings.timeZoneId)
    } catch (_: Exception) {
        TimeZone.getDefault()
    }

    val cal = Calendar.getInstance(tz)
    val todayStart = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEnd = todayStart + (24 * 3600 * 1000)

    val overdueTasks = tasks.filter { it.getStatus() == TaskStatus.OVERDUE }
    val todayTasks = tasks.filter { task ->
        task.dueTimestamp != null && task.dueTimestamp in todayStart..todayEnd
    }
    val todayPending = todayTasks.filter { !it.isCompleted }
    val todayDone = todayTasks.filter { it.isCompleted }

    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).apply {
        timeZone = tz
    }
    val todayDateStr = dateFormat.format(Date())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalDarkest)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = todayDateStr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AmberOrangePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Today's Care",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = SoftWhite
                            )
                        )
                    }

                    // Count badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CharcoalSurfaceElevated)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${todayPending.size} pending",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (todayPending.isEmpty()) StatusSuccess else SoftWhite,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Overdue Alert Banner if any
            if (overdueTasks.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StatusOverdue.copy(alpha = 0.12f))
                            .border(1.dp, StatusOverdue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(StatusOverdue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Overdue tasks",
                                    tint = StatusOverdue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "${overdueTasks.size} Overdue Task${if (overdueTasks.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = StatusOverdue,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Needs your attention today",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SoftWhite)
                                )
                            }
                        }
                    }
                }

                items(overdueTasks, key = { "overdue_${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { viewModel.toggleTask(task.id) },
                        onEdit = { taskToEdit = task },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }

            // Today's Tasks section
            if (todayTasks.isEmpty() && overdueTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CharcoalSurface)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "All Clear for Today!",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SoftWhite
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No tasks or reminders are pending for today. Tell CareCue if something comes up!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MutedText
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                if (todayPending.isNotEmpty()) {
                    item {
                        Text(
                            text = "Due Today (${todayPending.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    items(todayPending, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTask(task.id) },
                            onEdit = { taskToEdit = task },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }

                if (todayDone.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Completed Today (${todayDone.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MutedText,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    items(todayDone, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTask(task.id) },
                            onEdit = { taskToEdit = task },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                // Open new task template for today
                val newToday = TaskItem(
                    title = "",
                    dueTimestamp = System.currentTimeMillis() + (2 * 3600 * 1000),
                    timeZoneId = settings.timeZoneId
                )
                taskToEdit = newToday
            },
            containerColor = AmberOrangePrimary,
            contentColor = CharcoalDarkest,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_today_task_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Today Task")
        }

        // Edit/Create Dialog
        if (taskToEdit != null) {
            EditTaskDialog(
                task = taskToEdit,
                onDismiss = { taskToEdit = null },
                onSave = { updated ->
                    if (tasks.any { it.id == updated.id }) {
                        viewModel.updateTask(updated)
                    } else {
                        viewModel.quickCreateTask(
                            title = updated.title,
                            description = updated.description,
                            category = updated.category,
                            dueTimestamp = updated.dueTimestamp
                        )
                    }
                    taskToEdit = null
                }
            )
        }
    }
}
