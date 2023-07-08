package io.tpersson.ufw.jobqueue

import java.util.*

public data class JobId(val value: String) {
    public companion object {
        public fun new(): JobId = JobId(UUID.randomUUID().toString())
    }
}