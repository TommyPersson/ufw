package io.tpersson.ufw.db.jdbc

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate

internal class UtilsTest {

    private lateinit var preparedStatement: PreparedStatement

    @BeforeEach
    fun beforeEach() {
        preparedStatement = mock<PreparedStatement>()
    }

    @Test
    fun `setParameters - Basic Types`() {
        preparedStatement.setParameters(
            listOf(
                Short::class to 0.toShort(),
                Short::class to null,
                Int::class to 1,
                Int::class to null,
                Long::class to 2L,
                Long::class to null,
                Double::class to 3.0,
                Double::class to null,
                Float::class to 4.0f,
                Float::class to null,
                Boolean::class to true,
                Boolean::class to null,
                Char::class to 'a',
                Char::class to null,
                String::class to "Hello, World!",
                String::class to null,
            )
        )

        verify(preparedStatement).setObject(1, 0.toShort())
        verify(preparedStatement).setNull(2, Types.SMALLINT)
        verify(preparedStatement).setObject(3, 1)
        verify(preparedStatement).setNull(4, Types.INTEGER)
        verify(preparedStatement).setObject(5, 2L)
        verify(preparedStatement).setNull(6, Types.BIGINT)
        verify(preparedStatement).setObject(7, 3.0)
        verify(preparedStatement).setNull(8, Types.DOUBLE)
        verify(preparedStatement).setObject(9, 4.0f)
        verify(preparedStatement).setNull(10, Types.FLOAT)
        verify(preparedStatement).setObject(11, true)
        verify(preparedStatement).setNull(12, Types.BOOLEAN)
        verify(preparedStatement).setObject(13, 'a')
        verify(preparedStatement).setNull(14, Types.CHAR)
        verify(preparedStatement).setObject(15, "Hello, World!")
        verify(preparedStatement).setNull(16, Types.VARCHAR)
    }

    @Test
    fun `setParameters - Time Types`() {
        preparedStatement.setParameters(
            listOf(
                Instant::class to Instant.ofEpochMilli(1),
                Instant::class to null,
                LocalDate::class to LocalDate.of(2000, 1, 1),
                LocalDate::class to null,
            )
        )

        verify(preparedStatement).setObject(1, Timestamp.from(Instant.ofEpochMilli(1)))
        verify(preparedStatement).setNull(2, Types.TIMESTAMP_WITH_TIMEZONE)
        verify(preparedStatement).setObject(3, Date.valueOf(LocalDate.of(2000, 1, 1)))
        verify(preparedStatement).setNull(4, Types.DATE)
    }
}