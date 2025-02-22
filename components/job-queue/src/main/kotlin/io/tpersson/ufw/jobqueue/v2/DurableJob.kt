package io.tpersson.ufw.jobqueue.v2

public interface DurableJob<TData : Any> {
    public val id: String
}