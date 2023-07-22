package io.tpersson.ufw.mediator

import io.micrometer.core.instrument.MeterRegistry

public interface Mediator {
    public suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult
}
