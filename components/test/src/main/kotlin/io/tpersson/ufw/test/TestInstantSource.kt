package io.tpersson.ufw.test

import java.time.Duration
import java.time.Instant
import java.time.InstantSource
import java.time.temporal.ChronoUnit
import kotlin.time.toJavaDuration

public class TestInstantSource : InstantSource {
    private var now = Instant.now()

    override fun instant(): Instant {
        return now
    }

    public fun advance(duration: Duration) {
        now += duration
    }

    public fun advance(duration: kotlin.time.Duration) {
        advance(duration.toJavaDuration())
    }

    public fun reset(instant: Instant) {
        now = instant
    }

    public val dbNow: Instant get() = instant().truncatedTo(ChronoUnit.MILLIS)
}