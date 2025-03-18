package io.tpersson.ufw.durablejobs.admin.contracts

import java.time.Instant

public data class JobFailureDTO(
    val failureId: String,
    val jobId: String,
    val timestamp: Instant,
    val errorType: String,
    val errorMessage: String,
    val errorStackTrace: String,
)