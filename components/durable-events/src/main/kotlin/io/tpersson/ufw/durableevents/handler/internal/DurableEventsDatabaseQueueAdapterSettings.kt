package io.tpersson.ufw.durableevents.handler.internal

import io.tpersson.ufw.databasequeue.DatabaseQueueAdapterSettings

public object DurableEventsDatabaseQueueAdapterSettings : DatabaseQueueAdapterSettings {
    override val metricsQueueStateMetricName: String = "ufw.event_queue.size"
    override val metricsProcessingDurationMetricName: String = "ufw.event_queue.duration.seconds"

    override val queueIdPrefix: String = "eq__"

    override val mdcQueueIdLabel: String = "eventQueueId"
    override val mdcItemIdLabel: String = "eventId"
    override val mdcItemTypeLabel: String = "eventType"
    override val mdcHandlerClassLabel: String = "eventHandlerClass"
}
