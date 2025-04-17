package io.tpersson.ufw.mediator.internal

import io.tpersson.ufw.mediator.MediatorMiddlewareRegistry
import io.tpersson.ufw.mediator.Middleware
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import java.util.*

public class SimpleMediatorMiddlewareRegistry(initial: Set<Middleware<*, *>> = emptySet()) : MediatorMiddlewareRegistry {

    private val middlewares: MutableSet<Middleware<*, *>> = Collections.synchronizedSet(initial.toMutableSet())
    private val flow = MutableSharedFlow<Set<Middleware<*, *>>>(replay = 1)

    override fun getAll(): Set<Middleware<*, *>> {
        return middlewares.toSet()
    }

    override fun register(middleware: Middleware<*, *>) {
        middlewares.add(middleware)

        runBlocking {
            flow.emit(middlewares.toSet())
        }
    }

    override fun observe(): Flow<Set<Middleware<*, *>>> {
        return flow.asSharedFlow()
    }
}