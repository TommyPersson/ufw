package io.tpersson.ufw.mediator.admin

import io.tpersson.ufw.admin.contracts.toApplicationModuleDTO
import io.tpersson.ufw.core.utils.findModuleMolecule
import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.Query
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestDTO
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestParameterDTO
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestParameterType
import io.tpersson.ufw.mediator.annotations.AdminRequest
import io.tpersson.ufw.mediator.internal.MediatorInternal
import jakarta.inject.Inject
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

public class AdminRequestsAdminFacadeImpl @Inject constructor(
    private val mediator: MediatorInternal,
) : AdminRequestsAdminFacade {

    private val commandClasses get() = mediator.requestClasses.filter { Command::class.java.isAssignableFrom(it.java) }
    private val queryClasses get() = mediator.requestClasses.filter { Query::class.java.isAssignableFrom(it.java) }

    override fun getRequests(requestType: RequestType): List<AdminRequestDTO> {
        val requestClasses = when (requestType) {
            RequestType.QUERY -> queryClasses
            RequestType.COMMAND -> commandClasses
        }

        return requestClasses.mapNotNull { requestClass ->
            val annotation = requestClass.findAnnotation<AdminRequest>()
                ?: return@mapNotNull null // TODO cache

            val parameters = requestClass.primaryConstructor!!.parameters.map {
                AdminRequestParameterDTO(
                    name = it.name!!,
                    type = getParameterType(it.type),
                    description = "TODO",
                    required = !it.isOptional,
                )
            }

            AdminRequestDTO(
                name = annotation.name,
                description = annotation.description,
                className = requestClass.simpleName!!,
                fullClassName = requestClass.qualifiedName!!,
                type = requestType,
                parameters = parameters,
                applicationModule = requestClass.findModuleMolecule().toApplicationModuleDTO()
            )
        }
    }

    private fun getParameterType(type: KType): AdminRequestParameterType {
        return when (type) {
            typeOf<Int>(), typeOf<Long>() -> AdminRequestParameterType.INTEGER
            typeOf<String>() -> AdminRequestParameterType.STRING
            else -> TODO("not implemented: ${type}")
        }
    }

}