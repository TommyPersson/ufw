package io.tpersson.ufw.databasequeue.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAO
import io.tpersson.ufw.databasequeue.internal.WorkItemsDAOImpl
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactory
import io.tpersson.ufw.databasequeue.worker.DatabaseQueueWorkerFactoryImpl

public class DatabaseQueueGuiceModule() : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(WorkItemsDAO::class.java).to(WorkItemsDAOImpl::class.java)
            bind(DatabaseQueueWorkerFactory::class.java).to(DatabaseQueueWorkerFactoryImpl::class.java)
        }
    }
}
