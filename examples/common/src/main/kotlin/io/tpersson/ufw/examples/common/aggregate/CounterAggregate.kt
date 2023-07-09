package io.tpersson.ufw.examples.common.aggregate

import com.fasterxml.jackson.annotation.JsonTypeName
import io.tpersson.ufw.aggregates.*
import java.time.Instant
import java.util.*

public class CounterAggregate(
    id: AggregateId,
    originalVersion: Long,
    facts: List<Facts> = emptyList()
) : AbstractAggregate<CounterAggregate.Facts>(id, originalVersion, facts) {

    public companion object {
        public fun new(now: Instant): CounterAggregate {
            return CounterAggregate(now)
        }
    }

    private constructor(now: Instant) : this(AggregateId(UUID.randomUUID().toString()), 0) {
        record(Facts.Created(now))
    }

    private var _value: Long = 0

    public val value: Long get() = _value

    public fun increment(now: Instant) {
        record(Facts.Incremented(now))
    }

    override fun mutate(fact: Facts) {
        when (fact) {
            is Facts.Created -> _value = 0
            is Facts.Incremented -> _value++
        }
    }

    public sealed class Facts : Fact() {
        @JsonTypeName("CREATED")
        public class Created(
            override val timestamp: Instant
        ) : Facts()

        @JsonTypeName("INCREMENTED")
        public class Incremented(
            override val timestamp: Instant
        ) : Facts()
    }
}

