package io.tpersson.ufw.databasequeue

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant

/*
public data class WorkItemEvent(
    val type: WorkItemEventType,
    val timestamp: Instant,
)

 */

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "@type",
    visible = true
)
public sealed class WorkItemEvent {
    public abstract val timestamp: Instant

    @JsonProperty("@type")
    private lateinit var _type: String

    public val type: String get() = _type

    @JsonTypeName("SCHEDULED")
    public data class Scheduled(
        override val timestamp: Instant,
        val scheduledFor: Instant,
    ) : WorkItemEvent()

    @JsonTypeName("TAKEN")
    public data class Taken(
        override val timestamp: Instant
    ) : WorkItemEvent()

    @JsonTypeName("FAILED")
    public data class Failed(
        override val timestamp: Instant
    ) : WorkItemEvent()

    @JsonTypeName("SUCCESSFUL")
    public data class Successful(
        override val timestamp: Instant
    ) : WorkItemEvent()

    @JsonTypeName("AUTOMATICALLY_RESCHEDULED")
    public data class AutomaticallyRescheduled(
        override val timestamp: Instant,
        public val scheduledFor: Instant
    ) : WorkItemEvent()

    @JsonTypeName("MANUALLY_RESCHEDULED")
    public data class ManuallyRescheduled(
        override val timestamp: Instant,
        public val scheduledFor: Instant
    ) : WorkItemEvent()

    @JsonTypeName("CANCELLED")
    public data class Cancelled(
        override val timestamp: Instant
    ) : WorkItemEvent()

    @JsonTypeName("HANGED")
    public data class Hanged(
        override val timestamp: Instant
    ) : WorkItemEvent()
}

public enum class WorkItemEventType {
    SCHEDULED,
    TAKEN,
    SUCCESSFUL,
    FAILED,
    AUTOMATICALLY_RESCHEDULED,
    MANUALLY_RESCHEDULED,
    PAUSED,
    UNPAUSED,
    CANCELLED,
}