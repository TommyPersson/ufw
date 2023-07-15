package io.tpersson.ufw.mediator.middleware.loggable

/**
 * Marker interface for the [LoggableMiddleware].
 *
 * The middleware will log a message including [logText] whenever a request succeeds, or fails.
 *
 * Upon failure, the original exception will be rethrown after logging.
 */
public interface Loggable {
    public val logText: String
}