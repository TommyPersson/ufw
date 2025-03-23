package io.tpersson.ufw.core.utils

import java.time.Duration
import kotlin.system.measureTimeMillis

public inline fun <T> measureTimedValue(block: () -> T): Pair<T, Duration> {
    var result: T

    val millis = measureTimeMillis {
        result = block()
    }

    return result to Duration.ofMillis(millis)
}

