package io.tpersson.ufw.durablejobs.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacade
import io.tpersson.ufw.databasequeue.admin.DatabaseQueueAdminFacadeImpl
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.DurableJobsComponent
import io.tpersson.ufw.durablejobs.DurableJobsComponentImpl
import io.tpersson.ufw.durablejobs.DurableJobsComponentInternal
import io.tpersson.ufw.durablejobs.internal.DurableJobHandlersProvider
import io.tpersson.ufw.durablejobs.internal.DurableJobQueueImpl
import io.tpersson.ufw.durablejobs.periodic.internal.PeriodicJobScheduler
import io.tpersson.ufw.durablejobs.periodic.internal.PeriodicJobSchedulerImpl
import io.tpersson.ufw.durablejobs.periodic.internal.PeriodicJobSpecsProvider
import io.tpersson.ufw.durablejobs.periodic.internal.PeriodicJobSpecsProviderImpl
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAO
import io.tpersson.ufw.durablejobs.periodic.internal.dao.PeriodicJobsDAOImpl

public class DurableJobsGuiceModule() : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(DurableJobQueue::class.java).to(DurableJobQueueImpl::class.java)
            bind(DurableJobHandlersProvider::class.java).to(GuiceDurableJobHandlersProvider::class.java).asEagerSingleton()
            bind(DatabaseQueueAdminFacade::class.java).to(DatabaseQueueAdminFacadeImpl::class.java)

            bind(PeriodicJobsDAO::class.java).to(PeriodicJobsDAOImpl::class.java)
            bind(PeriodicJobSpecsProvider::class.java).to(PeriodicJobSpecsProviderImpl::class.java)
            bind(PeriodicJobScheduler::class.java).to(PeriodicJobSchedulerImpl::class.java)

            bind(DurableJobsComponent::class.java).to(DurableJobsComponentImpl::class.java).asEagerSingleton()
            bind(DurableJobsComponentInternal::class.java).to(DurableJobsComponentImpl::class.java).asEagerSingleton()
        }
    }
}

