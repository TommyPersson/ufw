package io.tpersson.ufw.transactionalevents

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant
import kotlin.reflect.full.findAnnotation

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type"
)
public interface Event {
    public val id: EventId
    public val timestamp: Instant
}

public val Event.type: String
    get() = this::class.findAnnotation<JsonTypeName>()?.value
        ?: error("${this::class} not annotated with @JsonTypeName!")
