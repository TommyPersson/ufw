package io.tpersson.ufw.cluster.admin

import io.tpersson.ufw.admin.contracts.PaginatedListDTO
import io.tpersson.ufw.admin.contracts.toDTO
import io.tpersson.ufw.cluster.admin.contracts.ClusterInstanceDTO
import io.tpersson.ufw.cluster.internal.ClusterInstancesServiceImpl
import io.tpersson.ufw.core.utils.PaginationOptions
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
public class ClusterAdminFacadeImpl @Inject constructor(
    private val clusterInstancesService: ClusterInstancesServiceImpl,
) : ClusterAdminFacade {

    override suspend fun getInstances(paginationOptions: PaginationOptions): PaginatedListDTO<ClusterInstanceDTO> {
        return clusterInstancesService.listInstances(paginationOptions).toDTO { it }
    }
}