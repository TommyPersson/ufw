package io.tpersson.ufw.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.db.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.db.jdbc.asMaps
import io.tpersson.ufw.db.jdbc.useInTransaction
import io.tpersson.ufw.db.unitofwork.UnitOfWorkFactoryImpl
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.UUID

internal class IntegrationTests {

    private companion object {
        @JvmStatic
        var postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:15"))

        val config by lazy {
            HikariConfig().also {
                it.jdbcUrl = postgres.jdbcUrl
                it.username = postgres.username
                it.password = postgres.password
                it.maximumPoolSize = 5
                it.isAutoCommit = false
            }
        }

        val dataSource by lazy {
            HikariDataSource(config)
        }

        init {
            Startables.deepStart(postgres).join()

            dataSource.connection.useInTransaction {
                it.prepareStatement(
                    """
                    CREATE TABLE test (id UUID NOT NULL PRIMARY KEY)
                    """.trimIndent()
                ).execute()
            }
        }
    }

    private val testItem1 = UUID.fromString("8b00ce00-4523-412b-b1fe-98d4737bf991")
    private val testItem2 = UUID.fromString("d20f1adc-c1cf-4fc7-8752-948eed61cd4e")

    private val unitOfWorkFactory = UnitOfWorkFactoryImpl(ConnectionProviderImpl(dataSource), DbModuleConfig.Default)

    @BeforeEach
    fun setUp() {
        deleteAllItems()
    }

    @Test
    fun `Basic test`(): Unit = runBlocking {
        val unitOfWork = unitOfWorkFactory.create()

        val testItem = UUID.fromString("8b00ce00-4523-412b-b1fe-98d4737bf991")

        unitOfWork.add {
            it.insertItem(testItem)
        }

        assertThat(doesItemExist(testItem)).isFalse()

        unitOfWork.commit()

        assertThat(doesItemExist(testItem)).isTrue()
    }

    @Test
    fun `Multiple items in UnitOfWork`(): Unit = runBlocking {
        val unitOfWork = unitOfWorkFactory.create()

        unitOfWork.add {
            it.insertItem(testItem1)
        }

        unitOfWork.add {
            it.insertItem(testItem2)
        }

        unitOfWork.commit()

        assertThat(doesItemExist(testItem1)).isTrue()
        assertThat(doesItemExist(testItem2)).isTrue()
    }

    @Test
    fun `Error in UnitOfWork item prevents commit`(): Unit = runBlocking {
        val unitOfWork = unitOfWorkFactory.create()

        unitOfWork.add {
            it.insertItem(testItem1)
        }

        unitOfWork.add {
            it.insertItem(testItem2)
        }

        unitOfWork.add {
            error("Oh, no")
        }

        assertThatThrownBy { runBlocking { unitOfWork.commit() } }.isNotNull()

        assertThat(doesItemExist(testItem1)).isFalse()
        assertThat(doesItemExist(testItem2)).isFalse()
    }

    @Test
    fun `Failed 'minimumAffectedRows' check prevents commit`(): Unit = runBlocking {
        val unitOfWork = unitOfWorkFactory.create()

        unitOfWork.add(minimumAffectedRows = 2) {
            it.insertItem(testItem1)
        }

        assertThatThrownBy { runBlocking { unitOfWork.commit() } }.isNotNull()

        assertThat(doesItemExist(testItem1)).isFalse()
    }

    @Test
    fun `'minimumAffectedRows' can be '0'`(): Unit = runBlocking {
        val unitOfWork = unitOfWorkFactory.create()

        unitOfWork.add(minimumAffectedRows = 1) {
            it.insertItem(testItem1)
        }

        unitOfWork.add(minimumAffectedRows = 0) {
            it.insertItem(testItem1)
        }

        unitOfWork.commit()

        assertThat(doesItemExist(testItem1)).isTrue()
    }

    private fun deleteAllItems() {
        dataSource.connection.useInTransaction {
            it.prepareStatement("DELETE FROM test").execute()
        }
    }

    private fun Connection.insertItem(item: UUID): PreparedStatement {
        return prepareStatement(
            """
            INSERT INTO test (id) VALUES (?) ON CONFLICT DO NOTHING
            """.trimIndent()
        ).also {
            it.setObject(1, item)
        }
    }

    private fun doesItemExist(id: UUID): Boolean {
        val result = dataSource.connection.use {
            it.prepareStatement("SELECT * FROM test WHERE id = '$id'").executeQuery().asMaps()
        }

        return result.any()
    }
}
