package com.example.carecue.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.carecue.data.model.ActionType
import com.example.carecue.data.model.ChatMessage
import com.example.carecue.data.model.MessageSender
import com.example.carecue.data.model.ProposedAction
import com.example.carecue.data.model.TaskCategory
import com.example.carecue.data.model.TaskItem
import com.example.carecue.data.repository.CareCueSettings
import com.example.carecue.data.repository.SettingsRepository
import com.example.carecue.data.repository.TaskRepository
import com.example.carecue.engine.CareCueSmartEngine
import com.example.carecue.notifications.CareCueNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.UUID

class CareCueViewModel(
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val smartEngine: CareCueSmartEngine = CareCueSmartEngine()
) : ViewModel() {

    val tasks: StateFlow<List<TaskItem>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = taskRepository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<CareCueSettings> = settingsRepository.settings

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TaskCategory?>(null)
    val selectedCategory: StateFlow<TaskCategory?> = _selectedCategory.asStateFlow()

    init {
        viewModelScope.launch {
            val messages = taskRepository.chatMessages.first()
            if (messages.isEmpty()) {
                taskRepository.initializeCleanChat(settings.value.userName)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoryFilter(category: TaskCategory?) {
        _selectedCategory.value = category
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = trimmed,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            taskRepository.saveChatMessage(userMessage)
            _isProcessing.value = true

            try {
                val currentTasks = tasks.value
                val tzId = settings.value.timeZoneId

                // Process through the CareCue Smart Engine
                val parseResult = smartEngine.processInput(trimmed, currentTasks, tzId)

                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = MessageSender.ASSISTANT,
                    text = parseResult.replyText,
                    timestamp = System.currentTimeMillis(),
                    proposedActions = parseResult.proposedActions
                )
                taskRepository.saveChatMessage(assistantMessage)
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = MessageSender.ASSISTANT,
                    text = "I encountered an error processing that request: ${e.message ?: "Unknown error"}. Please try again.",
                    timestamp = System.currentTimeMillis(),
                    isError = true
                )
                taskRepository.saveChatMessage(errorMsg)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun confirmAction(messageId: String, actionId: String) {
        viewModelScope.launch {
            val messages = chatMessages.value
            val targetMessage = messages.find { it.id == messageId } ?: return@launch
            val targetAction = targetMessage.proposedActions.find { it.id == actionId } ?: return@launch

            executeAction(targetAction, targetMessage, listOf(targetAction))
        }
    }

    fun confirmAllActions(messageId: String) {
        viewModelScope.launch {
            val messages = chatMessages.value
            val targetMessage = messages.find { it.id == messageId } ?: return@launch
            if (targetMessage.proposedActions.isEmpty()) return@launch

            for (action in targetMessage.proposedActions) {
                executeSingleAction(action)
            }

            val count = targetMessage.proposedActions.size
            val updatedMsg = targetMessage.copy(
                isConfirmed = true,
                executionResult = "Successfully validated and saved $count action${if (count > 1) "s" else ""}."
            )
            taskRepository.updateChatMessage(updatedMsg)
        }
    }

    fun cancelAllActions(messageId: String) {
        viewModelScope.launch {
            val messages = chatMessages.value
            val targetMessage = messages.find { it.id == messageId } ?: return@launch
            val updatedMsg = targetMessage.copy(
                isCancelled = true,
                executionResult = "Action cancelled."
            )
            taskRepository.updateChatMessage(updatedMsg)
        }
    }

    private suspend fun executeAction(
        action: ProposedAction,
        parentMessage: ChatMessage,
        allActions: List<ProposedAction>
    ) {
        val success = executeSingleAction(action)
        if (success) {
            val updatedMsg = parentMessage.copy(
                isConfirmed = true,
                executionResult = "Validated and saved to your schedule."
            )
            taskRepository.updateChatMessage(updatedMsg)
        } else {
            val updatedMsg = parentMessage.copy(
                executionResult = "Failed to save action. Please try again.",
                isError = true
            )
            taskRepository.updateChatMessage(updatedMsg)
        }
    }

    private suspend fun executeSingleAction(action: ProposedAction): Boolean {
        return try {
            when (action.actionType) {
                ActionType.CREATE_REMINDER -> {
                    val result = taskRepository.createTask(
                        title = action.title,
                        description = action.description,
                        category = action.category,
                        dueTimestamp = action.dueTimestamp,
                        timeZoneId = action.timeZoneId
                    )
                    result.isSuccess
                }
                ActionType.ADD_TO_SHOPPING_LIST -> {
                    val result = taskRepository.addShoppingItems(
                        items = action.itemsToAdd,
                        targetDateTimestamp = action.dueTimestamp
                    )
                    result.isSuccess
                }
                ActionType.RESCHEDULE_TASK -> {
                    val taskId = action.targetTaskId
                    val newTs = action.dueTimestamp
                    if (taskId != null && newTs != null) {
                        taskRepository.rescheduleTask(taskId, newTs).isSuccess
                    } else false
                }
                ActionType.COMPLETE_TASK -> {
                    val taskId = action.targetTaskId
                    if (taskId != null) {
                        taskRepository.toggleTaskCompletion(taskId).isSuccess
                    } else false
                }
                ActionType.DELETE_TASK -> {
                    val taskId = action.targetTaskId
                    if (taskId != null) {
                        taskRepository.deleteTask(taskId).isSuccess
                    } else false
                }
                ActionType.QUERY_TASKS -> true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun quickCreateTask(
        title: String,
        description: String?,
        category: TaskCategory,
        dueTimestamp: Long?
    ) {
        viewModelScope.launch {
            taskRepository.createTask(
                title = title,
                description = description,
                category = category,
                dueTimestamp = dueTimestamp,
                timeZoneId = settings.value.timeZoneId
            )
        }
    }

    fun updateTask(task: TaskItem) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun toggleChecklistItem(taskId: String, itemId: String) {
        viewModelScope.launch {
            taskRepository.toggleChecklistItem(taskId, itemId)
        }
    }

    fun addChecklistItem(taskId: String, text: String) {
        viewModelScope.launch {
            taskRepository.addChecklistItem(taskId, text)
        }
    }

    fun removeChecklistItem(taskId: String, itemId: String) {
        viewModelScope.launch {
            taskRepository.removeChecklistItem(taskId, itemId)
        }
    }

    fun updateTimeZone(timeZoneId: String) {
        settingsRepository.updateTimeZone(timeZoneId)
    }

    fun updateAiProvider(provider: String) {
        settingsRepository.updateAiProvider(provider)
    }

    fun updateApiKey(key: String) {
        settingsRepository.updateApiKey(key)
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        settingsRepository.updateNotificationsEnabled(enabled)
    }

    fun sendTestNotification(context: Context): Boolean {
        return CareCueNotificationHelper.sendTestNotification(context)
    }

    fun completeOnboarding(userName: String, focus: String, timeZoneId: String) {
        settingsRepository.completeOnboarding(userName, focus, timeZoneId)
        viewModelScope.launch {
            taskRepository.initializeCleanChat(userName)
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            taskRepository.resetAllData()
            taskRepository.initializeCleanChat(settings.value.userName)
        }
    }

    class Factory(
        private val taskRepository: TaskRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CareCueViewModel::class.java)) {
                return CareCueViewModel(taskRepository, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
