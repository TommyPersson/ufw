package io.tpersson.ufw.databasequeue.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.databasequeue.DatabaseQueueComponent
import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.databasequeue.WorkQueue
import io.tpersson.ufw.databasequeue.internal.*
import io.tpersson.ufw.databasequeue.worker.*

public class DatabaseQueueGuiceModule(private val config: DatabaseQueueConfig = DatabaseQueueConfig()) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(DatabaseQueueConfig::class.java).toInstance(config)
            bind(WorkItemsDAO::class.java).to(WorkItemsDAOImpl::class.java)
            bind(WorkItemFailuresDAO::class.java).to(WorkItemFailuresDAOImpl::class.java)
            bind(WorkQueue::class.java).to(WorkQueueImpl::class.java)
            bind(WorkQueueInternal::class.java).to(WorkQueueImpl::class.java)
            bind(WorkQueuesDAO::class.java).to(WorkQueuesDAOImpl::class.java)
            bind(DatabaseQueueWorkerFactory::class.java).to(DatabaseQueueWorkerFactoryImpl::class.java)
            bind(SingleWorkItemProcessorFactory::class.java).to(SingleWorkItemProcessorFactoryImpl::class.java)
            bind(QueueStateChecker::class.java).to(CachingQueueStateCheckerImpl::class.java)
            bind(DatabaseQueueComponent::class.java).asEagerSingleton()
        }
    }
}
