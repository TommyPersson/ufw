package io.tpersson.ufw.database.locks.internal

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.builder.UFW
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.test.TestClock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration

internal class DatabaseLocksTest {

    private companion object {
        @JvmStatic
        var postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15")).also {
            Startables.deepStart(it).join()
        }

        val config = HikariConfig().also {
            it.jdbcUrl = postgres.jdbcUrl
            it.username = postgres.username
            it.password = postgres.password
            it.maximumPoolSize = 5
            it.isAutoCommit = false
        }

        val testClock = TestClock()

        val ufw = UFW.build {
            installCore {
                clock = testClock
            }
            installDatabase {
                dataSource = HikariDataSource(config)
            }
        }

        val locks = ufw.database.locks as DatabaseLocksImpl

        init {
            ufw.database.migrator.run()
        }
    }

    @BeforeEach
    fun setUp(): Unit = runBlocking {
    }

    @AfterEach
    fun afterEach(): Unit = runBlocking {
        locks.debugDeleteAll()
    }

    @Test
    fun `Can acquire and release locks`(): Unit = runBlocking {
        val lock = locks.create("test-lock-1", "test-instance-1")

        val handle = lock.tryAcquire()!!

        handle.release()
    }

    @Test
    fun `Cannot acquire same lock as another instance`(): Unit = runBlocking {
        val lock1 = locks.create("test-lock-1", "test-instance-1")
        val lock2 = locks.create("test-lock-1", "test-instance-2")

        val handle1 = lock1.tryAcquire()
        assertThat(handle1).isNotNull()

        val handle2 = lock2.tryAcquire()
        assertThat(handle2).isNull()
    }

    @Test
    fun `Can steal lock from another instance`(): Unit = runBlocking {
        val lock1 = locks.create("test-lock-1", "test-instance-1")
        val lock2 = locks.create("test-lock-1", "test-instance-2")

        val handle1 = lock1.tryAcquire()!!

        testClock.advance(Duration.ofMinutes(2))

        val handle2 = lock2.tryAcquire(stealIfOlderThan = Duration.ofMinutes(1))
        assertThat(handle2).isNotNull()

        assertThat(handle1.refresh()).isFalse()
    }

    @Test
    fun `Refreshes prevent theft`(): Unit = runBlocking {
        val lock1 = locks.create("test-lock-1", "test-instance-1")
        val lock2 = locks.create("test-lock-1", "test-instance-2")

        val handle1 = lock1.tryAcquire()!!

        testClock.advance(Duration.ofMinutes(2))

        handle1.refresh()

        val handle2 = lock2.tryAcquire(stealIfOlderThan = Duration.ofMinutes(1))
        assertThat(handle2).isNull()
    }

    @Test
    fun `Releasing allows other instance to acquire lock`(): Unit = runBlocking {
        val lock1 = locks.create("test-lock-1", "test-instance-1")
        val lock2 = locks.create("test-lock-1", "test-instance-2")

        val handle1 = lock1.tryAcquire()!!
        handle1.release()

        val handle2 = lock2.tryAcquire()
        assertThat(handle2).isNotNull()
    }
}