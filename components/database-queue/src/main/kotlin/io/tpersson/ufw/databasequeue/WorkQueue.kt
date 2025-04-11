package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import kotlinx.coroutines.flow.SharedFlow
import java.time.Instant

public interface WorkQueue {

    public val stateChanges: SharedFlow<WorkItemStateChange>

    public suspend fun schedule(
        item: NewWorkItem,
        now: Instant,
        unitOfWork: UnitOfWork? = null,
    )
}

