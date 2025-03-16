package io.tpersson.ufw.databasequeue.internal

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.unitofwork.UnitOfWork

public interface WorkItemFailuresDAO {
    public fun insertFailure(
        failure: WorkItemFailureDbEntity,
        unitOfWork: UnitOfWork,
    )

    public suspend fun debugTruncate()

    public suspend fun listFailuresForWorkItem(
        itemUid: Long,
        paginationOptions: PaginationOptions,
    ): PaginatedList<WorkItemFailureDbEntity>
}

