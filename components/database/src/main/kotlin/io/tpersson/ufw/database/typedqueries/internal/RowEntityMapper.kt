package io.tpersson.ufw.database.typedqueries.internal

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

public class RowEntityMapper<T : Any?>(
    private val type: KClass<T & Any>,
) {
    private val objectMapper = jacksonObjectMapper().findAndRegisterModules().also {
        it.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        it.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    public fun map(row: Map<String, Any?>): T {
        return objectMapper.convertValue(row, type.java)
    }
}