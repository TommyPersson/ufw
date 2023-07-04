package io.tpersson.ufw.db.typedqueries

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.db.DbModuleConfig
import io.tpersson.ufw.db.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.db.jdbc.useInTransaction
import io.tpersson.ufw.db.unitofwork.UnitOfWorkFactoryImpl
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.util.UUID

internal class IntegrationTestsTypedQueries {

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
                    CREATE TABLE basic_types (
                    id UUID NOT NULL PRIMARY KEY,
                    the_short SMALLINT,
                    the_int INT,
                    the_long BIGINT,
                    the_double DOUBLE PRECISION,
                    the_float FLOAT,
                    the_boolean BOOLEAN,
                    the_char CHAR,
                    the_string TEXT                    
                    )
                    """.trimIndent()
                ).execute()
            }
        }

        val unitOfWorkFactory by lazy { UnitOfWorkFactoryImpl(ConnectionProviderImpl(dataSource), DbModuleConfig()) }
    }

    @BeforeEach
    fun beforeEach() {
        dataSource.connection.useInTransaction { it.performUpdate(TruncateTestTables) }
    }

    @Test
    fun `Writing & Reading basic types`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        val originalData = BasicTypesEntity(
            theShort = 1.toShort(),
            theInt = 2,
            theLong = 3L,
            theDouble = 4.0,
            theFloat = 5.0f,
            theBoolean = true,
            theChar = 'A',
            theString = "Hello, World!"
        )

        val unitOfWork = unitOfWorkFactory.create()
        unitOfWork.add(InsertBasicTypesData(id, originalData))
        unitOfWork.commit()

        val selectedData = dataSource.connection.selectSingle(SelectBasicTypesData(id))

        assertThat(selectedData).isEqualTo(originalData)
    }

    @Test
    fun `Writing & Reading basic types with nulls`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        val originalData = BasicTypesEntity(
            theShort = null,
            theInt = null,
            theLong = null,
            theDouble = null,
            theFloat = null,
            theBoolean = null,
            theChar = null,
            theString = null,
        )

        val unitOfWork = unitOfWorkFactory.create()
        unitOfWork.add(InsertBasicTypesData(id, originalData))
        unitOfWork.commit()

        val selectedData = dataSource.connection.selectSingle(SelectBasicTypesData(id))

        assertThat(selectedData).isEqualTo(originalData)
    }

    data class BasicTypesEntity(
        val theShort: Short?,
        val theInt: Int?,
        val theLong: Long?,
        val theDouble: Double?,
        val theFloat: Float?,
        val theBoolean: Boolean?,
        val theChar: Char?,
        val theString: String?
    )

    class InsertBasicTypesData(
        val id: UUID,
        val data: BasicTypesEntity
    ): TypedUpdate(
        """
        INSERT INTO basic_types (
            id,
            the_short,
            the_int,
            the_long,
            the_double,
            the_float,
            the_boolean,
            the_char,
            the_string)
        VALUES
            (:id,
             :data.theShort,
             :data.theInt,
             :data.theLong,
             :data.theDouble,
             :data.theFloat,
             :data.theBoolean,
             :data.theChar,
             :data.theString)
    """.trimIndent()
    )

    class SelectBasicTypesData(
        val id: UUID
    ) : TypedSelect<BasicTypesEntity>("SELECT * FROM basic_types WHERE id = :id")

    object TruncateTestTables : TypedUpdate("DELETE FROM basic_types")
}