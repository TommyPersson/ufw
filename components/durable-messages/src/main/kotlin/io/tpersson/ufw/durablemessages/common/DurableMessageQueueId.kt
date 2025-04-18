package io.tpersson.ufw.durablemessages.common

import com.fasterxml.jackson.annotation.JsonValue

public data class DurableMessageQueueId(@get:JsonValue val id: String) {
    override fun toString(): String = id

    public companion object {
        public fun fromString(str: String): DurableMessageQueueId {
            return DurableMessageQueueId(str)
        }
    }
}