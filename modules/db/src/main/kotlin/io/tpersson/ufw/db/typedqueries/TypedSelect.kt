package io.tpersson.ufw.db.typedqueries

import io.tpersson.ufw.db.jdbc.asMaps
import io.tpersson.ufw.db.typedqueries.internal.RowEntityMapper
import io.tpersson.ufw.db.typedqueries.internal.TypedQuery
import org.intellij.lang.annotations.Language
import java.sql.Connection
import kotlin.reflect.KClass

public abstract class TypedSelect<T>(
    @Language("sql")
    pseudoSql: String
) : TypedQuery(pseudoSql)

public fun <T : Any> Connection.selectSingle(select: TypedSelect<T>): T? {
    val returnType = select::class.supertypes.find { it.classifier == TypedSelect::class }
        ?.arguments?.get(0)?.type?.classifier as? KClass<T>
        ?: error("No return type found for ${select::class.simpleName}")

    val rowEntityMapper = RowEntityMapper(returnType)

    val statement = select.asPreparedStatement(this)

    return statement.executeQuery().asMaps().map { rowEntityMapper.map(it) }.singleOrNull()
}

public fun <T : Any> Connection.selectList(select: TypedSelect<T>): List<T> {
    val returnType = select::class.supertypes.find { it.classifier == TypedSelect::class }
        ?.arguments?.get(0)?.type?.classifier as? KClass<T>
        ?: error("No return type found for ${select::class.simpleName}")

    val rowEntityMapper = RowEntityMapper(returnType)

    val statement = select.asPreparedStatement(this)

    return statement.executeQuery().asMaps().map { rowEntityMapper.map(it) }
}


internal fun main() {
    data class Test1(val snakeCase: String)

    val rowEntityMapper = RowEntityMapper(LinkedHashMap::class)

    val result = rowEntityMapper.map(mapOf("snake_case" to "value"))

    println(result)
}