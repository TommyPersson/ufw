package io.tpersson.ufw.core.configuration

public data class ConfigEntry<T>(
    val element: ConfigElement<T>,
    val value: T,
)

public fun <T> ConfigElement<T>.entry(value: T): ConfigEntry<T> {
    return ConfigEntry(this, value)
}