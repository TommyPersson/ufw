package io.tpersson.ufw.cluster.internal

import io.tpersson.ufw.cluster.admin.contracts.ClusterInstanceDTO
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions

public interface ClusterInstancesService {
    public suspend fun recordHeartbeat()
    public suspend fun removeInstance()
    public suspend fun listInstances(paginationOptions: PaginationOptions): PaginatedList<ClusterInstanceDTO>
}