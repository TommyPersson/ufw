package io.tpersson.ufw.durableevents.common

import com.fasterxml.jackson.annotation.JsonValue

public data class DurableEventQueueId(@get:JsonValue val id: String) {
    override fun toString(): String = id

    public companion object {
        public fun fromString(str: String): DurableEventQueueId {
            return DurableEventQueueId(str)
        }
    }
}