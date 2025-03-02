package io.tpersson.ufw.jobqueue.v2.internal

import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings

public object DurableJobsDatabaseQueueAdapterSettings : DatabaseQueueAdapterSettings {
    override val queueStateMetricName: String = "ufw.job_queue.size"
    override val queueIdPrefix: String = "jq__"

    override val mdcQueueIdLabel: String = "jobQueueId"
    override val mdcItemIdLabel: String = "jobId"
    override val mdcItemTypeLabel: String = "jobType"
    override val mdcHandlerClassLabel: String = "jobHandlerClass"

}