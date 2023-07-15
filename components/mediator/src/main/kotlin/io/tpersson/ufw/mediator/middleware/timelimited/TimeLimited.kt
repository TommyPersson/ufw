package io.tpersson.ufw.mediator.middleware.timelimited

import java.time.Duration

/**
 * Marker interface for the [TimeLimitedMiddleware].
 *
 * Will cancel the coroutine of the request and throw a [kotlinx.coroutines.TimeoutCancellationException]
 * if the specified [timeout] is exceeded.
 */
public interface TimeLimited {
    public val timeout: Duration
}