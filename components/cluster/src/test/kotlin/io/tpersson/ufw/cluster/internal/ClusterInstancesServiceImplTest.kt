package io.tpersson.ufw.cluster.internal

import io.tpersson.ufw.cluster.admin.contracts.ClusterInstanceDTO
import io.tpersson.ufw.cluster.configuration.Cluster
import io.tpersson.ufw.core.AppInfoProvider
import io.tpersson.ufw.core.component.CoreComponent
import io.tpersson.ufw.core.configuration.ConfigProvider
import io.tpersson.ufw.core.configuration.Configs
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.keyvaluestore.KeyValueStore
import io.tpersson.ufw.keyvaluestore.KeyValueStoreImpl
import io.tpersson.ufw.keyvaluestore.storageengine.InMemoryStorageEngine
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.time.Duration
import java.time.Instant

internal class ClusterInstancesServiceImplTest {

    private lateinit var keyValueStore: KeyValueStore
    private lateinit var appInfoProvider: AppInfoProvider
    private lateinit var configProvider: ConfigProvider
    private lateinit var clock: TestClock

    private lateinit var startedAt: Instant

    private lateinit var service: ClusterInstancesServiceImpl

    private val expectedKey = KeyValueStore.Key.of<ClusterInstanceDTO>("__cluster:instances:test-instance")

    @BeforeEach
    fun setUp() {
        clock = TestClock()

        keyValueStore = KeyValueStoreImpl(
            clock = clock,
            storageEngine = InMemoryStorageEngine(),
            objectMapper = CoreComponent.defaultObjectMapper,
        )
        appInfoProvider = AppInfoProvider.simple(version = "0.123.0", instanceId = "test-instance")
        configProvider = ConfigProvider.empty()

        service = ClusterInstancesServiceImpl(
            keyValueStore = keyValueStore,
            appInfoProvider = appInfoProvider,
            configProvider = configProvider,
            clock = clock
        )

        startedAt = clock.instant()
        clock.advance(Duration.ofMinutes(10))
    }

    @Test
    fun `recordHeartbeat - Shall set entry in KVS`(): Unit = runBlocking {
        service.recordHeartbeat()

        val entry = keyValueStore.get(expectedKey)!!

        assertThat(entry.value).isEqualTo(ClusterInstanceDTO(
            instanceId = "test-instance",
            heartbeatTimestamp = clock.instant(),
            startedAt = startedAt,
            appVersion = "0.123.0",
        ))

        assertThat(entry.expiresAt).isEqualTo(clock.instant().plus(Configs.Cluster.InstanceStalenessAge.default))
    }

    @Test
    fun removeInstance(): Unit = runBlocking {
        service.recordHeartbeat()

        service.removeInstance()

        val entry = keyValueStore.get(expectedKey)

        assertThat(entry).isNull()
    }

    @Test
    fun listInstances(): Unit = runBlocking {
        service.recordHeartbeat()

        val entries = service.listInstances(PaginationOptions.DEFAULT).items

        assertThat(entries).hasSize(1)
        assertThat(entries[0].instanceId).isEqualTo("test-instance")
    }

}