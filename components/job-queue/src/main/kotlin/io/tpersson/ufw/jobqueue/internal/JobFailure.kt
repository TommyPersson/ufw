package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.JobId
import java.time.Instant

public data class JobFailure(
    val jobId: JobId,
    val timestamp: Instant,
    val errorType: String,
    val errorMessage: String,
    val errorStackTrace: String,
)