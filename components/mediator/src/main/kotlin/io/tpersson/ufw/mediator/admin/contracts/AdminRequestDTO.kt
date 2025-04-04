package io.tpersson.ufw.mediator.admin.contracts

import io.tpersson.ufw.admin.contracts.ApplicationModuleDTO
import io.tpersson.ufw.mediator.admin.RequestType

public data class AdminRequestDTO(
    val name: String,
    val className: String,
    val fullClassName: String,
    val description: String,
    val type: RequestType,
    val parameters: List<AdminRequestParameterDTO>,
    val applicationModule: ApplicationModuleDTO
)

