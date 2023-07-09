package io.tpersson.ufw.jobqueue

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

public data class JobId(@JsonValue val value: String) {
    override fun toString(): String = value

    public companion object {
        public fun new(): JobId = JobId(UUID.randomUUID().toString())
    }
}