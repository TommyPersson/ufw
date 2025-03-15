package io.tpersson.ufw.database.typedqueries

import io.tpersson.ufw.core.utils.PaginatedList
import io.tpersson.ufw.core.utils.PaginationOptions
import io.tpersson.ufw.database.jdbc.asMaps
import io.tpersson.ufw.database.typedqueries.internal.RowEntityMapper
import io.tpersson.ufw.database.typedqueries.internal.TypedQuery
import org.intellij.lang.annotations.Language
import java.sql.Connection
import kotlin.reflect.KClass

public abstract class TypedSelectList<T>(
    @Language("sql")
    pseudoSql: String,
) : TypedQuery("$pseudoSql LIMIT :paginationOptions.limit + 1 OFFSET :paginationOptions.offset") {
    public abstract val paginationOptions: PaginationOptions
}

public fun <T : Any> Connection.select(select: TypedSelectList<T>): PaginatedList<T> {
    val returnType = select::class.supertypes.find { it.classifier == TypedSelectList::class }
        ?.arguments?.get(0)?.type?.classifier as? KClass<T>
        ?: error("No return type found for ${select::class.simpleName}")

    val rowEntityMapper = RowEntityMapper(returnType)

    val statement = select.asPreparedStatement(this)

    val items = statement.executeQuery().asMaps().map { rowEntityMapper.map(it) }

    return PaginatedList(
        items = items.take(select.paginationOptions.limit),
        options = select.paginationOptions,
        hasMoreItems = items.size > select.paginationOptions.limit,
    )
}
