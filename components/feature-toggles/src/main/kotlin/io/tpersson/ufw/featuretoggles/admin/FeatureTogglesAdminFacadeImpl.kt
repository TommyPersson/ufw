package io.tpersson.ufw.featuretoggles.admin

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.featuretoggles.internal.Constants
import io.tpersson.ufw.featuretoggles.FeatureToggle
import io.tpersson.ufw.featuretoggles.internal.FeatureToggleData
import io.tpersson.ufw.featuretoggles.internal.FeatureTogglesInternal
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject
import jakarta.inject.Singleton

// TODO tests

@Singleton
public class FeatureTogglesAdminFacadeImpl @Inject constructor(
    private val featureToggles: FeatureTogglesInternal,
    private val keyValueStore: KeyValueStore,
) : FeatureTogglesAdminFacade {
    override suspend fun listAll(paginationOptions: PaginationOptions): PaginatedList<FeatureToggle> {
        val toggles = keyValueStore.list(Constants.KEY_PREFIX, paginationOptions)

        return toggles.mapNotNull {
            val data = it.parseAs(FeatureToggleData::class)
            val definition = featureToggles.knownFeatureToggles[data.value.id]
                ?: return@mapNotNull null

            FeatureToggle(
                id = it.key.substringAfter(Constants.KEY_PREFIX),
                definition = definition,
                title = definition.title,
                description = definition.description,
                stateChangedAt = data.value.stateChangedAt,
                createdAt = data.createdAt,
                isEnabled = data.value.isEnabled,
            )
        }
    }

    override suspend fun disable(featureToggleId: String) {
        val definition = featureToggles.knownFeatureToggles[featureToggleId]
            ?: error("Unknown feature toggle ID: $featureToggleId")

        featureToggles.get(definition).disable()
    }

    override suspend fun enable(featureToggleId: String) {
        val definition = featureToggles.knownFeatureToggles[featureToggleId]
            ?: error("Unknown feature toggle ID: $featureToggleId")

        featureToggles.get(definition).enable()
    }

}