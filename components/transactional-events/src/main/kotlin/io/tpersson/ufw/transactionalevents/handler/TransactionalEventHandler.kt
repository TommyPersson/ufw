package io.tpersson.ufw.transactionalevents.handler

import com.fasterxml.jackson.annotation.JsonTypeName
import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.handler.internal.EventHandlerFunction
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

public abstract class TransactionalEventHandler {
    public val eventQueueId: EventQueueId get() = EventQueueId(this::class.simpleName!!)

    internal val functions: Map<Pair<String, String>, EventHandlerFunction> = this::class.declaredMemberFunctions
        .filter { fn ->
            fn.hasAnnotation<EventHandler>()
        }.map { fn ->
            EventHandlerFunction(
                topic = fn.findAnnotation<EventHandler>()!!.topic,
                type = (fn.parameters[1].type.classifier as KClass<out Event>).findAnnotation<JsonTypeName>()!!.value,
                instance = this,
                function = fn,
                eventClass = (fn.parameters[1].type.classifier as KClass<out Event>)
            )
        }
        .associateBy { it.topic to it.type }
}


