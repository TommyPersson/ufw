package io.tpersson.ufw.database.typedqueries

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.tpersson.ufw.core.component.installCore
import io.tpersson.ufw.core.builder.UFW
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.core.utils.paginate
import io.tpersson.ufw.database.component.installDatabase
import io.tpersson.ufw.database.component.database
import io.tpersson.ufw.database.exceptions.MinimumAffectedRowsException
import io.tpersson.ufw.database.jdbc.useInTransaction
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

        val ufw = UFW.build {
            installCore()
            installDatabase {
                dataSource = HikariDataSource(config)
            }
        }

        val databaseComponent = ufw.database
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
        database.update(Queries.Updates.TruncateBasicTypes)
        database.update(Queries.Updates.TruncateTimeTypes)
    }

    @Test
    fun `Writing & Reading basic types`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        val originalData = BasicTypesEntity(
            id = id,
            theShort = 1.toShort(),
            theInt = 2,
            theLong = 3L,
            theDouble = 4.0,
            theFloat = 5.0f,
            theBoolean = true,
            theChar = 'A',
            theString = "Hello, World!"
        )

        database.update(Queries.Updates.InsertBasicTypesData(originalData))

        val selectedData = connectionProvider.get().select(Queries.Selects.SelectBasicTypesData(id))

        assertThat(selectedData).isEqualTo(originalData)
    }

    @Test
    fun `Writing & Reading basic types with nulls`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        val originalData = BasicTypesEntity(
            id = id,
            theShort = null,
            theInt = null,
            theLong = null,
            theDouble = null,
            theFloat = null,
            theBoolean = null,
            theChar = null,
            theString = null,
        )

        database.update(Queries.Updates.InsertBasicTypesData(originalData))

        val selectedData = database.select(Queries.Selects.SelectBasicTypesData(id))

        assertThat(selectedData).isEqualTo(originalData)
    }

    @Test
    fun `Writing & Reading Time types`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        val originalData = TimeTypesEntity(
            theInstant = Instant.ofEpochMilli(1),
            theLocalDate = LocalDate.of(2000, 1, 1),
        )

        database.update(Queries.Updates.InsertTimeTypesData(id, originalData))

        val selectedData = database.select(Queries.Selects.SelectTimeTypesData(id))

        assertThat(selectedData).isEqualTo(originalData)
    }

    @Test
    fun `TypedSelectList - Returns a list of results`(): Unit = runBlocking {
        val originalData1 = BasicTypesEntity(UUID.randomUUID())
        database.update(Queries.Updates.InsertBasicTypesData(originalData1))

        val originalData2 = BasicTypesEntity(UUID.randomUUID())
        database.update(Queries.Updates.InsertBasicTypesData(originalData2))

        val selectedData = database.select(Queries.Selects.SelectBasicTypesDataList(PaginationOptions.DEFAULT)).items

        assertThat(selectedData).contains(originalData1)
        assertThat(selectedData).contains(originalData2)
    }

    @Test
    fun `TypedSelectList - Can be paginated`(): Unit = runBlocking {
        val expectedInts = 1..101
        for (i in expectedInts) {
            val originalData1 = BasicTypesEntity(UUID.randomUUID(), theInt = i)
            database.update(Queries.Updates.InsertBasicTypesData(originalData1))
        }

        val actualInts = paginate(PaginationOptions.DEFAULT.copy(limit = 2)) {
            database.select(Queries.Selects.SelectBasicTypesDataList(it))
        }.toList().flatMap { it.items }.map { it.theInt!! }

        assertThat(actualInts).isEqualTo(expectedInts.toList())
    }

    @Test
    fun `TypedUpdate - Can throws 'MinimumAffectedRowsException'`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        assertThatThrownBy {
            runBlocking {
                database.update(
                    Queries.Updates.UpdateData(
                        id = id,
                        int = 2
                    )
                )
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `TypedUpdateReturningSingle - Updates and returns a single item`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        val originalData = BasicTypesEntity(
            id = id,
            theShort = 1,
            theInt = 2,
            theLong = 3,
            theDouble = 4.0,
            theFloat = 5.0f,
            theBoolean = true,
            theChar = 'A',
            theString = "ABC"
        )

        val insertedData = database.update(Queries.Updates.InsertBasicTypesDataReturning(originalData))

        assertThat(insertedData).isEqualTo(originalData)
    }

    @Test
    fun `TypedUpdateReturningSingle - Can throws 'MinimumAffectedRowsException'`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        assertThatThrownBy {
            runBlocking {
                database.update(
                    Queries.Updates.UpdateDataReturningSingle(
                        id = id,
                        int = 2
                    )
                )
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    @Test
    fun `TypedUpdateReturningList - Updates and returns a list of results`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        val originalData = BasicTypesEntity(
            id = id,
            theShort = 1,
            theInt = 2,
            theLong = 3,
            theDouble = 4.0,
            theFloat = 5.0f,
            theBoolean = true,
            theChar = 'A',
            theString = "ABC"
        )

        val insertedData = database.update(Queries.Updates.InsertBasicTypesDataReturningList(originalData))

        assertThat(insertedData.single()).isEqualTo(originalData)
    }

    @Test
    fun `TypedUpdateReturningList - Can throws 'MinimumAffectedRowsException'`(): Unit = runBlocking {
        val id = UUID.fromString("b5f78035-1871-434c-8606-f16399e27e43")

        assertThatThrownBy {
            runBlocking {
                database.update(
                    Queries.Updates.UpdateDataReturningSingle(
                        id = id,
                        int = 2
                    )
                )
            }
        }.isInstanceOf(MinimumAffectedRowsException::class.java)
    }

    data class BasicTypesEntity(
        val id: UUID,
        val theShort: Short? = null,
        val theInt: Int? = null,
        val theLong: Long? = null,
        val theDouble: Double? = null,
        val theFloat: Float? = null,
        val theBoolean: Boolean? = null,
        val theChar: Char? = null,
        val theString: String? = null,
    )

    data class TimeTypesEntity(
        val theInstant: Instant?,
        val theLocalDate: LocalDate?,
    )

    object Queries {
        object Selects {
            class SelectBasicTypesData(
                val id: UUID
            ) : TypedSelectSingle<BasicTypesEntity>("SELECT * FROM basic_types WHERE id = :id")

            class SelectBasicTypesDataList(
                override val paginationOptions: PaginationOptions
            ) : TypedSelectList<BasicTypesEntity>("SELECT * FROM basic_types")

            class SelectTimeTypesData(
                val id: UUID
            ) : TypedSelectSingle<TimeTypesEntity>("SELECT * FROM time_types WHERE id = :id")
        }

        object Updates {
            val insertBasicTypesSql = """
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
                    (:data.id,
                     :data.theShort,
                     :data.theInt,
                     :data.theLong,
                     :data.theDouble,
                     :data.theFloat,
                     :data.theBoolean,
                     :data.theChar,
                     :data.theString)
                """

            class InsertBasicTypesData(
                val data: BasicTypesEntity
            ) : TypedUpdate(
                """
                $insertBasicTypesSql
                """.trimIndent()
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

            class InsertBasicTypesDataReturning(
                val data: BasicTypesEntity,
            ) : TypedUpdateReturningSingle<BasicTypesEntity>(
                """
                $insertBasicTypesSql
                RETURNING *     
                """.trimIndent()
            )

            class InsertBasicTypesDataReturningList(
                val data: BasicTypesEntity,
            ) : TypedUpdateReturningList<BasicTypesEntity>(
                """
                $insertBasicTypesSql
                RETURNING *     
                """.trimIndent()
            )

            val updateSql = """
                UPDATE basic_types 
                SET the_int = :int
                WHERE id = :id
                """

            class UpdateData(
                val id: UUID,
                val int: Int,
            ) : TypedUpdateReturningList<BasicTypesEntity>(
                """
                $updateSql
                RETURNING *
                """.trimIndent()
            )

            class UpdateDataReturningSingle(
                val id: UUID,
                val int: Int,
            ) : TypedUpdateReturningList<BasicTypesEntity>(
                """
                $updateSql
                RETURNING *
                """.trimIndent()
            )

            class UpdateDataReturningList(
                val id: UUID,
                val int: Int,
            ) : TypedUpdateReturningList<BasicTypesEntity>(
                """
                $updateSql
                RETURNING *     
                """.trimIndent()
            )

            object TruncateBasicTypes : TypedUpdate("DELETE FROM basic_types", minimumAffectedRows = 0)

            object TruncateTimeTypes : TypedUpdate("DELETE FROM time_types", minimumAffectedRows = 0)
        }
    }
}