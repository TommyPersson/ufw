package io.tpersson.ufw.database.jdbc

import java.sql.*
import java.sql.Date
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

public fun <T> Connection.useInTransaction(block: (Connection) -> T): T = use {
    try {
        val result = block(this)
        commit()
        return result
    } catch (e: Exception) {
        rollback()
        throw e
    } finally {
        try {
            close()
        } catch (_: Throwable) {
        }
    }
}

public fun ResultSet.asMaps(): List<Map<String, Any?>> {
    val result = mutableListOf<Map<String, Any?>>()

    val metadata = metaData
    val numColumns = metadata.columnCount

    while (next()) {
        val map = mutableMapOf<String, Any?>()
        for (i in 1..numColumns) {
            val columnName = metadata.getColumnName(i)
            val columnValue = getObject(i)

            map[columnName] = when (columnValue) {
                is Date -> columnValue.toLocalDate()
                is Timestamp -> columnValue.toInstant()
                // TODO more
                else -> columnValue
            }
        }

        result.add(map)
    }

    return result
}


public fun PreparedStatement.setParameters(
    values: List<Pair<KClass<out Any>, Any?>>,
) {
    for ((index, typeAndValue) in values.withIndex()) {
        val (type, value) = typeAndValue


        if (value == null) {
            val sqlType = when (type) {
                Short::class -> Types.SMALLINT
                Int::class -> Types.INTEGER
                Long::class -> Types.BIGINT
                Double::class -> Types.DOUBLE
                Float::class -> Types.FLOAT
                Boolean::class -> Types.BOOLEAN
                Char::class -> Types.CHAR
                String::class -> Types.VARCHAR
                Instant::class -> Types.TIMESTAMP_WITH_TIMEZONE
                LocalDate::class -> Types.DATE

                // TODO more
                else -> Types.BIT
            }

            setNull(index + 1, sqlType)
        } else {
            val nativeValue = when (value) {
                is Instant -> Timestamp.from(value)
                is LocalDate -> Date.valueOf(value)
                // TODO more
                else -> value
            }

            setObject(index + 1, nativeValue)
        }
    }
}

private val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
