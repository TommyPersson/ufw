package io.tpersson.ufw.transactionalevents

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

public data class EventId(@JsonProperty val value: UUID = UUID.randomUUID()) {
    @JsonValue
    override fun toString(): String = value.toString()
}