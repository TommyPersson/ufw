package io.tpersson.ufw.cluster.admin

import io.tpersson.ufw.admin.contracts.PaginatedListDTO
import io.tpersson.ufw.cluster.admin.contracts.ClusterInstanceDTO
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.time.Duration
import java.time.Instant
import java.util.*

@Singleton
public class ClusterAdminFacadeImpl @Inject constructor(
    private val keyValueStore: KeyValueStore,
) : ClusterAdminFacade {
    private val instanceId = UUID.randomUUID() // TODO applicationIdProvider

    private val startedAt = Instant.now()

    private val kvsKey = KeyValueStore.Key.of<ClusterInstanceDTO>("__cluster:instances:$instanceId")

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                withTimeout(1_000) {
                    keyValueStore.remove(kvsKey)
                }
            }
        })
    }

    override suspend fun getInstances(paginationOptions: PaginationOptions): PaginatedListDTO<ClusterInstanceDTO> {


        keyValueStore.put(
            key = kvsKey,
            value = ClusterInstanceDTO(
                instanceId = instanceId.toString(),
                appVersion = "2",
                startedAt = startedAt,
                heartbeatTimestamp = Instant.now()
            ),
            ttl = Duration.ofMinutes(5)
        )

        val instances = keyValueStore.list("__cluster:instances:", paginationOptions.limit + 1, paginationOptions.offset)

        return PaginatedListDTO(
            items = instances.take(paginationOptions.limit).map { it.parseAs(ClusterInstanceDTO::class).value },
            hasMoreItems = instances.size > paginationOptions.limit
        )
    }
}