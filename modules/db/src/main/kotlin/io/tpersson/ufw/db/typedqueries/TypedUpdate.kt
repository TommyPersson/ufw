package io.tpersson.ufw.db.typedqueries

import org.intellij.lang.annotations.Language
import java.sql.*
import java.time.Instant
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

public abstract class TypedUpdate(
    @Language("sql")
    public val pseudoSql: String,
    public val minimumAffectedRows: Int = 1,
) {
    private val regex = Regex("(?<!:):[a-zA-Z]+(\\.[a-zA-Z]+)*")

    public val argNames: List<String> = regex.findAll(pseudoSql).toList().map { it.value.trimStart(':') }
    public val rawSql: String = regex.replace(pseudoSql, "?")

    public fun asPreparedStatement(connection: Connection): PreparedStatement {
        val statement = connection.prepareStatement(rawSql)
        val values = argNames.map { getValueForArgument(it) }

        for ((index, typeAndValue) in values.withIndex()) {
            val (type, value) = typeAndValue
            val nativeValue = when (value) {
                is Instant -> Timestamp.from(value)
                is LocalDate -> Date.valueOf(value)
                else -> value
            }

            if (value == null) {
                val sqlType = when (type) {
                    Instant::class -> Types.TIMESTAMP
                    Int::class -> Types.INTEGER
                    else -> Types.BIT
                }

                statement.setNull(index + 1, sqlType)
            } else {
                statement.setObject(index + 1, nativeValue)
            }
        }

        return statement
    }

    // TODO do some smarter reflection or LambdaMetaFactory stuff later
    private fun getValueForArgument(argName: String): Pair<KClass<Any>, Any?> {
        return if (!argName.contains(".")) {
            getValueForSimpleArgument(argName)
        } else {
            getValueForNestedArgument(argName)
        }
    }

    private fun getValueForSimpleArgument(argName: String): Pair<KClass<Any>, Any?> {
        @Suppress("UNCHECKED_CAST")
        val property = this::class.declaredMemberProperties
            .map { it as KProperty1<TypedUpdate, Any?> }
            .firstOrNull { it.name == argName }
            ?: error("Unable to find parameter for '$argName'")

        val propertyType = property.returnType.classifier as KClass<Any>

        return propertyType to property.get(this)
    }

    private fun getValueForNestedArgument(argName: String): Pair<KClass<Any>, Any?> {
        val fullPath = argName.split(".")
        var path = fullPath[0]

        var (type, obj) = getValueForSimpleArgument(path)
        if (obj == null) {
            return type to null
        }

        var i = 1
        while (i < fullPath.size) {
            path = fullPath[i]

            @Suppress("UNCHECKED_CAST")
            val property = obj!!::class.declaredMemberProperties.firstOrNull { it.name == path } as? KProperty1<Any, Any?>
                ?: error("Unable to find parameter for '$argName'")

            type = property.returnType.classifier as KClass<Any>

            val propertyValue = property.get(obj)
                ?: return type to null

            obj = propertyValue
            i++
        }

        return type to obj
    }
}
