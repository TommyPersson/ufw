package io.tpersson.ufw.mediator.internal

import io.tpersson.ufw.mediator.MediatorRequestHandlerRegistry
import io.tpersson.ufw.mediator.RequestHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import java.util.*

public class SimpleMediatorRequestHandlerRegistry(initial: Set<RequestHandler<*, *>> = emptySet()) : MediatorRequestHandlerRegistry {

    private val handlers: MutableSet<RequestHandler<*, *>> = Collections.synchronizedSet(initial.toMutableSet())
    private val flow = MutableSharedFlow<Set<RequestHandler<*, *>>>(replay = 1)

    override fun getAll(): Set<RequestHandler<*, *>> {
        return handlers.toSet()
    }

    override fun register(handler: RequestHandler<*, *>) {
        handlers.add(handler)

        runBlocking {
            flow.emit(handlers.toSet())
        }
    }

    override fun observe(): Flow<Set<RequestHandler<*, *>>> {
        return flow.asSharedFlow()
    }
}