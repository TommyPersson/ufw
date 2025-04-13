package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity
import org.slf4j.Logger

public interface WorkItemHandler<TItem> {

    public val handlerClassName: String

    public val logger: Logger

    public fun transformItem(rawItem: WorkItemDbEntity): TItem & Any

    public suspend fun handle(
        item: TItem,
        context: WorkItemContext,
    )

    public suspend fun onFailure(
        item: TItem,
        error: Exception,
        context: WorkItemFailureContext
    ): FailureAction
}

