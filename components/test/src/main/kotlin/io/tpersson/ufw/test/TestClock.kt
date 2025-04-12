package io.tpersson.ufw.test

import java.time.Duration
import java.time.Instant
import java.time.Clock
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.toJavaDuration

public class TestClock(
    initial: Instant? = null,
    private var zone: ZoneId = ZoneId.systemDefault()
) : Clock() {
    private var now = initial ?: Instant.now()

    override fun instant(): Instant {
        return now
    }

    override fun withZone(zone: ZoneId): TestClock {
        return TestClock(now, zone)
    }

    override fun getZone(): ZoneId {
        return zone
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