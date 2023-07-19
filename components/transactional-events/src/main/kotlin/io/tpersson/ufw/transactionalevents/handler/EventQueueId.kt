package io.tpersson.ufw.transactionalevents.handler

import com.fasterxml.jackson.annotation.JsonValue

public data class EventQueueId(@get:JsonValue val id: String) {
    override fun toString(): String = id
}