package io.tpersson.ufw.durablemessages.admin.contracts

import java.time.Instant

public data class MessageFailureDTO(
    val failureId: String,
    val messageId: String,
    val timestamp: Instant,
    val errorType: String,
    val errorMessage: String,
    val errorStackTrace: String,
)