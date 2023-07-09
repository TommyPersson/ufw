package io.tpersson.ufw.jobqueue.internal

import java.time.Instant
import java.util.*

public data class JobFailure(
    val id: UUID,
    val jobUid: Long,
    val timestamp: Instant,
    val errorType: String,
    val errorMessage: String,
    val errorStackTrace: String,
)