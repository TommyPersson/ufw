package io.tpersson.ufw.durablejobs

import com.fasterxml.jackson.annotation.JsonValue

public data class DurableJobQueueId(@get:JsonValue val value: String) : Comparable<DurableJobQueueId> {
    override fun toString(): String {
        return value
    }

    override fun compareTo(other: DurableJobQueueId): Int {
        return this.value.compareTo(other.value)
    }

    public companion object {
        public fun fromString(str: String): DurableJobQueueId {
            return DurableJobQueueId(str)
        }
    }
}