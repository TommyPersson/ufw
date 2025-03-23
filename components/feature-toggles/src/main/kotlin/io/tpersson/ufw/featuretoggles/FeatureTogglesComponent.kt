package io.tpersson.ufw.featuretoggles

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.featuretoggles.admin.FeatureTogglesAdminFacadeImpl
import io.tpersson.ufw.featuretoggles.admin.FeatureTogglesAdminModule
import io.tpersson.ufw.featuretoggles.internal.FeatureTogglesImpl
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class FeatureTogglesComponent @Inject constructor(
    public val featureToggles: FeatureToggles
) {
    public companion object {
        public fun create(
            coreComponent: CoreComponent,
            keyValueStoreComponent: KeyValueStoreComponent,
            adminComponent: AdminComponent,
        ): FeatureTogglesComponent {
            val featureToggles = FeatureTogglesImpl(
                keyValueStore = keyValueStoreComponent.keyValueStore,
                clock = coreComponent.clock,
            )

            val featureTogglesAdminFacade = FeatureTogglesAdminFacadeImpl(
                featureToggles = featureToggles,
                keyValueStore = keyValueStoreComponent.keyValueStore,
            )

            val featureTogglesAdminModule = FeatureTogglesAdminModule(
                featureTogglesAdminFacade = featureTogglesAdminFacade
            )

            adminComponent.register(featureTogglesAdminModule)

            return FeatureTogglesComponent(
               featureToggles = featureToggles
           )
        }
    }
}