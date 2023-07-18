package io.tpersson.ufw.aggregates

import io.tpersson.ufw.transactionalevents.Event

public abstract class AbstractAggregate<TFactType>(
    public val id: AggregateId,
    public val originalVersion: Long,
    facts: List<TFactType> = emptyList()
) {
    init {
        facts.forEach { mutate(it) }
    }

    public val pendingFacts: MutableList<TFactType> = mutableListOf()
    public val pendingEvents: MutableList<PendingEvent> = mutableListOf()

    protected fun record(fact: TFactType) {
        pendingFacts.add(fact)
        pendingEvents.addAll(mapFactToEvent(fact))
        mutate(fact)
    }

    protected abstract fun mutate(fact: TFactType)

    protected open fun mapFactToEvent(fact: TFactType): List<PendingEvent> = emptyList()
}

public data class PendingEvent(
    val topic: String,
    val event: Event,
)