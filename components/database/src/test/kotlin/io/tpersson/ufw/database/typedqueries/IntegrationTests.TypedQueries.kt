package io.tpersson.ufw.database.typedqueries

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.database.DatabaseComponent
import io.tpersson.ufw.database.DatabaseModuleConfig
import io.tpersson.ufw.database.jdbc.ConnectionProviderImpl
import io.tpersson.ufw.database.jdbc.useInTransaction
import io.tpersson.ufw.database.unitofwork.UnitOfWorkFactoryImpl
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.time.LocalDate
import java.util.*

internal class IntegrationTestsTypedQueries {

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

        val dataSource = HikariDataSource(config)
        val databaseComponent = DatabaseComponent.create(dataSource)
        val connectionProvider = databaseComponent.connectionProvider
        val database = databaseComponent.database

        init {
            runBlocking {
                connectionProvider.get().useInTransaction {
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

                    it.prepareStatement(
                        """
                        CREATE TABLE time_types (
                        id UUID NOT NULL PRIMARY KEY,
                        the_instant TIMESTAMPTZ,
                        the_local_date DATE
                        )
                        """.trimIndent()
                    ).execute()
                }
            }
        }
    }

    @BeforeEach
    fun beforeEach(): Unit = runBlocking {
        database.update(TruncateBasicTypes)
        database.update(TruncateTimeTypes)
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

        database.update(InsertBasicTypesData(id, originalData))

        val selectedData = connectionProvider.get().selectSingle(SelectBasicTypesData(id))

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

        database.update(InsertBasicTypesData(id, originalData))

        val selectedData = database.select(SelectBasicTypesData(id))

        assertThat(selectedData).isEqualTo(originalData)
    }

    @Test
    fun `Writing & Reading Time types`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        val originalData = TimeTypesEntity(
            theInstant = Instant.ofEpochMilli(1),
            theLocalDate = LocalDate.of(2000, 1, 1),
        )

        database.update(InsertTimeTypesData(id, originalData))

        val selectedData = database.select(SelectTimeTypesData(id))

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
    ) : TypedUpdate(
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

    object TruncateBasicTypes : TypedUpdate("DELETE FROM basic_types", minimumAffectedRows = 0)

    data class TimeTypesEntity(
        val theInstant: Instant?,
        val theLocalDate: LocalDate?,
    )

    class InsertTimeTypesData(
        val id: UUID,
        val data: TimeTypesEntity
    ) : TypedUpdate(
        """
        INSERT INTO time_types (
            id,
            the_instant,
            the_local_date)
        VALUES
            (:id,
             :data.theInstant,
             :data.theLocalDate)
    """.trimIndent()
    )

    class SelectTimeTypesData(
        val id: UUID
    ) : TypedSelect<TimeTypesEntity>("SELECT * FROM time_types WHERE id = :id")

    object TruncateTimeTypes : TypedUpdate("DELETE FROM time_types", minimumAffectedRows = 0)

}