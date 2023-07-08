package io.tpersson.ufw.jobqueue

public interface JobHandlersProvider {
    public fun get(): Set<JobHandler<*>>
}