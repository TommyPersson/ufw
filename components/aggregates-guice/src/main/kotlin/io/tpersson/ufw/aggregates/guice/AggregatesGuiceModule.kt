package io.tpersson.ufw.aggregates.guice

import com.google.inject.Binder
import com.google.inject.Module
import com.google.inject.Scopes
import io.tpersson.ufw.aggregates.AggregateFactRepository
import io.tpersson.ufw.aggregates.internal.AggregateFactRepositoryImpl
import io.tpersson.ufw.aggregates.AggregatesComponent
import io.tpersson.ufw.aggregates.admin.AggregatesAdminFacade
import io.tpersson.ufw.aggregates.admin.AggregatesAdminFacadeImpl

public class AggregatesGuiceModule : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(AggregatesComponent::class.java).asEagerSingleton()
            bind(AggregatesAdminFacade::class.java).to(AggregatesAdminFacadeImpl::class.java)
            bind(AggregateFactRepository::class.java).to(AggregateFactRepositoryImpl::class.java)
        }
    }
}