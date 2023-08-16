package io.tpersson.ufw.database.typedqueries

import io.tpersson.ufw.database.jdbc.asMaps
import io.tpersson.ufw.database.typedqueries.internal.RowEntityMapper
import org.intellij.lang.annotations.Language
import java.sql.Connection
import kotlin.reflect.KClass

public abstract class TypedUpdateReturningSingle<T>(
    @Language("sql")
    pseudoSql: String,
    minimumAffectedRows: Int = 1,
) : TypedUpdate(pseudoSql, minimumAffectedRows)

public fun <T : Any> Connection.performUpdateReturning(update: TypedUpdateReturningSingle<T>): T? {
    val returnType = update::class.supertypes.find { it.classifier == TypedUpdateReturningSingle::class }
        ?.arguments?.get(0)?.type?.classifier as? KClass<T>
        ?: error("No return type found for ${update::class.simpleName}")

    val rowEntityMapper = RowEntityMapper(returnType)

    val statement = update.asPreparedStatement(this)

    return statement.executeQuery().asMaps().map { rowEntityMapper.map(it) }.singleOrNull()
}