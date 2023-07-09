package io.tpersson.ufw.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.inject.Binder
import com.google.inject.Module

public class CoreGuiceModule : Module {
    override fun configure(binder: Binder) {
        binder.bind(CoreComponent::class.java)
        binder.bind(UFWObjectMapper::class.java).toInstance(UFWObjectMapper(CoreComponent.defaultObjectMapper))
    }
}

