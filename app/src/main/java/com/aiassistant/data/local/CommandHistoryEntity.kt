package com.aiassistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aiassistant.domain.model.CommandHistory

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey
    val id: String,
    val userCommand: String,
    val actions: String, // JSON serialized
    val success: Boolean,
    val timestamp: Long
)

fun CommandHistoryEntity.toDomain(actions: List<String>): CommandHistory {
    return CommandHistory(
        id = id,
        userCommand = userCommand,
        actions = actions,
        success = success,
        timestamp = timestamp
    )
}

fun CommandHistory.toEntity(): CommandHistoryEntity {
    return CommandHistoryEntity(
        id = id,
        userCommand = userCommand,
        actions = actions.joinToString("|||"),
        success = success,
        timestamp = timestamp
    )
}
