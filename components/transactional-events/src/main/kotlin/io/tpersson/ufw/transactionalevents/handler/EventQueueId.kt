package io.tpersson.ufw.transactionalevents.handler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

public data class EventQueueId(@JsonProperty val id: String) {
    @JsonValue
    override fun toString(): String = id
}