package io.tpersson.ufw.core

import requireValidIdentifier

public data class AppInfo(
    val name: String,
    val version: String,
    val environment: String,
    val instanceId: String,
) {
    init {
        name.requireValidIdentifier()
        version.requireValidIdentifier()
        environment.requireValidIdentifier()
        instanceId.requireValidIdentifier()
    }
}