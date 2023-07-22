package io.tpersson.ufw.keyvaluestore.exceptions

public class VersionMismatchException(
    public val key: String,
    public val expected: Int?,
    cause: Throwable
) : Exception("Version mismatch when committing key '$key'. Expected = $expected", cause) {
}