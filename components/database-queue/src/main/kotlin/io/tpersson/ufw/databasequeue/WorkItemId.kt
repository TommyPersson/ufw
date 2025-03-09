package io.tpersson.ufw.databasequeue

import com.fasterxml.jackson.annotation.JsonValue

public data class WorkItemId(@get:JsonValue val value: String) {
    override fun toString(): String {
        return value
    }
}