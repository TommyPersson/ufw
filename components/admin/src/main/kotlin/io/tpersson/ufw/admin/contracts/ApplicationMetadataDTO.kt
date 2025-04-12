package io.tpersson.ufw.admin.contracts

public data class ApplicationMetadataDTO(
    val name: String,
    val version: String,
    val environment: String,
    val availableModuleIds: List<String>,
)