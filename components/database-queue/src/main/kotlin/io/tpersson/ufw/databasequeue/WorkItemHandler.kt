package io.tpersson.ufw.databasequeue

import io.tpersson.ufw.databasequeue.internal.WorkItemDbEntity

public interface WorkItemHandler<TItem> {

    public val handlerClassName: String

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

