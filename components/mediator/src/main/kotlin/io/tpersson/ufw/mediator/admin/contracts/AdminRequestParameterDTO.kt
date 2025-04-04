package io.tpersson.ufw.mediator.admin.contracts

public data class AdminRequestParameterDTO(
    val name: String,
    val type: AdminRequestParameterType,
    val description: String,
    val required: Boolean,
)

