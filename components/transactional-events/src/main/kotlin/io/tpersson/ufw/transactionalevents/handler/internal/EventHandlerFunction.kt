package io.tpersson.ufw.transactionalevents.handler.internal

import io.tpersson.ufw.transactionalevents.Event
import io.tpersson.ufw.transactionalevents.handler.TransactionalEventHandler
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

public data class EventHandlerFunction(
    val topic: String,
    val type: String,
    val instance: TransactionalEventHandler,
    val eventClass: KClass<out Event>,
    val function: KFunction<*>,
)