package io.tpersson.ufw.jobqueue.v2.internal

import io.tpersson.ufw.databasequeue.DatabaseQueueMdcLabels

public object DurableJobsMdcLabels : DatabaseQueueMdcLabels {
    override val queueIdLabel: String = "jobQueueId"
    override val itemIdLabel: String = "jobId"
    override val itemTypeLabel: String = "jobType"
    override val handlerClassLabel: String = "jobHandlerClass"
}