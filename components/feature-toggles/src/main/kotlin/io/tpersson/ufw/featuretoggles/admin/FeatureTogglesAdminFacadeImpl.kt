package io.tpersson.ufw.featuretoggles.admin

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.featuretoggles.FeatureToggles
import io.tpersson.ufw.featuretoggles.internal.FeatureToggle
import io.tpersson.ufw.featuretoggles.internal.FeatureToggleData
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject

public class FeatureTogglesAdminFacadeImpl @Inject constructor(
    private val featureToggles: FeatureToggles,
    private val keyValueStore: KeyValueStore,
) : FeatureTogglesAdminFacade {
    override suspend fun listAll(paginationOptions: PaginationOptions): PaginatedList<FeatureToggle> {
        val toggles = keyValueStore.list(
            "__ft__",
            limit = paginationOptions.limit + 1,
            offset = paginationOptions.offset,
        )

        return PaginatedList(
            items = toggles.take(paginationOptions.limit).map {
                val data = it.parseAs(FeatureToggleData::class)
                FeatureToggle(
                    id = it.key.substringAfter("__ft__"),
                    isEnabled = data.value.isEnabled,
                    stateChangedAt = data.value.stateChangedAt,
                )
            },
            options = paginationOptions,
            hasMoreItems = toggles.size > paginationOptions.limit
        )
    }

    override suspend fun disable(featureToggleId: String) {
        featureToggles.get(featureToggleId).disable()
    }

    override suspend fun enable(featureToggleId: String) {
        featureToggles.get(featureToggleId).enable()
    }

}