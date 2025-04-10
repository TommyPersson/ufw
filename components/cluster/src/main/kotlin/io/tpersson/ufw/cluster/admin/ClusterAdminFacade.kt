package io.tpersson.ufw.cluster.admin

import io.tpersson.ufw.admin.contracts.PaginatedListDTO
import io.tpersson.ufw.cluster.admin.contracts.ClusterInstanceDTO
import io.tpersson.ufw.core.utils.PaginationOptions

public interface ClusterAdminFacade {
    public suspend fun getInstances(
        paginationOptions: PaginationOptions
    ): PaginatedListDTO<ClusterInstanceDTO>
}