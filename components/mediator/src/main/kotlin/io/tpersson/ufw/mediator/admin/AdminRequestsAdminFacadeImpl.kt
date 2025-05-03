package io.tpersson.ufw.mediator.admin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.tpersson.ufw.admin.contracts.toApplicationModuleDTO
import io.tpersson.ufw.core.NamedBindings
import io.tpersson.ufw.core.utils.findModuleMolecule
import io.tpersson.ufw.core.utils.nullIfBlank
import io.tpersson.ufw.mediator.Command
import io.tpersson.ufw.mediator.Query
import io.tpersson.ufw.mediator.Request
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestDTO
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestExecutionResponseDTO
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestParameterDTO
import io.tpersson.ufw.mediator.admin.contracts.AdminRequestParameterType
import io.tpersson.ufw.mediator.annotations.AdminRequest
import io.tpersson.ufw.mediator.annotations.AdminRequestParameter
import io.tpersson.ufw.mediator.internal.MediatorInternal
import jakarta.inject.Inject
import jakarta.inject.Named
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf

public class AdminRequestsAdminFacadeImpl @Inject constructor(
    private val mediator: MediatorInternal,
    @Named(NamedBindings.ObjectMapper) private val objectMapper: ObjectMapper
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
                val parameterAnnotation = it.findAnnotation<AdminRequestParameter>()
                AdminRequestParameterDTO(
                    field = it.name!!,
                    displayName = parameterAnnotation?.name?.nullIfBlank() ?: it.name!!,
                    type = getParameterType(it.type),
                    helperText = parameterAnnotation?.helperText,
                    required = !it.type.isMarkedNullable,
                    defaultValue = parameterAnnotation?.defaultValue?.nullIfBlank(),
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

    override suspend fun executeRequest(requestFqcn: String, requestBody: JsonNode, requestType: RequestType): AdminRequestExecutionResponseDTO {
        val klass = Class.forName(requestFqcn)

        if (klass.isAssignableFrom(Query::class.java) && requestType != RequestType.QUERY) {
            error("'${requestType.name}' is not a Query!")
        }

        if (klass.isAssignableFrom(Command::class.java) && requestType != RequestType.COMMAND) {
            error("'${requestType.name}' is not a Command!")
        }

        val requestInstance = objectMapper.convertValue(requestBody, klass) as Request<*>

        val responseBody = mediator.send(requestInstance)

        return AdminRequestExecutionResponseDTO(
            body = responseBody ?: Unit,
        )
    }

    private fun getParameterType(type: KType): AdminRequestParameterType {
        return when (type) {
            typeOf<Int>(), typeOf<Long>() -> AdminRequestParameterType.INTEGER
            typeOf<String>() -> AdminRequestParameterType.STRING
            typeOf<Double>() -> AdminRequestParameterType.DECIMAL
            typeOf<Float>() -> AdminRequestParameterType.STRING
            typeOf<LocalDate>() -> AdminRequestParameterType.LOCAL_DATE
            typeOf<LocalTime>() -> AdminRequestParameterType.LOCAL_TIME
            typeOf<LocalDateTime>() -> AdminRequestParameterType.LOCAL_DATE_TIME
            else -> AdminRequestParameterType.STRING
        }
    }

}