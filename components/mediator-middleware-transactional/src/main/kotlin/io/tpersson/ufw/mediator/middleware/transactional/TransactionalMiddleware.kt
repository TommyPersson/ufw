package io.tpersson.ufw.mediator.middleware.transactional

import io.tpersson.ufw.database.unitofwork.UnitOfWork
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactory
import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.Context
import io.tpersson.ufw.mediator.ContextKey
import io.tpersson.ufw.mediator.Middleware
import io.tpersson.ufw.mediator.middleware.StandardMiddlewarePriorities
import jakarta.inject.Inject

@Suppress("unused")
public class TransactionalMiddleware @Inject constructor(
    private val unitOfWorkFactory: UnitOfWorkFactory
) : Middleware<Command<Any>, Any> {

    override val priority: Int
        get() = StandardMiddlewarePriorities.Transactional

    public object ContextKeys {
        public val UnitOfWork: ContextKey<UnitOfWork> = ContextKey("UnitOfWork")
    }

    override suspend fun handle(
        request: Command<Any>,
        context: Context,
        next: suspend (request: Command<Any>, context: Context) -> Any
    ): Any {
        if (request !is Transactional) {
            return next(request, context)
        }

        val unitOfWork = unitOfWorkFactory.create()
        context[ContextKeys.UnitOfWork] = unitOfWork

        val result = next(request, context)

        unitOfWork.commit()

        return result
    }
}

/**
 * Marker interface for the TransactionalMiddleware
 */
public interface Transactional

public val Context.unitOfWork: UnitOfWork
    get() = this[TransactionalMiddleware.ContextKeys.UnitOfWork] ?: error("No UnitOfWork found")