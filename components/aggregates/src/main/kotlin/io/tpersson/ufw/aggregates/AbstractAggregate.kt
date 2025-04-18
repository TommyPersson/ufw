package io.tpersson.ufw.aggregates

import com.fasterxml.jackson.annotation.JsonIgnore
import io.tpersson.ufw.durablemessages.common.DurableMessage

public abstract class AbstractAggregate<TFactType>(
    id: AggregateId,
    @JsonIgnore
    public val originalVersion: Long,
    facts: List<TFactType> = emptyList()
) : AbstractEntity<AggregateId>(id) {
    init {
        facts.forEach { mutate(it) }
    }

    @JsonIgnore
    public val pendingFacts: MutableList<TFactType> = mutableListOf()

    @JsonIgnore
    public val pendingEvents: MutableList<DurableMessage> = mutableListOf()

    protected fun record(fact: TFactType) {
        pendingFacts.add(fact)
        pendingEvents.addAll(mapFactToEvent(fact))
        mutate(fact)
    }

    protected abstract fun mutate(fact: TFactType)

    protected open fun mapFactToEvent(fact: TFactType): List<DurableMessage> = emptyList()
}

public data class PendingEvent(
    val topic: String,
    val event: DurableMessage,
)