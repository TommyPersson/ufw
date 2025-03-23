package io.tpersson.ufw.durablecaches.internal

import com.github.benmanes.caffeine.cache.Ticker
import java.time.InstantSource

public class InstantSourceTicker(
    private val clock: InstantSource
) : Ticker {
    override fun read(): Long {
        return clock.millis() * 1_000_000 // read() should return nanos
    }
}