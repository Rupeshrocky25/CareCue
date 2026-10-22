package com.example.carecue.ui.schedule

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.carecue.ui.components.EditTaskDialog
import com.example.carecue.ui.components.TaskCard
import com.example.carecue.ui.viewmodel.CareCueViewModel
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDarkest
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.SunsetOrangePrimary
import java.util.Calendar
import java.util.TimeZone

@Composable
fun ScheduleScreen(
    viewModel: CareCueViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var showCompletedOnly by remember { mutableStateOf(false) }

    val tz = try {
        TimeZone.getTimeZone(settings.timeZoneId)
    } catch (_: Exception) {
        TimeZone.getDefault()
    }

    // Clean, streamlined filtering
    val filteredTasks = tasks.filter { task ->
        val matchesQuery = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                (task.description?.contains(searchQuery, ignoreCase = true) == true)
        val matchesStatus = if (showCompletedOnly) task.isCompleted else !task.isCompleted
        matchesQuery && matchesStatus
    }

    val cal = Calendar.getInstance(tz)
    val todayStart = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrowStart = todayStart + (24 * 3600 * 1000)

    val overdueGroup = filteredTasks.filter { it.dueTimestamp != null && it.dueTimestamp < todayStart && !it.isCompleted }
    val todayGroup = filteredTasks.filter { it.dueTimestamp != null && it.dueTimestamp in todayStart until tomorrowStart }
    val tomorrowGroup = filteredTasks.filter { it.dueTimestamp != null && it.dueTimestamp in tomorrowStart until (tomorrowStart + 24 * 3600 * 1000) }
    val upcomingGroup = filteredTasks.filter { it.dueTimestamp != null && it.dueTimestamp >= (tomorrowStart + 24 * 3600 * 1000) }
    val generalGroup = filteredTasks.filter { it.dueTimestamp == null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDarkest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Streamlined, Clean Search & Toggle Bar (Single clean row!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search schedule...", color = SlateMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = SunsetOrangePrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("schedule_search_input"),
                        shape = RoundedCornerShape(20.dp),
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

                    Spacer(modifier = Modifier.width(10.dp))

                    // Clean 2-state toggle pill: Active vs Done
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (showCompletedOnly) SunsetOrangePrimary else ObsidianSurfaceElevated)
                            .border(1.dp, if (showCompletedOnly) SunsetOrangePrimary else ObsidianBorder, RoundedCornerShape(20.dp))
                            .clickable { showCompletedOnly = !showCompletedOnly }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("toggle_completed_filter")
                    ) {
                        Text(
                            text = if (showCompletedOnly) "Completed" else "Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showCompletedOnly) ObsidianDarkest else PureWhite
                        )
                    }
                }
            }

            // Grouped Tasks List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = SlateMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (showCompletedOnly) "No completed tasks" else "No upcoming reminders",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (showCompletedOnly) "Tasks marked done will appear here." else "Say “Remind me to...” to add one!",
                            style = MaterialTheme.typography.bodySmall.copy(color = SlateMuted)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (overdueGroup.isNotEmpty()) {
                        item {
                            CleanSectionHeader(title = "Overdue (${overdueGroup.size})", isWarning = true)
                        }
                        items(overdueGroup, key = { "overdue_${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTask(task.id) },
                                onEdit = { taskToEdit = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }

                    if (todayGroup.isNotEmpty()) {
                        item {
                            CleanSectionHeader(title = "Today (${todayGroup.size})")
                        }
                        items(todayGroup, key = { "today_${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTask(task.id) },
                                onEdit = { taskToEdit = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }

                    if (tomorrowGroup.isNotEmpty()) {
                        item {
                            CleanSectionHeader(title = "Tomorrow (${tomorrowGroup.size})")
                        }
                        items(tomorrowGroup, key = { "tomorrow_${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTask(task.id) },
                                onEdit = { taskToEdit = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }

                    if (upcomingGroup.isNotEmpty()) {
                        item {
                            CleanSectionHeader(title = "Upcoming (${upcomingGroup.size})")
                        }
                        items(upcomingGroup, key = { "upcoming_${it.id}" }) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { viewModel.toggleTask(task.id) },
                                onEdit = { taskToEdit = task },
                                onDelete = { viewModel.deleteTask(task.id) }
                            )
                        }
                    }

                    if (generalGroup.isNotEmpty()) {
                        item {
                            CleanSectionHeader(title = "General Tasks (${generalGroup.size})")
                        }
                        items(generalGroup, key = { "general_${it.id}" }) { task ->
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
        }

        // Add Task FAB
        FloatingActionButton(
            onClick = {
                val newTask = TaskItem(
                    title = "",
                    timeZoneId = settings.timeZoneId
                )
                taskToEdit = newTask
            },
            containerColor = SunsetOrangePrimary,
            contentColor = ObsidianDarkest,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_schedule_task_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }

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

@Composable
private fun CleanSectionHeader(title: String, isWarning: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        if (isWarning) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StatusOverdue,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = if (isWarning) StatusOverdue else SunsetOrangePrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
    }
}
