package io.tpersson.ufw.mediator.middleware.retryable

import io.github.resilience4j.retry.RetryConfig

/**
 * Marker interface for the [RetryableMiddleware].
 */
public interface Retryable {
    public val retryConfig: RetryConfig
}