package io.tpersson.ufw.core.configuration

public class FixedConfigProvider(
    private val entries: Set<ConfigEntry<*>>,
) : ConfigProvider {
    override fun <T> get(element: ConfigElement<T>): T {
        @Suppress("UNCHECKED_CAST")
        val entry = entries.firstOrNull { it.element.path == element.path } as? ConfigEntry<T>
        return entry?.value ?: element.default
    }
}