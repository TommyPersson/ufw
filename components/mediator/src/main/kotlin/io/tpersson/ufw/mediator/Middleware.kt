package io.tpersson.ufw.mediator

public interface Middleware<TRequest : Any, TResult> {
    public val priority: Int

    public suspend fun handle(
        request: TRequest,
        context: Context,
        next: suspend (request: TRequest, context: Context) -> TResult
    ): TResult
}
