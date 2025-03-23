package io.tpersson.ufw.featuretoggles.guice

import com.google.inject.Binder
import com.google.inject.Module
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.featuretoggles.admin.FeatureTogglesAdminFacade
import io.tpersson.ufw.featuretoggles.admin.FeatureTogglesAdminFacadeImpl
import io.tpersson.ufw.featuretoggles.internal.FeatureTogglesImpl
import io.tpersson.ufw.featuretoggles.internal.FeatureTogglesInternal


public class FeatureTogglesGuiceModule(
) : Module {
    override fun configure(binder: Binder) {
        with(binder) {
            bind(FeatureToggles::class.java).to(FeatureTogglesImpl::class.java)
            bind(FeatureTogglesInternal::class.java).to(FeatureTogglesImpl::class.java)
            bind(FeatureTogglesAdminFacade::class.java).to(FeatureTogglesAdminFacadeImpl::class.java)
            bind(FeatureTogglesGuiceModule::class.java).asEagerSingleton()
        }
    }
}