package io.tpersson.ufw.admin.utils

import io.ktor.server.application.*
import io.tpersson.ufw.core.utils.PaginationOptions

public fun ApplicationCall.getPaginationOptions(
    defaultLimit: Int = 100,
    defaultOffset: Int = 0,
): PaginationOptions {
    return PaginationOptions(
        limit = parameters["limit"]?.toInt() ?: defaultLimit,
        offset = parameters["offset"]?.toInt() ?: defaultOffset
    )
}