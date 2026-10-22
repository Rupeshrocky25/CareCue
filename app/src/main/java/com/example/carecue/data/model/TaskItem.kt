package com.example.carecue.data.model

import java.util.UUID

enum class TaskCategory(val displayName: String, val iconName: String) {
    FAMILY("Family & Care", "favorite"),
    HOUSEHOLD("Household", "home"),
    HEALTH("Health & Wellness", "medical_services"),
    SHOPPING("Shopping & Errands", "shopping_cart"),
    GENERAL("General", "task_alt");

    companion object {
        fun fromString(value: String): TaskCategory {
            return values().find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: GENERAL
        }
    }
}

enum class TaskStatus {
    PENDING,
    COMPLETED,
    OVERDUE
}

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false
)

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val category: TaskCategory = TaskCategory.GENERAL,
    val dueTimestamp: Long? = null,
    val timeZoneId: String = "UTC",
    val isCompleted: Boolean = false,
    val completedTimestamp: Long? = null,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Overdue is derived dynamically from dueTimestamp and completion status,
     * not stored as an unreliable static flag.
     */
    fun getStatus(currentTimeMillis: Long = System.currentTimeMillis()): TaskStatus {
        return when {
            isCompleted -> TaskStatus.COMPLETED
            dueTimestamp != null && dueTimestamp < currentTimeMillis -> TaskStatus.OVERDUE
            else -> TaskStatus.PENDING
        }
    }
}
