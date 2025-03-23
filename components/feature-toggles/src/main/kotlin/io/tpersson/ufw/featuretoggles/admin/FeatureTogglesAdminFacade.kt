package io.tpersson.ufw.featuretoggles.admin

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.featuretoggles.FeatureToggle

public interface FeatureTogglesAdminFacade {
    public suspend fun listAll(paginationOptions: PaginationOptions): PaginatedList<FeatureToggle>

    public suspend fun disable(featureToggleId: String)

    public suspend fun enable(featureToggleId: String)
}