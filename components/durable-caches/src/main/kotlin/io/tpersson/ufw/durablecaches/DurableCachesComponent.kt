package io.tpersson.ufw.durablecaches

import io.tpersson.ufw.admin.AdminComponent
import io.tpersson.ufw.core.CoreComponent
import io.tpersson.ufw.core.dsl.ComponentKey
import io.tpersson.ufw.core.dsl.UFWComponent
import io.tpersson.ufw.durablecaches.admin.DurableCachesAdminFacadeImpl
import io.tpersson.ufw.durablecaches.admin.DurableCachesAdminModule
import io.tpersson.ufw.durablecaches.internal.DurableCachesImpl
import io.tpersson.ufw.keyvaluestore.KeyValueStoreComponent
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class DurableCachesComponent @Inject constructor(
    public val durableCaches: DurableCaches
) : UFWComponent<DurableCachesComponent> {

    public companion object : ComponentKey<DurableCachesComponent> {
        public fun create(
            coreComponent: CoreComponent,
            keyValueStoreComponent: KeyValueStoreComponent,
            adminComponent: AdminComponent,
        ): DurableCachesComponent {
            val durableCaches = DurableCachesImpl(
                keyValueStore = keyValueStoreComponent.keyValueStore,
                clock = coreComponent.clock,
            )

            val durableCachesAdminModule = DurableCachesAdminModule(
                adminFacade = DurableCachesAdminFacadeImpl(
                    durableCaches = durableCaches,
                ),
                objectMapper = coreComponent.objectMapper,
            )

            adminComponent.register(durableCachesAdminModule)

            return DurableCachesComponent(
                durableCaches = durableCaches
            )
        }
    }
}

