package io.tpersson.ufw.jobqueue.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.jobqueue.JobQueue
import io.tpersson.ufw.jobqueue.JobQueueComponent
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.internal.*
import io.tpersson.ufw.jobqueue.internal.DurableJobHandlersProvider

public class JobQueueGuiceModule(private val config: JobQueueConfig = JobQueueConfig()) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(JobQueueConfig::class.java).toInstance(config)
            bind(JobQueue::class.java).to(JobQueueImpl::class.java)
            bind(JobQueueInternal::class.java).to(JobQueueImpl::class.java)
            bind(DurableJobHandlersProvider::class.java).to(GuiceDurableJobHandlersProvider::class.java).asEagerSingleton()
            bind(JobQueueComponent::class.java).asEagerSingleton()
        }
    }
}

