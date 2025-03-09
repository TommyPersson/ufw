package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings

public object DurableJobsDatabaseQueueAdapterSettings : DatabaseQueueAdapterSettings {
    override val metricsQueueStateMetricName: String = "ufw.job_queue.size"
    override val metricsProcessingDurationMetricName: String = "ufw.job_queue.duration.seconds"

    override val queueIdPrefix: String = "jq__"

    override val mdcQueueIdLabel: String = "jobQueueId"
    override val mdcItemIdLabel: String = "jobId"
    override val mdcItemTypeLabel: String = "jobType"
    override val mdcHandlerClassLabel: String = "jobHandlerClass"

}