package io.tpersson.ufw.database.exceptions

import io.tpersson.ufw.database.typedqueries.TypedUpdate

public class TypedUpdateMinimumAffectedRowsException(
    expected: Int,
    actual: Int,
    public val query: TypedUpdate
) : MinimumAffectedRowsException(expected, actual)