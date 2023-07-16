package io.tpersson.ufw.test

import java.time.Duration
import java.time.Instant
import java.time.InstantSource

public class TestInstantSource : InstantSource {
    private var now = Instant.now()

    override fun instant(): Instant {
        return now
    }

    public fun advance(duration: Duration) {
        now += duration
    }
}