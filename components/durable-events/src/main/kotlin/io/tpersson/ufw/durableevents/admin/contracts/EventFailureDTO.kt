package io.tpersson.ufw.durableevents.admin.contracts

import java.time.Instant

public data class EventFailureDTO(
    val failureId: String,
    val eventId: String,
    val timestamp: Instant,
    val errorType: String,
    val errorMessage: String,
    val errorStackTrace: String,
)