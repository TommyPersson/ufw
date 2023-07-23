package io.tpersson.ufw.mediator

public interface Mediator {
    public suspend fun <TRequest : Request<TResult>, TResult> send(request: TRequest): TResult
}
