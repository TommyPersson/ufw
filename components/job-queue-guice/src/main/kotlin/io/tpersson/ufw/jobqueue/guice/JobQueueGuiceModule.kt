package io.tpersson.ufw.jobqueue.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.jobqueue.JobHandlersProvider
import io.tpersson.ufw.jobqueue.JobQueue
import io.tpersson.ufw.jobqueue.JobQueueModuleConfig
import io.tpersson.ufw.jobqueue.internal.JobQueueImpl
import io.tpersson.ufw.jobqueue.internal.JobRepository
import io.tpersson.ufw.jobqueue.internal.JobRepositoryImpl

public class JobQueueGuiceModule(
    private val scanPackages: List<String>
) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            val config = JobQueueModuleConfig(scanPackages)

            bind(JobQueueModuleConfig::class.java).toInstance(config)
            bind(JobQueue::class.java).to(JobQueueImpl::class.java)
            bind(JobHandlersProvider::class.java).to(GuiceJobHandlersProvider::class.java).asEagerSingleton()
            bind(JobRepository::class.java).to(JobRepositoryImpl::class.java)
        }
    }
}

