package io.tpersson.ufw.durableevents.common

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

public data class DurableEventId(
    @get:JsonValue val value: String = UUID.randomUUID().toString() // TODO UUIDv7
) {
    override fun toString(): String = value

    public companion object {
        public fun fromString(str: String): DurableEventId {
            return DurableEventId(str)
        }
    }
}