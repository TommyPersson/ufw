package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.inject.Binder
import com.google.inject.Module

public class CoreGuiceModule(
    private val configureObjectMapper: ObjectMapper.() -> Unit
) : Module {
    override fun configure(binder: Binder) {
        val objectMapper = UFWObjectMapper.default.objectMapper.also(configureObjectMapper)

        binder.bind(CoreComponent::class.java)
        binder.bind(UFWObjectMapper::class.java).toInstance(UFWObjectMapper(objectMapper))
    }
}

