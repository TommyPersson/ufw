package io.tpersson.ufw.transactionalevents

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

public data class EventId(@get:JsonValue val value: String = UUID.randomUUID().toString()) {
    override fun toString(): String = value
}