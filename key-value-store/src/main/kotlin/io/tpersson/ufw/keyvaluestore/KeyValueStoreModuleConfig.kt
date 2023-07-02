package io.tpersson.ufw.keyvaluestore

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Clock
import java.time.InstantSource

// TODO make StorageEngine configurable somehow

public data class KeyValueStoreModuleConfig(
    val instantSource: InstantSource,
    val objectMapper: ObjectMapper,
) {
    public companion object {
        public val Default: KeyValueStoreModuleConfig = KeyValueStoreModuleConfig(
            instantSource = Clock.systemUTC(),
            objectMapper = jacksonObjectMapper().findAndRegisterModules(),
        )
    }
}