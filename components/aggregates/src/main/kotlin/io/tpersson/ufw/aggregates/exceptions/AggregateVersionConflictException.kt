package io.tpersson.ufw.aggregates.exceptions

import io.tpersson.ufw.aggregates.AggregateId

public class AggregateVersionConflictException(
    public val aggregateId: AggregateId,
    cause: Exception
) : Exception(cause)