package io.tpersson.ufw.durableevents.handler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.core.utils.nullIfBlank
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.worker.AbstractWorkQueueManager
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.durableevents.common.DurableEvent
import io.tpersson.ufw.durableevents.common.DurableEventQueueId
import io.tpersson.ufw.durableevents.common.eventDefinition
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import io.tpersson.ufw.durableevents.handler.annotations.EventHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions


@Singleton
public class DurableEventQueueWorkersManager @Inject constructor(
    private val workerFactory: DatabaseQueueWorkerFactory,
    private val durableEventHandlersProvider: DurableEventHandlersProvider,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : AbstractWorkQueueManager(
    workerFactory = workerFactory,
    adapterSettings = DurableEventsDatabaseQueueAdapterSettings
) {
    public val handlerMethodsByQueue: Map<DurableEventQueueId, List<DurableEventHandlerMethod<*>>>
            by Memoized({ durableEventHandlersProvider.get() }) { handlers ->
                handlers.associate { handler ->
                    val queueId = handler.queueId
                    val methods = handler.findHandlerMethods()

                    queueId to methods
                }
            }

    protected override val handlersByTypeByQueueId: Map<WorkItemQueueId, Map<String, WorkItemHandler<*>>>
            by Memoized({ handlerMethodsByQueue }) { entries ->
                entries.map { (queueId, methods) ->
                    queueId.toWorkItemQueueId() to methods.associate { method ->
                        method.eventType to DurableEventHandlerAdapter(method.handler, method, objectMapper)
                    }
                }.toMap()
            }
}

public fun DurableEventHandler.findHandlerMethods(): List<DurableEventHandlerMethod<*>> {
    val functions = this::class.memberFunctions.filter { it.findAnnotation<EventHandler>() != null }

    // TODO tests, validations

    @Suppress("UNCHECKED_CAST")
    return functions.map { function ->
        val functionAnnotation = function.findAnnotation<EventHandler>()!!
        val eventParam = function.parameters[1]
        val eventParamClass: KClass<out DurableEvent> = eventParam.type.classifier as KClass<out DurableEvent>
        val eventAnnotation = eventParamClass.eventDefinition

        val topic = functionAnnotation.topic.nullIfBlank() ?: eventAnnotation.topic

        DurableEventHandlerMethod(
            handler = this,
            eventTopic = topic,
            eventType = eventAnnotation.type,
            eventClass = eventParamClass,
            eventDescription = eventAnnotation.description,
            method = { e, ctx -> function.callSuspend(this, e, ctx) }
        )
    }
}