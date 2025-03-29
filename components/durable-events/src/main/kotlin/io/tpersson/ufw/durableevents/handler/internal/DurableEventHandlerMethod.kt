package io.tpersson.ufw.durableevents.handler.internal

import io.tpersson.ufw.durableevents.common.DurableEvent
import io.tpersson.ufw.durableevents.handler.DurableEventContext
import io.tpersson.ufw.durableevents.handler.DurableEventHandler
import kotlin.reflect.KClass

public class DurableEventHandlerMethod<TEvent : DurableEvent>(
    public val handler: DurableEventHandler,
    public val eventTopic: String,
    public val eventType: String,
    public val eventClass: KClass<TEvent>,
    public val eventDescription: String,
    public val method: suspend (TEvent, DurableEventContext) -> Unit,
)