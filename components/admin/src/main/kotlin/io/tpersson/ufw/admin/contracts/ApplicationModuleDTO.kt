package io.tpersson.ufw.admin.contracts

import org.jmolecules.ddd.annotation.Module

public data class ApplicationModuleDTO(
    val id: String,
    val name: String,
    val description: String,
)

public fun Module.toApplicationModuleDTO(): ApplicationModuleDTO {
    return ApplicationModuleDTO(
        id = id,
        name = name,
        description = description,
    )
}