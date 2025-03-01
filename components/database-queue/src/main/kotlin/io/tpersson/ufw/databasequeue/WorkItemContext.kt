package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import java.time.Instant
import java.time.InstantSource

public interface WorkItemContext {
    public val clock: InstantSource
    public val timestamp: Instant
    public val failureCount: Int
    public val unitOfWork: UnitOfWork
}