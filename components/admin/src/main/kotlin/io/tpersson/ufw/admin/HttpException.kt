package io.tpersson.ufw.admin

import io.ktor.http.*

public open class HttpException(
    public val statusCode: HttpStatusCode,
    public val statusText: String = statusCode.description,
) : Exception("$statusText: $statusText")

public fun HttpStatusCode.raise(): Nothing {
    throw HttpException(this)
}