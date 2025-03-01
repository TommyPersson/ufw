package io.tpersson.ufw.databasequeue.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.databasequeue.DatabaseQueueConfig
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemFailuresDAOImpl
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactoryImpl
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessorFactory
import io.tpersson.ufw.databasequeue.worker.SingleWorkItemProcessorFactoryImpl

public class DatabaseQueueGuiceModule(private val config: DatabaseQueueConfig = DatabaseQueueConfig()) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(DatabaseQueueConfig::class.java).toInstance(config)
            bind(WorkItemsDAO::class.java).to(WorkItemsDAOImpl::class.java)
            bind(WorkItemFailuresDAO::class.java).to(WorkItemFailuresDAOImpl::class.java)
            bind(DatabaseQueueWorkerFactory::class.java).to(DatabaseQueueWorkerFactoryImpl::class.java)
            bind(SingleWorkItemProcessorFactory::class.java).to(SingleWorkItemProcessorFactoryImpl::class.java)
        }
    }
}
