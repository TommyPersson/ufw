package io.tpersson.ufw.mediator

import kotlinx.coroutines.flow.Flow

public interface MediatorRequestHandlerRegistry {
    public fun getAll(): Set<RequestHandler<*, *>>

    public fun register(handler: RequestHandler<*, *>)

    public fun observe(): Flow<Set<RequestHandler<*, *>>>
}