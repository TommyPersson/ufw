package io.tpersson.ufw.jobqueue.v2

import com.fasterxml.jackson.annotation.JsonValue

public data class JobQueueId(@get:JsonValue val value: String) : Comparable<JobQueueId> {
    override fun toString(): String {
        return value
    }

    override fun compareTo(other: JobQueueId): Int {
        return this.value.compareTo(other.value)
    }

    public companion object {
        public fun fromString(str: String): JobQueueId {
            return JobQueueId(str)
        }
    }
}