package io.tpersson.ufw.aggregates

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Instant
import java.util.UUID
import kotlin.reflect.full.findAnnotation

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
public abstract class Fact(
    public val id: UUID = UUID.randomUUID()
) {
    public abstract val timestamp: Instant
}

public val Fact.typeName: String
    get() = this::class.findAnnotation<JsonTypeName>()?.value
        ?: error("${this::class} not annotated with @JsonTypeName!")

