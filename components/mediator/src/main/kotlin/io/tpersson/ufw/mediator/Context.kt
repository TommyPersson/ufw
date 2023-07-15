package io.tpersson.ufw.mediator

public interface Context {
    public operator fun <T : Any> set(key: ContextKey<T>, value: T)
    public operator fun <T : Any> get(key: ContextKey<T>): T?
    public fun toMap(): Map<ContextKey<*>, Any>
}

public data class ContextKey<T>(val key: String)