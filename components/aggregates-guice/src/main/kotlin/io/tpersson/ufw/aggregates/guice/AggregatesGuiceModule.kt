package io.tpersson.ufw.aggregates.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.aggregates.AggregateFactRepository
import io.tpersson.ufw.aggregates.internal.AggregateFactRepositoryImpl
import io.tpersson.ufw.aggregates.component.AggregatesComponent
import io.tpersson.ufw.aggregates.admin.AggregatesAdminFacade
import io.tpersson.ufw.aggregates.admin.AggregatesAdminFacadeImpl
import io.tpersson.ufw.aggregates.internal.AggregateRepositoryProvider

public class AggregatesGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(AggregateRepositoryProvider::class.java).to(GuiceAggregateRepositoryProvider::class.java)
            bind(AggregateFactRepository::class.java).to(AggregateFactRepositoryImpl::class.java)
            bind(AggregatesAdminFacade::class.java).to(AggregatesAdminFacadeImpl::class.java)
            bind(AggregatesComponent::class.java).asEagerSingleton()
        }
    }
}