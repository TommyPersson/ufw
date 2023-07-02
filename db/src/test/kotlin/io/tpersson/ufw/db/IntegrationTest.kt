package io.tpersson.ufw.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.db.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.db.jdbc.toMap
import io.tpersson.ufw.db.jdbc.useInTransaction
import io.tpersson.ufw.db.unitofwork.UnitOfWorkFactoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import java.util.UUID
import kotlin.test.fail

internal class IntegrationTest {

    companion object {
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
        }
    }

    @Test
    fun `Basic test`(): Unit = runBlocking {
        dataSource.connection.useInTransaction {
            it.prepareStatement(
                """
                CREATE TABLE test (id UUID NOT NULL PRIMARY KEY)
            """.trimIndent()
            ).execute()
        }

        val unitOfWorkFactory = UnitOfWorkFactoryImpl(ConnectionProviderImpl(dataSource))

        val unitOfWork = unitOfWorkFactory.create()

        val testId = UUID.fromString("8b00ce00-4523-412b-b1fe-98d4737bf991")

        unitOfWork.add {
            prepareStatement(
                """
                INSERT INTO test (id) VALUES (?)
            """.trimIndent()
            ).also {
                it.setObject(1, testId)
            }
        }

        assertThat(doesIdExistInDb(testId)).isFalse()

        unitOfWork.commit()

        assertThat(doesIdExistInDb(testId)).isTrue()
    }

    private fun doesIdExistInDb(id: UUID): Boolean {
        val result = dataSource.connection.use {
            it.prepareStatement("SELECT * FROM test").executeQuery().toMap()
        }

        return result.any { it.get("id") == id }
    }
}
