package io.tpersson.ufw.keyvaluestore

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.InstantSource

// TODO make StorageEngine configurable somehow

public data class KeyValueStoreModuleConfig(
    val instantSource: InstantSource,
    val objectMapper: ObjectMapper,
) {
}