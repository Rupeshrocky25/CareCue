package com.example.carecue.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.carecue.data.model.ChatMessage
import com.example.carecue.data.model.MessageSender
import com.example.carecue.data.model.ProposedAction

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val proposedActions: List<ProposedAction>,
    val isConfirmed: Boolean,
    val isCancelled: Boolean,
    val executionResult: String?,
    val isError: Boolean
) {
    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            sender = MessageSender.valueOf(sender),
            text = text,
            timestamp = timestamp,
            proposedActions = proposedActions,
            isConfirmed = isConfirmed,
            isCancelled = isCancelled,
            executionResult = executionResult,
            isError = isError
        )
    }

    companion object {
        fun fromChatMessage(msg: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = msg.id,
                sender = msg.sender.name,
                text = msg.text,
                timestamp = msg.timestamp,
                proposedActions = msg.proposedActions,
                isConfirmed = msg.isConfirmed,
                isCancelled = msg.isCancelled,
                executionResult = msg.executionResult,
                isError = msg.isError
            )
        }
    }
}
