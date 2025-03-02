package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings

public interface SingleWorkItemProcessorFactory {
    public fun create(
        watchdogId: String,
        adapterSettings: DatabaseQueueAdapterSettings,
    ): SingleWorkItemProcessor
}