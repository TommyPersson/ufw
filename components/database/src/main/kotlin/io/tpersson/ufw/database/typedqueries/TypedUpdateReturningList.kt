package io.tpersson.ufw.database.typedqueries

import io.tpersson.ufw.database.jdbc.asMaps
import io.tpersson.ufw.database.typedqueries.internal.RowEntityMapper
import org.intellij.lang.annotations.Language
import java.sql.Connection
import kotlin.reflect.KClass

public abstract class TypedUpdateReturningList<T>(
    @Language("sql")
    pseudoSql: String,
    minimumAffectedRows: Int = 1,
) : TypedUpdate(pseudoSql, minimumAffectedRows)

public fun <T : Any> Connection.performUpdateReturningList(update: TypedUpdateReturningList<T>): List<T> {
    val returnType = update::class.supertypes.find { it.classifier == TypedUpdateReturningList::class }
        ?.arguments?.get(0)?.type?.classifier as? KClass<T>
        ?: error("No return type found for ${update::class.simpleName}")

    val rowEntityMapper = RowEntityMapper(returnType)

    val statement = update.asPreparedStatement(this)

    return statement.executeQuery().asMaps().map { rowEntityMapper.map(it) }
}