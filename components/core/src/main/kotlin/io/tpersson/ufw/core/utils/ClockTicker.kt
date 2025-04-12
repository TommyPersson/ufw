package io.tpersson.ufw.core.utils

import com.github.benmanes.caffeine.cache.Ticker
import java.time.Clock

public class ClockTicker(
    private val clock: Clock
) : Ticker {
    override fun read(): Long {
        return clock.millis() * 1_000_000 // read() should return nanos
    }
}