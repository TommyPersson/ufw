package io.tpersson.ufw.database.jdbc

import org.postgresql.util.PGobject
import java.sql.*
import java.sql.Date
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

public suspend fun <T> Connection.useInTransaction(block: suspend (Connection) -> T): T = use {
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

// TODO total rewrite, needs to know target properties to do proper type conversions (and support new deserializers easily)
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
                is PGobject -> columnValue.value
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
                List::class -> Types.ARRAY
                ByteArray::class -> Types.BINARY

                // TODO more
                else -> Types.BIT
            }

            setNull(index + 1, sqlType)
        } else if (value is List<*>) {
            // TODO support any array type
            val arr = this.connection.createArrayOf("BIGINT", value.toTypedArray())

            setArray(index + 1, arr)
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
