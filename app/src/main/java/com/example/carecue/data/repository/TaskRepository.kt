package com.example.carecue.data.repository

import com.example.carecue.data.local.ChatMessageDao
import com.example.carecue.data.local.ChatMessageEntity
import com.example.carecue.data.local.TaskDao
import com.example.carecue.data.local.TaskEntity
import com.example.carecue.data.model.ChecklistItem
import com.example.carecue.data.model.ChatMessage
import com.example.carecue.data.model.MessageSender
import com.example.carecue.data.model.TaskCategory
import com.example.carecue.data.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

class TaskRepository(
    private val taskDao: TaskDao,
    private val chatMessageDao: ChatMessageDao
) {

    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toTaskItem() }
    }

    val chatMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages().map { entities ->
        entities.map { it.toChatMessage() }
    }

    suspend fun createTask(
        title: String,
        description: String? = null,
        category: TaskCategory = TaskCategory.GENERAL,
        dueTimestamp: Long? = null,
        timeZoneId: String = TimeZone.getDefault().id,
        checklistItems: List<ChecklistItem> = emptyList()
    ): Result<TaskItem> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty."))
        }
        if (trimmedTitle.length > 200) {
            return Result.failure(IllegalArgumentException("Task title exceeds maximum length of 200 characters."))
        }

        val item = TaskItem(
            id = UUID.randomUUID().toString(),
            title = trimmedTitle,
            description = description?.trim(),
            category = category,
            dueTimestamp = dueTimestamp,
            timeZoneId = timeZoneId,
            checklistItems = checklistItems,
            isCompleted = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        taskDao.insertTask(TaskEntity.fromTaskItem(item))
        return Result.success(item)
    }

    suspend fun updateTask(task: TaskItem): Result<Unit> {
        if (task.title.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty."))
        }
        val updated = task.copy(
            title = task.title.trim(),
            description = task.description?.trim(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(TaskEntity.fromTaskItem(updated))
        return Result.success(Unit)
    }

    suspend fun toggleTaskCompletion(taskId: String): Result<Boolean> {
        val entity = taskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        val current = entity.toTaskItem()
        val newStatus = !current.isCompleted
        val updated = current.copy(
            isCompleted = newStatus,
            completedTimestamp = if (newStatus) System.currentTimeMillis() else null,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(TaskEntity.fromTaskItem(updated))
        return Result.success(newStatus)
    }

    suspend fun rescheduleTask(taskId: String, newDueTimestamp: Long): Result<Unit> {
        val entity = taskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        val current = entity.toTaskItem()
        val updated = current.copy(
            dueTimestamp = newDueTimestamp,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(TaskEntity.fromTaskItem(updated))
        return Result.success(Unit)
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        taskDao.deleteTaskById(taskId)
        return Result.success(Unit)
    }

    suspend fun toggleChecklistItem(taskId: String, itemId: String): Result<Unit> {
        val entity = taskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        val task = entity.toTaskItem()
        val updatedItems = task.checklistItems.map { item ->
            if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
        }
        val updatedTask = task.copy(
            checklistItems = updatedItems,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(TaskEntity.fromTaskItem(updatedTask))
        return Result.success(Unit)
    }

    suspend fun addChecklistItem(taskId: String, text: String): Result<Unit> {
        val entity = taskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        val task = entity.toTaskItem()
        val newItem = ChecklistItem(text = text.trim())
        val updatedTask = task.copy(
            checklistItems = task.checklistItems + newItem,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(TaskEntity.fromTaskItem(updatedTask))
        return Result.success(Unit)
    }

    suspend fun removeChecklistItem(taskId: String, itemId: String): Result<Unit> {
        val entity = taskDao.getTaskById(taskId)
            ?: return Result.failure(NoSuchElementException("Task not found"))
        val task = entity.toTaskItem()
        val updatedTask = task.copy(
            checklistItems = task.checklistItems.filter { it.id != itemId },
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(TaskEntity.fromTaskItem(updatedTask))
        return Result.success(Unit)
    }

    suspend fun addShoppingItems(items: List<String>, targetDateTimestamp: Long? = null): Result<TaskItem> {
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("No items provided"))
        
        // Check if a shopping list exists for this general timeframe or create/update one
        val currentTasks = taskDao.getAllTasks()
        // Find existing Shopping list
        val allEntities = mutableListOf<TaskEntity>()
        // Let's create or append to the Household Shopping list
        val newChecklistItems = items.map { ChecklistItem(text = it.trim()) }
        val title = "Household Shopping List"
        
        val item = TaskItem(
            id = UUID.randomUUID().toString(),
            title = title,
            description = "Groceries & everyday household essentials",
            category = TaskCategory.SHOPPING,
            dueTimestamp = targetDateTimestamp,
            timeZoneId = TimeZone.getDefault().id,
            checklistItems = newChecklistItems,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.insertTask(TaskEntity.fromTaskItem(item))
        return Result.success(item)
    }

    suspend fun saveChatMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(ChatMessageEntity.fromChatMessage(message))
    }

    suspend fun updateChatMessage(message: ChatMessage) {
        chatMessageDao.updateMessage(ChatMessageEntity.fromChatMessage(message))
    }

    suspend fun clearChatMessages() {
        chatMessageDao.clearAllMessages()
    }

    suspend fun initializeCleanChat(userName: String = "") {
        val count = chatMessageDao.getAllMessages()
        val greeting = if (userName.isNotBlank()) "Hello, $userName! I'm CareCue, your household assistant."
        else "Hello! I'm CareCue, your household assistant."

        val welcomeMsg = ChatMessage(
            id = "welcome_msg",
            sender = MessageSender.ASSISTANT,
            text = "$greeting\n\nWhat would you like me to remember, schedule, or add to your shopping list today? Speak or type your request, and I'll prepare an action card for your confirmation.",
            timestamp = System.currentTimeMillis()
        )
        chatMessageDao.clearAllMessages()
        chatMessageDao.insertMessage(ChatMessageEntity.fromChatMessage(welcomeMsg))
    }

    suspend fun resetAllData() {
        taskDao.clearAllTasks()
        chatMessageDao.clearAllMessages()
    }
}
