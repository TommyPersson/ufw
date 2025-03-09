package io.tpersson.ufw.jobqueue.v2

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

public data class JobId(@get:JsonValue val value: String) {

    override fun toString(): String {
        return this.value
    }

    public companion object {
        public fun fromString(str: String): JobId {
            return JobId(str)
        }

        public fun new(): JobId {
            return JobId(UUID.randomUUID().toString()) // UUIDv7
        }
    }
}

