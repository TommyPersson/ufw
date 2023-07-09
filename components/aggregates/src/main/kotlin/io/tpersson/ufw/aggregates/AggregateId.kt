package io.tpersson.ufw.aggregates

import com.fasterxml.jackson.annotation.JsonValue

public data class AggregateId(@JsonValue val value: String) {
    override fun toString(): String = value
}