package io.tpersson.ufw.admin

import io.ktor.http.*

public class ApiException(
    public val errorCode: String,
    public val errorMessage: String,
    public val statusCode: HttpStatusCode = HttpStatusCode.BadRequest,
): Exception(errorMessage)