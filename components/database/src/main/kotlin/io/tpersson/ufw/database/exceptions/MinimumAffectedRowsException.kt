package io.tpersson.ufw.database.exceptions

public open class MinimumAffectedRowsException(
    public val expected: Int,
    public val actual: Int,
) : Exception("minimumAffectedRows not hit! Expected: $expected, Actual: $actual")