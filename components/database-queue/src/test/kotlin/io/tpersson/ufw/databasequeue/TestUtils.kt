package io.tpersson.ufw.databasequeue


internal fun String.toWorkItemQueueId() = WorkItemQueueId(this)

internal fun String.toWorkItemId() = WorkItemId(this)