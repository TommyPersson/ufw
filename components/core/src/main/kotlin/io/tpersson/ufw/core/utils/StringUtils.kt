package io.tpersson.ufw.core.utils

public fun String.nullIfBlank(): String? {
    return ifBlank { null }
}