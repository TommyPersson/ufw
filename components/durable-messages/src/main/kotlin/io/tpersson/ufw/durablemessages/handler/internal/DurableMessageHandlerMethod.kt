package io.tpersson.ufw.durablemessages.handler.internal

import io.tpersson.ufw.durablemessages.common.DurableMessage
import io.tpersson.ufw.durablemessages.handler.DurableMessageContext
import io.tpersson.ufw.durablemessages.handler.DurableMessageHandler
import kotlin.reflect.KClass

public class DurableMessageHandlerMethod<TMessage : DurableMessage>(
    public val handler: DurableMessageHandler,
    public val messageTopic: String,
    public val messageType: String,
    public val messageClass: KClass<TMessage>,
    public val messageDescription: String,
    public val method: suspend (TMessage, DurableMessageContext) -> Unit,
)