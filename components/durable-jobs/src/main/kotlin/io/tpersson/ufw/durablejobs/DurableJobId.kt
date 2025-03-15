package io.tpersson.ufw.durablejobs

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

public data class DurableJobId(@get:JsonValue val value: String) {

    override fun toString(): String {
        return this.value
    }

    public companion object {
        public fun fromString(str: String): DurableJobId {
            return DurableJobId(str)
        }

        public fun new(): DurableJobId {
            return DurableJobId(UUID.randomUUID().toString()) // UUIDv7
        }
    }
}

