package io.tpersson.ufw.mediator.admin.contracts

public data class AdminRequestParameterDTO(
    val field: String,
    val displayName: String,
    val type: AdminRequestParameterType,
    val helperText: String?,
    val required: Boolean,
    val defaultValue: String?,
)

