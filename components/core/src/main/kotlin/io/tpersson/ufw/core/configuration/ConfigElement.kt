package io.tpersson.ufw.core.configuration

import kotlin.reflect.KClass

public data class ConfigElement<T : Any?>(val type: KClass<T & Any>, val path: List<String>, val default: T) {
    public companion object {
        public inline fun <reified T : Any?> of(vararg configPath: String, default: T): ConfigElement<T> {
            @Suppress("UNCHECKED_CAST")
            return ConfigElement(T::class as KClass<T & Any>, configPath.toList(), default)
        }
    }
}