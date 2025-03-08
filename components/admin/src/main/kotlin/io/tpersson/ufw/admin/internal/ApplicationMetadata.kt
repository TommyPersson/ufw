package io.tpersson.ufw.admin.internal

public data class ApplicationMetadata(
    val name: String,
    val version: String,
    val availableModuleIds: List<String>,
)