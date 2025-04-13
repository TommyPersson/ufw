package io.tpersson.ufw.core.configuration

public class EmptyConfigProvider : ConfigProvider {
    override fun <T> get(element: ConfigElement<T>): T {
        return element.default
    }
}