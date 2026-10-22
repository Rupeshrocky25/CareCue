package com.example.carecue.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.carecue.data.model.ChecklistItem
import com.example.carecue.data.model.TaskCategory
import com.example.carecue.data.model.TaskItem

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val category: String,
    val dueTimestamp: Long?,
    val timeZoneId: String,
    val isCompleted: Boolean,
    val completedTimestamp: Long?,
    val checklistItems: List<ChecklistItem>,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toTaskItem(): TaskItem {
        return TaskItem(
            id = id,
            title = title,
            description = description,
            category = TaskCategory.fromString(category),
            dueTimestamp = dueTimestamp,
            timeZoneId = timeZoneId,
            isCompleted = isCompleted,
            completedTimestamp = completedTimestamp,
            checklistItems = checklistItems,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromTaskItem(item: TaskItem): TaskEntity {
            return TaskEntity(
                id = item.id,
                title = item.title,
                description = item.description,
                category = item.category.name,
                dueTimestamp = item.dueTimestamp,
                timeZoneId = item.timeZoneId,
                isCompleted = item.isCompleted,
                completedTimestamp = item.completedTimestamp,
                checklistItems = item.checklistItems,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
        }
    }
}
