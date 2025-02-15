package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import java.time.Instant

public interface WorkItemsDAO {
    public suspend fun insert(
        item: WorkItemDbEntity,
        unitOfWork: UnitOfWork
    )

    public suspend fun getById(
        id: String
    ): WorkItemDbEntity?

    public suspend fun listAllItems(): List<WorkItemDbEntity>

    public suspend fun takeNext(queueId: String, watchdogId: String, now: Instant): WorkItemDbEntity?

    public suspend fun debugTruncate()
}