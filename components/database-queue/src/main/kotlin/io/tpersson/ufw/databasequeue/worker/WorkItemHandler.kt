package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity

public interface WorkItemHandler {
    public suspend fun handle(item: WorkItemDbEntity)
}