package io.tpersson.ufw.durablejobs.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.durablejobs.DurableJobQueue
import io.tpersson.ufw.durablejobs.DurableJobsComponent
import io.tpersson.ufw.durablejobs.DurableJobsConfig
import io.tpersson.ufw.durablejobs.internal.*
import io.tpersson.ufw.durablejobs.internal.DurableJobHandlersProvider

public class DurableJobsGuiceModule(private val config: DurableJobsConfig = DurableJobsConfig()) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(DurableJobsConfig::class.java).toInstance(config)
            bind(DurableJobQueue::class.java).to(DurableJobQueueImpl::class.java)
            bind(DurableJobQueueInternal::class.java).to(DurableJobQueueImpl::class.java)
            bind(DurableJobHandlersProvider::class.java).to(GuiceDurableJobHandlersProvider::class.java).asEagerSingleton()
            bind(DurableJobsComponent::class.java).asEagerSingleton()
        }
    }
}

