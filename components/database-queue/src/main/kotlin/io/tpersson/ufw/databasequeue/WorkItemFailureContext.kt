package io.tpersson.ufw.databasequeue

import java.time.Instant
import java.time.InstantSource

public interface WorkItemFailureContext {
    public val clock: InstantSource
    public val timestamp: Instant
    public val failureCount: Int
}