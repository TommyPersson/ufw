package io.tpersson.ufw.durablemessages.common

import com.fasterxml.jackson.annotation.JsonProperty
import org.intellij.lang.annotations.Language
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

public abstract class DurableMessage {
    public abstract val id: DurableMessageId
    public abstract val timestamp: Instant

    @get:JsonProperty("@type")
    public val type: String by lazy { this::class.messageDefinition.type }

    @get:JsonProperty("@topic")
    public val topic: String by lazy { this::class.messageDefinition.topic }
}

public val KClass<out DurableMessage>.messageDefinition: MessageDefinition
    get() = findAnnotation<MessageDefinition>()
        ?: error("${this::class} not annotated with @MessageDefinition!")

public annotation class MessageDefinition(
    val type: String,
    val topic: String,
    @Language("Markdown")
    val description: String = ""
)
