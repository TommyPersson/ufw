package io.tpersson.ufw.transactionalevents.handler.internal

import java.time.Instant
import java.util.*

public data class EventFailure(
    val id: UUID,
    val eventUid: Long,
    val timestamp: Instant,
    val errorType: String,
    val errorMessage: String,
    val errorStackTrace: String,
)