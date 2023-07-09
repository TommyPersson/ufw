package io.tpersson.ufw.aggregates

public abstract class AbstractAggregate<TFactType>(
    public val id: AggregateId,
    public val originalVersion: Long,
    facts: List<TFactType> = emptyList()
) {
    init {
        facts.forEach { mutate(it) }
    }

    public val pendingFacts: MutableList<TFactType> = mutableListOf()

    protected fun record(fact: TFactType) {
        pendingFacts.add(fact)
        mutate(fact)
    }

    protected abstract fun mutate(fact: TFactType)
}