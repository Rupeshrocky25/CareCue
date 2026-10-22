package com.example.carecue.data.model

import java.util.UUID

enum class ActionType(val label: String) {
    CREATE_REMINDER("Create Reminder"),
    ADD_TO_SHOPPING_LIST("Add to Shopping List"),
    RESCHEDULE_TASK("Reschedule Reminder"),
    COMPLETE_TASK("Mark Complete"),
    DELETE_TASK("Delete Reminder"),
    QUERY_TASKS("Query Tasks");
}

data class ProposedAction(
    val id: String = UUID.randomUUID().toString(),
    val actionType: ActionType,
    val title: String,
    val description: String? = null,
    val details: String? = null,
    val dueTimestamp: Long? = null,
    val timeZoneId: String = "UTC",
    val category: TaskCategory = TaskCategory.GENERAL,
    val targetTaskId: String? = null,
    val itemsToAdd: List<String> = emptyList(),
    val clarificationQuestion: String? = null,
    val originalQuery: String = ""
)

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposedActions: List<ProposedAction> = emptyList(),
    val isConfirmed: Boolean = false,
    val isCancelled: Boolean = false,
    val executionResult: String? = null,
    val isError: Boolean = false
)
