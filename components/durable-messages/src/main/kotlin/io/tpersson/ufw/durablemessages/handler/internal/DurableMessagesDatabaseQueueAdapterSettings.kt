package io.tpersson.ufw.durablemessages.handler.internal

import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings

public object DurableMessagesDatabaseQueueAdapterSettings : DatabaseQueueAdapterSettings {
    override val metricsQueueStateMetricName: String = "ufw.message_queue.size"
    override val metricsProcessingDurationMetricName: String = "ufw.message_queue.duration.seconds"

    override val queueIdPrefix: String = "mq__"

    override val mdcQueueIdLabel: String = "messageQueueId"
    override val mdcItemIdLabel: String = "messageId"
    override val mdcItemTypeLabel: String = "messageType"
    override val mdcHandlerClassLabel: String = "messageHandlerClass"
}
