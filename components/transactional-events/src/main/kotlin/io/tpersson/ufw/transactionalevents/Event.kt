package io.tpersson.ufw.transactionalevents

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import io.tpersson.ufw.core.CoreComponent.Companion.defaultObjectMapper
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

public abstract class Event {
    public abstract val id: EventId
    public abstract val timestamp: Instant

    @get:JsonProperty("@type")
    public val type: String by lazy { this::class.eventDefinition.type }

    @get:JsonProperty("@topic")
    public val topic: String by lazy { this::class.eventDefinition.topic }
}

public val KClass<out Event>.eventDefinition: EventDefinition
    get() = findAnnotation<EventDefinition>()
        ?: error("${this::class} not annotated with @EventDefinition!")

public annotation class EventDefinition(
    val type: String,
    val topic: String
)

@EventDefinition("my-test-event-1", "test-topic")
public data class TestEvent1(
    override val id: EventId,
    override val timestamp: Instant,
    val myData: String
) : Event()

internal fun main() {
    val objectMapper = defaultObjectMapper

    val original = TestEvent1(
        id = EventId(),
        timestamp = Instant.now(),
        myData = "Hello, World!"
    )

    println(original)

    val json = objectMapper.writeValueAsString(original)


    println(json)

    val deserialized = objectMapper.readValue<TestEvent1>(json)

    println(deserialized)
}