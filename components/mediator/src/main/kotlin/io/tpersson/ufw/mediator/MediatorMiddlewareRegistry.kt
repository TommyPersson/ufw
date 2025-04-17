package io.tpersson.ufw.mediator

import kotlinx.coroutines.flow.Flow

public interface MediatorMiddlewareRegistry {
    public fun getAll(): Set<Middleware<*, *>>

    public fun register(middleware: Middleware<*, *>)

    public fun observe(): Flow<Set<Middleware<*, *>>>
}