package io.tpersson.ufw.core

import kotlinx.coroutines.*
import org.slf4j.Logger
import java.lang.Exception
import java.time.Duration


public suspend fun forever(
    logger: Logger,
    errorDelay: Duration = Duration.ofMillis(500),
    block: suspend () -> Unit
): Unit = coroutineScope {
    while (isActive) {
        try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) {
                return@coroutineScope
            }

            logger.error("forever: Unhandled exception: $e", e)

            withContext(NonCancellable) {
                delay(errorDelay.toMillis())
            }
        }
    }
}