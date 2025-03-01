package io.tpersson.ufw.databasequeue.worker

import io.tpersson.ufw.databasequeue.DatabaseQueueMdcLabels

public interface SingleWorkItemProcessorFactory {
    public fun create(
        watchdogId: String,
        mdcLabels: DatabaseQueueMdcLabels,
    ): SingleWorkItemProcessor
}