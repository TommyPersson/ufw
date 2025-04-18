package io.tpersson.ufw.durablemessages.handler.internal

import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.utils.Memoized
import io.tpersson.ufw.core.utils.nullIfBlank
import io.tpersson.ufw.databasequeue.WorkItemHandler
import io.tpersson.ufw.databasequeue.WorkItemQueueId
import io.tpersson.ufw.databasequeue.worker.AbstractWorkQueueManager
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.durablemessages.common.DurableMessage
import io.tpersson.ufw.durablemessages.common.DurableMessageQueueId
import io.tpersson.ufw.durablemessages.common.messageDefinition
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import io.tpersson.ufw.durablemessages.handler.annotations.MessageHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions


@Singleton
public class DurableMessageQueueWorkersManager @Inject constructor(
    private val workerFactory: DatabaseQueueWorkerFactory,
    private val durableMessageHandlersRegistry: DurableMessageHandlersRegistry,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper,
) : AbstractWorkQueueManager(
    workerFactory = workerFactory,
    adapterSettings = DurableMessagesDatabaseQueueAdapterSettings
) {
    public val handlerMethodsByQueue: Map<DurableMessageQueueId, List<DurableMessageHandlerMethod<*>>>
            by Memoized({ durableMessageHandlersRegistry.get() }) { handlers ->
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
                        method.messageType to DurableMessageHandlerAdapter(method.handler, method, objectMapper)
                    }
                }.toMap()
            }
}

public fun DurableMessageHandler.findHandlerMethods(): List<DurableMessageHandlerMethod<*>> {
    val functions = this::class.memberFunctions.filter { it.findAnnotation<MessageHandler>() != null }

    // TODO tests, validations

    @Suppress("UNCHECKED_CAST")
    return functions.map { function ->
        val functionAnnotation = function.findAnnotation<MessageHandler>()!!
        val messageParam = function.parameters[1]
        val messageParamClass: KClass<out DurableMessage> = messageParam.type.classifier as KClass<out DurableMessage>
        val messageAnnotation = messageParamClass.messageDefinition

        val topic = functionAnnotation.topic.nullIfBlank() ?: messageAnnotation.topic

        DurableMessageHandlerMethod(
            handler = this,
            messageTopic = topic,
            messageType = messageAnnotation.type,
            messageClass = messageParamClass,
            messageDescription = messageAnnotation.description,
            method = { e, ctx -> function.callSuspend(this, e, ctx) }
        )
    }
}