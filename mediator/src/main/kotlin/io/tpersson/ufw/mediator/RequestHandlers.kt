package io.tpersson.ufw.mediator

public interface RequestHandler<TRequest : Request<TResult>, TResult> {
    public suspend fun handle(request: TRequest, context: Context): TResult
}

public interface QueryHandler<TQuery : Query<TResult>, TResult> : RequestHandler<TQuery, TResult> {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override suspend fun handle(query: TQuery, context: Context): TResult
}

public interface CommandHandler<TCommand : Command<TResult>, TResult> : RequestHandler<TCommand, TResult> {
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override suspend fun handle(command: TCommand, context: Context): TResult
}