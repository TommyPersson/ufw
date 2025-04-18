package io.tpersson.ufw.cluster.internal

import io.tpersson.ufw.cluster.admin.contracts.ClusterInstanceDTO
import io.tpersson.ufw.cluster.configuration.Cluster
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Clock

@Singleton
public class ClusterInstancesServiceImpl @Inject constructor(
    private val keyValueStore: KeyValueStore,
    private val appInfoProvider: AppInfoProvider,
    private val configProvider: ConfigProvider,
    private val clock: Clock,
) : ClusterInstancesService {
    private val instanceStalenessAge = configProvider.get(Configs.Cluster.InstanceStalenessAge)

    private val startedAt = clock.instant()

    private val appInfo = appInfoProvider.get()
    private val instanceId = appInfo.instanceId

    private val kvsPrefix = "__cluster:instances"
    private val kvsKey = KeyValueStore.Key.of<ClusterInstanceDTO>("$kvsPrefix:$instanceId")

    public override suspend fun recordHeartbeat() {
        keyValueStore.put(
            key = kvsKey,
            value = ClusterInstanceDTO(
                instanceId = instanceId,
                appVersion = appInfo.version,
                startedAt = startedAt,
                heartbeatTimestamp = clock.instant()
            ),
            ttl = instanceStalenessAge
        )
    }

    public override suspend fun removeInstance() {
        keyValueStore.remove(kvsKey)
    }

    public override suspend fun listInstances(paginationOptions: PaginationOptions): PaginatedList<ClusterInstanceDTO> {
        val entries = keyValueStore.list("$kvsPrefix:", paginationOptions.limit + 1, paginationOptions.offset)

        return PaginatedList(
            items = entries.take(paginationOptions.limit).map { it.parseAs(ClusterInstanceDTO::class).value },
            options = paginationOptions,
            hasMoreItems = entries.size > paginationOptions.limit
        )
    }
}