package io.tpersson.ufw.aggregates

public abstract class AbstractEntity<TId>(
    public override val id: TId
) : Entity<TId>