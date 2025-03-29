package io.tpersson.ufw.mediator.admin

import io.tpersson.ufw.admin.contracts.toApplicationModuleDTO
import io.tpersson.ufw.core.utils.findModuleMolecule
import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.Query
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestDTO
import io.tpersson.ufw.mediator.annotations.AdminRequest
import io.tpersson.ufw.mediator.internal.MediatorInternal
import jakarta.inject.Inject
import kotlin.reflect.full.findAnnotation

public class AdminRequestsAdminFacadeImpl @Inject constructor(
    private val mediator: MediatorInternal,
) : AdminRequestsAdminFacade {

    private val commandClasses = mediator.requestClasses.filter { Command::class.java.isAssignableFrom(it.java) }
    private val queryClasses = mediator.requestClasses.filter { Query::class.java.isAssignableFrom(it.java) }

    override fun getRequests(requestType: RequestType): List<AdminRequestDTO> {
        val requestClasses = when (requestType) {
            RequestType.QUERY -> queryClasses
            RequestType.COMMAND -> commandClasses
        }

        return requestClasses.mapNotNull {
            val annotation = it.findAnnotation<AdminRequest>()
                ?: return@mapNotNull null // TODO cache

            AdminRequestDTO(
                name = annotation.name,
                description = annotation.description,
                className = it.simpleName!!,
                fullClassName = it.qualifiedName!!,
                type = requestType,
                applicationModule = it.findModuleMolecule().toApplicationModuleDTO()
            )
        }
    }

}