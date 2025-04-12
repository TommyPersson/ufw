package io.tpersson.ufw.durableevents.common

import com.fasterxml.jackson.annotation.JsonProperty
import org.intellij.lang.annotations.Language
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

public abstract class DurableEvent {
    public abstract val id: DurableEventId
    public abstract val timestamp: Instant

    @get:JsonProperty("@type")
    public val type: String by lazy { this::class.eventDefinition.type }

    @get:JsonProperty("@topic")
    public val topic: String by lazy { this::class.eventDefinition.topic }
}

public val KClass<out DurableEvent>.eventDefinition: EventDefinition
    get() = findAnnotation<EventDefinition>()
        ?: error("${this::class} not annotated with @EventDefinition!")

public annotation class EventDefinition(
    val type: String,
    val topic: String,
    @Language("Markdown")
    val description: String = ""
)
