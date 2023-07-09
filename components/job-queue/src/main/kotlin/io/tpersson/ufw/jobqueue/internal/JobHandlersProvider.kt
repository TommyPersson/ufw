package io.tpersson.ufw.jobqueue.internal

import io.tpersson.ufw.jobqueue.JobHandler

public interface JobHandlersProvider {
    public fun get(): Set<JobHandler<*>>
}