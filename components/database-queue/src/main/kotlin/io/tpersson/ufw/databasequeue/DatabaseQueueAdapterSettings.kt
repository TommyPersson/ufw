package io.tpersson.ufw.databasequeue

public interface DatabaseQueueAdapterSettings {
    public val queueStateMetricName: String
    public val queueIdPrefix: String

    public val mdcQueueIdLabel: String
    public val mdcItemIdLabel: String
    public val mdcItemTypeLabel: String
    public val mdcHandlerClassLabel: String
}