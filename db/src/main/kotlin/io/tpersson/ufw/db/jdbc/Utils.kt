package io.tpersson.ufw.db.jdbc

import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp

public fun Connection.useInTransaction(block: (Connection) -> Unit): Unit = use {
    try {
        block(this)
        commit()
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
                else -> columnValue
            }
        }

        result.add(map)
    }

    return result
}