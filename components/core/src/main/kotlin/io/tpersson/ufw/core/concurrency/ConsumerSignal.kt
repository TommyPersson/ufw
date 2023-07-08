package io.tpersson.ufw.core.concurrency

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.time.withTimeoutOrNull
import java.time.Duration

public class ConsumerSignal {
    private val channel = Channel<Unit>(1)

    public fun signal() {
        channel.trySend(Unit)
    }
    
    public suspend fun wait(timeout: Duration): Boolean {
        return withTimeoutOrNull(timeout) {
            channel.receive()
            true
        } ?: false
    }
}
