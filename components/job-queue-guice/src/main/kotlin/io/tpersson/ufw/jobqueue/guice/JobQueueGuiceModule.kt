package io.tpersson.ufw.jobqueue.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.jobqueue.JobQueue
import io.tpersson.ufw.jobqueue.JobQueueComponent
import io.tpersson.ufw.jobqueue.JobQueueConfig
import io.tpersson.ufw.jobqueue.internal.*

public class JobQueueGuiceModule(private val config: JobQueueConfig = JobQueueConfig()) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(JobQueueConfig::class.java).toInstance(config)
            bind(JobQueue::class.java).to(JobQueueImpl::class.java)
            bind(JobQueueInternal::class.java).to(JobQueueImpl::class.java)
            bind(JobHandlersProvider::class.java).to(GuiceJobHandlersProvider::class.java).asEagerSingleton()
            bind(JobsDAO::class.java).to(JobsDAOImpl::class.java)
            bind(JobFailureRepository::class.java).to(JobFailureRepositoryImpl::class.java)
            bind(JobQueueComponent::class.java).asEagerSingleton()
        }
    }
}

