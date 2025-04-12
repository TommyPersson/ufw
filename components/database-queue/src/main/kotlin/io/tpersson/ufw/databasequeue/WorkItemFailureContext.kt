package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import java.time.Instant
import java.time.Clock

public interface WorkItemFailureContext {
    public val clock: Clock
    public val timestamp: Instant
    public val failureCount: Int
    public val unitOfWork: UnitOfWork
}

