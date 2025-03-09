package io.tpersson.ufw.jobqueue.v2

import com.fasterxml.jackson.annotation.JsonValue

public data class JobQueueId(@get:JsonValue val value: String) {

    override fun toString(): String {
        return value
    }

    public companion object {
        public fun fromString(str: String): JobQueueId {
            return JobQueueId(str)
        }
    }
}