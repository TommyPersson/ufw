package io.tpersson.ufw.databasequeue

import java.time.Instant

public data class WorkItemFailureDbEntity(
    val id: String,
    val itemUid: Long,
    val timestamp: Instant,
    val errorType: String,
    val errorMessage: String,
    val errorStackTract: String,
)
